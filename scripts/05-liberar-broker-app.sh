#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need RESOURCE_GROUP WEBAPP VM_NAME
VM_RESOURCE_GROUP="${VM_RESOURCE_GROUP:-$RESOURCE_GROUP}"
ips="$(az webapp show -g "$RESOURCE_GROUP" -n "$WEBAPP" --query possibleOutboundIpAddresses -o tsv)"
[[ -n "$ips" ]] || { echo 'Sem IPs de saida do App Service'; exit 1; }
IFS=',' read -r -a sources <<< "$ips"
az network nsg rule create -g "$VM_RESOURCE_GROUP" --nsg-name "${VM_NAME}NSG" -n RabbitAppService --priority 1100 --access Allow --protocol Tcp --source-address-prefixes "${sources[@]}" --destination-port-ranges 5672 -o none
printf '%s
' 'Regra limitada aos IPs de saida do App Service. Rode novamente se plano/regiao alterar.'
