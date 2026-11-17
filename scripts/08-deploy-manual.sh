#!/usr/bin/env bash
# Bootstrap/diagnostic only. Final video must deploy through CI -> Release.
source "$(dirname "$0")/common.sh"
need_az
need RESOURCE_GROUP WEBAPP
cd "$(dirname "$0")/.."
mvn -B -ntp clean verify
az webapp deploy -g "$RESOURCE_GROUP" -n "$WEBAPP" --src-path target/luz-vital.jar --type jar --clean true -o none
