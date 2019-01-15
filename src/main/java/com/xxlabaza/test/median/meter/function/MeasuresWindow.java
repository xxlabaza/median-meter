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

import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import com.xxlabaza.test.median.meter.Transportable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Represents a time window measures.
 */
@Slf4j
@ToString
@EqualsAndHashCode(of = "deviceId")
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MeasuresWindow implements Transportable, AutoCloseable {

  static final long WINDOW_MILLISECONDS = SECONDS.toMillis(1);

  private static final long serialVersionUID = -1002656752971481790L;

  @Getter
  int deviceId;

  Queue<Measure> queue = new PriorityQueue<>(new MeasureComparator());

  /**
   * Constructor.
   *
   * @param deviceId ID for which window is.
   */
  public MeasuresWindow (int deviceId) {
    if (deviceId < 0) {
      val msg = String.format("Device ID must be greater than 0, but it is %d",
                              deviceId);
      throw new IllegalArgumentException(msg);
    }
    this.deviceId = deviceId;
  }

  @Override
  public void close () {
    clear();
  }

  void clear () {
    queue.clear();
  }

  double median (@NonNull Measure measure) {
    queue.add(measure);
    removeExpiredMeasures();

    double[] array = queue.stream()
        .mapToDouble(Measure::getValue)
        .sorted()
        .toArray();

    log.debug("array - {}", array);

    val median = findMedian(array);
    log.debug("median is ", median);
    return median;
  }

  private void removeExpiredMeasures () {
    log.debug("queue - {}", queue);

    val expired = System.currentTimeMillis() - WINDOW_MILLISECONDS;
    val iterator = queue.iterator();
    while (iterator.hasNext()) {
      val measure = iterator.next();
      if (measure.getTimestamp() >= expired) {
        break;
      }
      iterator.remove();
      log.debug("removed - ", measure);
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

  // TODO: check - why static function Comparator.comparing(Measure::getTimestamp) doesn't work
  private class MeasureComparator implements Comparator<Measure>, Serializable {

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
