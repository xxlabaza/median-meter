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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.xxlabaza.test.median.meter.Transportable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;

@ToString
@EqualsAndHashCode
@FieldDefaults(level = PRIVATE, makeFinal = true)
class Partition<T extends Transportable> implements Transportable, AutoCloseable {

  private static final long serialVersionUID = 2496887013859874565L;

  @Getter
  int index;

  Map<Integer, T> map = new HashMap<>();

  Partition (int index) {
    if (index < 0) {
      val msg = String.format("Partition index must be greater than 0, but it is %d", index);
      throw new IllegalArgumentException(msg);
    }
    this.index = index;
  }

  @Override
  public void close () {
    clear();
  }

  void clear () {
    map.clear();
  }

  T put (int id, @NonNull T object) {
    validateId(id);

    if (map.containsKey(id)) {
      val msg = String.format("ID %d already exists", id);
      throw new IllegalArgumentException(msg);
    }

    map.put(id, object);
    return object;
  }

  Optional<T> get (int id) {
    validateId(id);

    return ofNullable(map.get(id));
  }

  void remove (int id) {
    validateId(id);

    map.remove(id);
  }

  private void validateId (int id) {
    if (id < 0) {
      val msg = String.format("ID must be greater than 0, but it is %d", id);
      throw new IllegalArgumentException(msg);
    }
  }
}
