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
package org.apache.twill.internal.appmaster;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.RunId;
import org.apache.twill.internal.Constants;
import org.apache.twill.internal.EnvKeys;
import org.apache.twill.internal.RunIds;
import org.apache.twill.internal.ServiceMain;
import org.apache.twill.internal.kafka.EmbeddedKafkaServer;
import org.apache.twill.internal.logging.Loggings;
import org.apache.twill.internal.utils.Networks;
import org.apache.twill.internal.yarn.VersionDetectYarnAMClientFactory;
import org.apache.twill.internal.yarn.YarnAMClient;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.twill.zookeeper.ZKOperations;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Main class for launching {@link ApplicationMasterService}.
 */
public final class ApplicationMasterMain extends ServiceMain {

  private final String kafkaZKConnect;

  private ApplicationMasterMain(String kafkaZKConnect) {
    this.kafkaZKConnect = kafkaZKConnect;
  }

  /**
   * Starts the application master.
   */
  public static void main(String[] args) throws Exception {
    String zkConnect = System.getenv(EnvKeys.TWILL_ZK_CONNECT);
    File twillSpec = new File(Constants.Files.TWILL_SPEC);
    RunId runId = RunIds.fromString(System.getenv(EnvKeys.TWILL_RUN_ID));

    ZKClientService zkClientService = createZKClient(zkConnect);
    Configuration conf = new YarnConfiguration(new HdfsConfiguration(new Configuration()));
    setRMSchedulerAddress(conf);

    final YarnAMClient amClient = new VersionDetectYarnAMClientFactory(conf).create();
    ApplicationMasterService service = new ApplicationMasterService(runId, zkClientService,
                                                                    twillSpec, amClient, createAppLocation(conf));
    TrackerService trackerService = new TrackerService(service);

    new ApplicationMasterMain(String.format("%s/%s/kafka", zkConnect, runId.getId()))
      .doMain(
        service,
        new YarnAMClientService(amClient, trackerService),
        zkClientService,
        new TwillZKPathService(zkClientService, runId),
        new ApplicationKafkaService(zkClientService, runId)
      );
  }

  /**
   * Optionally sets the RM scheduler address based on the environment variable if it is not set in the cluster config.
   */
  private static void setRMSchedulerAddress(Configuration conf) {
    String schedulerAddress = System.getenv(EnvKeys.YARN_RM_SCHEDULER_ADDRESS);
    if (schedulerAddress == null) {
      return;
    }

    // If the RM scheduler address is not in the config or it's from yarn-default.xml,
    // replace it with the one from the env, which is the same as the one client connected to.
    String[] sources = conf.getPropertySources(YarnConfiguration.RM_SCHEDULER_ADDRESS);
    if (sources == null || sources.length == 0 || "yarn-default.xml".equals(sources[sources.length - 1])) {
      conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, schedulerAddress);
    }
  }

  @Override
  protected String getHostname() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  @Override
  protected String getKafkaZKConnect() {
    return kafkaZKConnect;
  }

  @Override
  protected String getRunnableName() {
    return System.getenv(EnvKeys.TWILL_RUNNABLE_NAME);
  }


  /**
   * A service wrapper for starting/stopping {@link EmbeddedKafkaServer} and make sure the ZK path for
   * Kafka exists before starting the Kafka server.
   */
  private static final class ApplicationKafkaService extends AbstractIdleService {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationKafkaService.class);

    private final ZKClient zkClient;
    private final String kafkaZKPath;
    private final EmbeddedKafkaServer kafkaServer;

    private ApplicationKafkaService(ZKClient zkClient, RunId runId) {
      this.zkClient = zkClient;
      this.kafkaZKPath = "/" + runId.getId() + "/kafka";
      this.kafkaServer = new EmbeddedKafkaServer(generateKafkaConfig(zkClient.getConnectString() + kafkaZKPath));
    }

    @Override
    protected void startUp() throws Exception {
      ZKOperations.ignoreError(
        zkClient.create(kafkaZKPath, null, CreateMode.PERSISTENT),
        KeeperException.NodeExistsException.class, kafkaZKPath).get();
      kafkaServer.startAndWait();
    }

    @Override
    protected void shutDown() throws Exception {
      // Flush all logs before shutting down Kafka server
      Loggings.forceFlush();
      // Delay for 2 seconds to give clients chance to poll the last batch of log messages.
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
        // Ignore
        LOG.info("Kafka shutdown delay interrupted", e);
      } finally {
        kafkaServer.stopAndWait();
      }
    }

    private Properties generateKafkaConfig(String kafkaZKConnect) {
      int port = Networks.getRandomPort();
      Preconditions.checkState(port > 0, "Failed to get random port.");

      Properties prop = new Properties();
      prop.setProperty("log.dir", new File("kafka-logs").getAbsolutePath());
      prop.setProperty("port", Integer.toString(port));
      prop.setProperty("broker.id", "1");
      prop.setProperty("socket.send.buffer.bytes", "1048576");
      prop.setProperty("socket.receive.buffer.bytes", "1048576");
      prop.setProperty("socket.request.max.bytes", "104857600");
      prop.setProperty("num.partitions", "1");
      prop.setProperty("log.retention.hours", "24");
      prop.setProperty("log.flush.interval.messages", "10000");
      prop.setProperty("log.flush.interval.ms", "1000");
      prop.setProperty("log.segment.bytes", "536870912");
      prop.setProperty("zookeeper.connect", kafkaZKConnect);
      prop.setProperty("zookeeper.connection.timeout.ms", "1000000");
      prop.setProperty("default.replication.factor", "1");
      return prop;
    }
  }


  /**
   * A Service wrapper that starts {@link TrackerService} and {@link YarnAMClient}. It is needed because
   * the tracker host and url needs to be provided to {@link YarnAMClient} before it starts {@link YarnAMClient}.
   */
  private static final class YarnAMClientService extends AbstractIdleService {

    private final YarnAMClient yarnAMClient;
    private final TrackerService trackerService;

    private YarnAMClientService(YarnAMClient yarnAMClient, TrackerService trackerService) {
      this.yarnAMClient = yarnAMClient;
      this.trackerService = trackerService;
    }

    @Override
    protected void startUp() throws Exception {
      trackerService.setHost(yarnAMClient.getHost());
      trackerService.startAndWait();

      yarnAMClient.setTracker(trackerService.getBindAddress(), trackerService.getUrl());
      try {
        yarnAMClient.startAndWait();
      } catch (Exception e) {
        trackerService.stopAndWait();
        throw e;
      }
    }

    @Override
    protected void shutDown() throws Exception {
      try {
        yarnAMClient.stopAndWait();
      } finally {
        trackerService.stopAndWait();
      }
    }
  }
}
