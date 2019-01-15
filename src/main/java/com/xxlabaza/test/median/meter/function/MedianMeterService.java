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

import static lombok.AccessLevel.PRIVATE;

import java.util.Map;

import com.xxlabaza.test.median.meter.storage.StorageService;

import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Service for searching median devices values.
 */
public interface MedianMeterService extends AutoCloseable {

  /**
   * Creates builder object.
   *
   * @return a new builder.
   */
  static MedianMeterServiceBuilder builder () {
    return new MedianMeterServiceBuilder();
  }

  /**
   * Starts median meter service.
   *
   * @return {@code this} instance
   */
  MedianMeterService start ();

  /**
   * Tells is this median meter service was started or not.
   *
   * @return {@code true} if service running, {@code false} otherwise.
   */
  boolean isRunning ();

  /**
   * Stops the median meter service.
   */
  void stop ();

  /**
   * Builder object for holding user's settings and
   * constructing a new {@link MedianMeterService} instance.
   */
  @FieldDefaults(level = PRIVATE)
  @SuppressWarnings("checkstyle:HiddenField")
  class MedianMeterServiceBuilder {

    Map<String, Object> map;

    StorageService<MeasuresWindow> storageService;

    /**
     * Sets parsed properties.
     *
     * @param map properties.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public MedianMeterServiceBuilder properties (Map<String, Object> map) {
      this.map = map;
      return this;
    }

    /**
     * Sets storage service implementation.
     *
     * @param storageService storage.
     *
     * @return {@code this} builder object for chaining calls.
     */
    public MedianMeterServiceBuilder storage (@NonNull StorageService<MeasuresWindow> storageService) {
      this.storageService = storageService;
      return this;
    }

    /**
     * Builds a new {@link MedianMeterService} instance.
     *
     * @return a new {@link MedianMeterService} instance.
     */
    public MedianMeterService build () {
      val properties = MqttMedianMeterServiceProperties.of(map);
      return new MqttMedianMeterServiceImpl(properties, storageService);
    }
  }
}
