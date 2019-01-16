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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.EqualsAndHashCode;
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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Wrapper for MQTT client with few helper methods.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MqttClientWrapper implements AutoCloseable {

  private static final Map<String, MqttClientWrapper> CLIENTS = new ConcurrentHashMap<>();

  /**
   * Static constructor method with result caching.
   * <p>
   * The second method's call always return the same (cached) result as at first call.
   *
   * @param uri uri to MQTT service.
   *
   * @return new or cached MQTT client.
   */
  public static MqttClientWrapper of (@NonNull String uri) {
    return CLIENTS.compute(uri, (key, value) -> {
      if (value != null) {
        return value;
      }
      return new MqttClientWrapper(key);
    });
  }

  /**
   * Close all known MQTT clients.
   */
  public static void closeAll () {
    CLIENTS.values().forEach(MqttClientWrapper::close);
    CLIENTS.clear();
  }

  MqttClient client;

  @NonFinal
  String username;

  @NonFinal
  String password;

  @SneakyThrows
  private MqttClientWrapper (@NonNull String uri) {
    val id = UUID.randomUUID().toString();
    client = new MqttClient(uri, id);
  }

  /**
   * Returns client's unique ID.
   *
   * @return client's ID.
   */
  public String getId () {
    return client.getClientId();
  }

  /**
   * Sets username to MQTT server connection.
   * <p>
   * If client is already connected - nothing happen.
   *
   * @param username MQTT server's username
   *
   * @return {@code this} object for chaining calls.
   */
  @SuppressWarnings("checkstyle:HiddenField")
  public MqttClientWrapper withUsername (String username) {
    if (!client.isConnected()) {
      this.username = username;
    }
    return this;
  }

  /**
   * Sets password to MQTT server connection.
   * <p>
   * If client is already connected - nothing happen.
   *
   * @param password MQTT server's password
   *
   * @return {@code this} object for chaining calls.
   */
  @SuppressWarnings("checkstyle:HiddenField")
  public MqttClientWrapper withPassword (String password) {
    if (!client.isConnected()) {
      this.password = password;
    }
    return this;
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
      log.debug("MQTT client '{}' was already connected to URI '{}'",
                client.getClientId(), client.getServerURI());
    } else {
      try {
        client.connectWithResult(options).waitForCompletion(10_000);
      } catch (MqttException ex) {
        log.error("Error during connection to MQTT server to '{}'",
                  client.getServerURI(), ex);
        throw ex;
      }
      log.debug("MQTT client '{}' is connected to URI '{}' - {}",
                client.getClientId(), client.getServerURI(), client.isConnected());
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
      log.debug("MQTT client '{}' is disconnected from URI '{}'",
                client.getClientId(), client.getServerURI());
    } else {
      log.debug("MQTT client '{}' was already disconnected from URI '{}'",
                client.getClientId(), client.getServerURI());
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
    try {
      client.publish(topic, message);
    } catch (MqttException ex) {
      log.error("ERROR, client is connected - {}", client.isConnected());
      throw ex;
    }
    log.debug("MQTT client '{}' sent this message to '{}' topic\n{}",
              client.getClientId(), topic, payload);
    return this;
  }

  Map<String, Map<String, IMqttMessageListener>> subscriptions = new ConcurrentHashMap<>();

  /**
   * Subscribes to a topic with specific handler.
   *
   * @param topicFilter MQTT topic filter (wildcards friendly).
   *
   * @param listener    user's listener.
   *
   * @return unique identifier of the subscription.
   */
  public String subscribe (@NonNull String topicFilter, @NonNull IMqttMessageListener listener) {
    val id = UUID.randomUUID().toString();

    subscriptions.compute(topicFilter, (key, value) -> {
      if (value == null) {
        value = new LinkedHashMap<>();
        subscribe(key);
      }
      value.put(id, listener);
      return value;
    });

    log.debug("MQTT client '{}' has new listener for topic '{}'",
              client.getClientId(), topicFilter);
    return id;
  }

  /**
   * Removes subscription by topic name and its ID.
   *
   * @param topicFilter MQTT topic filter (wildcards friendly).
   *
   * @param id          unique identifier of one of topic's handlers.
   */
  public void unsubscribe (@NonNull String topicFilter, @NonNull String id) {
    val map = subscriptions.get(topicFilter);
    if (map == null) {
      return;
    }

    map.remove(id);
    if (map.isEmpty()) {
      unsubscribe(topicFilter);
    }
  }

  /**
   * Removes subscription by topic name.
   *
   * @param topicFilter MQTT topic filter (wildcards friendly).
   */
  @SneakyThrows
  @SuppressWarnings("PMD.AvoidCatchingNPE")
  public void unsubscribe (@NonNull String topicFilter) {
    subscriptions.remove(topicFilter);
    try {
      client.unsubscribe(topicFilter);
    } catch (NullPointerException ex) {
      log.warn("Error during unsubscription from nonexistent topic - '{}'", topicFilter);
    }
  }

  @Override
  @SneakyThrows
  public void close () {
    if (client.isConnected()) {
      return;
    }

    disconnect();
    client.close(true);
    CLIENTS.remove(client.getServerURI());
  }

  @SneakyThrows
  private void subscribe (@NonNull String topicFilter) {
    client.subscribe(topicFilter, (topic, message) -> {
      val map = subscriptions.get(topicFilter);
      if (map == null) {
        return;
      }

      map.values().forEach(it -> {
        try {
          it.messageArrived(topic, message);
        } catch (Exception ex) {
          log.error("Error during handling a new message from topic '{}'\n{}",
                    topic, message, ex);
        }
      });
    });
  }
}
