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

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.Map;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class CustomDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

  DiscoveryServiceClient discoveryClient;

  @Override
  public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType () {
    return MqttDiscoveryStrategy.class;
  }

  @Override
  public DiscoveryStrategy newDiscoveryStrategy (DiscoveryNode discoveryNode,
                                                 ILogger logger,
                                                 Map<String, Comparable> properties
  ) {
    return MqttDiscoveryStrategy.builder()
        .discoveryClient(discoveryClient)
        .logger(logger)
        .properties(properties)
        .build();
  }

  @Override
  public Collection<PropertyDefinition> getConfigurationProperties () {
    return emptyList();
  }
}
