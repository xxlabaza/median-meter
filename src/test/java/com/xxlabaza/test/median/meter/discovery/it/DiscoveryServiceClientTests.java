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

package com.xxlabaza.test.median.meter.discovery.it;

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.NEW_APPLICATION;
import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.REMOVE_APPLICATION;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.xxlabaza.test.median.meter.MqttClientWrapper;
import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

@DisplayName("Discovery service client integration tests")
class DiscoveryServiceClientTests {

  GenericContainer<?> mqtt = new GenericContainer<>("eclipse-mosquitto:1.5.5")
      .withExposedPorts(1883);

  @BeforeEach
  void beforeEach () {
    mqtt.start();
  }

  @AfterEach
  void afterEach () {
    mqtt.stop();
    MqttClientWrapper.closeAll();
  }

  @Test
  @DisplayName("check if MQTT is running")
  void mqttIsRunning () {
    assertThat(mqtt.isRunning())
        .isTrue();

    assertThat(mqtt.getMappedPort(1883))
        .isNotNull();
  }

  @Test
  @DisplayName("instantiate a discovery service client")
  void newInstance () {
    try (val clusterClient = DiscoveryServiceClient.newInstance(emptyMap())) {
      assertThat(clusterClient).isNotNull();

      assertThat(clusterClient.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient.self());

      assertThat(clusterClient.getApplicationsCount())
          .isEqualTo(1);
    }
  }

  @Test
  @DisplayName("check self Application object")
  void self () {
    try (val clusterClient = DiscoveryServiceClient.newInstance(emptyMap())) {
      val self = clusterClient.self();

      assertThat(self).isNotNull();

      assertThat(self.getId())
          .isNotBlank();
    }
  }

  @Test
  @DisplayName("start and stop discovery client")
  void startAndStop () {
    val properties = createProperties();

    try (val clusterClient = DiscoveryServiceClient.newInstance(properties)) {
      assertThat(clusterClient.isRunning())
          .isFalse();

      clusterClient.start();
      assertThat(clusterClient.isRunning())
          .isTrue();

      clusterClient.stop();
      assertThat(clusterClient.isRunning())
          .isFalse();
    }
  }

  @Test
  @DisplayName("check idempotence of start method")
  void idempotentStart () throws Exception {
    val properties = createProperties();

    try (val clusterClient = DiscoveryServiceClient.newInstance(properties)) {
      assertThat(clusterClient.isRunning())
          .isFalse();

      clusterClient.start();
      assertThat(clusterClient.isRunning())
          .isTrue();

      assertThat(clusterClient.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient.self());

      assertThat(clusterClient.getApplicationsCount())
          .isEqualTo(1);

      clusterClient.start();
      assertThat(clusterClient.isRunning())
          .isTrue();

      SECONDS.sleep(5);

      assertThat(clusterClient.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient.self());

      assertThat(clusterClient.getApplicationsCount())
          .isEqualTo(1);
    }
  }

  @Test
  @DisplayName("check idempotence of stop method")
  void idempotentStop () {
    val properties = createProperties();

    try (val clusterClient = DiscoveryServiceClient.newInstance(properties)) {
      assertThat(clusterClient.isRunning())
          .isFalse();

      clusterClient.start();
      assertThat(clusterClient.isRunning())
          .isTrue();

      clusterClient.stop();
      assertThat(clusterClient.isRunning())
          .isFalse();

      clusterClient.stop();
      assertThat(clusterClient.isRunning())
          .isFalse();
    }
  }

  @Test
  @DisplayName("form a cluster")
  void formCluster () throws Exception {
    val properties = createProperties();

    try (val clusterClient1 = DiscoveryServiceClient.newInstance(properties);
         val clusterClient2 = DiscoveryServiceClient.newInstance(properties)) {

      assertThat(waitUntilClusterFormed(5, SECONDS, clusterClient1, clusterClient2))
          .isTrue();

      assertThat(clusterClient1.getApplicationsCount())
          .isEqualTo(2);
      assertThat(clusterClient1.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self(), clusterClient2.self());

      assertThat(clusterClient2.getApplicationsCount())
          .isEqualTo(2);
      assertThat(clusterClient2.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self(), clusterClient2.self());
    }
  }

