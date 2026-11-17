#!/usr/bin/env bash
# Execute INSIDE Ubuntu VM, using a regular sudo-enabled account.
set -Eeuo pipefail
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk maven git curl ca-certificates gnupg docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
# Official Microsoft repositories for CLI and SQL client.
curl -fsSL https://aka.ms/InstallAzureCLIDeb | sudo bash
curl -fsSL https://packages.microsoft.com/keys/microsoft.asc | sudo gpg --dearmor --yes -o /usr/share/keyrings/microsoft-prod.gpg
curl -fsSL https://packages.microsoft.com/config/ubuntu/22.04/prod.list | sudo tee /etc/apt/sources.list.d/mssql-release.list >/dev/null
sudo apt-get update
sudo ACCEPT_EULA=Y apt-get install -y mssql-tools18 unixodbc-dev
printf '%s
' 'Instalacao concluida. Saia e reconecte no SSH para aplicar o grupo docker.' 'sqlcmd: /opt/mssql-tools18/bin/sqlcmd'
