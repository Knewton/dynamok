# Dynamok

[![Build Status](https://travis-ci.org/Knewton/dynamok.svg)](https://travis-ci.org/Knewton/dynamok)
[![Coverage Status](https://coveralls.io/repos/Knewton/dynamok/badge.svg?branch=master)](https://coveralls.io/r/Knewton/
dynamok?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.knewton.dynamok/dynamok-scaling/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.knewton/dynamok-scaling/)
## Maintainers

This project is maintained by

* [Paul Sastrasinh](https://github.com/psastras)
* [Celia La](https://github.com/celiala)
* [Rob Murcek](https://github.com/rmurcek)

## What is Dynamok?
Dynamok is a library providing automatic provisioned throughput scaling for Amazon's DynamoDB service.  This allows you to lower provisioned throughput for a Dynamo table when usage is low and automatically increase it when usage is high.

Dynamok shares much in common with https://github.com/sebdah/dynamic-dynamodb.  The main difference is that dynamok is a Java library where as dynamic-dynamodb is a python service. Since this is a Java library, it can be easier to integrate to an existing Java service, and allows easy on-the-fly configuration changes from Java (or other JVM languages).

Dynamok is written in [Kotlin](http://kotlinlang.org/), and is fully compatible with Java.

## Maven or Gradle

```
<dependency>
    <groupId>com.knewton.dynamok</groupId>
    <artifactId>dynamok-scaling</artifactId>
    <version>0.0.1</version>
</dependency>
```

```
'com.knewton.dynamok:dynamok-scaling:0.0.1'
```

## Building

Dynamok uses [Gradle](https://gradle.org/) - to build, simply run:
```
gradle clean build
```

## Example Usage

For more detailed information about including and using Dynamok in your project, see the Documentation section.

Dynamok provides a single service (DynamoScalingService) which is responsible for periodically polling Cloud Watch and updating throughput if necessary.  All that is required is to start the service and register your table(s).

```java
// Create and start the service
ScalingServiceConfig serviceConfig = new ScalingServiceConfig();
AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretAccessKey);
DynamoScalingService service = new DynamoScalingService(serviceConfig,
                                                        new AWSClientFactory(credentials));
service.start();

// Add a table to watch
IndexScalingConfig indexConfig = new IndexScalingConfig(DynamoIndex("users", ""));
service.addIndex(indexConfig);
```

The above code starts the service, then adds the table "users" to watch.  "users" will be polled every five minutes by default (configurable in the ScalingServiceConfig).  If throughput consumption meets certain criteria (specified in IndexScalingConfig), the throughput will be updated.

## Documentation

Documentation may be found on the project site: [knewton.github.io/dynamok](http://knewton.github.io/dynamok/).

## Reporting Bugs and Requesting Features

You may file issues to report bugs or request certain features.  We will also accept contributions via pull requests to fix bugs or add new features.

## License
Dynamok is licensed under the Apache 2.0 license.

Copyright 2015 Knewton

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
