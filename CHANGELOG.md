# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

[Tags on this repository](https://github.com/xxlabaza/median-meter/tags)

## [Unreleased]

\-

## [1.0.0](https://github.com/xxlabaza/median-meter/releases/tag/1.0.0) - 2019-01-15

Add Kubernetes support.

### Added

- Write Kubernetes deploy file;
- Add `LeaderService` to Hazelcast cluster, which is responsible for calling different handlers on becoming leader/regular member;
- Complex (discovery, hazelcast, median logic) test;
- `pom.xml` parameter `skipAllTests` - for skipping all tests and checking, for example, `PMD`, `spotbugs` or `checkstyle`.

### Changed

- Separate CLI-arguments parsing and app launching;
- `MqttClientWrapper` has a caching static method for building a client;
- `MqttClientWrapper` has a registry of all subscriptions;
- Don't disconnect from `MQTT`, when `MqttDiscoveryServiceClient` is stopped;
- Add more debug logs;
- Properly stop/close services (add null-checks);
- Add configuration argument to `Dockerfile`.

## [0.3.0](https://github.com/xxlabaza/median-meter/releases/tag/0.3.0) - 2019-01-15

Create median meter function.

### Added

- Median function implementation.

## [0.2.0](https://github.com/xxlabaza/median-meter/releases/tag/0.2.0) - 2019-01-15

Use Hazelcast for application clusterization.

### Added

- Custom Hazelcast cluster, which use `MQTT`-based service discovery;
- Unit and integration tests;
- JavaDocs.

### Changed

- Enable JavaDoc's checkstyle rules;
- Maven dependencies.

## [0.1.0](https://github.com/xxlabaza/median-meter/releases/tag/0.1.0) - 2019-01-14

Discovery.

### Added

- Service discovery interface and its `MQTT` implementation;
- Unit and integration tests;
- JavaDocs.

### Changed

- Maven dependencies.

## [0.0.1](https://github.com/xxlabaza/median-meter/releases/tag/0.0.1) - 2019-01-14

Initial release
