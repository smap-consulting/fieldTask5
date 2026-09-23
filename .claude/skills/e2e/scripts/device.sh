#!/usr/bin/env bash
# fieldTask5 e2e device helpers.
#
#   scripts/device.sh state      # server, user, login time, counts, current screen
#   scripts/device.sh launch     # start the app at SplashScreenActivity
#   scripts/device.sh forms      # forms downloaded from the local server
#   scripts/device.sh instances  # saved instances
#   scripts/device.sh relogin    # force the login screen on next launch
#   scripts/device.sh logcat [n] # recent app log lines
#   scripts/device.sh reset      # clear all app data (loses the login)
#
# Set FT_E2E_SERIAL to target a specific device.
set -uo pipefail

PKG=org.smap.smapTask.android
SPLASH="$PKG/au.smap.fieldTask.activities.SplashScreenActivity"
SERIAL="${FT_E2E_SERIAL:-}"
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL") || ADB=(adb)

# The app's settings live in a project-scoped prefs file whose name carries the
# project uuid. Several can exist - instrumented test runs leave one behind per
# run - so the current project id in meta.xml decides which one is live.
prefs_file() {
    local uuid
    uuid=$("${ADB[@]}" shell run-as "$PKG" cat "/data/data/$PKG/shared_prefs/meta.xml" 2>/dev/null \
        | tr -d '\r' | sed -n 's/.*name="current_project_id">\([^<]*\)<.*/\1/p' | head -1)
    if [ -n "$uuid" ]; then
        echo "general_prefs${uuid}.xml"
        return
    fi
    "${ADB[@]}" shell run-as "$PKG" ls /data/data/$PKG/shared_prefs/ 2>/dev/null \
        | tr -d '\r' | grep '^general_prefs' | head -1
}

pref() {
    local f; f=$(prefs_file)
    [ -z "$f" ] && return 1
    "${ADB[@]}" shell run-as "$PKG" cat "/data/data/$PKG/shared_prefs/$f" 2>/dev/null \
        | tr -d '\r' | sed -n "s/.*name=\"$1\">\([^<]*\)<.*/\1/p" | head -1
}

project_dir() {
    "${ADB[@]}" shell ls -d /sdcard/Android/data/$PKG/files/projects/*/ 2>/dev/null \
        | tr -d '\r' | head -1
}

case "${1:-state}" in
state)
    echo "device:    ${SERIAL:-$(adb devices | awk 'NR==2{print $1}')}"
    echo "server:    $(pref server_url)"
    echo "user:      $(pref username)"
    ll=$(pref last_login)
    if [ -n "$ll" ] && [ "$ll" != "" ]; then
        echo "last login: $(date -r $((ll / 1000)) 2>/dev/null || echo "$ll")"
    else
        echo "last login: none - login required"
    fi
    echo "pw_policy: $(pref pw_policy)  (0 = login every launch, -1 = never re-prompt)"
    d=$(project_dir)
    if [ -n "$d" ]; then
        echo "forms:     $("${ADB[@]}" shell "ls ${d}forms/*.xml 2>/dev/null | wc -l" | tr -d ' \r')"
        echo "instances: $("${ADB[@]}" shell "ls -d ${d}instances/*/ 2>/dev/null | wc -l" | tr -d ' \r')"
    fi
    echo "screen:    $("${ADB[@]}" shell dumpsys activity activities 2>/dev/null \
        | sed -n 's/.*topResumedActivity=ActivityRecord{[^ ]* [^ ]* \([^ ]*\).*/\1/p' | head -1)"
    ;;
launch)
    # The launcher intent resolves to LeakCanary in debug builds, so the splash
    # activity has to be named explicitly.
    "${ADB[@]}" shell am force-stop "$PKG"
    "${ADB[@]}" shell am start -n "$SPLASH" >/dev/null 2>&1
    sleep 4
    "$0" state
    ;;
