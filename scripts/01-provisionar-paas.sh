#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need LOCATION RESOURCE_GROUP APP_PLAN WEBAPP SQL_SERVER SQL_DATABASE SQL_USER DB_PASSWORD
if ! az group exists --name "$RESOURCE_GROUP" | tr -d '\r' | grep -qx true; then
 az group create -n "$RESOURCE_GROUP" -l "$LOCATION" -o none
fi
az sql server create -g "$RESOURCE_GROUP" -n "$SQL_SERVER" -l "$LOCATION" -u "$SQL_USER" -p "$DB_PASSWORD" --enable-public-network true -o none
az sql db create -g "$RESOURCE_GROUP" -s "$SQL_SERVER" -n "$SQL_DATABASE" --service-objective Basic --backup-storage-redundancy Local -o none
# Azure services firewall rule for academic demonstration; tighten for production.
az sql server firewall-rule create -g "$RESOURCE_GROUP" -s "$SQL_SERVER" -n AllowAzureServices --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o none
if [[ -n "${MY_IP:-}" ]]; then
 az sql server firewall-rule create -g "$RESOURCE_GROUP" -s "$SQL_SERVER" -n Workstation --start-ip-address "$MY_IP" --end-ip-address "$MY_IP" -o none
fi
az appservice plan create -g "$RESOURCE_GROUP" -n "$APP_PLAN" -l "$LOCATION" --is-linux --sku B1 -o none
az webapp create -g "$RESOURCE_GROUP" -p "$APP_PLAN" -n "$WEBAPP" --runtime 'JAVA:17-java17' -o none
az webapp update -g "$RESOURCE_GROUP" -n "$WEBAPP" --https-only true -o none
az webapp config set -g "$RESOURCE_GROUP" -n "$WEBAPP" --always-on true --min-tls-version 1.2 --generic-configurations '{"healthCheckPath":"/actuator/health"}' -o none
printf 'App: https://%s.azurewebsites.net
SQL: %s.database.windows.net
' "$WEBAPP" "$SQL_SERVER"
