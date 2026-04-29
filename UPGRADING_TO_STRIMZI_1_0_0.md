# Upgrading to Access Operator 0.3.0 and Strimzi Cluster Operator 1.0.0

The v1 CRD API for resources such as `Kafka` and `KafkaUser` was introduced alongside the existing CRD API versions in the 
Strimzi Cluster Operator [v0.49.0 release](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/0.49.0). 
As of the [v1.0.0 release](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/1.0.0), it is the only CRD API 
version supported for those resources. 
You must be using Access Operator 0.3.0 or later before moving to Strimzi Cluster Operator 1.0.0 or later. 
This document describes how to coordinate upgrading the Strimzi Cluster Operator and Access Operator 
to these versions whilst ensuring the operators are always running with compatible versions.

## Version compatibility

* Access Operator v0.2.0 and earlier can interact with Kafka clusters managed by a Strimzi Cluster 
Operator v0.51.0 and earlier.
* Access Operator v0.3.0 and later can interact with Kafka clusters managed by Strimzi Cluster Operator v0.49.0 
and later.

## Upgrade steps

1. Upgrade Strimzi Cluster Operator to 0.49.0 or later, but no higher than 0.51.0.
2. Upgrade the Access Operator to version 0.3.0.
3. Wait for the Access Operator logs to include the message `Kafka Access operator is now ready`.
4. Verify the Access Operator logs **DO NOT** include the message `The client is using resource type 'kafkas' with unstable version 'v1beta2'`.
5. Upgrade Strimzi Cluster Operator to version 1.0.0 or later.
