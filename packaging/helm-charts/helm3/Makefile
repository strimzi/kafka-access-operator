PROJECT_NAME=helm-charts

include ../../../Makefile.os

RELEASE_VERSION ?= latest
CHART_NAME=strimzi-access-operator
CHART_PATH ?= ../helm3/strimzi-access-operator
CHART_RENDERED_TEMPLATES_TMP=../../../target/charts
CHART_RENDERED_TEMPLATES_INSTALL=../../install
HELM_CLI=helm

helm_clean:
	rm -rfv $(CHART_RENDERED_TEMPLATES_TMP)
	rm -rf strimzi-$(RELEASE_VERSION)/charts/
	rm -f $(CHART_NAME)-*.tgz

helm_lint:
	# Linting Helm Chart
	$(HELM_CLI) lint --debug $(CHART_PATH)

helm_install:
	# Copying rendered template files to: $(CHART_RENDERED_TEMPLATES_INSTALL)
	mkdir -p $(CHART_RENDERED_TEMPLATES_TMP)
	helm template --namespace strimzi-access-operator --output-dir $(CHART_RENDERED_TEMPLATES_TMP) $(CHART_PATH)
	$(FIND) $(CHART_RENDERED_TEMPLATES_TMP) -type f -name '*.yaml' -exec $(SED) -i '/^---/d' {} \;
	$(FIND) $(CHART_RENDERED_TEMPLATES_TMP) -type f -name '*.yaml' -exec $(SED) -i '/^# Source: /d' {} \;
	$(FIND) $(CHART_RENDERED_TEMPLATES_TMP) -type f -name '*.yaml' \
	    | xargs -IFILE $(CP) FILE $(CHART_RENDERED_TEMPLATES_INSTALL)
	rm -rf $(CHART_RENDERED_TEMPLATES_TMP)

helm_pkg: helm_clean helm_lint helm_install
	# Copying unarchived Helm Chart to release directory
	mkdir -p strimzi-$(RELEASE_VERSION)/charts/
	$(CP) -r $(CHART_PATH) strimzi-$(RELEASE_VERSION)/charts/$(CHART_NAME)
	# Packaging helm chart with semantic version: $(RELEASE_VERSION)
	$(HELM_CLI) package --version $(RELEASE_VERSION) --app-version $(RELEASE_VERSION) --destination ./ $(CHART_PATH)
	rm -rf strimzi-$(RELEASE_VERSION)

all: helm_install
clean: helm_clean

.PHONY: all clean