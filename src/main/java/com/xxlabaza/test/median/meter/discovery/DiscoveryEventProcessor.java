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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class DiscoveryEventProcessor implements AutoCloseable {

  ExecutorService executor = Executors.newSingleThreadExecutor();

  Map<DiscoveryEvent.Type, Map<String, Consumer<DiscoveryEvent>>> listeners = new ConcurrentHashMap<>();

  @Override
  @SneakyThrows
  public void close () {
    listeners.values().forEach(Map::clear);
    listeners.clear();

    executor.shutdown();

    val terminated = executor.awaitTermination(1, SECONDS);
    log.debug("Successfully closed executor - {}", terminated);
  }

  String subscribe (@NonNull DiscoveryEvent.Type type, @NonNull Consumer<DiscoveryEvent> consumer) {
    log.debug("A new event '{}' listener added", type);

    val map = listeners.compute(type, (key, value) ->
        ofNullable(value).orElseGet(() -> new ConcurrentHashMap<>()));

    val id = UUID.randomUUID().toString();
    map.put(id, consumer);

    return id;
  }

  void submit (@NonNull DiscoveryEvent.Type type, @NonNull Application application) {
    val event = DiscoveryEvent.of(type, application);
    log.debug("A new event submitted\n{}", event);

    executor.execute(() -> ofNullable(event.getType())
        .map(listeners::get)
        .filter(Objects::nonNull)
        .map(Map::values)
        .orElse(emptyList())
        .forEach(it -> it.accept(event))
    );
  }

  void unsubscribe (@NonNull DiscoveryEvent.Type type, String id) {
    ofNullable(listeners.get(type))
        .ifPresent(map -> map.remove(id));
  }
}
