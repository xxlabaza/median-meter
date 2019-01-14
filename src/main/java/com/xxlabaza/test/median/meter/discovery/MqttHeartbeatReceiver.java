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

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.HEARTBEAT;
import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.atomic.AtomicBoolean;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MqttHeartbeatReceiver {

  AtomicBoolean running;

  Application self;

  MqttClientWrapper mqttClient;

  String heartbeatTopicFilter;

  DiscoveryEventProcessor eventProcessor;

  @Builder
  MqttHeartbeatReceiver (@NonNull Application self,
                         @NonNull MqttClientWrapper mqttClient,
                         @NonNull DiscoveryEventProcessor eventProcessor,
                         @NonNull String heartbeatTopicPrefix
  ) {
    this.self = self;
    this.mqttClient = mqttClient;
    this.heartbeatTopicFilter = heartbeatTopicPrefix + '+';
    this.eventProcessor = eventProcessor;

    running = new AtomicBoolean(false);
  }

  void start () {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    mqttClient.listen(heartbeatTopicFilter, new Receiver());
  }

  void stop () {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    mqttClient.removeListeners(heartbeatTopicFilter);
  }

  private class Receiver implements IMqttMessageListener {

    @Override
    public void messageArrived (String topic, MqttMessage message) throws Exception {
      val payload = message.getPayload();
      val application = Application.from(payload);
      if (application.equals(self)) {
        return;
      }
      eventProcessor.submit(HEARTBEAT, application);
      log.debug("A new application's heartbeat, from topic '{}', has arrived\n{}", topic, application);
    }
  }
}
