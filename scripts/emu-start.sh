#!/usr/bin/env bash
# usage: emu-start.sh [--window]  -- boots the phone AVD (5554) then the watch AVD (5556); skips ones already running
set -e; source "$(dirname "$0")/env.sh"; mkdir -p "$ROOT/build"
FLAGS="-no-window -no-audio -no-snapshot-save"
[ "$1" = "--window" ] && FLAGS="-no-snapshot-save"
boot() { # avd port serial
  if adb devices | grep -q "^$3[[:space:]]*device$"; then echo "$3 already running"; return; fi
  nohup emulator -avd "$1" -port "$2" $FLAGS -gpu host >"$ROOT/build/emulator-$1.log" 2>&1 &
  adb -s "$3" wait-for-device
  for i in $(seq 1 150); do
    [ "$(adb -s "$3" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && { echo "$1 booted"; return; }
    sleep 2
  done
  echo "boot timeout for $1; see build/emulator-$1.log" >&2; exit 1
}
boot "$PHONE_AVD" 5554 "$PHONE_SERIAL"
boot "$WEAR_AVD" 5556 "$WEAR_SERIAL"
