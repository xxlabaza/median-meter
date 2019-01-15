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

package com.xxlabaza.test.median.meter.hazelcast.it;

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.NEW_APPLICATION;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;
import com.xxlabaza.test.median.meter.hazelcast.CustomHazelcast;

import com.hazelcast.core.HazelcastInstance;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

@DisplayName("Hazelcast integration tests")
class CustomHazelcastTests {

  GenericContainer<?> mqtt = new GenericContainer<>("eclipse-mosquitto:1.5.5")
      .withExposedPorts(1883);

  @BeforeEach
  void beforeEach () {
    mqtt.start();
  }

  @AfterEach
  void afterEach () {
    mqtt.stop();
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
  @DisplayName("form a cluster")
  void formCluster () throws Exception {
    HazelcastInstance hz1 = null;
    HazelcastInstance hz2 = null;

    try (val clusterClient1 = DiscoveryServiceClient.newInstance(createProperties(15546));
         val clusterClient2 = DiscoveryServiceClient.newInstance(createProperties(15547))) {

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

      hz1 = CustomHazelcast.builder().discoveryClient(clusterClient1).ignite();
      hz2 = CustomHazelcast.builder().discoveryClient(clusterClient2).ignite();

      SECONDS.sleep(3);

      assertThat(hz1.getCluster().getMembers())
            .hasSize(2);
      assertThat(hz2.getCluster().getMembers())
            .hasSize(2);

    } finally {
      ofNullable(hz1).ifPresent(HazelcastInstance::shutdown);
      ofNullable(hz2).ifPresent(HazelcastInstance::shutdown);
    }
  }

  @Test
  @DisplayName("check leader election algorithm")
  void leaderElection () throws InterruptedException {
    HazelcastInstance hz1 = null;
    HazelcastInstance hz2 = null;

    try (val clusterClient1 = DiscoveryServiceClient.newInstance(createProperties(15541));
         val clusterClient2 = DiscoveryServiceClient.newInstance(createProperties(15542))) {

      assertThat(waitUntilClusterFormed(5, SECONDS, clusterClient1, clusterClient2))
          .isTrue();

      val countDownLatch = new CountDownLatch(1);
      hz1 = CustomHazelcast.builder()
          .discoveryClient(clusterClient1)
          .onBecomeMasterAction((hz, event) -> countDownLatch.countDown())
          .ignite();

      hz2 = CustomHazelcast.builder()
          .discoveryClient(clusterClient2)
          .ignite();

      assertThat(countDownLatch.await(15, SECONDS))
          .isTrue();

      assertThat(hz1.getCluster().getMembers())
          .hasSize(2);
      assertThat(hz1.getCluster().getMembers().iterator().next())
          .isEqualTo(hz1.getCluster().getLocalMember());

      assertThat(hz2.getCluster().getMembers())
          .hasSize(2);
      assertThat(hz2.getCluster().getMembers().iterator().next())
          .isEqualTo(hz1.getCluster().getLocalMember());

    } finally {
      ofNullable(hz1).ifPresent(HazelcastInstance::shutdown);
      ofNullable(hz2).ifPresent(HazelcastInstance::shutdown);
    }
  }

  private Map<String, Object> createProperties (int port) {
    val mqttProperties = new HashMap<String, Object>();
    mqttProperties.put("host", mqtt.getContainerIpAddress());
    mqttProperties.put("port", mqtt.getMappedPort(1883));

    val selfProperties = new HashMap<String, Object>();
    selfProperties.put("port", port);

    val heartbeatProperties = new HashMap<String, Object>();
    heartbeatProperties.put("rate-seconds", 1);

    val clusterClientProperties = new HashMap<String, Object>();
    clusterClientProperties.put("self", selfProperties);
    clusterClientProperties.put("heartbeat", heartbeatProperties);

    val result = new HashMap<String, Object>();
    result.put("inbound", mqttProperties);
    result.put("cluster", clusterClientProperties);
    return result;
  }

  @SneakyThrows
  private boolean waitUntilClusterFormed (long timeout, TimeUnit unit, DiscoveryServiceClient... clusterClients) {
    val clusterFormed = new CountDownLatch(clusterClients.length);

    Map<String, DiscoveryServiceClient> listenerIdToCluster = Stream.of(clusterClients)
        .collect(toMap(
            it -> it.subscribe(NEW_APPLICATION, event -> clusterFormed.countDown()),
            it -> it
        ));

    listenerIdToCluster.values().forEach(DiscoveryServiceClient::start);

    val result = clusterFormed.await(timeout, unit);

    listenerIdToCluster.forEach((id, clusterClient) -> clusterClient.unsubscribe(NEW_APPLICATION, id));
    return result;
  }
}
