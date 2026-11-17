#!/usr/bin/env bash
set -Eeuo pipefail
need(){ for name in "$@"; do [[ -n "${!name:-}" ]] || { echo "Defina $name no ambiente." >&2; exit 1; }; done; }
need_az(){ command -v az >/dev/null || { echo 'Instale Azure CLI e execute az login.' >&2; exit 1; }; az account show --output none; }
