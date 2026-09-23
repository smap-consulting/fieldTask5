#!/usr/bin/env bash
# fieldTask5 e2e preflight.
#
# Brings up the dedicated e2e emulator, checks the local Smap server, and
# installs the current debug build. Prints a status line per check and, on
# success, the device serial to drive.
#
#   scripts/preflight.sh            # check, boot and install as needed
#   scripts/preflight.sh --no-build # skip the gradle build, use the existing apk
#
set -uo pipefail

AVD="${FT_E2E_AVD:-Pixel_9a}"
PKG=org.smap.smapTask.android
SERVER="${FT_E2E_SERVER:-http://localhost}"
SDK="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
APK="$REPO/collect_app/build/outputs/apk/standard/debug/FieldTask5-standard-debug.apk"

BUILD=1
[ "${1:-}" = "--no-build" ] && BUILD=0

fail=0
ok()  { printf '  %-16s ok    %s\n' "$1" "$2"; }
bad() { printf '  %-16s FAIL  %s\n' "$1" "$2"; fail=1; }

echo "fieldTask5 e2e preflight"

# ---------------------------------------------------------------- local server
code=$(curl -s -o /dev/null -w '%{http_code}' -m 8 "$SERVER/login" 2>/dev/null || echo 000)
case "$code" in
    401) ok   "server"  "$SERVER (/login -> 401, auth required)" ;;
    200) ok   "server"  "$SERVER (/login -> 200)" ;;
    000) bad  "server"  "$SERVER unreachable - is Apache running on port 80?" ;;
    *)   bad  "server"  "$SERVER /login -> $code" ;;
esac

# -------------------------------------------------------------------- database
if psql -d survey_definitions -tAc 'select 1' >/dev/null 2>&1; then
    ok  "database" "survey_definitions reachable (used to verify submissions)"
else
    bad "database" "cannot query survey_definitions - submissions cannot be verified"
fi

# ---------------------------------------------------------------------- device
serial_for_avd() {
    local s
    for s in $(adb devices | awk 'NR>1 && $2=="device"{print $1}'); do
        if [ "$(adb -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r')" = "$1" ]; then
            echo "$s"; return 0
        fi
    done
    return 1
}

SERIAL=$(serial_for_avd "$AVD")
if [ -z "$SERIAL" ]; then
    if [ ! -d "$HOME/.android/avd/$AVD.avd" ]; then
        bad "emulator" "AVD '$AVD' does not exist - set FT_E2E_AVD or create it"
    else
        echo "  emulator         booting $AVD ..."
        nohup "$SDK/emulator/emulator" -avd "$AVD" -netdelay none -netspeed full \
            >/tmp/ft-e2e-emulator.log 2>&1 &
        for _ in $(seq 1 90); do
            sleep 2
            SERIAL=$(serial_for_avd "$AVD") || continue
            [ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
            SERIAL=""
        done
        [ -n "$SERIAL" ] && ok "emulator" "$AVD booted as $SERIAL" \
                         || bad "emulator" "$AVD did not boot within 3 minutes"
    fi
else
    ok "emulator" "$AVD already running as $SERIAL"
fi

# ------------------------------------------------------------------------- apk
if [ "$fail" -eq 0 ]; then
    if [ "$BUILD" -eq 1 ]; then
        echo "  apk              building ..."
        if (cd "$REPO" && ./gradlew -q assembleStandardDebug >/tmp/ft-e2e-build.log 2>&1); then
            ok "apk" "assembleStandardDebug"
        else
            bad "apk" "build failed - see /tmp/ft-e2e-build.log"
        fi
    fi

    if [ "$fail" -eq 0 ] && [ -f "$APK" ]; then
        out=$(adb -s "$SERIAL" install -r -d "$APK" 2>&1 | tail -2)
        if echo "$out" | grep -q Success; then
            ok "install" "$PKG"
        elif echo "$out" | grep -q CONFLICTING_PROVIDER; then
            other=$(echo "$out" | grep -oE 'org\.smap\.smapTask\.android[a-zA-Z0-9._]*' | tail -1)
            bad "install" "another flavour ($other) owns the provider authority; uninstall it first"
        else
            bad "install" "$(echo "$out" | tr '\n' ' ')"
        fi
    elif [ "$fail" -eq 0 ]; then
        bad "apk" "not found at $APK"
    fi
fi

echo
if [ "$fail" -eq 0 ]; then
    echo "ready. device: $SERIAL"
    echo "FT_E2E_SERIAL=$SERIAL"
    exit 0
fi
echo "preflight failed"
exit 1
