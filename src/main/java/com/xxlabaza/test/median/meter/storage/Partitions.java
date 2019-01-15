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

import java.util.Optional;
import java.util.stream.IntStream;

import com.xxlabaza.test.median.meter.Transportable;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;

@ToString
@EqualsAndHashCode
@FieldDefaults(level = PRIVATE, makeFinal = true)
class Partitions<T extends Transportable> implements AutoCloseable {

  Partition<T>[] partitions;

  @SuppressWarnings("unchecked")
  Partitions (int count) {
    if (count <= 0) {
      val msg = String.format("Partitions count must be greater than 0, but it is %d", count);
      throw new IllegalArgumentException(msg);
    }
    partitions = new Partition[count];
  }

  @Override
  public void close () {
    clear();
  }

  void clear () {
    IntStream.range(0, partitions.length)
        .forEach(this::remove);
  }

  Partition<T> create (int index) {
    validatePartitionIndex(index);

    if (partitions[index] != null) {
      val msg = String.format("Partition with index %d already exist", index);
      throw new IllegalArgumentException(msg);
    }
    partitions[index] = new Partition<T>(index);
    return partitions[index];
  }

  void add (@NonNull Partition<T> partition) {
    val index = partition.getIndex();
    validatePartitionIndex(index);

    partitions[index] = partition;
  }

  int getCount () {
    return partitions.length;
  }

  Optional<Partition<T>> get (int index) {
    validatePartitionIndex(index);

    return ofNullable(partitions[index]);
  }

  Partition<T> getOrCreate (int index) {
    return get(index)
        .orElseGet(() -> create(index));
  }

  @SuppressWarnings("PMD.NullAssignment")
  void remove (int index) {
    validatePartitionIndex(index);

    val partition = partitions[index];
    if (partition == null) {
      return;
    }

    partition.close();
    partitions[index] = null;
  }

  private void validatePartitionIndex (int index) {
    if (index < 0 || index >= partitions.length) {
      val msg = String.format(
          "Partition index must be greater or equals than 0 and lower than partitions upper bound (%d), but it is %d",
          partitions.length, index
      );
      throw new ArrayIndexOutOfBoundsException(msg);
    }
  }
}
