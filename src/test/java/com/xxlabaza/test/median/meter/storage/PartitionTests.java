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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xxlabaza.test.median.meter.Transportable;
import com.xxlabaza.test.median.meter.function.MeasuresWindow;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Partition class tests")
class PartitionTests {

  @Test
  @DisplayName("negative partition index")
  void negativeIndex () {
    assertThatThrownBy(() -> new Partition<MeasuresWindow>(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Partition index must be greater than 0, but it is -1");
  }

  @Test
  @DisplayName("serialize/deserialize empty partition")
  void serializeAndDeserializeEmpty () {
    try (val partition = new Partition<MeasuresWindow>(5)) {
      val bytes = partition.toBytes();
      val parsed = (Partition<MeasuresWindow>) Transportable.from(bytes);

      assertThat(parsed)
          .isNotNull();

      assertThat(parsed.getIndex())
          .isEqualTo(partition.getIndex());

      assertThat(parsed)
          .isEqualTo(partition);
    }
  }

  @Test
  @DisplayName("serialize/deserialize partition with several values")
  void serializeAndDeserializeNonEmpty () {
    val windows = asList(
        new MeasuresWindow(7),
        new MeasuresWindow(155),
        new MeasuresWindow(111),
        new MeasuresWindow(6784),
        new MeasuresWindow(9)
    );

    try (val partition = new Partition<MeasuresWindow>(32)) {
      windows.forEach(it -> partition.put(it.getDeviceId(), it));

      val bytes = partition.toBytes();
      val parsed = (Partition<MeasuresWindow>) Transportable.from(bytes);

      assertThat(parsed)
          .isNotNull();

      assertThat(parsed.getIndex())
          .isEqualTo(partition.getIndex());

      windows.forEach(it -> {
        assertThat(parsed.get(it.getDeviceId()))
            .isPresent()
            .hasValue(it);
      });

      assertThat(parsed)
          .isEqualTo(partition);
    }
  }

  @Nested
  @DisplayName("Create method tests")
  class CreateMethod {

    Partition<MeasuresWindow> partition = new Partition<>(42);

    @BeforeEach
    void beforeEach () {
      partition.clear();
    }

    @Test
    @DisplayName("create window")
    void createWindow () {
      val window = new MeasuresWindow(7);
      val returned = partition.put(window.getDeviceId(), window);

      assertThat(returned)
          .isNotNull();

      assertThat(returned.getDeviceId())
          .isEqualTo(7);

      assertThat(returned)
          .isEqualTo(window);
    }

    @Test
    @DisplayName("create many windows")
    void createManyWindows () {
      val window1 = new MeasuresWindow(17);
      assertThat(partition.put(window1.getDeviceId(), window1))
          .isNotNull()
          .isEqualTo(window1);

      val window2 = new MeasuresWindow(18);
      assertThat(partition.put(window2.getDeviceId(), window2))
          .isNotNull()
          .isEqualTo(window2);
    }

    @Test
    @DisplayName("create window with invalid ID")
    void createWindowWithInvalidId () {
      assertThatThrownBy(() -> partition.put(-9, new MeasuresWindow(0)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID must be greater than 0, but it is -9");
    }

    @Test
    @DisplayName("create already existent ID")
    void createAlreadyExistentId () {
      val window = new MeasuresWindow(3);
      val returned = partition.put(window.getDeviceId(), window);

      assertThat(returned)
          .isNotNull();

      assertThatThrownBy(() -> partition.put(window.getDeviceId(), window))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID %d already exists", returned.getDeviceId());
    }
  }

  @Nested
  @DisplayName("Get method tests")
  class GetMethod {

    Partition<MeasuresWindow> partition = new Partition<>(89);

    @BeforeEach
    void beforeEach () {
      partition.clear();
    }

    @Test
    @DisplayName("get window by id")
    void simpleGet () {
      val window = new MeasuresWindow(63);
      val returned = partition.put(window.getDeviceId(), window);

      assertThat(partition.get(63))
          .isPresent()
          .hasValue(returned);
    }

    @Test
    @DisplayName("get nonexistent ID")
    void emptyGet () {
      assertThat(partition.get(0))
          .isNotPresent();
    }

    @Test
    @DisplayName("get invalid ID")
    void invalidId () {
      assertThatThrownBy(() -> partition.get(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID must be greater than 0, but it is -1");
    }
  }

  @Nested
  @DisplayName("Remove method tests")
  class RemoveMethod {

    Partition<MeasuresWindow> partition = new Partition<>(90);

    @Test
    @DisplayName("remove by ID")
    void remove () {
      val window = new MeasuresWindow(72);
      partition.put(window.getDeviceId(), window);

      assertThat(partition.get(72))
          .isPresent();

      partition.remove(72);

      assertThat(partition.get(72))
          .isNotPresent();
    }

    @Test
    @DisplayName("remove nonexistent ID")
    void removeNonesistent () {
      partition.remove(0);
    }

    @Test
    @DisplayName("remove invalid ID")
    void invalidId () {
      assertThatThrownBy(() -> partition.remove(-2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID must be greater than 0, but it is -2");
    }
  }
}
