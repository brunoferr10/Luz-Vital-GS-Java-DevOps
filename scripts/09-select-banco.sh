#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need SQL_SERVER SQL_DATABASE SQL_USER DB_PASSWORD
export SQLCMDPASSWORD="$DB_PASSWORD"
"${SQLCMD:-/opt/mssql-tools18/bin/sqlcmd}" -S "tcp:${SQL_SERVER}.database.windows.net,1433" -d "$SQL_DATABASE" -U "$SQL_USER" -N -b -i "$(dirname "$0")/select-evidencias.sql" -W -s '|'
unset SQLCMDPASSWORD
