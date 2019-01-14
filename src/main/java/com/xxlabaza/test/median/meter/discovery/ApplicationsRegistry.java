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

import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.HEARTBEAT;
import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.NEW_APPLICATION;
import static com.xxlabaza.test.median.meter.discovery.DiscoveryEvent.Type.REMOVE_APPLICATION;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@ToString
@FieldDefaults(level = PRIVATE, makeFinal = true)
class ApplicationsRegistry implements AutoCloseable {

  AtomicBoolean running;

  Map<String, ApplicationWithScore> applications;

  DiscoveryEventProcessor eventProcessor;

  ScheduledExecutorService executor;

  long checkExpiredPeriodSeconds;

  @NonFinal
  String heartbeatListenerId;

  @NonFinal
  ScheduledFuture<?> taskFuture;

  @Builder
  ApplicationsRegistry (@NonNull DiscoveryEventProcessor eventProcessor,
                        long checkExpiredPeriodSeconds
  ) {
    this.eventProcessor = eventProcessor;
    this.checkExpiredPeriodSeconds = checkExpiredPeriodSeconds;
    applications = new ConcurrentHashMap<>();

    running = new AtomicBoolean(false);
    executor = Executors.newScheduledThreadPool(1);
  }

  @Override
  @SneakyThrows
  public void close () {
    stop();
    executor.shutdown();

    val terminated = executor.awaitTermination(1, SECONDS);
    log.debug("Successfully closed executor - {}", terminated);
  }

  void start () {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    heartbeatListenerId = eventProcessor.subscribe(HEARTBEAT, new HertbeatEventConsumer());

    taskFuture = executor.scheduleAtFixedRate(
        new RemoveExpiredApplicationsTask(),
        0,
        checkExpiredPeriodSeconds,
        SECONDS
    );
  }

  void stop () {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    eventProcessor.unsubscribe(HEARTBEAT, heartbeatListenerId);
    taskFuture.cancel(false);

    applications.clear();
  }

  Set<Application> applications () {
    return applications.values()
        .stream()
        .map(ApplicationWithScore::getApplication)
        .collect(toSet());
  }

  int size () {
    return applications.size() + 1; // plus self
  }

  private class HertbeatEventConsumer implements Consumer<DiscoveryEvent> {

    @Override
    public void accept (DiscoveryEvent event) {
      val application = event.getApplication();
      applications.compute(application.getId(), (key, value) -> {
        if (value == null) {
          eventProcessor.submit(NEW_APPLICATION, application);
          log.debug("A new application added to cluster\n{}", application);
          return new ApplicationWithScore(application);
        }
        value.resetScore();
        return value;
      });
    }
  }

  private class RemoveExpiredApplicationsTask implements Runnable {

    static final int MAX_SCORE = 3;

    @Override
    public void run () {
      val iterator = applications.entrySet().iterator();
      while (iterator.hasNext()) {
        val value = iterator.next().getValue();

        value.scoreUp();
        if (value.getScore() < MAX_SCORE) {
          continue;
        }

        iterator.remove();
        eventProcessor.submit(REMOVE_APPLICATION, value.getApplication());
        log.debug("Application don't send heartbeat messages, was removed\n{}", value.getApplication());
      }
    }
  }

  @Value
  @RequiredArgsConstructor
  private class ApplicationWithScore {

    @NonNull
    Application application;

    @NonFinal
    int score;

    void scoreUp () {
      score++;
    }

    void resetScore () {
      score = 0;
    }
  }
}
