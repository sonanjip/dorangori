#!/usr/bin/env bash
# usage: screenshot.sh [phone|wear] [out.png]
source "$(dirname "$0")/env.sh"
S="$PHONE_SERIAL"; [ "$1" = "wear" ] && S="$WEAR_SERIAL"; { [ "$1" = "phone" ] || [ "$1" = "wear" ]; } && shift
OUT="${1:-$ROOT/build/screens/$(date +%H%M%S).png}"; mkdir -p "$(dirname "$OUT")"
adb -s "$S" exec-out screencap -p > "$OUT" && echo "$OUT"
