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
package org.apache.twill.filesystem;

/**
 * Contains status of a {@link Location}.
 */
public class LocationStatus {

  private final Location location;
  private final boolean directory;
  private final long lastModified;
  private final long length;

  /**
   * Constructs a location status.
   */
  public LocationStatus(Location location, boolean directory, long lastModified, long length) {
    this.location = location;
    this.directory = directory;
    this.lastModified = lastModified;
    this.length = length;
  }

  /**
   * Returns the {@link Location} that this status is for.
   */
  public Location getLocation() {
    return location;
  }

  /**
   * Returns {@code true} if the location is a directory, {@code false} otherwise.
   */
  public boolean isDirectory() {
    return directory;
  }

  /**
   * Returns the last modified timestamp in milliseconds for the location.
   */
  public long getLastModified() {
    return lastModified;
  }

  /**
   * Returns the length of the file represented by the location.
   */
  public long getLength() {
    return length;
  }
}
