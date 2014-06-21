/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.twill.internal;

/**
 * This class contains collection of common constants used in Twill.
 */
public final class Constants {

  public static final String LOG_TOPIC = "log";

  /** Maximum number of seconds for AM to start. */
  public static final int APPLICATION_MAX_START_SECONDS = 60;
  /** Maximum number of seconds for AM to stop. */
  public static final int APPLICATION_MAX_STOP_SECONDS = 60;

  public static final long PROVISION_TIMEOUT = 30000;

  /**
   * Milliseconds AM should wait for RM to allocate a constrained provision request.
   * On timeout, AM relaxes the request constraints.
   */
  public static final int CONSTRAINED_PROVISION_REQUEST_TIMEOUT = 5000;

  /** Memory size of AM. */
  public static final int APP_MASTER_MEMORY_MB = 512;

  public static final int APP_MASTER_RESERVED_MEMORY_MB = 150;

  public static final String STDOUT = "stdout";
  public static final String STDERR = "stderr";

  /**
   * Constants for names of internal files that are shared between client, AM and containers.
   */
  public static final class Files {

    public static final String LAUNCHER_JAR = "launcher.jar";
    public static final String APP_MASTER_JAR = "appMaster.jar";
    public static final String CONTAINER_JAR = "container.jar";
    public static final String LOCALIZE_FILES = "localizeFiles.json";
    public static final String TWILL_SPEC = "twillSpec.json";
    public static final String ARGUMENTS = "arguments.json";
    public static final String LOGBACK_TEMPLATE = "logback-template.xml";
    public static final String JVM_OPTIONS = "jvm.opts";
    public static final String CREDENTIALS = "credentials.store";

    private Files() {
    }
  }

  /**
   * Constants related to {@link org.apache.twill.internal.BundleCache}.
   */
  public static final class BundleCache {

    /** Key in Configuration for enable usage of bundle cache. */
    public static final String ENABLE = "twill.bundle.cache.enable";

    /** Key in Configuration for how often to perform cache cleanup. */
    public static final String CLEANUP_SECONDS = "twill.bundle.cache.cleanup.seconds";

    /** Key in Configuration for maximum cached entries to maintain. */
    public static final String MAX_ENTRIES = "twill.bundle.cache.cleanup.max.entries";

    // For bundle jar caching
    public static final String CACHE_DIR = ".cached";

    /** By default how often does the bundle cache cleanup run. */
    public static final long DEFAULT_CLEANUP_SECONDS = 1800;

    /** By default the max. number of entries in the cache. */
    public static final int DEFAULT_MAX_ENTRIES = 1000;
  }

  private Constants() {
  }
}
