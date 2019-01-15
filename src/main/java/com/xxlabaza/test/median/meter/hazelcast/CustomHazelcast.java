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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MembershipEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Utility class for creating custom Hazelcast instances.
 */
public final class CustomHazelcast {

  /**
   * Creates custom hazelcast builder.
   *
   * @return a new builder object.
   */
  public static CustomHazelcastBuilder builder () {
    return new CustomHazelcastBuilder();
  }

  private CustomHazelcast () {
    throw new UnsupportedOperationException();
  }

  /**
   * Builder object for holding user's settings and
   * constructing a nea hazelcast instance.
   */
  @FieldDefaults(level = PRIVATE)
  @NoArgsConstructor(access = PRIVATE)
  @SuppressWarnings("checkstyle:HiddenField")
  public static final class CustomHazelcastBuilder {

    Config config;

    List<MapConfig> mapConfigs = new ArrayList<>();

    DiscoveryServiceClient discoveryClient;

    BiConsumer<HazelcastInstance, MembershipEvent> onBecomeMasterAction;

    ConcurrentMap<String, Object> userContext;

    /**
     * Sets user's Hazelcast {@link Config} object.
     * <p>
     * It is an optional parameter - it creates automatically, if not set.
     *
     * @param config user's config for further customization.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public CustomHazelcastBuilder config (@NonNull Config config) {
      this.config = config;
      return this;
    }

    /**
     * Sets {@link DiscoveryServiceClient} object.
     *
     * @param discoveryClient user's specific discovery service.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public CustomHazelcastBuilder discoveryClient (@NonNull DiscoveryServiceClient discoveryClient) {
      this.discoveryClient = discoveryClient;
      return this;
    }

    /**
     * Sets an action handler, which invokes,
     * when Hazelcast instance become a cluster master.
     *
     * @param handler user's handler.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public CustomHazelcastBuilder onBecomeMasterAction (@NonNull BiConsumer<HazelcastInstance, MembershipEvent> handler) {
      this.onBecomeMasterAction = handler;
      return this;
    }

    /**
     * Sets a user context for further accessing it in,
     * for example, {@link com.hazelcast.map.EntryProcessor}s.
     *
     * @param userContext user's shared context.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public CustomHazelcastBuilder userContext (@NonNull ConcurrentMap<String, Object> userContext) {
      this.userContext = userContext;
      return this;
    }

    /**
     * Sets Hazelcast's map config. Could be many items.
     *
     * @param mapConfig specific map configuration.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public CustomHazelcastBuilder mapConfig (@NonNull MapConfig mapConfig) {
      mapConfigs.add(mapConfig);
      return this;
    }

    /**
     * Creates a new Hazelcast instance, based on previously setted objects.
     *
     * @return a new Hazelcast instance.
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public HazelcastInstance ignite () { // nice name for starting HZ))
      HazelcastInstance result = null;

      if (config == null) {
        config = new Config();
      }

      if (onBecomeMasterAction != null) {
        val listener = new ClusterFormationChangeListener(result, onBecomeMasterAction);
        val listenerConfig = new ListenerConfig(listener);
        config.addListenerConfig(listenerConfig);
      }

      if (discoveryClient != null) {
        val selfAddress = discoveryClient.self().getAddress().getHostAddress();

        config.setProperty("hazelcast.discovery.enabled", "true");
        config.setProperty("hazelcast.socket.server.bind.any", "true");
        config.setProperty("hazelcast.socket.bind.any", "true");

        val networkingConfig = config.getNetworkConfig();
        networkingConfig.setPublicAddress(selfAddress);
        networkingConfig.setPort(discoveryClient.self().getPort());
        networkingConfig.setPortAutoIncrement(false);
        networkingConfig.setInterfaces(new InterfacesConfig().addInterface(selfAddress));

        val joinConfig = networkingConfig.getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);

        val discoveryStrategyFactory = new CustomDiscoveryStrategyFactory(discoveryClient);
        val discoveryStrategyConfig = new DiscoveryStrategyConfig(discoveryStrategyFactory);
        joinConfig.getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);
      }

      ofNullable(userContext)
          .ifPresent(config::setUserContext);

      mapConfigs
          .forEach(config::addMapConfig);

      result = Hazelcast.newHazelcastInstance(config);
      return result;
    }
  }
}
