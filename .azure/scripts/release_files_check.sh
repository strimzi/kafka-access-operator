#!/usr/bin/env bash

set -eu

source ./.checksums
SHA1SUM=sha1sum

RETURN_CODE=0

# Arrays holding the relevant information for each directory
# TODO: after release add here "Helm Charts" HELM_CHART_CHECKSUM checksum_helm "./helm-charts" and "./packaging/helm-charts" in each line
ITEMS=("install" "examples")
CHECKSUM_VARS=("INSTALL_CHECKSUM" "EXAMPLES_CHECKSUM")
MAKE_TARGETS=("checksum_install" "checksum_examples")
DIRECTORIES=("./install" "./examples")
PACKAGING_DIRS=("./packaging/install" "./packaging/examples")

for i in "${!ITEMS[@]}"; do
  NAME="${ITEMS[$i]}"
  CHECKSUM_VAR="${CHECKSUM_VARS[$i]}"
  MAKE_TARGET="${MAKE_TARGETS[$i]}"
  DIRECTORY="${DIRECTORIES[$i]}"
  PACKAGING_DIR="${PACKAGING_DIRS[$i]}"

  CHECKSUM="$(make --no-print-directory $MAKE_TARGET)"
  EXPECTED_CHECKSUM="${!CHECKSUM_VAR}"

  if [ "$CHECKSUM" != "$EXPECTED_CHECKSUM" ]; then
    echo "ERROR checksum of $DIRECTORY does not match expected"
    echo "expected ${EXPECTED_CHECKSUM}"
    echo "found ${CHECKSUM}"
    echo "if your changes are not related to a release please check your changes into"
    echo "$PACKAGING_DIR"
    echo "instead of $DIRECTORY"
    echo ""
    echo "if this is part of a release instead update the checksum i.e."
    echo "$CHECKSUM_VAR=\"${EXPECTED_CHECKSUM}\""
    echo "->"
    echo "$CHECKSUM_VAR=\"${CHECKSUM}\""
    RETURN_CODE=$((RETURN_CODE+1))
  else
    echo "checksum of $DIRECTORY matches expected checksum => OK"
  fi
done

exit $RETURN_CODE