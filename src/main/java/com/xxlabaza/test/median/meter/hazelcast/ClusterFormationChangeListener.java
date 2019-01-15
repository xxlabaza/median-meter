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

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class ClusterFormationChangeListener implements MembershipListener {

  @Nullable
  HazelcastInstance hazelcastInstance;

  @NonNull
  BiConsumer<HazelcastInstance, MembershipEvent> action;

  @Override
  public void memberAdded (MembershipEvent membershipEvent) {
    process(membershipEvent);
  }

  @Override
  public void memberRemoved (MembershipEvent membershipEvent) {
    process(membershipEvent);
  }

  @Override
  public void memberAttributeChanged (MemberAttributeEvent memberAttributeEvent) {
    // no op
  }

  private void process (MembershipEvent event) {
    val iterator = event.getMembers().iterator();
    val leader = iterator.next();
    val localMember = event.getCluster().getLocalMember();
    if (leader.equals(localMember)) {
      action.accept(hazelcastInstance, event);
    }
  }
}
