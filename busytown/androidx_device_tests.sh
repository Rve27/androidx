#!/bin/bash
set -e

echo "Starting $0 at $(date)"
source "$(dirname "$0")/setup_build_env_vars.sh"
source "$(dirname "$0")/record_build_metrics.sh"

cd "$(dirname $0)"

setup_build_env_vars
start_time=$(initialize_start_time)

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

# This target runs in incremental mode, but we do not want to restore the APKs and configs from
# previous runs
rm -fr "../../out/androidx/build/test-xml-configs"
rm -fr "../../out/androidx/build/app-apks-files"

impl/build.sh zipTestConfigsWithApks zipOwnersFiles createModuleInfo "$@"

record_build_metrics "$start_time"

echo "Completing $0 at $(date)"