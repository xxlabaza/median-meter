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

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;
import com.xxlabaza.test.median.meter.function.MeasuresWindow;

import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Service for storing data.
 *
 * @param <T> serializable type of stored objects.
 */
public interface StorageService<T extends Serializable> extends AutoCloseable {

  /**
   * Creates builder object.
   *
   * @return a new builder.
   */
  static StorageServiceBuilder builder () {
    return new StorageServiceBuilder();
  }

  /**
   * Checks if ID belongs to this storage, or not.
   *
   * @param id ID for check.
   *
   * @return {@code true} if ID belongs to this storage,
   *         {@code false} otherwise.
   */
  boolean isMyId (int id);

  /**
   * Adds a new object to the storage.
   *
   * @param id     object's ID.
   *
   * @param object value.
   *
   * @return added object.
   */
  T add (int id, T object);

  /**
   * Returns an stored object by its ID.
   *
   * @param id object's ID
   *
   * @return optional object value.
   */
  Optional<T> get (int id);

  /**
   * Starts storage service.
   *
   * @return {@code this} instance
   */
  StorageService<T> start ();

  /**
   * Tells is this storage service was started or not.
   *
   * @return {@code true} if service running, {@code false} otherwise.
   */
  boolean isRunning ();

  /**
   * Stops the storage service.
   */
  void stop ();

  @Override
  void close ();

  /**
   * Builder object for holding user's settings and
   * constructing a new {@link StorageService} instance.
   */
  @FieldDefaults(level = PRIVATE)
  @SuppressWarnings("checkstyle:HiddenField")
  class StorageServiceBuilder {

    Map<String, Object> map;

    DiscoveryServiceClient discoveryClient;

    /**
     * Sets parsed properties.
     *
     * @param map properties.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public StorageServiceBuilder properties (Map<String, Object> map) {
      this.map = map;
      return this;
    }

    /**
     * Sets discovery client implementation.
     *
     * @param discoveryClient service discovery client.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public StorageServiceBuilder clusterClient (@NonNull DiscoveryServiceClient discoveryClient) {
      this.discoveryClient = discoveryClient;
      return this;
    }

    /**
     * Builds a new {@link StorageService} instance.
     *
     * @return a new {@link StorageService} instance.
     */
    public StorageService<MeasuresWindow> build () {
      val properties = DistributedStorageServiceProperties.of(map);
      return new DistributedStorageServiceImpl<>(properties, discoveryClient);
    }
  }
}
