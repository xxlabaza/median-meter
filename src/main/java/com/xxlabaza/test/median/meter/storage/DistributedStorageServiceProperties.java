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

package com.xxlabaza.test.median.meter.storage;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import org.yaml.snakeyaml.Yaml;

@Data
@Setter(PRIVATE)
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = PRIVATE)
class DistributedStorageServiceProperties {

  static DistributedStorageServiceProperties load (@NonNull String fileName) {
    val path = Paths.get(fileName);
    return load(path);
  }

  static DistributedStorageServiceProperties load (@NonNull File file) {
    val path = file.toPath();
    return load(path);
  }

  @SneakyThrows
  static DistributedStorageServiceProperties load (@NonNull Path path) {
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

  static DistributedStorageServiceProperties of (@NonNull Map<String, Object> properties) {
    val result = new DistributedStorageServiceProperties();

    val map = properties.get("storage");
    if (map instanceof Map) {
      val storageProperties = (Map<String, Object>) map;

      ofNullable(storageProperties.get("partitions"))
        .map(Object::toString)
        .map(Integer::parseInt)
        .ifPresent(result::setPartitions);
    }
    result.getMqtt().overwrite(properties);

    return result;
  }

  int partitions = 271;

  Mqtt mqtt = new Mqtt();

  @Data
  @Setter(PRIVATE)
  class Mqtt {

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
