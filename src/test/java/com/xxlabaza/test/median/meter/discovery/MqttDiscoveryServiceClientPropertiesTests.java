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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MQTT-based discovery service client properties tests")
class MqttDiscoveryServiceClientPropertiesTests {

  @Test
  @DisplayName("parse empty map")
  void parseEmptyMap () {
    val properties = MqttDiscoveryServiceClientProperties.of(emptyMap());

    assertThat(properties).isNotNull();

    val self = properties.getSelf();
    assertSoftly(softly -> {
      softly.assertThat(self).isNotNull();

      softly.assertThat(self.getId())
          .isNotEmpty();

      try {
        softly.assertThat(self.getAddress())
            .isEqualTo(InetAddress.getLocalHost());
      } catch (UnknownHostException ex) {
        softly.fail("Couldn't extract localhost", ex);
      }

      softly.assertThat(self.getPort())
          .isEqualTo(8913);
    });

    val heartbeat = properties.getHeartbeat();
    assertSoftly(softly -> {
      softly.assertThat(heartbeat).isNotNull();

      softly.assertThat(heartbeat.getTopicPrefix())
          .isEqualTo("cluster/member/");

      softly.assertThat(heartbeat.getRateSeconds())
          .isEqualTo(5);
    });

    val mqtt = properties.getMqtt();
    assertSoftly(softly -> {
      softly.assertThat(mqtt).isNotNull();

      softly.assertThat(mqtt.getHost())
          .isEqualTo("localhost");

      softly.assertThat(mqtt.getPort())
          .isEqualTo(1883);

      softly.assertThat(mqtt.getUri())
          .isEqualTo("tcp://localhost:1883");

      softly.assertThat(mqtt.getUsername())
          .isNull();

      softly.assertThat(mqtt.getPassword())
          .isNull();
    });
  }

  @Test
  @DisplayName("parse null map")
  void parseNullMap () {
    assertThatThrownBy(() -> MqttDiscoveryServiceClientProperties.of(null))
          .isInstanceOf(NullPointerException.class);
  }

  @Nested
  @DisplayName("Loading properties from file")
  class Load {

    @Test
    @DisplayName("load by file name")
    void loadByFileName () {
      val fileName = getConfigurationPath().toString();
      val properties = MqttDiscoveryServiceClientProperties.load(fileName);

      assertProperties(properties);
    }

    @Test
    @DisplayName("load by File")
    void loadByFile () {
      val file = getConfigurationPath().toFile();
      val properties = MqttDiscoveryServiceClientProperties.load(file);

      assertProperties(properties);
    }

    @Test
    @DisplayName("load by Path")
    void loadByPath () {
      val path = getConfigurationPath();
      val properties = MqttDiscoveryServiceClientProperties.load(path);

      assertProperties(properties);
    }

    @Test
    @DisplayName("load null")
    void loadNull () {
      assertThatThrownBy(() -> MqttDiscoveryServiceClientProperties.load((Path) null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("load nonexistent file")
    void loadNonexistentFile () {
      assertThatThrownBy(() -> MqttDiscoveryServiceClientProperties.load("popa.yml"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("File 'popa.yml' doesn't exist");
    }

    @SneakyThrows
    private Path getConfigurationPath () {
      val classLoader = Thread.currentThread().getContextClassLoader();
      val url = classLoader.getResource("MqttDiscoveryServiceClientProperties.yml");
      val uri = url.toURI();
      return Paths.get(uri);
    }

    private void assertProperties (MqttDiscoveryServiceClientProperties properties) {
      assertThat(properties).isNotNull();

      val self = properties.getSelf();
      assertSoftly(softly -> {
        softly.assertThat(self).isNotNull();

        softly.assertThat(self.getId())
            .isEqualTo("1");

        softly.assertThat(self.getAddress())
            .isNotNull()
            .extracting(InetAddress::toString)
            .isEqualTo("localhost/127.0.0.1");

        softly.assertThat(self.getPort())
            .isEqualTo(8888);
      });

      val heartbeat = properties.getHeartbeat();
      assertSoftly(softly -> {
        softly.assertThat(heartbeat).isNotNull();

        softly.assertThat(heartbeat.getTopicPrefix())
            .isEqualTo("some/");

        softly.assertThat(heartbeat.getRateSeconds())
            .isEqualTo(1);
      });

      val mqtt = properties.getMqtt();
      assertSoftly(softly -> {
        softly.assertThat(mqtt).isNotNull();

        softly.assertThat(mqtt.getHost())
            .isEqualTo("localhost");

        softly.assertThat(mqtt.getPort())
            .isEqualTo(1883);

        softly.assertThat(mqtt.getUri())
            .isEqualTo("tcp://localhost:7865");

        softly.assertThat(mqtt.getUsername())
            .isEqualTo("admin");

        softly.assertThat(mqtt.getPassword())
            .isEqualTo("secret");
      });
    }
  }
}
