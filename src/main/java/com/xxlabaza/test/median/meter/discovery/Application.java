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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

/**
 * Holds info about remote or local application.
 */
@Value
@Builder
public class Application {

  // int4 - ID length
  // int4 - address length
  // int4 - port
  // int8 - creation timestamp
  private static final int MINIMAL_BYTES_LENGTH = Integer.BYTES * 3 + Long.BYTES;

  static Application from (@NonNull byte[] bytes) {
    val buffer = ByteBuffer.wrap(bytes);
    return from(buffer);
  }

  @SneakyThrows
  static Application from (@NonNull ByteBuffer buffer) {
    if (buffer.remaining() <= MINIMAL_BYTES_LENGTH) {
      val msg = String.format(
          "Invalid buffer length, it must be at least %d bytes, but it is %d",
          MINIMAL_BYTES_LENGTH, buffer.remaining()
      );
      throw new IllegalArgumentException(msg);
    }

    val idBytesLength = buffer.getInt();
    val idBytes = new byte[idBytesLength];
    buffer.get(idBytes);

    val addressBytesLength = buffer.getInt();
    val addressBytes = new byte[addressBytesLength];
    buffer.get(addressBytes);

    return Application.builder()
        .id(new String(idBytes, UTF_8))
        .address(InetAddress.getByAddress(addressBytes))
        .port(buffer.getInt())
        .createdTimestamp(buffer.getLong())
        .build();
  }

  @NonNull
  String id;

  @NonNull
  InetAddress address; // don't use, just for an example

  int port; // don't use, just for an example

  long createdTimestamp;

  final byte[] toBytes () {
    val idBytes = id.getBytes(UTF_8);
    val addressBytes = address.getAddress();

    return ByteBuffer.allocate(Integer.BYTES + idBytes.length + // id + it's length
                               Integer.BYTES + addressBytes.length + // ip + it's length
                               Integer.BYTES + // port
                               Long.BYTES) // creation timestamp
        .putInt(idBytes.length)
        .put(idBytes)
        .putInt(addressBytes.length)
        .put(addressBytes)
        .putInt(port)
        .putLong(createdTimestamp)
        .array();
  }
}
