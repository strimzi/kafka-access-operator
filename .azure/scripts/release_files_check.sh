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
    echo "ERROR: Checksums of $DIRECTORY do not match."
    echo "    Expected: ${EXPECTED_CHECKSUM}"
    echo "    Actual: ${CHECKSUM}"
    echo "If your changes to $DIRECTORY are related to a new release, please update the checksums. Otherwise, please change only the files in the $PACKAGING_DIR directory. "
    RETURN_CODE=$((RETURN_CODE+1))
  else
    echo "checksum of $DIRECTORY matches expected checksum => OK"
  fi
done

exit $RETURN_CODE