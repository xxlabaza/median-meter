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

import static com.xxlabaza.test.median.meter.function.BecomeLeaderMemberAction.SENSORS_LISTENING_TOPIC_FILTER;
import static com.xxlabaza.test.median.meter.function.BecomeLeaderMemberAction.getCreatedMqttClient;

import java.util.function.BiConsumer;

import com.xxlabaza.test.median.meter.MqttClientWrapper;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MembershipEvent;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Action handler on Hazelcast become a cluster regular member.
 */
@Slf4j
@Value
public class BecomeRegularMemberAction implements BiConsumer<HazelcastInstance, MembershipEvent> {

  @Override
  public void accept (@NonNull HazelcastInstance hazelcastInstance, @NonNull MembershipEvent event) {
    log.info("Node '{}' become a regular node", hazelcastInstance.getName());

    val userContext = hazelcastInstance.getUserContext();
    getCreatedMqttClient(userContext)
        .map(it -> {
          it.unsubscribe(SENSORS_LISTENING_TOPIC_FILTER);
          return it;
        })
        .ifPresent(MqttClientWrapper::disconnect);
  }
}