  @Test
  @DisplayName("form and break a cluster")
  void breakCluster () throws Exception {
    val properties = createProperties();

    try (val clusterClient1 = DiscoveryServiceClient.newInstance(properties);
         val clusterClient2 = DiscoveryServiceClient.newInstance(properties)) {

      assertThat(waitUntilClusterFormed(5, SECONDS, clusterClient1, clusterClient2))
          .isTrue();

      val applicationRemoved = new CountDownLatch(1);
      clusterClient1.subscribe(REMOVE_APPLICATION, event -> applicationRemoved.countDown());

      clusterClient2.stop();
      assertThat(applicationRemoved.await(5, SECONDS))
          .isTrue();

      assertThat(clusterClient1.getApplicationsCount())
          .isEqualTo(1);
      assertThat(clusterClient1.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self());
    }
  }

  @Test
  @DisplayName("form a cluster and restore it after a break")
  void restoreCluster () throws Exception {
    val properties = createProperties();

    try (val clusterClient1 = DiscoveryServiceClient.newInstance(properties);
         val clusterClient2 = DiscoveryServiceClient.newInstance(properties)) {

      assertThat(waitUntilClusterFormed(5, SECONDS, clusterClient1, clusterClient2))
          .isTrue();

      val applicationRemoved = new CountDownLatch(1);
      clusterClient1.subscribe(REMOVE_APPLICATION, event -> applicationRemoved.countDown());

      clusterClient2.stop();
      assertThat(applicationRemoved.await(5, SECONDS))
          .isTrue();

      assertThat(clusterClient1.getApplicationsCount())
          .isEqualTo(1);
      assertThat(clusterClient1.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self());

      assertThat(waitUntilClusterFormed(5, SECONDS, clusterClient1, clusterClient2))
          .isTrue();

      assertThat(clusterClient1.getApplicationsCount())
          .isEqualTo(2);
      assertThat(clusterClient1.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self(), clusterClient2.self());

      assertThat(clusterClient2.getApplicationsCount())
          .isEqualTo(2);
      assertThat(clusterClient2.getAllApplications())
          .containsExactlyInAnyOrder(clusterClient1.self(), clusterClient2.self());
    }
  }

  private Map<String, Object> createProperties () {
    val mqttProperties = new HashMap<String, Object>();
    mqttProperties.put("host", mqtt.getContainerIpAddress());
    mqttProperties.put("port", mqtt.getMappedPort(1883));

    val heartbeatProperties = new HashMap<String, Object>();
    heartbeatProperties.put("rate-seconds", 1);

    val clusterClientProperties = new HashMap<String, Object>();
    clusterClientProperties.put("heartbeat", heartbeatProperties);

    val result = new HashMap<String, Object>();
    result.put("inbound", mqttProperties);
    result.put("cluster", clusterClientProperties);
    return result;
  }

  @SneakyThrows
  private boolean waitUntilClusterFormed (long timeout, TimeUnit unit, DiscoveryServiceClient... clusterClients) {
    val clusterFormed = new CountDownLatch(clusterClients.length);

    val listenerIdToCluster = Stream.of(clusterClients)
        .collect(toMap(it -> it.subscribe(NEW_APPLICATION, event -> clusterFormed.countDown()),
            it -> it
        ));

    listenerIdToCluster.values().forEach(DiscoveryServiceClient::start);

    val result = clusterFormed.await(timeout, unit);

    listenerIdToCluster.forEach((id, clusterClient) -> clusterClient.unsubscribe(NEW_APPLICATION, id));
    return result;
  }
}
