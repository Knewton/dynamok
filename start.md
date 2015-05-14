---
title: Getting Started
layout: page
---

### Including
#### Maven
```
<dependency>
    <groupId>com.knewton.dynamok</groupId>
    <artifactId>dynamok-scaling</artifactId>
    <version>0.0.1</version>
</dependency>
```
#### Gradle
```
'com.knewton.dynamok:dynamok-scaling:0.0.1'
```

### Building

Dynamok uses [Gradle](https://gradle.org/) - to build and install, simply run

```
gradle clean install
```

### Using

Before using Dynamok you must first ensure you have adequate access rights in AWS.  This can be done through the [AWS Console](http://aws.amazon.com/console/) and applying the appropriate [IAM](http://aws.amazon.com/iam/) settings.  Dynamok requires:

- DynamoDB (describeTable, updateTable)
- CloudWatch (getMetricStatistics)
- SNS (publish)

#### Quick Start

Dynamok provides a single service, DynamoScalingService, which is responsible for periodically polling Cloud Watch and updating throughput if necessary.  All that is required is to start the service and register your table(s).

In Java,

```java
// Create and start the service
ScalingServiceConfig serviceConfig = new ScalingServiceConfig();
AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretAccessKey);
DynamoScalingService service = new DynamoScalingService(serviceConfig,
                                                        new AWSClientFactory(credentials));
// Begin polling
service.start();

// Add a table to watch
IndexScalingConfig indexConfig = new IndexScalingConfig(DynamoIndex("users", ""));
service.addIndex(indexConfig);
```

Or if you're using Kotlin,

```kotlin
// Create and start the service
val serviceConfig = ScalingServiceConfig()
val credentials = BasicAWSCredentials(accessKey, secretAccessKey)
val service = DynamoScalingService(serviceConfig, AWSClientFactory(credentials))

// Begin polling
service.start()

// Add a table to watch
val indexConfig = IndexScalingConfig(DynamoIndex("users", ""))
service.addIndex(indexConfig)
```

In the above examples, the service is first started, and the "users" tables is added to watch with default configuration parameters.

And that's it.  Of course, you can customize how often to poll and when to scale up or down by adjusting the service configuration and the index configurations (see the configuration section below).

#### Configuration

Dynamok has two main configuration classes, ScalingServiceConfig and IndexScalingConfig.

###### ScalingServiceConfig

[ScalingServiceConfig.kt](https://github.com/Knewton/dynamok/blob/master/dynamok-scaling/src/main/kotlin/com/knewton/dynamok/config/ScalingServiceConfig.kt) contains configuration settings for the polling service, DynamoScalingService.

`checkIntervalSeconds`
Controls how often the service polls Cloud Watch for new table throughput usage information.  Since Cloud Watch updates data every five minutes, this should probably set to at least 300.

`notificationARN`
If this field is non-empty, Dynamok will publish notifications to the ARN set here.  Notifications will be sent for any unexpeced exceptions or when provisioned throughput has reached the maximum allowed throughput for a particular index (see IndexScalingConfig).

###### IndexScalingConfig

[IndexScalingConfig.kt](https://github.com/Knewton/dynamok/blob/master/dynamok-scaling/src/main/kotlin/com/knewton/dynamok/config/IndexScalingConfig.kt) contains configuration settings for each index you wish to submit to DynamoScalingService.  Be careful when choosing settings for this class, as a badly chosen configuration can lead to unwanted results (there is no validation on the config you enter here, so scaling up to tens of thousands of throughput is completely possible).

`index`
Index represents either a primary index or global secondary index (which have their own provisioning).

`minRead`
The lower bound of reads that the scaling service can update to (the scaling service will never update the table go lower than this limit even if usage is lower).

`maxRead`
The upper bound of reads that the scaling service can update to (the scaling service will never update the table go higher than this limit even if usage is higher).

`minWrite`
The lower bound of writes that the scaling service can update to (the scaling service will never update the table go lower than this limit even if usage is lower).

`maxRead`
The upper bound of writes that the scaling service can update to (the scaling service will never update the table go higher than this limit even if usage is higher).

`enableUpscale`
If true, allows the scaling service to increase provisioned throughput.  If false, the scaling service will not increase provisioned throughput.

`enableDownscale`
If true, allows the scaling service to decrease provisioned throughput.  If false, the scaling service will not decrease provisioned throughput.

`upscalePercent`
The percent of consumed capacity (consumed / provisioned) needed to trigger an upscale in throughput.  This value should be (0.0, 1.0) and higher than downscalePercent.

`downscalePercent`
The percent of consumed capacity (consumed / provisioned) needed to trigger an downscale in throughput.  This value should be (0.0, 1.0) and lower than upscalePercent.

`scaleUpFactor`
The amount to scale the provisioned throughput by when upscaling.  The new throughput will be equal to (currentThroughput * (1 + scaleUpFactor)), clamped between min and max read / write.  This value should be between (0.0, 1.0).

`scaleDownFactor`
The amount to scale the provisioned throughput by when downscaling.  The new throughput will be equal to (currentThroughput * (1 - scaleUpFactor)), clamped between min and max read / write.  This value should be between (0.0, 1.0).

`downscaleWaitMinutes`
Configures the amount of time to wait after the last update throughput operation on the index (downscales will not occur until this time has elapsed since the last update, however upscales may still occur, resetting the elapsed time).  This is used to throttle how often downscales occur since AWS limits the number of downscales to 4 per 24 hours.

---

###### Overriding

At any point, you can manually override a configuration by going into [AWS Console](http://aws.amazon.com/console/) and manually setting either read or write provisioned throughput to be higher or lower than the configured limit for a particular index.  In this case, the scaling service will stop updating that index until the throughput is manually readjusted again to fall in between the min and max reads / writes.

You can also programatically temporally remove the index from the scaling service by calling `DynamoScalingService.removeIndex` and re-add it later by calling `DynamoScalingService.addIndex`.
