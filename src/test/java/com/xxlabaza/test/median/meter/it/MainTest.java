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

package com.xxlabaza.test.median.meter.it;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import com.xxlabaza.test.median.meter.Main;
import com.xxlabaza.test.median.meter.MqttClientWrapper;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@DisplayName("Main integration tests")
class MainTest {

  GenericContainer<?> mqtt = new GenericContainer<>("eclipse-mosquitto:1.5.5")
      .withExposedPorts(1883);

  ExecutorService appRunner = Executors.newSingleThreadExecutor();

  @BeforeEach
  void beforeEach () {
    mqtt.start();
  }

  @AfterEach
  void afterEach () throws InterruptedException {
    mqtt.stop();

    appRunner.shutdown();
    log.info("App runner was terminated successfully - {}",
             appRunner.awaitTermination(5, SECONDS));

    MqttClientWrapper.closeAll();
  }

  @Test
  @DisplayName("check if MQTTs are running")
  void mqttsAreRunning () {

    assertThat(mqtt.isRunning())
        .isTrue();

    assertThat(mqtt.getMappedPort(1883))
        .isNotNull();
  }

  @Test
  @DisplayName("main complex test")
  void main () throws Exception {
    startApplication();

    SECONDS.sleep(5);

    val medians = asList(
        3.D,
        9.D,
        12.D,
        13.5D,
        12.D
    );
    val mediansIterator = medians.iterator();
    val countDownLatch = new CountDownLatch(medians.size());

    val client = MqttClientWrapper.of("tcp://" + mqtt.getContainerIpAddress() + ':' + mqtt.getMappedPort(1883));
    client.connect();
    client.subscribe("median-t/1", (topic, message) -> {
      log.info("A new message in topic 'median-t/1'");
      val median = ByteBuffer.wrap(message.getPayload())
          .getDouble();

      try {
        assertThat(median)
            .isEqualTo(mediansIterator.next());
      } catch (AssertionError ex) {
        log.error("Assertion error", ex);
        throw ex;
      }

      countDownLatch.countDown();
    });

    Stream.of(
        3.D,
        15.D,
        12.D,
        25.7D,
        10.D
    ).forEach(it -> client.send("t/1", it));

    assertThat(countDownLatch.await(15, SECONDS))
        .as("I have only %d", countDownLatch.getCount())
        .isTrue();
  }

  private void startApplication () throws InterruptedException {
    val properties = createProperties();
    appRunner.execute(() -> {
      try {
        Main.startApplication(properties);
      } catch (Throwable ex) {
        log.error("Error during application run", ex);
        throw ex;
      }
    });
  }

  private Map<String, Object> createProperties () {

    val inboundOutboundMqttProperties = new HashMap<String, Object>();
    inboundOutboundMqttProperties.put("url", new StringBuilder()
        .append("tcp://")
        .append(mqtt.getContainerIpAddress())
        .append(':')
        .append(mqtt.getMappedPort(1883))
        .toString());

    // val selfProperties = new HashMap<String, Object>();
    // selfProperties.put("port", 5546);

    // val heartbeatProperties = new HashMap<String, Object>();
    // heartbeatProperties.put("rate-seconds", 5);

    // val clusterClientProperties = new HashMap<String, Object>();
    // clusterClientProperties.put("self", selfProperties);
    // clusterClientProperties.put("heartbeat", heartbeatProperties);

    val result = new HashMap<String, Object>();
    result.put("inbound", inboundOutboundMqttProperties);
    result.put("outbound", inboundOutboundMqttProperties);
    result.put("temperatureTimeWindowInSec", 3);
    // result.put("cluster", clusterClientProperties);
    return result;
  }
}
