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
- [Kubernetes](#kubernetes)
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
$> ./mvnw package verify; && java -jar target/median-meter-1.0.0.jar
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
[INFO] Tagging xxlabaza/median-meter with 1.0.0
[INFO] Tagging xxlabaza/median-meter with latest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:07 min
[INFO] Finished at: 2019-01-15T10:23:02+03:00
[INFO] ------------------------------------------------------------------------
```

## Kubernetes

First of all, we need to provide a configuration file for median-meter service. I use a `ConfigMap` for that purpose and create it from already prepared `configuration.yml` file:

```bash
$> kubectl create configmap median-meter-configmap \
     --from-file=./src/main/kubernetes/configuration.yml

configmap "median-meter-configmap" created
```

Then, we could create our deployment, which persists previously created `ConfigMap` as file in container's file system:

```bash
$> kubectl create \
     -f ./src/main/kubernetes/deployment.yml

deployment.apps "median-meter" created
```

To see, deployments and existing pods - enter the commands below:

```bash
$> kubectl get deployments
NAME           DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
median-meter   3         3         3            3           20s

$> kubectl get pods
NAME                            READY     STATUS    RESTARTS   AGE
median-meter-6d78dbccf4-64l9p   1/1       Running   0          21s
median-meter-6d78dbccf4-q9crj   1/1       Running   0          21s
median-meter-6d78dbccf4-xq5st   1/1       Running   0          21s
```

You also are able to see `pod`'s logs, like this:

```bash
$> kubectl logs median-meter-6d78dbccf4-64l9p
...
INFO: [172.17.0.3]:8913 [dev] [3.11.1] [172.17.0.3]:8913 is STARTED
2019-01-17 14:08:13.862  INFO   --- [           main] com.xxlabaza.test.median.meter.Main      : Application started
Jan 17, 2019 2:08:15 PM com.hazelcast.nio.tcp.TcpIpConnection
INFO: [172.17.0.3]:8913 [dev] [3.11.1] Initialized new cluster connection between /172.17.0.3:8913 and /172.17.0.5:34241
Jan 17, 2019 2:08:18 PM com.hazelcast.nio.tcp.TcpIpConnection
INFO: [172.17.0.3]:8913 [dev] [3.11.1] Initialized new cluster connection between /172.17.0.3:8913 and /172.17.0.4:37567
Jan 17, 2019 2:08:24 PM com.hazelcast.internal.cluster.ClusterService
INFO: [172.17.0.3]:8913 [dev] [3.11.1]

Members {size:3, ver:3} [
        Member [172.17.0.3]:8913 - 704004ad-49a2-48aa-9de6-e6f749c740c3 this
        Member [172.17.0.5]:8913 - 3e19c3d6-9026-4874-a312-5220c3fee2c3
        Member [172.17.0.4]:8913 - 8334ed1f-678c-439b-8e24-785243955eb6
]
```

To remove `pod`s and `ConfigMap` entity, just type the following commands:

```bash
$> kubectl delete \
      daemonsets,replicasets,services,deployments,pods,rc,configmap \
      --all

replicaset.extensions "median-meter-749674d764" deleted
service "kubernetes" deleted
deployment.extensions "median-meter" deleted
pod "median-meter-749674d764-6mv59" deleted
pod "median-meter-749674d764-6st6k" deleted
pod "median-meter-749674d764-8hjdg" deleted
pod "median-meter-749674d764-9grzw" deleted
pod "median-meter-749674d764-fs9f4" deleted
pod "median-meter-749674d764-l5jth" deleted
pod "median-meter-749674d764-nwkft" deleted
pod "median-meter-749674d764-qjt9c" deleted
pod "median-meter-749674d764-w8wmc" deleted
configmap "median-meter-configmap" deleted
```

All previous commands together:

```bash
> kubectl delete \
      daemonsets,replicasets,services,deployments,pods,rc,configmap \
      --all \
    && \
  kubectl create configmap median-meter-configmap \
      --from-file=./src/main/kubernetes/configuration.yml \
    && \
  kubectl create \
      -f ./src/main/kubernetes/deployment.yml \
    && \
  sleep 20 \
    && \
  kubectl get deployments \
    && \
  kubectl get pods
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
