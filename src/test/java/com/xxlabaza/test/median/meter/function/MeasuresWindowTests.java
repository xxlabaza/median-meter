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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xxlabaza.test.median.meter.Transportable;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MeasuresWindow class tests")
class MeasuresWindowTests {

  @Test
  @DisplayName("negative device id")
  void negativeDeviceId () {
    assertThatThrownBy(() -> new MeasuresWindow(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Device ID must be greater than 0, but it is -1");
  }

  @Test
  @DisplayName("serialize/deserialize empty window")
  void serializeAndDeserializeEmpty () {
    val deviceId = 1;

    try (val window = new MeasuresWindow(deviceId)) {
      val bytes = window.toBytes();
      val parsed = (MeasuresWindow) Transportable.from(bytes);

      assertThat(parsed)
          .isNotNull();

      assertThat(parsed.getDeviceId())
          .isEqualTo(deviceId);

      assertThat(parsed)
          .isEqualTo(window);
    }
  }

  @Test
  @DisplayName("serialize/deserialize window with several measures")
  void serializeAndDeserializeNonEmpty () {
    val deviceId = 13;

    val measures = asList(
        new Measure(System.currentTimeMillis() + 10_000, 0),
        new Measure(System.currentTimeMillis() + 10_000, 1),
        new Measure(System.currentTimeMillis() + 10_000, 2),
        new Measure(System.currentTimeMillis() + 10_000, 3),
        new Measure(System.currentTimeMillis() + 10_000, 4)
    );

    try (val window = new MeasuresWindow(deviceId)) {
      measures.forEach(window::median);

      val bytes = window.toBytes();
      val parsed = (MeasuresWindow) Transportable.from(bytes);

      assertThat(parsed)
          .isNotNull();

      assertThat(parsed.getDeviceId())
          .isEqualTo(deviceId);

      assertThat(parsed)
          .isEqualTo(window);
    }
  }
}
