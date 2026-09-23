#!/usr/bin/env bash
# Verify what actually reached the local Smap server.
#
#   scripts/verify.sh submissions ["10 minutes"]  # rows in upload_event
#   scripts/verify.sh requests    [n]             # app requests in the Apache log
#   scripts/verify.sh tasks       [user]          # assignments for a user
#   scripts/verify.sh forms       ["A project"]   # forms in a project
#
# Checking the database and the web server log independently matters: the app
# can report a send as successful when the request never got past Apache.
set -uo pipefail

DB="${FT_E2E_DB:-survey_definitions}"
LOG="${FT_E2E_APACHE_LOG:-/opt/homebrew/var/log/httpd/access_log}"
UA="${FT_E2E_UA:-org.smap.smapTask.android}"

q() { psql -d "$DB" -P pager=off "$@"; }

case "${1:-submissions}" in
submissions)
    since="${2:-10 minutes}"
    echo "submissions in the last $since"
    q -c "select ue_id, upload_time, survey_name, user_name, status, instanceid
          from upload_event
          where upload_time > now() - interval '$since'
          order by ue_id desc limit 25;"
    ;;
requests)
    n="${2:-25}"
    if [ ! -r "$LOG" ]; then
        echo "cannot read $LOG - set FT_E2E_APACHE_LOG" >&2; exit 1
    fi
    echo "last $n requests from the app (field 3 is the authenticated user; '-' means none sent)"
    grep -a "$UA" "$LOG" | tail -"$n"
    ;;
tasks)
    user="${2:-}"
    echo "assignments${user:+ for $user}"
    q -c "select a.id, a.status, a.assignee_name, a.completed_date,
                 t.p_name as task, t.survey_name
          from assignments a join tasks t on t.id = a.task_id
          ${user:+where a.assignee_name = '$user'}
          order by a.id desc limit 25;"
    ;;
forms)
    proj="${2:-A project}"
    echo "forms in '$proj'"
    q -c "select s.s_id, s.display_name, s.version
          from survey s join project p on p.id = s.p_id
          where p.name = '$proj' and s.deleted = 'false'
          order by s.display_name;"
    ;;
*)
    sed -n '2,10p' "$0"
    exit 1
    ;;
esac
