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

import static lombok.AccessLevel.PRIVATE;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xxlabaza.test.median.meter.MqttClientWrapper;
import com.xxlabaza.test.median.meter.Transportable;
import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;

import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;

@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class DistributedStorageServiceImpl<T extends Transportable> implements StorageService<T> {

  AtomicBoolean running;

  Partitions<T> partitions;

  DiscoveryServiceClient discoveryClient;

  MqttClientWrapper mqttClient;

  MemberOrderListener memberOrderListener;

  MqttRebalanceStrategy rebalanceStrategy;

  DistributedStorageServiceImpl (@NonNull DistributedStorageServiceProperties properties,
                                 @NonNull DiscoveryServiceClient discoveryClient
  ) {
    partitions = new Partitions<T>(properties.getPartitions());
    this.discoveryClient = discoveryClient;

    mqttClient = MqttClientWrapper.builder()
        .uri(properties.getMqtt().getUri())
        .username(properties.getMqtt().getUsername())
        .password(properties.getMqtt().getPassword())
        .build();

    memberOrderListener = new MemberOrderListener(discoveryClient);

    rebalanceStrategy = MqttRebalanceStrategy.builder()
        .discoveryClient(discoveryClient)
        .mqttClient(mqttClient)
        .build();

    running = new AtomicBoolean(false);
  }

  @Override
  public boolean isMyId (int id) {
    return getPartitionIndex(id) != -1;
  }

  @Override
  public T add (int id, T object) {
    val partitionIndex = getPartitionIndex(id);
    if (partitionIndex == -1) {
      val msg = String.format("ID {} doesn't belog to this storage", id);
      throw new IllegalArgumentException(msg);
    }

    val partition = partitions.getOrCreate(partitionIndex);
    return partition.put(id, object);
  }

  @Override
  public Optional<T> get (int id) {
    val partitionIndex = getPartitionIndex(id);
    if (partitionIndex == -1) {
      val msg = String.format("ID {} doesn't belog to this storage", id);
      throw new IllegalArgumentException(msg);
    }

    val partition = partitions.getOrCreate(partitionIndex);
    return partition.get(id);
  }

  @Override
  public StorageService<T> start () {
    if (!running.compareAndSet(false, true)) {
      return this;
    }

    rebalanceStrategy.start();
    memberOrderListener.start();
    mqttClient.connect();

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

    rebalanceStrategy.stop();
    memberOrderListener.stop();
    mqttClient.disconnect();
    partitions.clear();
  }

  @Override
  public void close () {
    stop();

    mqttClient.close();
    partitions.close();
  }

  private int getPartitionIndex (int id) {
    val partitionIndex = id % partitions.getCount();
    val membersCount = discoveryClient.getApplicationsCount();
    val myOrder = memberOrderListener.getOrder();
    return partitionIndex % membersCount == myOrder
           ? partitionIndex
           : -1;
  }
}
