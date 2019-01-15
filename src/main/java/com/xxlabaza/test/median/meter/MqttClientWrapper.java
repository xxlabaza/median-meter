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

package com.xxlabaza.test.median.meter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Wrapper for MQTT client with few helper methods.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MqttClientWrapper implements AutoCloseable {

  @Getter
  String id;

  MqttClient client;

  Map<String, List<IMqttMessageListener>> subscribes;

  @NonFinal
  String username;

  @NonFinal
  String password;

  @Builder
  @SneakyThrows
  MqttClientWrapper (@NonNull String uri,
                     String username,
                     String password
  ) {
    this.username = username;
    this.password = password;

    id = UUID.randomUUID().toString();
    client = new MqttClient(uri, id);

    subscribes = new HashMap<>();
  }

  /**
   * Connects this client to MQTT server with default connection options.
   *
   * @return {@code this} object for chaining calls.
   */
  public MqttClientWrapper connect () {
    val options = new MqttConnectOptions();
    options.setAutomaticReconnect(true);
    options.setCleanSession(true);
    options.setConnectionTimeout(10);

    ofNullable(username)
        .ifPresent(options::setUserName);
    ofNullable(password)
        .map(String::toCharArray)
        .ifPresent(options::setPassword);

    return connect(options);
  }

  /**
   * Connects this client to MQTT server with user's connection options.
   *
   * @param options connection options.
   *
   * @return {@code this} object for chaining calls.
   */
  @SneakyThrows
  public MqttClientWrapper connect (@NonNull MqttConnectOptions options) {
    if (client.isConnected()) {
      log.debug("MQTT client '{}' was already connected to URI '{}'", id, client.getServerURI());
    } else {
      client.connect(options);
      log.debug("MQTT client '{}' is connected to URI '{}'", id, client.getServerURI());
    }
    return this;
  }

  /**
   * Disconnects this client from MQTT server.
   *
   * @return {@code this} object for chaining calls.
   */
  @SneakyThrows
  public MqttClientWrapper disconnect () {
    if (client.isConnected()) {
      client.disconnect(1_000);
      log.debug("MQTT client '{}' is disconnected from URI '{}'", id, client.getServerURI());
    } else {
      log.debug("MQTT client '{}' was already disconnected from URI '{}'", id, client.getServerURI());
    }
    return this;
  }

  /**
   * Sends user's message to a specific topic.
   *
   * @param topic   MQTT topic name.
   *
   * @param message user's payload.
   *
   * @return {@code this} object for chaining calls.
   */
  public MqttClientWrapper send (@NonNull String topic, @NonNull String message) {
    val payload = message.getBytes(UTF_8);
    return send(topic, payload);
  }

  /**
   * Sends user's message to a specific topic.
   *
   * @param topic MQTT topic name.
   *
   * @param value user's payload.
   *
   * @return {@code this} object for chaining calls.
   */
  public MqttClientWrapper send (@NonNull String topic, double value) {
    val payload = ByteBuffer.allocate(Double.BYTES)
        .putDouble(value)
        .array();

    return send(topic, payload);
  }

  /**
   * Sends user's message to a specific topic.
   *
   * @param topic   MQTT topic name.
   *
   * @param payload user's payload.
   *
   * @return {@code this} object for chaining calls.
   */
  @SneakyThrows
  public MqttClientWrapper send (@NonNull String topic, @NonNull byte[] payload) {
    val message = new MqttMessage(payload);
    client.publish(topic, message);
    log.debug("MQTT client '{}' sent this message to '{}' topic\n{}", id, topic, payload);
    return this;
  }

  /**
   * Subscribes to a topic with specific handler.
   *
   * @param topicFilter MQTT topic filter (wildcards friendly).
   *
   * @param listener    user's listener.
   *
   * @return {@code this} object for chaining calls.
   */
  @SneakyThrows
  public MqttClientWrapper listen (@NonNull String topicFilter, @NonNull IMqttMessageListener listener) {
    client.subscribe(topicFilter, listener);
    subscribes.compute(topicFilter, (key, value) -> {
      if (value == null) {
        value = new LinkedList<>();
      }
      value.add(listener);
      return value;
    });
    log.debug("MQTT client '{}' has new listener for topic '{}'", id, topicFilter);
    return this;
  }

  /**
   * Removes listeners by topic name.
   *
   * @param topicFilter MQTT topic filter (wildcards friendly).
   */
  @SneakyThrows
  public void removeListeners (@NonNull String topicFilter) {
    client.unsubscribe(topicFilter);
    subscribes.remove(topicFilter);
  }

  @Override
  @SneakyThrows
  public void close () {
    disconnect();
    client.close(true);
  }
}
