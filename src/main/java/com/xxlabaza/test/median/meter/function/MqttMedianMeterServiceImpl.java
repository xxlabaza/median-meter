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

import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.atomic.AtomicBoolean;

import com.xxlabaza.test.median.meter.MqttClientWrapper;
import com.xxlabaza.test.median.meter.storage.StorageService;

import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MqttMedianMeterServiceImpl implements MedianMeterService {

  private static final String SENSORS_LISTENING_TOPIC_FILTER = "t/+";

  private static final String OUTBOUND_TOPIC_PREFIX = "median-t/";

  AtomicBoolean running;

  StorageService<MeasuresWindow> storage;

  MqttClientWrapper inbound;

  MqttClientWrapper outbound;

  MqttSensorsListener listener;

  MqttMedianMeterServiceImpl (@NonNull MqttMedianMeterServiceProperties properties,
                              @NonNull StorageService<MeasuresWindow> storage
  ) {
    this.storage = storage;

    inbound = MqttClientWrapper.builder()
        .uri(properties.getInbound().getUri())
        .username(properties.getInbound().getUsername())
        .password(properties.getInbound().getPassword())
        .build();

    outbound = MqttClientWrapper.builder()
        .uri(properties.getOutbound().getUri())
        .username(properties.getOutbound().getUsername())
        .password(properties.getOutbound().getPassword())
        .build();

    listener = MqttSensorsListener.builder()
        .storage(storage)
        .outbound(outbound)
        .outboundTopicPrefix(OUTBOUND_TOPIC_PREFIX)
        .build();

    running = new AtomicBoolean(false);
  }

  @Override
  public MedianMeterService start () {
    if (!running.compareAndSet(false, true)) {
      return this;
    }

    storage.start();

    inbound.connect();
    outbound.connect();

    inbound.listen(SENSORS_LISTENING_TOPIC_FILTER, listener);

    return this;
  }

  @Override
  public boolean isRunning () {
    return running.get();
  }

  @Override
  public void stop () {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    inbound.removeListeners(SENSORS_LISTENING_TOPIC_FILTER);

    inbound.disconnect();
    outbound.disconnect();

    storage.stop();
  }

  @Override
  public void close () {
    stop();

    inbound.close();
    outbound.close();
    storage.close();
  }
}
