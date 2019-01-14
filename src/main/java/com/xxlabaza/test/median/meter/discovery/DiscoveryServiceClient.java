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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.NonNull;
import lombok.val;

/**
 * Discovery service client for forming cluster and
 * getting the information about the applications in it.
 */
public interface DiscoveryServiceClient extends AutoCloseable {

  /**
   * Creates a new {@link DiscoveryServiceClient} instance from properties.
   *
   * @param map configuration properties.
   *
   * @return a new {@link DiscoveryServiceClient} instance object.
   */
  static DiscoveryServiceClient newInstance (@NonNull Map<String, Object> map) {
    val properties = MqttDiscoveryServiceClientProperties.of(map);
    return new MqttDiscoveryServiceClient(properties);
  }

  /**
   * Returns sorted list of the applications in a cluster.
   * <p>
   * Applications are sorted by creation timestamp (earlierst - the first).
   *
   * @return applications in a cluster.
   */
  List<Application> getAllApplication ();

  /**
   * Amount of applications in a cluster.
   *
   * @return applications count
   */
  int getApplicationsCount ();

  /**
   * {@link Application} instance, which represents this cluster's node.
   *
   * @return an {@link Application} instance.
   */
  Application self ();

  /**
   * Subscribes to a type event.
   *
   * @param type     type to handle.
   *
   * @param consumer event handler.
   *
   * @return subscription ID.
   */
  String subscribe (DiscoveryEvent.Type type, Consumer<DiscoveryEvent> consumer);

  /**
   * Subscribes to all type events.
   *
   * @param consumer event handler.
   *
   * @return map {@link DiscoveryEvent.Type} of an event to its subscription ID.
   */
  Map<DiscoveryEvent.Type, String> subscribe (Consumer<DiscoveryEvent> consumer);

  /**
   * Unsubscribe the client.
   *
   * @param type event type.
   *
   * @param id   subscription ID.
   */
  void unsubscribe (DiscoveryEvent.Type type, String id);

  /**
   * Starts the discovery service client.
   *
   * @return {@code this} instance
   */
  DiscoveryServiceClient start ();

  /**
   * Tells is this discovery service was started or not.
   *
   * @return {@code true} if service running, {@code false} otherwise.
   */
  boolean isRunning ();

  /**
   * Stops the discovery service client.
   */
  void stop ();

  @Override
  void close ();
}
