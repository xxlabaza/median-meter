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

package com.xxlabaza.test.median.meter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Interface for serializable/deserializable objects.
 */
public interface Transportable extends Serializable {

  /**
   * Parse byte array to a new object.
   *
   * @param bytes byte array to parse.
   *
   * @param <T> serializable type of parsed objects.
   *
   * @return a new parsed object instance.
   */
  static <T extends Transportable> T from (@NonNull byte[] bytes) {
    if (bytes.length == 0) {
      throw new IllegalArgumentException("Couldn't deserialize an object, byte array is empty");
    }
    val byteArrayInputStream = new ByteArrayInputStream(bytes);
    return from(byteArrayInputStream);
  }

  /**
   * Read and parse input stream to a new object.
   *
   * @param inputStream input stream.
   *
   * @param <T> serializable type of parsed objects.
   *
   * @return a new parsed object instance.
   */
  @SneakyThrows
  @SuppressWarnings("unchecked")
  static <T extends Transportable> T  from (@NonNull InputStream inputStream) {
    try (val objectInputStream = new ObjectInputStream(inputStream)) {
      return (T) objectInputStream.readObject();
    }
  }

  /**
   * Converts an object to byte array.
   *
   * @return bytes which represent this object.
   */
  default byte[] toBytes () {
    val byteArrayOutputStream = new ByteArrayOutputStream();
    writeTo(byteArrayOutputStream);
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Writes this object to output stream.
   *
   * @param outputStream destination output stream.
   */
  @SneakyThrows
  default void writeTo (@NonNull OutputStream outputStream) {
    try (val objectOutputStream = new ObjectOutputStream(outputStream)) {
      objectOutputStream.writeObject(this);
    }
  }
}
