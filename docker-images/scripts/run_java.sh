#!/usr/bin/env bash
set -e
set +x

if [ -f /opt/strimzi/custom-config/log4j2.properties ]; then
    # if ConfigMap was not mounted and thus this file was not created, use properties file from the classpath
    export JAVA_OPTS="${JAVA_OPTS} -Dlog4j2.configurationFile=file:/opt/strimzi/custom-config/log4j2.properties"
else
    echo "Configuration file log4j2.properties not found. Using default static logging setting. Dynamic updates of logging configuration will not work."
fi

mem_file_cgroups_v2="/sys/fs/cgroup/memory.max"

# expand gc options based upon java version
function get_gc_opts {
  if [ "${STRIMZI_GC_LOG_ENABLED}" == "true" ]; then
    echo "-Xlog:gc*:stdout:time -XX:NativeMemoryTracking=summary"
  else
    # no gc options
    echo ""
  fi
}

calc_maximum_size_opt() {
  local max_mem="$1"
  local percentage="$2"

  local value_in_mb=$((max_mem*percentage/100/1048576))
  echo "-Xmx${value_in_mb}m"
}

# Calculate the value of -Xmx options base on cgroups_v2 values
calc_max_memory() {
  local mem_limit
  mem_limit="$(cat ${mem_file_cgroups_v2})"

  calc_maximum_size_opt "${mem_limit}" "20"
}

export MALLOC_ARENA_MAX=2

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Dvertx.cacheDirBase=/tmp/vertx-cache -Djava.security.egd=file:/dev/./urandom"

# Enable GC logging for memory tracking
JAVA_OPTS="${JAVA_OPTS} $(get_gc_opts)"

# Deny illegal access option is supported only on Java 9 and higher
JAVA_OPTS="${JAVA_OPTS} --illegal-access=deny"

# Exit when we run out of heap memory
JAVA_OPTS="${JAVA_OPTS} -XX:+ExitOnOutOfMemoryError"

DEFAULT_MEMORY_OPTIONS="-XX:MinRAMPercentage=10 -XX:MaxRAMPercentage=20 -XX:InitialRAMPercentage=10"

# Default memory options used when the user didn't configured any of these options, we set the defaults
if [[ ! -r "${mem_file_cgroups_v2}" && "$JAVA_OPTS" != *"MinRAMPercentage"* && "$JAVA_OPTS" != *"MaxRAMPercentage"* && "$JAVA_OPTS" != *"InitialRAMPercentage"* ]]; then
  JAVA_OPTS="${JAVA_OPTS} ${DEFAULT_MEMORY_OPTIONS}"
elif [[ -r "${mem_file_cgroups_v2}" && "$JAVA_OPTS" != *"Xmx"* ]]; then
  # This workaround for cgroups v2 must be removed once java version used by the operator is updated.
  # https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness#

  # Check whether /sys/fs/cgroup/memory.max contains 'max' or a number
  value=$(cat "${mem_file_cgroups_v2}")

  if [[ $value != "max" ]]; then
    # Calculate -Xmx java option
    JAVA_OPTS="${JAVA_OPTS} $(calc_max_memory)"
  else
    # Because /sys/fs/cgroup/memory.max contains 'max', pick defaults
    JAVA_OPTS="${JAVA_OPTS} ${DEFAULT_MEMORY_OPTIONS}"
  fi
fi

# Disable FIPS if needed
if [ "$FIPS_MODE" = "disabled" ]; then
    JAVA_OPTS="${JAVA_OPTS} -Dcom.redhat.fips=false"
fi

set -x

# shellcheck disable=SC2086
exec /usr/bin/tini -w -e 143 -- java $JAVA_OPTS -classpath "libs/*" "io.strimzi.kafka.access.KafkaAccessOperator" "$@"
