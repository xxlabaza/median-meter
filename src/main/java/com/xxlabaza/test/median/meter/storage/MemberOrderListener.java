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

package com.xxlabaza.test.median.meter.storage;

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.NEW_APPLICATION;
import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.REMOVE_APPLICATION;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.xxlabaza.test.median.meter.discovery.DiscoveryEvent;
import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;

@ToString
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class MemberOrderListener {

  AtomicInteger order = new AtomicInteger(0);

  Map<DiscoveryEvent.Type, String> subscriptionIds = new ConcurrentHashMap<>();

  DiscoveryServiceClient discoveryClient;

  void start () {
    setUpOrder();
    discoveryClient.subscribe(new AddOrRemoveMemberListener()).forEach((key, value) -> {
      subscriptionIds.put(key, value);
    });
  }

  int getOrder () {
    return order.get();
  }

  void stop () {
    subscriptionIds.forEach((type, id) -> discoveryClient.unsubscribe(type, id));
    subscriptionIds.clear();
  }

  private void setUpOrder () {
    val members = discoveryClient.getAllApplications();
    val self = discoveryClient.self();

    for (int index = 0; index < members.size(); index++) {
      if (members.get(index).equals(self)) {
        order.set(index);
        break;
      }
    }
  }

  private class AddOrRemoveMemberListener implements Consumer<DiscoveryEvent> {

    @Override
    public void accept (DiscoveryEvent event) {
      val type = event.getType();
      if (type != NEW_APPLICATION && type != REMOVE_APPLICATION) {
        return;
      }
      setUpOrder();
    }
  }
}
