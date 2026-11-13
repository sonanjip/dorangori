# source me
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export APP_ID="io.github.taehunkim.dorangori"
export PHONE_ACTIVITY="io.github.taehunkim.dorangori.MainActivity"
export WEAR_ACTIVITY="io.github.taehunkim.dorangori.wear.MainActivity"
export PHONE_AVD="dori_phone36"
export WEAR_AVD="dori_wear36"
# adb serials of the two emulators (first emulator started = 5554, second = 5556); override for real devices.
export PHONE_SERIAL="${PHONE_SERIAL:-emulator-5554}"
export WEAR_SERIAL="${WEAR_SERIAL:-emulator-5556}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
