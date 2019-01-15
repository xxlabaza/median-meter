# median-meter

[![build_status](https://travis-ci.org/xxlabaza/median-meter.svg?branch=master)](https://travis-ci.org/xxlabaza/median-meter)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## Contents

- [Description](#description)
  - [Nonfunctional requirements](#nonfunctional-requirements)
  - [MQTT protocol](#mqtt-protocol)
  - [Configuration file](#configuration-file)
- [Development](#development)
  - [Prerequisites](#prerequisites)
  - [Building](#building)
  - [Verify and run](#verify-and-run)
  - [Build docker image](#build-docker-image)
- [Built With](#built-with)
- [Changelog](#changelog)
- [Contributing](#contributing)
- [Versioning](#versioning)
- [Authors](#authors)
- [License](#license)

## Description

Design and implement a distributed temperature data-processing system evaluating a median temperature value for each device for a given time window. Temperature values are read from an MQTT message broker and submitted to a second MQTT broker (see protocol described below). MQTT brokers are out of the scope of this task, external services (considered reliable), credentials and URLs are given via config file mounted to the application docker container.
​
Sensor list is a diverse set of devices with integer IDs (up to 100 million instances).

Each device submits temperature values (in Celsius) with a variable random rate (1-10 times per sec) via MQTT protocol (qos = 1).

If there is no value submitted during 1 second from a single sensor system should delete last retained median temperature value from MQTT broker for a given device. Immediately after receiving the first value from a new device it should create a corresponding retained MQTT median value for a given device.

A median value time window is submitted to the system via a text-based configuration file (see file below).

The system should tolerate at least a single node failure (recover if a single instance becomes unavailable) and minimize rebalancing in case of changes of a number of nodes in the system (a kubernetes 'node').
​

### Nonfunctional requirements

- java-based solution;

- lean: minimise use of external libraries, keep only the necessary minimal set of tools;

- system should run on the top of a configured kubernetes cluster (k8s version: 1.11.2) running at least 3 nodes (on start).

### MQTT protocol

| Option name  | Inbound               | Outbound                     |
|-------------:|:----------------------|:-----------------------------|
| **topic**    | t/`<sensor_id>`       | median-t/`<sensor_id>`       |
| **message**  | `<temperature value>` | `<median temperature value>` |
| **qos**      | 1                     | 1                            |
| **retained** | flase                 | true                         |

### Configuration file

```yaml
temperatureTimeWindowInSec: <period_length_in_seconds> # up to 1000 seconds

# MQTT inbound client's settings
inbound:
 url: <url>
 user: <user>
 pass: <password>

# MQTT outbound client's settings
outbound:
 url: <url>
 user: <user>
 pass: <password>
```

## Development

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

For building the project you need only a [Java compiler](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

> **IMPORTANT:** `median-meter` was tested and running with Java **11**, but also should work with **8** version.

And, of course, you need to clone `median-meter` from GitHub:

```bash
$> git clone https://github.com/xxlabaza/median-meter
$> cd median-meter
```

### Building

For building routine automation, I am using [maven](https://maven.apache.org).

To build the `median-meter` project, do the following:

```bash
$> ./mvnw package
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.906 s
[INFO] Finished at: 2019-01-15T10:21:22+03:00
[INFO] ------------------------------------------------------------------------
```

### Verify and run

To verify (execute **test**, **integration test** and [PMD](.codestyle/pmd.xml)/[checkstyle](.codestyle/checkstyle.xml)/[spotbugs](.codestyle/findbugs.xml)) and run the project, do the following:

```bash
$> ./mvnw package verify; && java -jar target/median-meter-0.3.0.jar
...
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14.588 s
[INFO] Finished at: 2019-01-14T17:07:57+03:00
[INFO] ------------------------------------------------------------------------
Hello world
```

Also, if you do `package` or `install` goals, the tests launch automatically.

### Build docker image

```bash
$> ./mvnw package docker:build
...
[INFO] Building image xxlabaza/median-meter
Step 1/4 : FROM  xxlabaza/server_jre

 ---> 562ba6259a28
Step 2/4 : LABEL maintainer="Artem Labazin <xxlabaza@gmail.com>"

 ---> Running in f5731b55673e
Removing intermediate container f5731b55673e
 ---> 759fa3cd09bf
Step 3/4 : ADD *.jar app.jar

 ---> 1f98c030bb84
Step 4/4 : ENTRYPOINT ["java", "-jar", "/app.jar"]

 ---> Running in 44c0923b48ef
Removing intermediate container 44c0923b48ef
 ---> 8608d64b255a
ProgressMessage{id=null, status=null, stream=null, error=null, progress=null, progressDetail=null}
Successfully built 8608d64b255a
Successfully tagged xxlabaza/median-meter:latest
[INFO] Built xxlabaza/median-meter
[INFO] Tagging xxlabaza/median-meter with 0.3.0
[INFO] Tagging xxlabaza/median-meter with latest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:07 min
[INFO] Finished at: 2019-01-15T10:23:02+03:00
[INFO] ------------------------------------------------------------------------
```

## Built With

- [Java](http://www.oracle.com/technetwork/java/javase) - is a systems and applications programming language

- [Lombok](https://projectlombok.org) - is a java library that spicing up your java

- [Junit](http://junit.org/junit4/) - is a simple framework to write repeatable tests

- [AssertJ](http://joel-costigliola.github.io/assertj/) - AssertJ provides a rich set of assertions, truly helpful error messages, improves test code readability

- [Maven](https://maven.apache.org) - is a software project management and comprehension tool

## Changelog

To see what has changed in recent versions of `median-meter`, see the [changelog](./CHANGELOG.md) file.

## Contributing

Please read [contributing](./CONTRIBUTING.md) file for details on my code of conduct, and the process for submitting pull requests to me.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/xxlabaza/median-meter/tags).

## Authors

- **[Artem Labazin](https://github.com/xxlabaza)** - creator and the main developer

## License

This project is licensed under the Apache License 2.0 License - see the [license](./LICENSE) file for details
