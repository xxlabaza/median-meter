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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.xxlabaza.test.median.meter.discovery.DiscoveryServiceClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@SuppressWarnings({
    "PMD.AvoidLiteralsInIfCondition",
    "PMD.DoNotCallSystemExit"
})
public final class Main {

  public static void main (String[] args) throws IOException, InterruptedException {
    if (args.length != 1) {
      log.error("A user must provide exactly one argument - a path to a configuration YAML-file");
      System.exit(1);
    }

    val path = Paths.get(args[0]);
    if (Files.notExists(path) || Files.isDirectory(path)) {
      log.error("A configuration file '{}' doesn't exists", path);
      System.exit(1);
    }

    val yaml = new Yaml();
    Map<String, Object> properties;
    try (val inputStream = Files.newInputStream(path)) {
      properties = yaml.load(inputStream);
    }

    try (val discoveryClient = DiscoveryServiceClient.newInstance(properties)) {
      discoveryClient.start();
      SECONDS.sleep(5);
      log.info("Is discovery client running - {}\n{}",
               discoveryClient.isRunning(), discoveryClient.getAllApplications());
    }
  }

  private Main () {
    throw new UnsupportedOperationException();
  }
}
