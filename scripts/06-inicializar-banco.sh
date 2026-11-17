#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need SQL_SERVER SQL_DATABASE SQL_USER DB_PASSWORD
SQLCMD="${SQLCMD:-/opt/mssql-tools18/bin/sqlcmd}"
export SQLCMDPASSWORD="$DB_PASSWORD"
"$SQLCMD" -S "tcp:${SQL_SERVER}.database.windows.net,1433" -d "$SQL_DATABASE" -U "$SQL_USER" -N -b -i "$(dirname "$0")/script-bd.sql"
unset SQLCMDPASSWORD
