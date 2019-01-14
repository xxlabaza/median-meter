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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import org.yaml.snakeyaml.Yaml;

@Value
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = PRIVATE)
class MqttDiscoveryServiceClientProperties {

  static MqttDiscoveryServiceClientProperties load (@NonNull String fileName) {
    val path = Paths.get(fileName);
    return load(path);
  }

  static MqttDiscoveryServiceClientProperties load (@NonNull File file) {
    val path = file.toPath();
    return load(path);
  }

  @SneakyThrows
  static MqttDiscoveryServiceClientProperties load (@NonNull Path path) {
    if (Files.notExists(path)) {
      val msg = String.format("File '%s' doesn't exist", path.toString());
      throw new IllegalArgumentException(msg);
    }

    val yaml = new Yaml();
    Map<String, Object> properties;
    try (val inputStream = Files.newInputStream(path)) {
      properties = yaml.load(inputStream);
    }
    return of(properties);
  }

  static MqttDiscoveryServiceClientProperties of (@NonNull Map<String, Object> properties) {
    val result = new MqttDiscoveryServiceClientProperties();

    val map = properties.get("cluster");
    if (map instanceof Map) {
      val clusterClientProperties = (Map<String, Object>) map;

      result.getSelf().overwrite(clusterClientProperties);
      result.getHeartbeat().overwrite(clusterClientProperties);
    }
    result.getMqtt().overwrite(properties);

    return result;
  }

  Self self = new Self();

  Heartbeat heartbeat = new Heartbeat();

  Mqtt mqtt = new Mqtt();

  @Data
  @Setter(PRIVATE)
  static class Self {

    String id = UUID.randomUUID().toString();

    InetAddress address;

    int port = 8913;

    @SneakyThrows
    InetAddress getAddress () {
      if (address == null) {
        address = InetAddress.getLocalHost();
      }
      return address;
    }

    void overwrite (Map<String, Object> clusterClientProperties) {
      val map = clusterClientProperties.get("self");
      if (!(map instanceof Map)) {
        return;
      }
      val selfProperties = (Map<String, Object>) map;

      ofNullable(selfProperties.get("id"))
          .map(Object::toString)
          .ifPresent(this::setId);

      ofNullable(selfProperties.get("address"))
          .map(Object::toString)
          .map(it -> {
            try {
              return InetAddress.getByName(it);
            } catch (UnknownHostException ex) {
              val msg = String.format("Error during parsing address '%s'", it);
              throw new IllegalArgumentException(msg, ex);
            }
          })
          .ifPresent(this::setAddress);

      ofNullable(selfProperties.get("port"))
          .map(Object::toString)
          .map(Integer::parseInt)
          .ifPresent(this::setPort);
    }
  }

  @Data
  @Setter(PRIVATE)
  static class Heartbeat {

    String topicPrefix = "cluster/member/";

    int rateSeconds = 5;

    String getTopicPrefix () {
      if (!topicPrefix.endsWith("/")) {
        topicPrefix += "/";
      }
      return topicPrefix;
    }

    void overwrite (Map<String, Object> clusterClientProperties) {
      val map = clusterClientProperties.get("heartbeat");
      if (!(map instanceof Map)) {
        return;
      }
      val heartbeatProperties = (Map<String, Object>) map;

      ofNullable(heartbeatProperties.get("topic-prefix"))
          .map(Object::toString)
          .ifPresent(this::setTopicPrefix);

      ofNullable(heartbeatProperties.get("rate-seconds"))
          .map(Object::toString)
          .map(Integer::parseInt)
          .ifPresent(this::setRateSeconds);
    }
  }

  @Data
  @Setter(PRIVATE)
  static class Mqtt {

    String host = "localhost";

    int port = 1883;

    String uri;

    String username;

    String password;

    String getUri () {
      if (uri == null) {
        uri = new StringBuilder()
            .append("tcp://")
            .append(host)
            .append(':')
            .append(port)
            .toString();
      }
      return uri;
    }

    void overwrite (Map<String, Object> properties) {
      val map = properties.get("inbound");
      if (!(map instanceof Map)) {
        return;
      }
      val mqttProperties = (Map<String, Object>) map;

      ofNullable(mqttProperties.get("host"))
          .map(Object::toString)
          .ifPresent(this::setHost);

      ofNullable(mqttProperties.get("port"))
          .map(Object::toString)
          .map(Integer::parseInt)
          .ifPresent(this::setPort);

      ofNullable(mqttProperties.get("url"))
          .map(Object::toString)
          .ifPresent(this::setUri);

      ofNullable(mqttProperties.get("user"))
          .map(Object::toString)
          .ifPresent(this::setUsername);

      ofNullable(mqttProperties.get("pass"))
          .map(Object::toString)
          .ifPresent(this::setPassword);
    }
  }
}
