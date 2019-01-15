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

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.UNDEFINED;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MqttDiscoveryServiceClient implements DiscoveryServiceClient {

  AtomicBoolean running = new AtomicBoolean(false);

  Application self;

  DiscoveryEventProcessor eventProcessor;

  MqttClientWrapper mqttClient;

  MqttHeartbeatSender mqttHeartbeatSender;

  MqttHeartbeatReceiver mqttHeartbeatReceiver;

  ApplicationsRegistry applicationsRegistry;

  MqttDiscoveryServiceClient (@NonNull MqttDiscoveryServiceClientProperties properties) {
    self = Application.builder()
        .id(properties.getSelf().getId())
        .address(properties.getSelf().getAddress())
        .port(properties.getSelf().getPort())
        .createdTimestamp(System.currentTimeMillis())
        .build();

    eventProcessor = new DiscoveryEventProcessor();

    mqttClient = MqttClientWrapper.builder()
        .uri(properties.getMqtt().getUri())
        .username(properties.getMqtt().getUsername())
        .password(properties.getMqtt().getPassword())
        .build();

    mqttHeartbeatSender = MqttHeartbeatSender.builder()
        .self(self)
        .mqttClient(mqttClient)
        .heartbeatTopicPrefix(properties.getHeartbeat().getTopicPrefix())
        .heartbeatRateSeconds(properties.getHeartbeat().getRateSeconds())
        .build();

    mqttHeartbeatReceiver = MqttHeartbeatReceiver.builder()
        .self(self)
        .mqttClient(mqttClient)
        .eventProcessor(eventProcessor)
        .heartbeatTopicPrefix(properties.getHeartbeat().getTopicPrefix())
        .build();

    applicationsRegistry = ApplicationsRegistry.builder()
        .eventProcessor(eventProcessor)
        .checkExpiredPeriodSeconds(properties.getHeartbeat().getRateSeconds())
        .build();
  }

  @Override
  public List<Application> getAllApplications () {
    return Stream.concat(applicationsRegistry.applications().stream(), Stream.of(self))
        .sorted(comparing(Application::getCreatedTimestamp))
        .collect(toList());
  }

  @Override
  public int getApplicationsCount () {
    return applicationsRegistry.size();
  }

  @Override
  public Application self () {
    return self;
  }

  @Override
  public String subscribe (@NonNull DiscoveryEvent.Type type, @NonNull Consumer<DiscoveryEvent> consumer) {
    return eventProcessor.subscribe(type, consumer);
  }

  @Override
  public Map<DiscoveryEvent.Type, String> subscribe (@NonNull Consumer<DiscoveryEvent> consumer) {
    return Stream.of(DiscoveryEvent.Type.values())
        .filter(it -> it != UNDEFINED)
        .collect(toMap(type -> type, type -> subscribe(type, consumer)));
  }

  @Override
  public void unsubscribe (@NonNull DiscoveryEvent.Type type, @NonNull String id) {
    eventProcessor.unsubscribe(type, id);
  }

  @Override
  public DiscoveryServiceClient start () {
    if (!running.compareAndSet(false, true)) {
      return this;
    }

    mqttClient.connect();
    applicationsRegistry.start();
    mqttHeartbeatSender.start();
    mqttHeartbeatReceiver.start();

    return this;
  }

  @Override
  public boolean isRunning () {
    return running.get();
  }

  @Override
  public void stop () {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    mqttHeartbeatReceiver.stop();
    mqttHeartbeatSender.stop();
    applicationsRegistry.stop();
    mqttClient.disconnect();
  }

  @Override
  public void close () {
    stop();

    mqttHeartbeatSender.close();
    applicationsRegistry.close();
    mqttClient.close();
    eventProcessor.close();
  }
}
