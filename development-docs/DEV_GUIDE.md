# Development Guide for Access Operator

This document explains how to build the Access Operator.

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

## Build from source

To build the Docker image:

    make all

Once this completes you should have a new image called `strimzi/access-operator:latest`

The tests run during the build can be skipped by setting the `MVN_ARGS` environment variable and passing that to the
make command:

    make MVN_ARGS='-DskipTests' all
