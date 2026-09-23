# e2e environment

Everything the suites depend on. Verify with the scripts rather than trusting
this file — versions and form lists change.

## Local Smap server

| | |
|---|---|
| From the host | `http://localhost` (Apache 2.4 on port 80) |
| From the emulator | `http://10.0.2.2` |
| Behind Apache | Tomcat, contexts `surveyKPI`, `surveyMobileAPI`, `koboToolboxApi` |
| Auth | Basic, `WWW-Authenticate: Basic realm="smap"` |
| Apache access log | `/opt/homebrew/var/log/httpd/access_log` |

There is a **second, stale** Apache at `/var/log/apache2/` whose log stops
mid-morning. Reading it leads to the wrong conclusion that requests never
arrived. The homebrew log is the live one.

Plain http matters: upstream ODK only answers a Basic challenge over https.
`OkHttpOpenRosaServerClientProvider` wraps `BasicAuthenticator` so it also
answers for loopback, RFC1918 and link-local hosts, which is what makes
`http://10.0.2.2` work at all.

### Login endpoint

The app hits `<server>/login`. This server has no such endpoint and returns
404. That still counts as a successful login: `OkHttpConnection.loginRequest`
treats 404 as success for every host except `app.kontrolid.com` and
`*.smap.com.au`. A successful login is therefore two log lines — a 401
challenge, then a 404 carrying the authenticated user.

## Database

`survey_definitions` and `results`, owner `ws`, reachable as the local user with
no password.

| Table | Use |
|---|---|
| `upload_event` | Submissions. `ue_id, upload_time, instanceid, survey_name, user_name, status` |
| `assignments` | Task assignment and status |
| `tasks` | Task definitions |
| `survey`, `project` | Form and project metadata |

## Projects and forms

`A project` (16 forms) and `DHIS` (1 form) are the ones the suites use.

### Widget coverage

Open each, assert it renders, swipe through, exit without saving:

| Form | Exercises |
|---|---|
| `select and pulldata` | Cascading selects, `pulldata()` |
| `lookup` | Remote lookup handler |
| `submit lookup` | Lookup against submitted data |
| `test drill down` | Drill-down select |
| `reference locations` | Geo reference data |
| `pdf test` | PDF attachment rendering |
| `video` | Media capture widget |
| `Reference Test` | External reference data |
| `A read only form` | Read-only field handling |
| `Launcher` / `launched` | `SmapFormWidget` sub-form launch (run as a pair) |

### Submission targets

`submit test` is the simplest end-to-end form. `Inpatients` in `DHIS` is the one
with existing task assignments, so use it for the task suite.

## Device

| | |
|---|---|
| e2e AVD | `Pixel_9a` (override with `FT_E2E_AVD`) |
| Package | `org.smap.smapTask.android` |
| Launch activity | `au.smap.fieldTask.activities.SplashScreenActivity` |
| Settings | `/data/data/<pkg>/shared_prefs/general_prefs<project-uuid>.xml` |
| Forms and instances | `/sdcard/Android/data/<pkg>/files/projects/<uuid>/` |

Only one FieldTask flavour can be installed at a time — they share content
provider authorities, so installing `standard` over `xxx4` fails with
`INSTALL_FAILED_CONFLICTING_PROVIDER`. Uninstall the other flavour first.

### Settings keys

| Key | Meaning |
|---|---|
| `server_url`, `username`, `password` | Server credentials |
| `last_login` | Epoch ms, written **only** by a successful online login |
| `pw_policy` | `0` login every launch, `>0` days between logins, `-1` never re-prompt. Pushed by the server on each refresh, so a local change only survives until the next refresh |
| `smap_request_location_done` | Gates the location-permissions dialog that otherwise covers `SmapMain` |
