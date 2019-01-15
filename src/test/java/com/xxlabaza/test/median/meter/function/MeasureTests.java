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

import static org.assertj.core.api.Assertions.assertThat;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Measure class tests")
class MeasureTests {

  @Test
  @DisplayName("serialize/deserialize test")
  void serializeAndDeserialize () {
    val value = 3.D;
    val timestamp = System.currentTimeMillis();

    val measure = new Measure(timestamp, value);

    val bytes = measure.toBytes();
    val parsed = Measure.from(bytes);

    assertThat(parsed).isNotNull();

    assertThat(parsed.getTimestamp())
        .isEqualTo(timestamp);

    assertThat(parsed.getValue())
        .isEqualTo(value);

    assertThat(parsed)
        .isEqualTo(measure);
  }
}
