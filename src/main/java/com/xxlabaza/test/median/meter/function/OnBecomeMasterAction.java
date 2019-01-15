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

import java.util.Objects;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MembershipEvent;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

/**
 * Action handler on Hazelcast become a cluster master.
 */
@Value
public class OnBecomeMasterAction implements BiConsumer<HazelcastInstance, MembershipEvent> {

  private static final String SENSORS_LISTENING_TOPIC_FILTER = "t/+";

  String mapName;

  @Override
  @SuppressWarnings("unchecked")
  public void accept (@NonNull HazelcastInstance hazelcastInstance, @NonNull MembershipEvent event) {
    IMap<Integer, Queue<Measure>> map = hazelcastInstance.getMap(mapName);
    if (map == null) {
      throw new IllegalStateException("Hazelcast map '" + mapName + "' doesn't exist");
    }

    val mqttClient = ofNullable(hazelcastInstance.getUserContext())
        .filter(Objects::nonNull)
        .map(it -> it.get(INBOUND_MQTT_CLIENT_SUPPLIER))
        .filter(it -> it instanceof Supplier)
        .map(it -> (Supplier<MqttClientWrapper>) it)
        .map(Supplier::get)
        .orElseThrow(RuntimeException::new);

    mqttClient.listen(SENSORS_LISTENING_TOPIC_FILTER, new MqttSensorsListener(map));
  }
}
