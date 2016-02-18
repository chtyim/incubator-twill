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

package org.apache.twill.internal.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.yarn.client.ClientRMProxy;
import org.apache.hadoop.yarn.util.ConverterUtils;

/**
 * <p>
 * The service implementation of {@link YarnAppClient} for Apache Hadoop 2.4 and beyond.
 *
 * The {@link VersionDetectYarnAppClientFactory} class will decide to return instance of this class for
 * Apache Hadoop 2.4 and beyond.
 * </p>
 */public class Hadoop24YarnAppClient extends Hadoop21YarnAppClient {

  public Hadoop24YarnAppClient(Configuration configuration) {
    super(configuration);
  }

  @Override
  protected <T extends TokenIdentifier> Token<T> convertRMToken(Configuration config,
                                                                org.apache.hadoop.yarn.api.records.Token rmToken) {
    return ConverterUtils.convertFromYarn(rmToken, ClientRMProxy.getRMDelegationTokenService(config));
  }
}
