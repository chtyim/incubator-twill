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

import org.apache.twill.filesystem.Location;

import java.io.IOException;
import java.net.URI;

/**
 *
 */
public interface Bundler {


  void createBundle(Location target, Iterable<Class<?>> classes) throws IOException;

  /**
   * Same as calling {@link #createBundle(org.apache.twill.filesystem.Location, Iterable)}.
   */
  void createBundle(Location target, Class<?> clz, Class<?>... classes) throws IOException;

  /**
   * Creates a jar file which includes all the given classes and all the classes that they depended on.
   * The jar will also include all classes and resources under the packages as given as include packages
   * in the constructor.
   *
   * @param target Where to save the target jar file.
   * @param resources Extra resources to put into the jar file. If resource is a jar file, it'll be put under
   *                  lib/ entry, otherwise under the resources/ entry.
   * @param classes Set of classes to start the dependency traversal.
   * @throws java.io.IOException
   */
  void createBundle(Location target, Iterable<Class<?>> classes, Iterable<URI> resources) throws IOException;
}
