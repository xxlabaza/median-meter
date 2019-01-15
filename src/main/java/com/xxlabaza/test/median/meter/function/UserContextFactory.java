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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.xxlabaza.test.median.meter.MqttClientWrapper;
import com.xxlabaza.test.median.meter.function.MqttMedianMeterFunctionProperties.MqttConnection;

import lombok.NonNull;
import lombok.val;

/**
 * Utility class for working with user's context.
 */
public final class UserContextFactory {

  static final String INBOUND_MQTT_CLIENT_SUPPLIER = "inbound-mqtt-client-supplier";

  static final String EXPIRE_TIME_MILLISECONDS = "expire-time-milliseconds";

  static final String OUTBOUND_MQTT_TOPIC_PREFIX = "outbound-mqtt-topic-prefix";

  static final String OUTBOUND_MQTT_CLIENT = "outbound-mqtt-client";

  /**
   * Creates a new user contex object.
   *
   * @param map properties for creating context.
   *
   * @return a new user context.
   */
  public static ConcurrentHashMap<String, Object> create (@NonNull Map<String, Object> map) {
    val properties = MqttMedianMeterFunctionProperties.of(map);

    val result = new ConcurrentHashMap<String, Object>();
    result.put(INBOUND_MQTT_CLIENT_SUPPLIER, mqttClientSupplier(properties.getInbound()));
    result.put(EXPIRE_TIME_MILLISECONDS, SECONDS.toMillis(properties.getWindowSeconds()));
    result.put(OUTBOUND_MQTT_TOPIC_PREFIX, "median-t/");
    result.put(OUTBOUND_MQTT_CLIENT, mqttClientSupplier(properties.getOutbound()).get());

    return result;
  }

  private static Supplier<MqttClientWrapper> mqttClientSupplier (MqttConnection connectionProperties) {
    return () -> MqttClientWrapper.builder()
        .uri(connectionProperties.getUri())
        .username(connectionProperties.getUsername())
        .password(connectionProperties.getPassword())
        .build()
        .connect();
  }

  private UserContextFactory () {
    throw new UnsupportedOperationException();
  }
}
