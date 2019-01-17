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

package com.xxlabaza.test.median.meter.discovery;

import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MqttHeartbeatSender implements AutoCloseable {

  AtomicBoolean running;

  byte[] selfInfoBytes;

  MqttClientWrapper mqttClient;

  String heartbeatTopic;

  int heartbeatRateSeconds;

  ScheduledExecutorService executor;

  @NonFinal
  ScheduledFuture<?> taskFuture;

  @Builder
  MqttHeartbeatSender (@NonNull Application self,
                       @NonNull MqttClientWrapper mqttClient,
                       @NonNull String heartbeatTopicPrefix,
                       int heartbeatRateSeconds
  ) {
    this.selfInfoBytes = self.toBytes();
    this.mqttClient = mqttClient;
    this.heartbeatTopic = heartbeatTopicPrefix + self.getId();
    this.heartbeatRateSeconds = heartbeatRateSeconds;

    running = new AtomicBoolean(false);
    executor = Executors.newScheduledThreadPool(1);
  }

  @Override
  @SneakyThrows
  public void close () {
    stop();
    executor.shutdown();

    val terminated = executor.awaitTermination(1, SECONDS);
    log.debug("Successfully closed MQTT client's (id - {}) executor - {}",
              mqttClient.getId(), terminated);
  }

  void start () {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    taskFuture = executor.scheduleAtFixedRate(new Task(), 0, heartbeatRateSeconds, SECONDS);
  }

  void stop () {
    if (!running.compareAndSet(true, false)) {
      log.warn("Heartbeat sender already stopped");
      return;
    }
    log.info("Heartbeat sender stopping");

    taskFuture.cancel(true);

    log.info("Heartbeat sender stopped");
  }

  private class Task implements Runnable {

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public void run () {
      try {
        mqttClient.send(heartbeatTopic, selfInfoBytes);
        log.debug("Sent heartbeat message to '{}' topic", heartbeatTopic);
      } catch (Throwable ex) {
        log.error("Error, during heartbeat sending from '{}'",
                  mqttClient.getId(), ex);
      }
    }
  }
}
