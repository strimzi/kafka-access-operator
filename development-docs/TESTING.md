# Running the system-tests

This document gives a guide how to run the system-tests in the Kafka Access Operator repository.

## Pre-requisites

For running the STs, you will need a Kubernetes environment.
In case that you want to run the tests on your local machine, you can use [minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/) as we do in the Azure pipelines.
Or you can be logged into the remote cluster (for example OpenShift or any other cluster that is available for you) and 
the test automation will deploy everything and run the test cases there.

The following requirement is to have built the `systemtest` package dependency, which is the `api` module.
You can achieve that by running the

```bash
mvn clean install -DskipTests
```

command in the root of the repository, or alternatively run the command for the `systemtest` module, just with `-am` flag:

```bash
mvn clean install -pl systemtest -DskipTests -am
```

The `api` module is needed, because we are using the classes like `KafkaAccessBuilder` in the tests.

## Environment variables

The system-tests are also allowing to specify multiple environment variables, by which you can configure how the tests
should be executed.
You can configure the install type (Yaml or Helm), but also Docker registry, organisation, and tag.
The following table shows full list of environment variables that you can configure:

| Name            | Description                                                             | Default |
|:----------------|:------------------------------------------------------------------------|:--------|
| DOCKER_REGISTRY | Specifies the Docker registry where the images are located              | None    |
| DOCKER_ORG      | Specifies the organization/repository containing the image              | None    |
| DOCKER_TAG      | Specifies the image tag                                                 | None    |
| INSTALL_TYPE    | Specifies the method how the KAO should be installed - `Yaml` or `Helm` | Yaml    |

The default image for the KAO is used from [050-Deployment.yaml](../packaging/install/050-Deployment.yaml), so in case that you
don't specify one of the `DOCKER_REGISTRY`, `DOCKER_ORG`, or `DOCKER_TAG`, the default from the file will be used.

## Running the tests

To run the tests, you can use your IDE, which should show you the option to run the tests (in IntelliJ IDEA it's the "play" button)
or you can run the following Maven command:

```bash
mvn verify -pl systemtest -Pall
```

With `-pl` flag, you specify that you want to run the tests inside the `systemtest` repository.
The `-Pall` is needed, because by default the `skipTests` property is set to `true` in this repository - so the system-tests 
are not executed with every build of the project.
The `all` profile contains configuration of `skipTests` to `false`, so the tests will be executed.
You can alternatively run the command with `-DskipTests=false` instead of `-Pall`.

```bash
mvn verify -pl systemtest -DskipTests=false
```

These two commands will execute all system-tests present in the module.
In case that you would like to run just one specific test, you can run the following command:

```bash
 mvn verify -pl systemtest -Pall -Dit.test=CLASS_NAME#TEST_CASE_NAME
```

for example:
```bash
 mvn verify -pl systemtest -Pall -Dit.test=KafkaAccessOperatorST#testAccessToUnspecifiedMultipleListenersWithMultipleInternal
```

You can specify multiple test cases similarly, you just need to separate them by comma.