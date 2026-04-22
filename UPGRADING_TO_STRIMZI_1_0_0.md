# Upgrading to Access Operator 0.3.0 and Strimzi cluser operator 1.0.0

The v1 CRD API for resources such as Kafka and KafkaUser was introduced alongside the existing CRD API versions in the 
Strimzi cluster operator [v0.49.0 release](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/0.49.0), and 
as of the [v1.0.0 release](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/1.0.0) it is the only CRD API 
version supported for those resources. You must be using Access Operator 0.3.0 or higher before moving to Strimzi cluster 
operator 1.0.0 and higher . This document describes how to coordinate upgrading the Strimzi cluster operator and Access Operator 
to these versions whilst maintaining availability.

## Version compatibility

* Access Operator v0.2.0 and earlier are able to interact with Kafka clusters managed by a Strimzi cluster 
operator v0.51.0 and earlier.
* Access Operator v0.3.0 and later are able to interact with Kafka clusters managed by a Strimzi cluster operator v0.49.0 
and later.

## Upgrading steps

1. Upgrade Strimzi cluster operator to at least 0.49.0, but no higher than 0.51.0.
2. Follow the steps to [convert the Strimzi custom resources to the v1 API](https://strimzi.io/docs/operators/0.51.0/deploying.html#assembly-api-conversion-str).
3. Upgrade Access Operator to 0.3.0.
4. Upgrade Strimzi cluster operator to 1.0.0 or higher.
