/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.twill.internal;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.twill.api.RunId;
import org.apache.twill.filesystem.Location;
import org.apache.twill.filesystem.LocationStatus;
import org.apache.twill.filesystem.Locations;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Cache for generated bundle jars.
 */
public final class BundleCache extends AbstractIdleService implements Bundler {

  private static final Logger LOG = LoggerFactory.getLogger(BundleCache.class);
  private static final String ACTIVE_FILE_NAME = "active";

  private static final Function<Class<?>, InputSupplier<InputStream>> CLASS_INPUT =
    new Function<Class<?>, InputSupplier<InputStream>>() {
      @Override
      public InputSupplier<InputStream> apply(final Class<?> clz) {
        return new InputSupplier<InputStream>() {
          @Override
          public InputStream getInput() throws IOException {
            return clz.getClassLoader().getResourceAsStream(Type.getInternalName(clz) + ".class");
          }
        };
      }
    };

  private static final Function<URI, InputSupplier<InputStream>> URI_INPUT =
    new Function<URI, InputSupplier<InputStream>>() {
      @Override
      public InputSupplier<InputStream> apply(final URI uri) {
        return new InputSupplier<InputStream>() {
          @Override
          public InputStream getInput() throws IOException {
            return uri.toURL().openStream();
          }
        };
      }
    };

  private final Location cacheDir;
  private final Bundler bundler;
  private final long cleanupPeriodMs;
  private final int maxEntries;
  private final RunId sessionId;
  private Timer timer;

  public BundleCache(Location cacheDir, Bundler bundler, long cleanupPeriodMs, int maxEntries) {
    this.cacheDir = cacheDir;
    this.bundler = bundler;
    this.cleanupPeriodMs = cleanupPeriodMs;
    this.maxEntries = maxEntries;
    this.sessionId = RunIds.generate();
  }

  @Override
  public void createBundle(Location target, Iterable<Class<?>> classes) throws IOException {
    createBundle(target, classes, ImmutableList.<URI>of());
  }

  @Override
  public void createBundle(Location target, Class<?> clz, Class<?>... classes) throws IOException {
    createBundle(target, ImmutableSet.<Class<?>>builder().add(clz).add(classes).build());
  }

  @Override
  public void createBundle(Location target, Iterable<Class<?>> classes, Iterable<URI> resources) throws IOException {
    // Compute hash of all classes
    Iterable<InputSupplier<InputStream>> inputSuppliers = Iterables.concat(Iterables.transform(classes, CLASS_INPUT),
                                                                           Iterables.transform(resources, URI_INPUT));
    String hash = ByteStreams.hash(ByteStreams.join(inputSuppliers), Hashing.sha256()).toString();

    // See if there is a cached copy
    Location location = cacheDir.append(sessionId.getId()).append(hash);
    if (!location.exists()) {
      // No cache, call the application bundler.
      Location tmpLocation = location.getTempFile(".tmp");
      bundler.createBundle(tmpLocation, classes);

      tmpLocation.renameTo(location);
    } else {
      LOG.debug("Cached bundle found for {}.", target);
      // Touch the file with current time. It's for cache cleanup process to remove old cache.
      location.setLastModified(System.currentTimeMillis());
    }

    // Copy from the cached location to the target
    ByteStreams.copy(Locations.newInputSupplier(location), Locations.newOutputSupplier(target));
  }

  @Override
  protected void startUp() throws Exception {
    // Create the session and touch the active file inside the session directory.
    // It is needed for cleanup check.
    final Location activeFile = cacheDir.append(sessionId.getId()).append(ACTIVE_FILE_NAME);
    activeFile.createNew();

    // Schedule the cleanup task
    timer = new Timer("bundle-cache-cleanup", true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          long now = System.currentTimeMillis();

          // Touch the active file
          activeFile.setLastModified(now);

          // Remove all directories that don't have active file or it has not been updated 4 * the cleanup cycle
          List<Location> locations = cacheDir.list();
          long expireTime = now - cleanupPeriodMs * 4;
          for (Location location : locations) {
            // No need to handle current session
            if (location.getName().equals(sessionId.getId())) {
              continue;
            }

            Location active = location.append(ACTIVE_FILE_NAME);
            try {
              if (!active.exists() || active.lastModified() <= expireTime) {
                location.delete(true);
              }
            } catch (IOException e) {
              LOG.error("Failed to cleanup location {}", location, e);
            }
          }
        } catch (Throwable t) {
          LOG.error("Failed to perform bundle cache cleanup.", t);
        }

        try {
          // Handle current session cleanup
          List<LocationStatus> statusList = cacheDir.append(sessionId.getId()).listStatus();
          if (statusList.size() <= maxEntries) {
            return;
          }

          // Sort the cached entries based on time.
          statusList = Lists.newArrayList(statusList);
          Collections.sort(statusList, new Comparator<LocationStatus>() {
            @Override
            public int compare(LocationStatus o1, LocationStatus o2) {
              return Longs.compare(o1.getLastModified(), o2.getLastModified());
            }
          });

          // Remove N cached entries with smallest last modified time.
          int toIndex = statusList.size() - maxEntries;
          for (LocationStatus status : statusList.subList(0, toIndex)) {
            status.getLocation().delete();
          }

        } catch (Throwable t) {
          LOG.error("Failed to perform bundle cache cleanup.", t);
        }
      }
    }, 0L, cleanupPeriodMs);
  }

  @Override
  protected void shutDown() throws Exception {
    timer.cancel();
    Location activeFile = cacheDir.append(sessionId.getId()).append(ACTIVE_FILE_NAME);
    activeFile.delete();
  }
}
