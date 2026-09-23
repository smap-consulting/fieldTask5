---
name: e2e
description: Run end-to-end tests for FieldTask5 by driving the real app on a dedicated emulator against the local Smap server. Use when asked to run e2e tests, exercise the app end to end, smoke test FieldTask, check a change works in the real app rather than in unit tests, or verify login, task download, form filling or submission against localhost.
version: 0.1.0
---

# FieldTask5 end-to-end tests

Drives the real app on a dedicated emulator against the local Smap server, and
verifies the result on the **server**, not just on screen. The ODK Espresso
harness cannot be used for this: `CollectTestRule` asserts ODK's onboarding
screen ("Configure with QR code"), which smap never shows, so 85 of the 195
instrumented test files fail before their first assertion. Driving the app
directly sidesteps that entirely.

## Scope

Four suites. Run all of them unless the user names specific ones.

| Suite | What it proves |
|---|---|
| `login`    | Credentials reach the server and are accepted |
| `refresh`  | Forms and tasks download and land on the device |
| `submit`   | A filled form reaches the server and is recorded |
| `tasks`    | An assigned task can be completed and uploaded |
| `widgets`  | The varied forms in *A project* open and render |

## Before doing anything

```bash
.claude/skills/e2e/scripts/preflight.sh          # add --no-build to skip gradle
```

Checks the local server, the database, the emulator and the app, booting and
installing as needed. It prints `FT_E2E_SERIAL=<serial>`. **Export that and use
`adb -s $FT_E2E_SERIAL` for every device command** — the user usually has a
second emulator running that must not be touched.

If preflight fails, report which check failed and stop. Do not fall back to
another device.

```bash
export FT_E2E_SERIAL=emulator-5556
.claude/skills/e2e/scripts/device.sh state       # server, user, counts, screen
```

## Credentials

Never put a password in the repo, a script or a command line. Check
`device.sh state` first:

- `last login` set and `server` is the local one → already logged in, skip the
  login suite's credential entry and go straight to asserting state.
- otherwise → get to the login screen, then **ask the user to type the password
  into the emulator** and tell you when they have. Do not ask them to paste it
  into the conversation.

`device.sh relogin` forces the login screen on the next launch without clearing
anything else.

## Suite: login

1. `device.sh relogin` then `device.sh launch`.
2. Assert the login screen shows, with the server URL prefilled.
3. User types the password, taps Login.
4. Assert: screen is now `SmapMain`, and `device.sh state` shows a fresh
   `last login`. That value is written only by `SmapLoginActivity.loginSuccess()`,
   so it is the reliable signal — the offline fallback path does not set it.
5. Confirm the credentials actually reached the server:
   ```bash
   .claude/skills/e2e/scripts/verify.sh requests 5
   ```
   Field 3 is the authenticated user. `neil` means Basic auth succeeded; `-`
   means no credentials were sent and the app fell back to its offline check.

## Suite: refresh

1. Tap the refresh icon (`menu_gettasks`) on the toolbar.
2. Wait for the result dialog, read it with `get_all_text`, tap OK.
3. Assert every line ends in `Success`, `Created` or `deleted`. Report any that
   do not, verbatim.
4. Assert `device.sh forms` and `device.sh instances` counts rose, and that the
   form names are prefixed `10.0.2.2-` (a `dev.smap.com.au-` prefix means the
   device is still pointed at the wrong server).

## Suite: submit

1. Menu → Start new form → pick a form from *A project* (`submit test` is the
   simplest; see `references/environment.md`).
2. Fill each question, swipe to the end, Finalize.
3. Menu → Ready to send → send.
4. Verify on the server, not just from the app's toast:
   ```bash
   .claude/skills/e2e/scripts/verify.sh submissions "5 minutes"
   ```
   Assert a row exists with the expected `survey_name` and a `status` of
   `success`. A send can look fine on screen while the request never got past
   Apache — cross-check with `verify.sh requests`.

## Suite: tasks

1. Tasks tab → open an assigned task.
2. Complete and finalize it.
3. Assert the task disappears from the pending list and the instance is sent.
4. ```bash
   .claude/skills/e2e/scripts/verify.sh tasks neil
   ```
   Assert the assignment status moved to `complete`.

## Suite: widgets

For each form listed in `references/environment.md` under "widget coverage":
open it, assert the first question renders, swipe through, exit without saving.
Record pass/fail per form. This catches form-parsing and widget regressions
that unit tests miss.

## Driving the emulator

Use the `mcp__android-emulator__*` tools, with these gotchas:

- **Launching**: `launch_app` and `monkey -c LAUNCHER` both resolve to
  LeakCanary in debug builds. Always start the app explicitly:
  `am start -n org.smap.smapTask.android/au.smap.fieldTask.activities.SplashScreenActivity`
  (this is what `device.sh launch` does).
- **Typing**: `set_text` URL-encodes its input, so `http://10.0.2.2` is typed
  literally as `http%3A%2F%2F10.0.2.2`. Use
  `adb -s $FT_E2E_SERIAL shell input text '...'` instead, after clearing the
  field with `KEYCODE_MOVE_END` then repeated `KEYCODE_DEL`.
- **First run**: a "Location permissions" dialog covers `SmapMain` until
  dismissed, and it hides everything behind it. Dismiss it before asserting on
  any main-screen view.
- **Swipes**: keep clear of both screen edges. A vertical swipe within roughly
  80px of the left or right edge is taken as the system back gesture and closes
  the activity instead of scrolling. Swipe near the middle, over a label or a
  gap rather than a text field, since fields swallow the drag.
- **Serial changes**: the emulator gets a different serial each boot, and it can
  die mid-run (GPU context errors). Re-run `preflight.sh` and re-export
  `FT_E2E_SERIAL` rather than reusing an earlier one.
- **Physical devices**: a phone plugged in over USB shows up alongside the
  emulator. Never install onto it without asking - it holds a real install and
  real data.
- **Never trigger** alerts that block the UI thread; prefer `get_all_text` and
  `get_clickable_elements` over blind taps.

## Reporting

Finish with one table: suite, result, and for each failure the assertion that
broke plus the evidence (dialog text, log line, or SQL row). State plainly which
suites did not run. Do not describe a suite as passing on the strength of a
screenshot alone when a server-side check was available and skipped.

## References

- `references/environment.md` — server, database, projects, forms, key paths.
- `scripts/preflight.sh` — bring the environment up.
- `scripts/device.sh` — device state and control.
- `scripts/verify.sh` — server-side assertions.
