#!/usr/bin/env bash
source "$(dirname "$0")/common.sh"
need_az
need RESOURCE_GROUP LOCATION VM_NAME VM_USER VM_SIZE VM_DNS_LABEL SSH_PUBLIC_KEY_FILE MY_IP
# A separate group can be selected via VM_RESOURCE_GROUP when regional VM quota differs.
VM_RESOURCE_GROUP="${VM_RESOURCE_GROUP:-$RESOURCE_GROUP}"
if ! az group exists -n "$VM_RESOURCE_GROUP" | grep -qx true; then az group create -n "$VM_RESOURCE_GROUP" -l "$LOCATION" -o none; fi
# Expand leading ~ without eval.
SSH_PUBLIC_KEY_FILE="${SSH_PUBLIC_KEY_FILE/#\~/$HOME}"
[[ -f "$SSH_PUBLIC_KEY_FILE" ]] || { echo 'Crie a chave SSH com ssh-keygen primeiro.'; exit 1; }
az vm create -g "$VM_RESOURCE_GROUP" -n "$VM_NAME" -l "$LOCATION" --image Ubuntu2204 --size "$VM_SIZE" --admin-username "$VM_USER" --ssh-key-values "$SSH_PUBLIC_KEY_FILE" --public-ip-sku Standard --public-ip-address-dns-name "$VM_DNS_LABEL" --nsg-rule NONE -o none
az network nsg rule create -g "$VM_RESOURCE_GROUP" --nsg-name "${VM_NAME}NSG" -n SSHWorkstation --priority 1000 --access Allow --protocol Tcp --source-address-prefixes "$MY_IP/32" --destination-port-ranges 22 -o none
az vm show -d -g "$VM_RESOURCE_GROUP" -n "$VM_NAME" --query '{ip:publicIps,dns:fqdns}' -o table
printf '%s
' 'VM de apoio: terminal Linux e RabbitMQ. App e banco serao hospedados em PaaS.'
