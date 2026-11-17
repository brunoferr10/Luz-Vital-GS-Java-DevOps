#!/usr/bin/env bash
# Execute inside VM after loading .env.azure. Management port only listens locally.
source "$(dirname "$0")/common.sh"
need RABBIT_USER RABBIT_PASSWORD
# Password is passed via env file, not command-line literal; mode 600, ignored by git.
umask 077
printf 'RABBITMQ_DEFAULT_USER=%s\nRABBITMQ_DEFAULT_PASS=%s\n' "$RABBIT_USER" "$RABBIT_PASSWORD" > .rabbit.env
docker volume create luz-vital-rabbit >/dev/null
if docker container inspect luz-vital-rabbit >/dev/null 2>&1; then
 docker start luz-vital-rabbit >/dev/null
else
 docker run -d --name luz-vital-rabbit --restart unless-stopped --env-file .rabbit.env -p 5672:5672 -p 127.0.0.1:15672:15672 -v luz-vital-rabbit:/var/lib/rabbitmq rabbitmq:3.13-management >/dev/null
fi
rm -f .rabbit.env
printf '%s
' 'Broker iniciado. Abra no NSG somente as origens App Service com o script 05. Nao abra 5672 para Internet inteira.' 'Alterar senha no .env nao muda usuario ja persistido no volume; use rabbitmqctl ou mantenha a senha inicial.'
