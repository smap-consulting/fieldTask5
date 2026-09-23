#!/usr/bin/env bash
# Open each named form in turn and report whether it renders.
#
#   scripts/sweep.sh                 # the default widget coverage set
#   scripts/sweep.sh "lookup" "video"
#
# Catches form parsing, itemset and widget regressions that unit tests miss.
# Set FT_E2E_SERIAL to target a device.
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV="$HERE/device.sh"
SERIAL="${FT_E2E_SERIAL:-}"
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL") || ADB=(adb)

# "A read only form" is deliberately absent: it is tasks_only, so it never appears
# in the Forms tab and can only be reached through a task.
DEFAULT_FORMS=(
    "select and pulldata" "lookup" "submit lookup" "test drill down"
    "reference locations" "pdf test" "video" "Reference Test"
    "export test" "Launcher"
)
FORMS=("$@")
[ ${#FORMS[@]} -eq 0 ] && FORMS=("${DEFAULT_FORMS[@]}")

tap_at() {  # "(x,y)"
    local c=${1//[()]/}
    "${ADB[@]}" shell input tap "${c%,*}" "${c#*,}" >/dev/null 2>&1
}

# Leave whatever form is open and return to a scrolled-to-top Forms tab
back_to_forms() {
    local i scr
    for i in 1 2 3 4 5; do
        scr=$("$DEV" state 2>/dev/null | sed -n 's/^screen: *//p')
        case "$scr" in
            *SmapMain*) break ;;
            *)
                dump=$("$DEV" ui 2>/dev/null)
                if echo "$dump" | grep -q discard_changes; then
                    "$DEV" tapid discard_changes >/dev/null 2>&1
                elif echo "$dump" | grep -qE "Recover your work|Save form\?"; then
                    # A previous run left a savepoint. Discard it rather than reopening
                    # a half filled form, which would block every form after this one.
                    "$DEV" tapid button2 >/dev/null 2>&1
                else
                    "${ADB[@]}" shell input keyevent BACK >/dev/null 2>&1
                fi
                sleep 3
                ;;
        esac
    done
    "${ADB[@]}" shell input tap 180 373 >/dev/null 2>&1   # Forms tab
    sleep 2
    for i in 1 2 3 4 5; do
        "${ADB[@]}" shell input swipe 540 700 540 2000 200 >/dev/null 2>&1
        sleep 1
    done
}

find_and_tap() {
    local i c
    for i in $(seq 1 10); do
        c=$("$DEV" ui 2>/dev/null | grep -F "$1 (" | head -1 | grep -oE '^\([0-9]+,[0-9]+\)')
        if [ -n "$c" ]; then tap_at "$c"; return 0; fi
        "${ADB[@]}" shell input swipe 540 1800 540 800 250 >/dev/null 2>&1
        sleep 2
    done
    return 1
}

fails=0
printf '%-24s %-6s %s\n' FORM RESULT DETAIL
for f in "${FORMS[@]}"; do
    back_to_forms
    if ! find_and_tap "$f"; then
        printf '%-24s %-6s %s\n' "$f" "MISS" "not in the forms list"
        fails=$((fails + 1))
        continue
    fi
    sleep 7

    dump=$("$DEV" ui 2>/dev/null)
    if echo "$dump" | grep -q "Recover your work"; then
        "$DEV" tapid button2 >/dev/null 2>&1      # Discard the savepoint
        sleep 5
        dump=$("$DEV" ui 2>/dev/null)
    fi
    alert=$(echo "$dump" | awk '$2=="alertTitle"{ $1=""; $2=""; print; exit }' | sed 's/^ *//')
    warn=$(echo "$dump" | awk '$2=="warning_text"{ $1=""; $2=""; print; exit }' | sed 's/^ *//' | cut -c1-50)
    lbl=$(echo "$dump" | awk '$2=="text_label"{ $1=""; $2=""; print; exit }' | sed 's/^ *//' | cut -c1-30)

    if [ -n "$alert" ]; then
        printf '%-24s %-6s %s\n' "$f" "FAIL" "dialog: $alert"
        fails=$((fails + 1))
    elif echo "$dump" | grep -q "text_label\|save_exit_button"; then
        printf '%-24s %-6s %s\n' "$f" "OK" "first: ${lbl:-<no label>}${warn:+  WARN: $warn}"
        [ -n "$warn" ] && fails=$((fails + 1))
    else
        printf '%-24s %-6s %s\n' "$f" "FAIL" "no form view"
        fails=$((fails + 1))
    fi
done
back_to_forms

echo
echo "$((${#FORMS[@]} - fails))/${#FORMS[@]} clean"
exit $([ "$fails" -eq 0 ] && echo 0 || echo 1)
