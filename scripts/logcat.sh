#!/usr/bin/env bash
# usage: logcat.sh [phone|wear] [lines=200]  -- dumps app-process logs and exits
source "$(dirname "$0")/env.sh"
S="$PHONE_SERIAL"; [ "$1" = "wear" ] && S="$WEAR_SERIAL"; { [ "$1" = "phone" ] || [ "$1" = "wear" ]; } && shift
PID=$(adb -s "$S" shell pidof "$APP_ID" | tr -d '\r')
if [ -n "$PID" ]; then adb -s "$S" logcat -d -t "${1:-200}" --pid="$PID"; else echo "app not running; crash/fatal log:"; adb -s "$S" logcat -d -t "${1:-200}" '*:E'; fi
