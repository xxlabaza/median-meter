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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hazelcast.core.IMap;
import lombok.Builder;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
@Builder
@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MqttSensorsListener implements IMqttMessageListener {

  private static final Pattern SENSOR_ID_IN_TOPIC_NAME = Pattern.compile("\\w+/(?<id>\\d+)");

  IMap<Integer, Queue<Measure>> map;

  @Override
  public void messageArrived (String topic, MqttMessage message) throws Exception {
    val sensorId = ofNullable(topic)
        .map(SENSOR_ID_IN_TOPIC_NAME::matcher)
        .filter(Matcher::find)
        .map(it -> it.group("id"))
        .map(Integer::parseInt)
        .orElse(-1);

    if (sensorId == -1) {
      log.warn("Topic '{}' doesn't matche to expecting topic pattern - '{}",
               topic, SENSOR_ID_IN_TOPIC_NAME.pattern());
      return;
    }

    // TODO: what to do with invalid payload?
    ofNullable(message.getPayload())
        .map(ByteBuffer::wrap)
        .filter(it -> it.remaining() >= Double.BYTES)
        .map(ByteBuffer::getDouble)
        .map(Measure::new)
        .map(WindowMeasureEntryProcessor::new)
        .ifPresent(it -> map.executeOnKey(sensorId, it));
  }
}
