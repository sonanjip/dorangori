#!/usr/bin/env bash
source "$(dirname "$0")/env.sh"
adb -s "$WEAR_SERIAL" emu kill 2>/dev/null; adb -s "$PHONE_SERIAL" emu kill 2>/dev/null; true
