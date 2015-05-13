---
title: Home
layout: page
---

## What is Dynamok?

From the [README](https://github.com/Knewton/dynamok/blob/master/README.md),

> Dynamok is a library providing automatic provisioned throughput scaling for Amazon's DynamoDB service. This allows you to lower provisioned throughput for a Dynamo table when usage is low and automatically increase it when usage is high.

### Features

- Dynamok is written in [Kotlin](http://kotlinlang.org/), and is compatible and easy to use with all JVM languages (in particular, [Java is fully compatible with Kotlin](http://kotlinlang.org/docs/reference/java-interop.html)).

- Dynamok supports separate configurations (when and how much to scale provisioning) on a per index (primary index or global secondary index) basis.  Configurations can also be modified or added / removed on the fly.

- SNS notifications can be configured so that Dynamok will notify you when an error occurs.

- Minimal dependencies (just Kotlin, SL4FJ, and AWS)

---

To begin using Dynamok, see the [getting started](/dynamok/start/) guide.

If you have questions or wish to report bugs / request features, feel free to [open an issue](https://github.com/Knewton/dynamok/issues).  Dynamok is open source and [licensed under the Apache 2.0 License](/about).
