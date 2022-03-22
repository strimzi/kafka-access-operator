include ./Makefile.os
include ./Makefile.maven

PROJECT_NAME ?= access-operator
GITHUB_VERSION ?= main
RELEASE_VERSION ?= latest

ifneq ($(RELEASE_VERSION),latest)
  GITHUB_VERSION = $(RELEASE_VERSION)
endif

.PHONY: all
all: java_verify

.PHONY: clean
clean: java_clean

.PHONY: next_version
next_version:
	echo $(shell echo $(NEXT_VERSION) | tr a-z A-Z) > release.version
	mvn versions:set -DnewVersion=$(shell echo $(NEXT_VERSION) | tr a-z A-Z)
	mvn versions:commit
