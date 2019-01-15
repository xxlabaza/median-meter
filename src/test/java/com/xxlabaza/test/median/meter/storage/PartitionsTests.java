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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xxlabaza.test.median.meter.function.MeasuresWindow;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Partitions class tests")
class PartitionsTests {

  @Test
  @DisplayName("negative partitions count")
  void negativeCount () {
    assertThatThrownBy(() -> new Partitions<MeasuresWindow>(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Partitions count must be greater than 0, but it is -1");
  }

  @Nested
  @DisplayName("Create method tests")
  class CreateMethod {

    Partitions<MeasuresWindow> partitions = new Partitions<>(5);

    @BeforeEach
    void beforeEach () {
      partitions.clear();
    }

    @Test
    @DisplayName("create partition")
    void createPartition () {
      val partition = partitions.create(1);

      assertThat(partition)
          .isNotNull();

      assertThat(partition.getIndex())
          .isEqualTo(1);
    }

    @Test
    @DisplayName("create many partitions")
    void createManyPartitions () {
      assertThat(partitions.create(1))
          .isNotNull()
          .extracting(Partition::getIndex)
          .isEqualTo(1);

      assertThat(partitions.create(2))
          .isNotNull()
          .extracting(Partition::getIndex)
          .isEqualTo(2);
    }

    @Test
    @DisplayName("create partition with invalid index")
    void createPartitionWithInvalidIndex () {
      assertThatThrownBy(() -> partitions.create(-9))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (5), but it is -9");

      assertThatThrownBy(() -> partitions.create(5))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (5), but it is 5");
    }

    @Test
    @DisplayName("create already existent partition")
    void createAlreadyExistentIndex () {
      val partition = partitions.create(3);

      assertThat(partition)
          .isNotNull();

      assertThatThrownBy(() -> partitions.create(partition.getIndex()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Partition with index %d already exist", partition.getIndex());
    }
  }

  @Nested
  @DisplayName("Get method tests")
  class GetMethod {

    Partitions<MeasuresWindow> partitions = new Partitions<>(10);

    @BeforeEach
    void beforeEach () {
      partitions.clear();
    }

    @Test
    @DisplayName("get partition by index")
    void simpleGet () {
      val partition = partitions.create(3);

      assertThat(partitions.get(3))
          .isPresent()
          .hasValue(partition);
    }

    @Test
    @DisplayName("get nonexistent partition index")
    void emptyGet () {
      assertThat(partitions.get(0))
          .isNotPresent();
    }

    @Test
    @DisplayName("get invalid partition index")
    void invalidPartitionIndex () {
      assertThatThrownBy(() -> partitions.get(-9))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (10), but it is -9");

      assertThatThrownBy(() -> partitions.get(10))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (10), but it is 10");
    }
  }

  @Nested
  @DisplayName("Remove method tests")
  class RemoveMethod {

    Partitions<MeasuresWindow> partitions = new Partitions<>(10);

    @Test
    @DisplayName("remove partition by index")
    void removePartition () {
      partitions.create(7);

      assertThat(partitions.get(7))
          .isPresent();

      partitions.remove(7);

      assertThat(partitions.get(7))
          .isNotPresent();
    }

    @Test
    @DisplayName("remove nonexistent index")
    void removeNonesistent () {
      partitions.remove(0);
    }

    @Test
    @DisplayName("remove invalid partition index")
    void invalidPartitionIndex () {
      assertThatThrownBy(() -> partitions.remove(-9))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (10), but it is -9");

      assertThatThrownBy(() -> partitions.remove(10))
          .isInstanceOf(ArrayIndexOutOfBoundsException.class)
          .hasMessage("Partition index must be greater or equals than 0 and lower than partitions upper bound (10), but it is 10");
    }
  }
}
