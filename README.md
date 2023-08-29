[![Build Status](https://dev.azure.com/cncf/strimzi/_apis/build/status%2Faccess-operator%2Faccess-operator?branchName=main)](https://dev.azure.com/cncf/strimzi/_build/latest?definitionId=51&branchName=main)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# Access Operator for Strimzi

Kubernetes operator that collects connection details for an [Apache Kafka®](https://kafka.apache.org) cluster that is managed by the Strimzi cluster 
operator into a Kubernetes secret. (See [proposal #33](https://github.com/strimzi/proposals/blob/main/033-service-binding.md) for more details.)

This operator uses the [Java Operator SDK](https://github.com/java-operator-sdk/java-operator-sdk).

## Building the operator

The [Dev guide](https://github.com/strimzi/kafka-access-operator/blob/main/development-docs/DEV_GUIDE.md) describes how to build the Access Operator.
