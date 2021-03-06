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

package com.xxlabaza.test.median.meter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;
import com.xxlabaza.test.median.meter.function.BecomeLeaderMemberAction;
import com.xxlabaza.test.median.meter.function.BecomeRegularMemberAction;
import com.xxlabaza.test.median.meter.function.UserContextFactory;
import com.xxlabaza.test.median.meter.hazelcast.CustomHazelcast;

import com.hazelcast.config.MapConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.yaml.snakeyaml.Yaml;

/**
 * Application's entry point class.
 */
@Slf4j
@SuppressWarnings({
    "PMD.AvoidLiteralsInIfCondition",
    "PMD.DoNotCallSystemExit"
})
public final class Main {

  /**
   * Entry point - parses CLI arguments, YAML-configuration and launches the App.
   *
   * @param args application's CLI arguments.
   *
   * @throws IOException in case of any error during configuration reading.
   */
  public static void main (String[] args) throws IOException {
    if (args.length != 1) {
      log.error("A user must provide exactly one argument - a path to a configuration YAML-file");
      System.exit(1);
    }

    val path = Paths.get(args[0]);
    if (Files.notExists(path) || Files.isDirectory(path)) {
      log.error("A configuration file '{}' doesn't exists or is is not a file", path);
      System.exit(1);
    }

    val yaml = new Yaml();
    Map<String, Object> properties;
    try (val inputStream = Files.newInputStream(path)) {
      properties = yaml.load(inputStream);
    }

    log.info("Starting application");
    startApplication(properties);
  }

  /**
   * Starts application.
   *
   * @param properties user's configuration properties.
   */
  public static void startApplication (@NonNull Map<String, Object> properties) {
    val mapName = "popa";

    CustomHazelcast.builder()
        .discoveryClient(DiscoveryServiceClient.newInstance(properties).start())
        .userContext(UserContextFactory.create(properties))
        .onBecomeLeaderMemberAction(new BecomeLeaderMemberAction(mapName))
        .onBecomeRegularMemberAction(new BecomeRegularMemberAction())
        .mapConfig(new MapConfig()
          .setName(mapName)
          .setAsyncBackupCount(1)
          .setTimeToLiveSeconds(3)
        )
        .ignite(); // he-he=)

    log.info("Application started");
  }

  private Main () {
    throw new UnsupportedOperationException();
  }
}
