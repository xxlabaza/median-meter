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
class MqttMedianMeterFunctionProperties {

  static MqttMedianMeterFunctionProperties load (@NonNull String fileName) {
    val path = Paths.get(fileName);
    return load(path);
  }

  static MqttMedianMeterFunctionProperties load (@NonNull File file) {
    val path = file.toPath();
    return load(path);
  }

  @SneakyThrows
  static MqttMedianMeterFunctionProperties load (@NonNull Path path) {
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

  static MqttMedianMeterFunctionProperties of (@NonNull Map<String, Object> properties) {
    val result = new MqttMedianMeterFunctionProperties();

    ofNullable(properties.get("temperatureTimeWindowInSec"))
        .map(Object::toString)
        .map(Integer::parseInt)
        .ifPresent(result::setWindowSeconds);

    ofNullable(properties.get("inbound"))
        .filter(it -> it instanceof Map)
        .map(it -> (Map<String, Object>) it)
        .ifPresent(it -> result.getInbound().overwrite(it));

    ofNullable(properties.get("outbound"))
        .filter(it -> it instanceof Map)
        .map(it -> (Map<String, Object>) it)
        .ifPresent(it -> result.getInbound().overwrite(it));

    return result;
  }

  Integer windowSeconds = 1;

  MqttConnection inbound = new MqttConnection();

  MqttConnection outbound = new MqttConnection();

  @Data
  @Setter(PRIVATE)
  static class MqttConnection {

    String uri = "tcp://localhost:1883";

    String username;

    String password;

    void overwrite (Map<String, Object> properties) {
      ofNullable(properties.get("url"))
          .map(Object::toString)
          .ifPresent(this::setUri);

      ofNullable(properties.get("user"))
          .map(Object::toString)
          .ifPresent(this::setUsername);

      ofNullable(properties.get("pass"))
          .map(Object::toString)
          .ifPresent(this::setPassword);
    }
  }
}