forms)
    d=$(project_dir)
    "${ADB[@]}" shell "ls ${d}forms/ 2>/dev/null" | tr -d '\r' | grep '\.xml$' | sed 's/\.xml$//'
    ;;
instances)
    d=$(project_dir)
    "${ADB[@]}" shell "ls ${d}instances/ 2>/dev/null" | tr -d '\r'
    ;;
relogin)
    # pw_policy 0 means "login every launch". The server overwrites this on the
    # next task refresh, so it only forces the next login.
    f=$(prefs_file)
    tmp=$(mktemp)
    "${ADB[@]}" exec-out run-as "$PKG" cat "/data/data/$PKG/shared_prefs/$f" >"$tmp"
    if grep -q 'name="pw_policy"' "$tmp"; then
        sed -i '' 's|<string name="pw_policy">[^<]*</string>|<string name="pw_policy">0</string>|' "$tmp"
    else
        # A fresh install has no pw_policy entry at all, so it must be inserted
        # rather than replaced.
        sed -i '' 's|</map>|    <string name="pw_policy">0</string>\
</map>|' "$tmp"
    fi
    "${ADB[@]}" shell "run-as $PKG sh -c 'cat > /data/data/$PKG/shared_prefs/$f'" <"$tmp"
    rm -f "$tmp"
    echo "pw_policy set to 0 - the login screen will show on next launch"
    ;;
ui|tap|tapid|type|clear)
    # The mcp__android-emulator__* tools cannot target a device and fail with
    # "more than one device/emulator" whenever a second emulator is attached,
    # so the UI is driven through adb instead.
    case "$1" in
    ui)
        "${ADB[@]}" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
        "${ADB[@]}" exec-out cat /sdcard/ui.xml 2>/dev/null | python3 -c '
import re, sys
xml = sys.stdin.read()
for m in re.finditer(r"<node[^>]*>", xml):
    n = m.group(0)
    g = lambda a: (re.search(a + r'\''="([^"]*)"'\'', n) or [None, ""])[1]
    text, rid, desc, bounds = g("text"), g("resource-id"), g("content-desc"), g("bounds")
    if not (text or rid or desc):
        continue
    b = re.findall(r"\d+", bounds)
    c = f"({(int(b[0])+int(b[2]))//2},{(int(b[1])+int(b[3]))//2})" if len(b) == 4 else ""
    print(f"{c:14} {rid.split('\''/'\'')[-1]:24} {text or desc}")
'
        ;;
    tap)
        coords=$("$0" ui | grep -iF "${2:?text required}" | head -1 | grep -oE '^\([0-9]+,[0-9]+\)')
        [ -z "$coords" ] && { echo "no node matching '$2'" >&2; exit 1; }
        "${ADB[@]}" shell input tap ${coords//[(),]/ }
        echo "tapped '$2' at $coords"
        ;;
    tapid)
        coords=$("$0" ui | awk -v id="${2:?resource-id required}" '$2==id{print $1; exit}')
        [ -z "$coords" ] && { echo "no node with id '$2'" >&2; exit 1; }
        "${ADB[@]}" shell input tap ${coords//[(),]/ }
        echo "tapped id '$2' at $coords"
        ;;
    clear)
        "${ADB[@]}" shell input keyevent KEYCODE_MOVE_END
        for _ in $(seq 1 "${2:-60}"); do "${ADB[@]}" shell input keyevent KEYCODE_DEL; done
        ;;
    type)
        # input text mangles some characters; it does not URL-encode, unlike the
        # mcp set_text tool.
        "${ADB[@]}" shell input text "${2:?text required}"
        ;;
    esac
    ;;
logcat)
    "${ADB[@]}" logcat -d --pid="$("${ADB[@]}" shell pidof "$PKG" | tr -d '\r')" -t "${2:-200}"
    ;;
reset)
    "${ADB[@]}" shell pm clear "$PKG"
    echo "app data cleared - the next launch needs a full login"
    ;;
*)
    sed -n '2,12p' "$0"
    exit 1
    ;;
esac
