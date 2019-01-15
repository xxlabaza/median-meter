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

import static com.xxlabaza.test.median.meter.function.UserContextFactory.EXPIRE_TIME_MILLISECONDS;
import static com.xxlabaza.test.median.meter.function.UserContextFactory.OUTBOUND_MQTT_CLIENT;
import static com.xxlabaza.test.median.meter.function.UserContextFactory.OUTBOUND_MQTT_TOPIC_PREFIX;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.map.AbstractEntryProcessor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Data
@Slf4j
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@EqualsAndHashCode(callSuper = true)
class WindowMeasureEntryProcessor extends AbstractEntryProcessor<Integer, Queue<Measure>> implements HazelcastInstanceAware {

  private static final long serialVersionUID = -2112643784375407176L;

  transient long expireTimeMilliseconds;

  transient String outboundMqttTopicPrefix;

  transient MqttClientWrapper outboundMqttClient;

  Measure measure;

  WindowMeasureEntryProcessor (@NonNull Measure measure) {
    super();
    this.measure = measure;
  }

  @Override
  public void setHazelcastInstance (@NonNull HazelcastInstance hazelcastInstance) {
    val context = hazelcastInstance.getUserContext();

    expireTimeMilliseconds = ofNullable(context.get(EXPIRE_TIME_MILLISECONDS))
        .filter(it -> it instanceof Long)
        .map(it -> (Long) it)
        .orElse(SECONDS.toMillis(1));

    outboundMqttTopicPrefix = ofNullable(context.get(OUTBOUND_MQTT_TOPIC_PREFIX))
        .map(Object::toString)
        .orElse("median-t/");

    outboundMqttClient = ofNullable(context.get(OUTBOUND_MQTT_CLIENT))
        .filter(it -> it instanceof MqttClientWrapper)
        .map(it -> (MqttClientWrapper) it)
        .orElse(null);
  }

  @Override
  public Object process (@NonNull Entry<Integer, Queue<Measure>> entry) {
    Queue<Measure> queue = ofNullable(entry.getValue())
        .orElseGet(() -> new PriorityQueue<>(new MeasureComparator()));

    queue.add(measure);
    removeExpiredMeasures(queue);

    double[] array = queue.stream()
        .mapToDouble(Measure::getValue)
        .sorted()
        .toArray();

    log.debug("array - {}", array);

    double median = findMedian(array);
    sendMedian(entry.getKey(), median);

    entry.setValue(queue);
    return null;
  }

  private void removeExpiredMeasures (Queue<Measure> queue) {
    log.debug("queue - {}", queue);

    val expired = System.currentTimeMillis() - expireTimeMilliseconds;
    val iterator = queue.iterator();
    while (iterator.hasNext()) {
      val item = iterator.next();
      if (item.getTimestamp() >= expired) {
        break;
      }
      iterator.remove();
      log.debug("removed - {}", item);
    }
  }

  @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
  private double findMedian (@NonNull double[] array) {
    if (array.length == 0) {
      return 0.D; // maybe should throw an exception?
    } else if (array.length == 1) {
      return array[0];
    }

    val center = array.length / 2;
    if (array.length % 2 != 0) {
      return array[center];
    }

    val left = center - 1;
    val right = left + 1;
    return (array[left] + array[right]) / 2;
  }

  private void sendMedian (Integer deviceId, double median) {
    log.debug("Median temperature for device ID '{}' is '{}'",
              deviceId, median);

    ofNullable(outboundMqttClient)
        .ifPresent(it -> it.send(outboundMqttTopicPrefix + deviceId, median));
  }

  // TODO: check - why static function Comparator.comparing(Measure::getTimestamp) doesn't work
  private static class MeasureComparator implements Comparator<Measure>, Serializable {

    private static final long serialVersionUID = 5001656750971381790L;

    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public int compare (Measure left, Measure right) {
      if (left == right) {
        return 0;
      } else if (left == null) {
        return -1;
      } else if (right == null) {
        return 1;
      }
      return Long.compare(left.getTimestamp(), right.getTimestamp());
    }
  }
}
