#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need RESOURCE_GROUP WEBAPP SQL_SERVER SQL_DATABASE SQL_USER DB_PASSWORD JWT_SECRET BOOTSTRAP_EMAIL BOOTSTRAP_PASSWORD RABBIT_HOST RABBIT_USER RABBIT_PASSWORD
[[ ${#JWT_SECRET} -ge 32 ]] || { echo 'JWT_SECRET precisa de pelo menos 32 caracteres'; exit 1; }
# Temporary settings JSON prevents password literals in command arguments. CLI output suppressed.
umask 077
settings="$(mktemp)"
trap 'rm -f "$settings"' EXIT
export SQL_SERVER SQL_DATABASE SQL_USER DB_PASSWORD JWT_SECRET BOOTSTRAP_EMAIL BOOTSTRAP_PASSWORD RABBIT_HOST RABBIT_USER RABBIT_PASSWORD
python3 - "$settings" <<'CONFIG'
import json,os,sys
e=os.environ
s={"SPRING_PROFILES_ACTIVE":"prod","DB_URL":f'jdbc:sqlserver://{e["SQL_SERVER"]}.database.windows.net:1433;database={e["SQL_DATABASE"]};encrypt=true;trustServerCertificate=false;loginTimeout=30;',"DB_USER":e['SQL_USER'],"DB_PASSWORD":e['DB_PASSWORD'],"JWT_SECRET":e['JWT_SECRET'],"BOOTSTRAP_EMAIL":e['BOOTSTRAP_EMAIL'],"BOOTSTRAP_PASSWORD":e['BOOTSTRAP_PASSWORD'],"MESSAGING_ENABLED":"true","RABBIT_HOST":e['RABBIT_HOST'],"RABBIT_USER":e['RABBIT_USER'],"RABBIT_PASSWORD":e['RABBIT_PASSWORD'],"RABBIT_PORT":e.get('RABBIT_PORT','5672'),"RABBIT_SSL":e.get('RABBIT_SSL','false'),"AI_ENABLED":e.get('AI_ENABLED','false'),"AI_API_KEY":e.get('AI_API_KEY','disabled'),"AI_MODEL":e.get('AI_MODEL','gemini-2.5-flash'),"AI_BASE_URL":e.get('AI_BASE_URL','https://generativelanguage.googleapis.com'),"AI_COMPLETIONS_PATH":e.get('AI_COMPLETIONS_PATH','/v1beta/openai/chat/completions')}
json.dump(s,open(sys.argv[1],'w'))
CONFIG
az webapp config appsettings set -g "$RESOURCE_GROUP" -n "$WEBAPP" --settings "@$settings" -o none
