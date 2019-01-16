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

package com.xxlabaza.test.median.meter.hazelcast;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MembershipEvent;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@EqualsAndHashCode
@FieldDefaults(level = PRIVATE, makeFinal = true)
class LeaderService {

  AtomicBoolean isLeader = new AtomicBoolean(false);

  List<BiConsumer<HazelcastInstance, MembershipEvent>> onBecomeLeaderMemberActions = new CopyOnWriteArrayList<>();

  List<BiConsumer<HazelcastInstance, MembershipEvent>> onBecomeRegularMemberActions = new CopyOnWriteArrayList<>();

  void addOnBecomeLeaderMemberAction (@NonNull BiConsumer<HazelcastInstance, MembershipEvent> action) {
    onBecomeLeaderMemberActions.add(action);
  }

  void addOnBecomeRegularMemberAction (@NonNull BiConsumer<HazelcastInstance, MembershipEvent> action) {
    onBecomeRegularMemberActions.add(action);
  }

  void becomeLeaderMember (@NonNull HazelcastInstance hazelcast, MembershipEvent event) {
    if (!isLeader.compareAndSet(false, true)) {
      log.debug("It is already a cluster leader");
      return;
    }
    log.debug("Become a cluster leader");
    onBecomeLeaderMemberActions.forEach(it -> it.accept(hazelcast, event));
  }

  void becomeRegularMember (@NonNull HazelcastInstance hazelcast, MembershipEvent event) {
    if (!isLeader.compareAndSet(true, false)) {
      log.debug("It is already a regula cluster member");
      return;
    }
    log.debug("Become a regula cluster member");
    onBecomeRegularMemberActions.forEach(it -> it.accept(hazelcast, event));
  }
}
