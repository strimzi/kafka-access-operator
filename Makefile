include ./Makefile.os
include ./Makefile.docker
include ./Makefile.maven

PROJECT_NAME ?= access-operator
GITHUB_VERSION ?= main
RELEASE_VERSION ?= latest

ifneq ($(RELEASE_VERSION),latest)
  GITHUB_VERSION = $(RELEASE_VERSION)
endif

.PHONY: release
release: release_prepare release_maven release_version release_pkg

release_prepare:
	rm -rf ./strimzi-access-operator-$(RELEASE_VERSION)
	rm -f ./strimzi-access-operator-$(RELEASE_VERSION).tar.gz
	rm -f ./strimzi-access-operator-$(RELEASE_VERSION).zip
	mkdir ./strimzi-access-operator-$(RELEASE_VERSION)

release_version:
	echo "Update release.version to $(RELEASE_VERSION)"
	echo $(shell echo $(RELEASE_VERSION) | tr a-z A-Z) > release.version
	echo "Changing Docker image tags in install to :$(RELEASE_VERSION)"
	$(FIND) ./packaging/install -name '*.yaml' -type f -exec $(SED) -i '/image: "\?quay.io\/strimzi\/[a-zA-Z0-9_.-]\+:[a-zA-Z0-9_.-]\+"\?/s/:[a-zA-Z0-9_.-]\+/:$(RELEASE_VERSION)/g' {} \;

release_maven:
	echo "Update pom versions to $(RELEASE_VERSION)"
	mvn $(MVN_ARGS) versions:set -DnewVersion=$(shell echo $(RELEASE_VERSION) | tr a-z A-Z)
	mvn $(MVN_ARGS) versions:commit

release_pkg:
	$(CP) -r ./packaging/install ./
	$(CP) -r ./packaging/install ./strimzi-access-operator-$(RELEASE_VERSION)/
	$(CP) -r ./packaging/examples ./
	$(CP) -r ./packaging/examples ./strimzi-access-operator-$(RELEASE_VERSION)/
	tar -z -cf ./strimzi-access-operator-$(RELEASE_VERSION).tar.gz strimzi-access-operator-$(RELEASE_VERSION)/
	zip -r ./strimzi-access-operator-$(RELEASE_VERSION).zip strimzi-access-operator-$(RELEASE_VERSION)/
	rm -rf ./strimzi-access-operator-$(RELEASE_VERSION)
	$(FIND) ./packaging/install/ -mindepth 1 -maxdepth 1 ! -name Makefile -type f,d -exec $(CP) -rv {} ./install/ \;

.PHONY: all
all: java_package docker_build docker_push crd_install

.PHONY: build
build: java_verify crd_install docker_build

.PHONY: clean
clean: java_clean

.PHONY: crd_install
crd_install:
	$(CP) ./api/target/classes/META-INF/fabric8/kafkaaccesses.access.strimzi.io-v1.yml ./packaging/install/040-Crd-kafkaaccess.yaml
	yq eval -i '.metadata.labels."servicebinding.io/provisioned-service"="true"' ./packaging/install/040-Crd-kafkaaccess.yaml

.PHONY: next_version
next_version:
	echo $(shell echo $(NEXT_VERSION) | tr a-z A-Z) > release.version
	mvn versions:set -DnewVersion=$(shell echo $(NEXT_VERSION) | tr a-z A-Z)
	mvn versions:commit
