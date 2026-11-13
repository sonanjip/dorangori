#!/usr/bin/env bash
# usage: run.sh [phone|wear|both]  (default both) -- installDebug + launch
set -e; source "$(dirname "$0")/env.sh"; cd "$ROOT"
T="${1:-both}"
if [ "$T" = "phone" ] || [ "$T" = "both" ]; then
  ANDROID_SERIAL="$PHONE_SERIAL" ./gradlew :app:installDebug
  adb -s "$PHONE_SERIAL" shell am start -n "$APP_ID/$PHONE_ACTIVITY"
fi
if [ "$T" = "wear" ] || [ "$T" = "both" ]; then
  ANDROID_SERIAL="$WEAR_SERIAL" ./gradlew :wear:installDebug
  adb -s "$WEAR_SERIAL" shell am start -n "$APP_ID/$WEAR_ACTIVITY"
fi
