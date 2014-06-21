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

import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.annotation.Nullable;

/**
 * Utility class for interacting with {@link Location}.
 */
public final class Locations {

  /**
   * Creates a new {@link InputSupplier} that can provides {@link InputStream} from the given location.
   *
   * @param location Location for the input stream.
   * @return A {@link InputSupplier}.
   */
  public static InputSupplier<InputStream> newInputSupplier(final Location location) {
    return new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        return location.getInputStream();
      }
    };
  }

  /**
   * Creates a new {@link OutputSupplier} that can provides {@link OutputStream} for the given location.
   *
   * @param location Location for the output.
   * @return A {@link com.google.common.io.OutputSupplier}.
   */
  public static OutputSupplier<OutputStream> newOutputSupplier(final Location location) {
    return new OutputSupplier<OutputStream>() {
      @Override
      public OutputStream getOutput() throws IOException {
        return location.getOutputStream();
      }
    };
  }

  /**
   * Creates a {@link Location} instance which represents the parent of the given location.
   *
   * @param location location to extra parent from.
   * @return an instance representing the parent location or {@code null} if there is no parent.
   */
  @Nullable
  public static Location getParent(Location location) {
    URI source = location.toURI();

    // If it is root, return null
    if ("/".equals(source.getPath())) {
      return null;
    }

    URI resolvedParent = URI.create(source.toString() + "/..").normalize();
    return location.getLocationFactory().create(resolvedParent);
  }

  private Locations() {
  }
}
