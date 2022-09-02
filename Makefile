include ./Makefile.os
include ./Makefile.maven
include ./Makefile.docker

PROJECT_NAME ?= access-operator
GITHUB_VERSION ?= main
RELEASE_VERSION ?= latest

ifneq ($(RELEASE_VERSION),latest)
  GITHUB_VERSION = $(RELEASE_VERSION)
endif

.PHONY: all
all: java_verify crd_install docker_build docker_tag docker_push

.PHONY: build
build: java_verify crd_install docker_build

.PHONY: clean
clean: java_clean

.PHONY: crd_install
crd_install:
	$(CP) ./target/classes/META-INF/fabric8/kafkaaccesses.access.strimzi.io-v1.yml ./packaging/install/040-Crd-kafkaaccess.yaml

.PHONY: next_version
next_version:
	echo $(shell echo $(NEXT_VERSION) | tr a-z A-Z) > release.version
	mvn versions:set -DnewVersion=$(shell echo $(NEXT_VERSION) | tr a-z A-Z)
	mvn versions:commit
