#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need RESOURCE_GROUP
printf 'Excluir TODOS os recursos e dados de %s? Digite o nome: ' "$RESOURCE_GROUP"
read -r answer
[[ "$answer" == "$RESOURCE_GROUP" ]] || { echo 'Cancelado'; exit 1; }
az group delete -n "$RESOURCE_GROUP" --yes --no-wait
# If VM_RESOURCE_GROUP differs, review and remove that group separately.
