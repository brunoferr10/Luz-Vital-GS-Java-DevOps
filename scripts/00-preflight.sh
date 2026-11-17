#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need LOCATION VM_SIZE
az account show --query '{assinatura:name,id:id}' -o table
az policy assignment list --query '[].{nome:displayName,escopo:scope}' -o table
az vm list-usage --location "$LOCATION" -o table
az vm list-skus --location "$LOCATION" --resource-type virtualMachines --size "$VM_SIZE" --all --query '[].{nome:name,restricoes:restrictions}' -o json
az webapp list-runtimes --os linux --query "[?contains(@, 'JAVA:17')]" -o tsv
printf '%s
' 'Confirme regiao permitida, quota da familia da VM, App Service e SQL. A lista de SKU nao garante quota.'
