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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Application class tests")
class ApplicationTests {

  @Nested
  @DisplayName("Check serialization")
  class Serialization {

    @Test
    @DisplayName("serialization to byte array")
    void toBytes () throws Exception {
      val id = "1";
      val address = InetAddress.getLocalHost();
      val port = 9876;
      val createdTimestamp = 1_000;

      val application = Application.builder()
          .id(id)
          .address(address)
          .port(port)
          .createdTimestamp(createdTimestamp)
          .build();

      val bytes = application.toBytes();
      val expected = ByteBuffer.allocate(21 + address.getAddress().length)
          .putInt(id.getBytes().length)
          .put(id.getBytes())
          .putInt(address.getAddress().length)
          .put(address.getAddress())
          .putInt(port)
          .putLong(createdTimestamp)
          .array();

      assertThat(bytes)
          .inHexadecimal()
          .hasSize(expected.length)
          .containsSequence(expected);
    }
  }

  @Nested
  @DisplayName("Check deserialization")
  class Deserialization {

    @Test
    @DisplayName("deserialization from byte array")
    void ofBytes () throws Exception {
      val id = "9";
      val address = InetAddress.getLocalHost();
      val port = 5546;
      val createdTimestamp = 100500;

      val bytes = ByteBuffer.allocate(21 + address.getAddress().length)
          .putInt(id.getBytes().length)
          .put(id.getBytes())
          .putInt(address.getAddress().length)
          .put(address.getAddress())
          .putInt(port)
          .putLong(createdTimestamp)
          .array();

      val application = Application.from(bytes);

      assertThat(application)
          .isNotNull();

      assertThat(application.getId())
          .isEqualTo(id);

      assertThat(application.getAddress())
          .isEqualTo(address);

      assertThat(application.getPort())
          .isEqualTo(port);

      assertThat(application.getCreatedTimestamp())
          .isEqualTo(createdTimestamp);
    }

    @Test
    @DisplayName("deserialization from ByteBuffer")
    void ofByteBuffer () throws Exception {
      val id = "9";
      val address = InetAddress.getLocalHost();
      val port = 5546;
      val createdTimestamp = 100500;

      val buffer = ByteBuffer.allocate(21 + address.getAddress().length)
          .putInt(id.getBytes().length)
          .put(id.getBytes())
          .putInt(address.getAddress().length)
          .put(address.getAddress())
          .putInt(port)
          .putLong(createdTimestamp);

      buffer.rewind();
      val application = Application.from(buffer);

      assertThat(application)
          .isNotNull();

      assertThat(application.getId())
          .isEqualTo(id);

      assertThat(application.getAddress())
          .isEqualTo(address);

      assertThat(application.getPort())
          .isEqualTo(port);

      assertThat(application.getCreatedTimestamp())
          .isEqualTo(createdTimestamp);
    }

    @Test
    @DisplayName("parse empty byte array")
    void parseEmptyByteArray () {
      assertThatThrownBy(() -> Application.from(new byte[0]))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid buffer length, it must be at least 20 bytes, but it is 0");
    }

    @Test
    @DisplayName("parse empty byte array")
    void parseEmptyByteBuffer () {
      assertThatThrownBy(() -> Application.from(ByteBuffer.allocate(0)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid buffer length, it must be at least 20 bytes, but it is 0");
    }
  }
}
