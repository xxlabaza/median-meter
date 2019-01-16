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

package com.xxlabaza.test.median.meter.function;

import static com.xxlabaza.test.median.meter.function.UserContextFactory.INBOUND_MQTT_CLIENT_SUPPLIER;
import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MembershipEvent;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Action handler on Hazelcast become a cluster regular member.
 */
@Slf4j
@Value
public class BecomeLeaderMemberAction implements BiConsumer<HazelcastInstance, MembershipEvent> {

  static final String SENSORS_LISTENING_TOPIC_FILTER = "t/+";

  static final String INBOUND_MQTT_CLIENT = "inbound-mqtt-client";

  static Optional<MqttClientWrapper> getCreatedMqttClient (@NonNull Map<String, Object> context) {
    return ofNullable(context.get(INBOUND_MQTT_CLIENT))
        .filter(it -> it instanceof MqttClientWrapper)
        .map(it -> (MqttClientWrapper) it);
  }

  String mapName;

  @Override
  @SuppressWarnings("unchecked")
  public void accept (@NonNull HazelcastInstance hazelcastInstance, @NonNull MembershipEvent event) {
    log.info("Node '{}' become a leader node", hazelcastInstance.getName());

    IMap<Integer, Queue<Measure>> map = hazelcastInstance.getMap(mapName);
    if (map == null) {
      throw new IllegalStateException("Hazelcast map '" + mapName + "' doesn't exist");
    }

    val userContext = hazelcastInstance.getUserContext();
    val mqttClient = getCreatedMqttClient(userContext)
        .orElseGet(() -> ofNullable(userContext.get(INBOUND_MQTT_CLIENT_SUPPLIER))
            .filter(it -> it instanceof Supplier)
            .map(it -> (Supplier<MqttClientWrapper>) it)
            .map(Supplier::get)
            .orElseThrow(RuntimeException::new));

    mqttClient.connect();
    userContext.put(INBOUND_MQTT_CLIENT, mqttClient);

    mqttClient.subscribe(SENSORS_LISTENING_TOPIC_FILTER, new MqttSensorsListener(map));
  }
}
