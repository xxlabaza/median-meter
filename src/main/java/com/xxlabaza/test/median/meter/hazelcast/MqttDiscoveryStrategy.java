/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xxlabaza.test.median.meter.hazelcast;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;

import com.xxlabaza.test.median.meter.discovery.Application;
import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Custom MQTT-based discovery strategy.
 */
@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MqttDiscoveryStrategy extends AbstractDiscoveryStrategy {

  DiscoveryServiceClient discoveryClient;

  @Builder
  MqttDiscoveryStrategy (@NonNull DiscoveryServiceClient discoveryClient,
                         @NonNull ILogger logger,
                         @NonNull Map<String, Comparable> properties
  ) {
    super(logger, properties);
    this.discoveryClient = discoveryClient;
  }

  @Override
  public void start () {
    log.info("Starting discovery strategy...");
    while (!discoveryClient.isRunning()) {
      try {
        log.info("Waiting for starting cluster client");
        SECONDS.sleep(2);
      } catch (InterruptedException almostIgnore) {
        log.error("Error, during discovery strategy starting", almostIgnore);
        Thread.currentThread().interrupt();
      }
    }
    log.info("Discovery strategy started");
  }

  @Override
  public Iterable<DiscoveryNode> discoverNodes () {
    val self = discoveryClient.self();
    return discoveryClient.getAllApplications()
        .stream()
        .filter(it -> !it.equals(self))
        .map(this::toDiscoveryNode)
        .collect(toList());
  }

  private DiscoveryNode toDiscoveryNode (@NonNull Application application) {
    val address = new Address(application.getAddress(), application.getPort());
    return new SimpleDiscoveryNode(address);
  }
}
