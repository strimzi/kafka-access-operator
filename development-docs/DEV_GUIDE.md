# Development Guide for Access Operator

This document gives a detailed breakdown of the various build processes and options for building the Access Operator from source.

<!-- TOC depthFrom:2 -->

- [Build Pre-Requisites](#build-pre-requisites)
- [Build and deploy from source](#build-and-deploy-from-source)
- [Build details](#build-details)
   - [Make targets](#make-targets)
   - [Java versions](#java-versions)
   - [Java build options](#java-build-options)
   - [Docker build options](#docker-build-options)
   - [Tagging and pushing Docker image](#tagging-and-pushing-docker-image)
   - [Local build on Minikube](#local-build-on-minikube)
- [DCO Signoff](#dco-signoff)
- [Building container images for other platforms with Docker `buildx`](#building-container-images-for-other-platforms-with-docker-buildx)

<!-- /TOC -->

## Build Pre-Requisites

### Command line tools

To build this project you must first install several command line utilities.

- [`make`](https://www.gnu.org/software/make/) - Make build system
- [`mvn`](https://maven.apache.org/index.html) (version 3.5 and above) - Maven CLI
- [`docker`](https://docs.docker.com/install/) - Docker command line client

In order to use `make` these all need to be available on your `$PATH`.

### macOS

The `make` build uses GNU versions of `find`, `sed` and other utilities and is not compatible with the BSD versions
available on macOS. When using macOS, you have to install the GNU versions of `find` and `sed`. When using `brew`, you
can do `brew install gnu-sed findutils grep coreutils`.
This command will install the GNU versions as `gcp`, `ggrep`, `gsed` and `gfind` and our `make` build will automatically pick them up and use them.

The `mvn` tool might install the latest version of OpenJDK during the brew install. For builds on macOS to succeed,
OpenJDK version 11 needs to be installed. This can be done by running `brew install openjdk@11`. For maven to read the
new Java version, you will need to edit the `~/.mavenrc` file and paste the following
line `export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-11.jdk/Contents/Home`.

You may come across an issue of linking from the above step. To solve this run this command:
`sudo ln -sfn /usr/local/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk`.
If this throws an error that it cannot find the file or directory, navigate into `/Library/Java/` (or however deep you
can) and create a new folder named `JavaVirtualMachines` followed by creating a file named `openjdk-11.jdk`. The folder
structure after everything is said and done should look like `/Library/Java/JavaVirtualMachines/openjdk-11.jdk`. After
doing that run the command at the beginning again and this should link the file and allow you to use maven with OpenJDK
version 11.

## Build and deploy from source

To build the operator from source the code needs to be compiled into a container image and placed
in a location accessible to the Kubernetes/OpenShift nodes. The easiest way to make your personal build
accessible, is to place it on [Quay.io](https://quay.io), [Docker Hub](https://hub.docker.com/) or another 
container registry of your choice. Other build options (including options for limited or no network access) 
are available in the sections below this quick start guide.

1. If you don't have one already, create an account for your container registry. Then log your local
   Docker client into the registry using:

        docker login <registry_name>

   You can use `quay.io` for the registry name if using that registry or omit the registry name for Dockerhub. This 
   command sets the credentials for the `docker push` registry target.


2. Make sure that the `DOCKER_ORG` and `DOCKER_REGISTRY` environment variables are set to the same value as your
   repository on the container registry, and the container registry you are using. For Docker Hub  and Quay the repository is your 
   username.

        export DOCKER_ORG=repository
        export DOCKER_REGISTRY=registry_name  #defaults to quay.io if unset

   By default, the `docker_push` target will build the images under the strimzi organisation (
   e.g. `strimzi/access-operator:latest`) and attempt to push them to the strimzi repositories on Quay. Only certain
   users are approved to do this, so you should push to your own Docker Hub organisation (account) instead. To do this,
   make sure that the `DOCKER_ORG` and `DOCKER_REGISTRY` environment variables are set before running the `make` commands.\
   \
   When the Docker images are build, they will be labeled in the
   form: `registry_name/repository/access-operator:latest` in your local repository and pushed to your remote repository 
   under the same label.

3. Now build the Docker images and push them to your remote repository:

        make all

   Once this completes you should have a new repository under your registry:
    - `registry_name/repository/access-operator`

   The tests run during the build can be skipped by setting the `MVN_ARGS` environment variable and passing that to the
   make command:

        make MVN_ARGS='-DskipTests -DskipITs' all

4. To use the newly built images, update
   the `packaging/install/cluster-operator/050-Deployment.yaml` to obtain the image from your
   chosen repository rather than the official Strimzi images:
   \
   \
   **Linux**
   ```
   sed -Ei -e "s#image: quay.io/strimzi/access-operator:latest#image: $DOCKER_REGISTRY/$DOCKER_ORG/access-operator:latest#" \
            packaging/install/050-Deployment.yaml
    ```

   **macOS**
    ```
    sed -E -i '' -e "s#image: quay.io/strimzi/access-operator:latest#image: $DOCKER_REGISTRY/$DOCKER_ORG/access-operator:latest#" \
                  packaging/install/050-Deployment.yaml
    ```

   This updates `050-Deployment.yaml`, replacing the image reference (in the `image` property) with one with the same 
   name but with the repository changed.
   > *Note*: please ensure you don't commit these changes accidentally.

5. The ClusterRoleBinding file assumes you're installing into the namespace `strimzi`. If you wish to use a different one,
   you'll need to replace it in the file
   \
   \
   **Linux**

       sed -Ei "s/namespace: strimzi/namespace: <desired_namespace>/g" packaging/install/030-ClusterRoleBinding.yaml

   **macOS**

        sed -E -i '' -e "s/namespace: strimzi/namespace: <desired_namespace>/g" packaging/install/030-ClusterRoleBinding.yaml

   This updates the ClusterRoleBinding file to ensure that the roles binding to the service account use
   the correct namespace.
   > *Note*: please ensure you don't commit these changes accidentally.

6. Then deploy the Operator by running the following (replace `strimzi` with your desired namespace if
   necessary):

        # Running against Kubernetes
        kubectl -n strimzi create -f packaging/install

        # Running against OpenShift
        oc -n strimzi create -f packaging/install

7. Deploy the Strimzi cluster operator and a Kafka instance. You can use the [Strimzi quickstart guide](https://strimzi.io/quickstarts/) to do this.

8. Finally, you can deploy a KafkaAccess custom resource running:

        # Running against Kubernetes
        kubectl -n strimzi create -f packaging/examples/kafka-access.yaml

        # Running against OpenShift
        oc -n strimzi create -f packaging/examples/kafka-access.yaml

   Make sure the `name`, `namespace` and `listener` in the KafkaAccess custom resource match those of your Kafka instance.
   The `examples` directory also includes an example for connecting to a Kafka cluster with a specific KafkaUser.

9. The operator will create a Kubernetes secret with the same name as the KafkaAccess containing your connection details.
   You can run the following commands to see the contents with the values base64 decoded:

        # Running against Kubernetes
        kubectl -n strimzi get secret my-kafka-access -ojson | jq '.data|map_values(@base64d)'

        # Running against OpenShift
        oc -n strimzi get secret my-kafka-access -ojson | jq '.data|map_values(@base64d)'

## Build details

### Make targets

Strimzi includes a `Makefile` with various Make targets to build the project.

Commonly used Make targets:

- `java_verify` for building the Java code and running tests.
- `build` for building the Java code, copying the generated CRD into the packaging directory and building the Docker image.
- `docker_build` for building only the Docker image (this assumes you have built the Java code already).
- `docker_tag` for retagging the image built by `docker_build` (since the `docker_build` target will **always** build the images under 
the `strimzi` organization with the tag latest).
- `docker_push` for pushing the image to a Docker registry (this also invokes `docker_tag`).

### Java versions

To use different Java version for the Maven build, you can specify the environment variable `JAVA_VERSION_BUILD` and set
it to the desired Java version. For example, for building with Java 11 you can use `export JAVA_VERSION_BUILD=11`.

> *Note*: Operator currently developed and tested with Java 11.

### Java build options

Running `make` invokes Maven for packaging the Java code.
The `mvn` command can be customized by setting the `MVN_ARGS` environment variable when launching `make all`. For
example:
* `MVN_ARGS=-DskipTests make all` will compile test code, but not run unit or integration tests
* `MVN_ARGS=-DskipITs make all` will compile test code and run unit tests, but not integration tests
* `MVN_ARGS=-Dmaven.test.skip=true make all` won't compile test code and won't run unit or integration tests
  the integration tests.

### Docker build options

When building the Docker images you can use an alternative JRE or use an alternate base image.

#### Alternative Docker image JRE

The docker images can be built with an alternative Java version by setting the environment variable `JAVA_VERSION`. For
example, to build docker images that have the Java 11 JRE installed use `JAVA_VERSION=11 make docker_build`. If not
present, the container images will use Java **11** by default.

#### Alternative `docker` command

The build assumes the `docker` command is available on your `$PATH`. You can set the `DOCKER_CMD` environment variable
to use a different `docker` binary or an alternative implementation such as [`podman`](https://podman.io/).

### Tagging and pushing Docker image

Target `docker_tag` tags the Docker image built by the `docker_build` target. This target is automatically called as
part of the `docker_push` target, but can be called separately if you wish to avoid pushing images to an external
registry.

To configure the `docker_tag` and `docker_push` targets you can set following environment variables:

* `DOCKER_ORG` configures the Docker organization for tagging/pushing the images (defaults to the value of the `$USER`
  environment variable)
* `DOCKER_TAG` configured Docker tag (default is `latest`)
* `DOCKER_REGISTRY` configures the Docker registry where the image will be pushed (default is `quay.io`)

### Local build on Minikube

If you do not want to have the docker daemon running on your local development machine, you can build the container
images in your Minikube VM by setting your docker host to the address of the VM's daemon:

    eval $(minikube docker-env)

The images will then be built and stored in the cluster VM's local image store and then pushed to your configured Docker
registry.

#### Skipping the registry push

You can avoid the `docker_push` step and `sed` commands above by configuring the Docker Host as above and then running:

    make build

This labels your latest container build as `strimzi/access-operator:latest` and you can then deploy the standard deployment without
changing the image target. However, this will only work if all instances of the `imagePullPolicy:` setting are set
to `IfNotPresent` or `Never`. If not, then the cluster nodes will go to the upstream registry (Quay by default)
and pull the official images instead of using your freshly built image.

## DCO Signoff

The project requires that all commits are signed-off, indicating that _you_ certify the changes with the developer
certificate of origin (DCO) (https://developercertificate.org/). This can be done using `git commit -s` for each commit
in your pull request. Alternatively, to signoff a bunch of commits you can use `git rebase --signoff _your-branch_`.

You can add a commit-msg hook to warn you if the commit you just made locally has not been signed off. Add the following
line to you `.git/hooks/commit-msg` script to print the warning:

```
./tools/git-hooks/signoff-warning-commit-msg $1
```

## Checkstyle pre-commit hook

The Checkstyle plugin runs on all pull requests to the Strimzi repository. If you haven't compiled the code via maven,
before you submit the PR, then formatting bugs can slip through and this can lead to annoying extra pushes to fix
things. In the first instance you should see if your IDE has a Checkstyle plugin that can highlight errors in-line, such
as [this one](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea) for IntelliJ.

You can also run the Checkstyle plugin for every commit you make by adding a pre-commit hook to your local Strimzi git
repository. To do this, add the following line to your `.git/hooks/pre-commit` script, to execute the checks and fail
the commit if errors are detected:

```
./tools/git-hooks/checkstyle-pre-commit
```

## Building container images for other platforms with Docker `buildx`

Docker supports building images for different platforms using the `docker buildx` command. If you want to use it to
build the ioeratir unage, you can just set the environment variable `DOCKER_BUILDX` to `buildx`, set the environment
variable `DOCKER_BUILD_ARGS` to pass additional build options such as the platform and run the build. For example
following can be used to build the image for Linux on Arm64 / AArch64:

```
export DOCKER_BUILDX=buildx
export DOCKER_BUILD_ARGS="--platform linux/amd64 --load"
make all
```

_Note: Strimzi Access Operator currently does not officially support any other platforms than Linux on `amd64`._
