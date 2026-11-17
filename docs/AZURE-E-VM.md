# Implantação Azure e VM de apoio

## Hospedagem escolhida

| Recurso | Uso |
|---|---|
| VM Ubuntu 22.04 | Terminal Linux + RabbitMQ persistente |
| App Service Linux B1 | JAR Spring Boot Java 17 |
| Azure SQL Basic | Banco relacional das quatro tabelas |
| Azure DevOps | Boards, Repos e pipelines |
| Provedor LLM | Gemini via Spring AI, HTTPS |

VM não hospeda app/banco da entrega: o professor especificou App Service/ACI e banco PaaS/container. A VM mantém a experiência Linux da Sprint 4 e hospeda o broker. Não é necessário ACR nesse fluxo. O Dockerfile da API é alternativa local, não usado pela Release padrão.

Os recursos têm cobrança e dependem de crédito/quota/policy. O script não cria nada até você executá-lo. B1 e Basic são tamanhos iniciais, não promessa de gratuidade. Caso App Service não tenha quota, avalie região/SKU autorizados ou implemente a alternativa ACI prevista no enunciado; não troque a aplicação para localhost.

## 1. Preparar ambiente

No Azure Cloud Shell Bash ou Linux com Azure CLI:

```bash
az login
az account list -o table
az account set --subscription 'ID_DA_ASSINATURA'
cp .env.azure.example .env.azure
```

Edite `.env.azure`, incluindo nomes globais únicos, segredos e IPv4 público da estação em `MY_IP`. Não exponha esse arquivo. Use `eastus2` apenas se ainda estiver liberada; o modelo é um ponto de partida. Não reutilize o grupo da Sprint 4.

```bash
set -a
source .env.azure
set +a
bash scripts/00-preflight.sh
```

Confirme policy da assinatura e quota efetiva da família de VM. `list-skus` mostrar o tamanho não significa que há quota. Se houver quota zero, selecione outra família/região liberada antes de executar o script de criação. A região armazenada no Resource Group não obriga todos os recursos a usá-la.

## 2. Provisionar serviços PaaS

```bash
bash scripts/01-provisionar-paas.sh
```

Cria RG, SQL Server, banco, firewall, App Service Plan e Web App via CLI. O nome do App Service e SQL Server precisa ser globalmente exclusivo. A execução repetida reaplica configurações, mas mudanças de nome/região exigem planejamento. Mantenha os mesmos valores até terminar a entrega.

## 3. Criar VM

Crie uma chave na máquina da qual fará SSH:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/luz_vital
bash scripts/02-criar-vm-apoio.sh
ssh -i ~/.ssh/luz_vital bruno@IP_DA_VM
```

`SSH_PUBLIC_KEY_FILE` deve apontar para a chave **pública** `.pub`. Se a chave foi criada no Cloud Shell, ela não estará automaticamente no Windows. Use a mesma origem/chave ou configure uma chave pública da sua estação. SSH liberado só para `MY_IP/32`; atualize quando mudar seu IP.

## 4. Preparar Linux

Na VM, clone seu repositório privado usando autenticação Git autorizada (PAT mínimo ou SSH, sem gravar o PAT no remoto).

```bash
git clone URL_DO_SEU_REPOSITORIO
cd luz-vital-gs-devops
bash scripts/03-instalar-vm.sh
```

Saia e volte no SSH para ativar grupo Docker. Crie `.env.azure` na VM com seus valores, sem compartilhar. Instalação requer acesso aos repositórios oficiais Ubuntu/Microsoft.

```bash
set -a
source .env.azure
set +a
bash scripts/04-iniciar-broker-vm.sh
```

RabbitMQ persiste em volume Docker. Porta de gestão 15672 só escuta em loopback: use túnel SSH para demonstração, nunca libere a gestão publicamente.

```bash
# Execute no computador, mantendo o terminal aberto:
ssh -i ~/.ssh/luz_vital -L 15672:127.0.0.1:15672 bruno@IP_DA_VM
```

Abra `http://localhost:15672`. Isso é apenas a interface administrativa do broker via túnel; app e banco permanecem na nuvem.

## 5. Rede e banco

No terminal com `az login`, preencha `RABBIT_HOST` com DNS público/IP fixo da VM. Se a VM estiver em grupo separado, exporte `VM_RESOURCE_GROUP`. Libere a porta 5672 **somente para os IPs de saída do App Service**:

```bash
bash scripts/05-liberar-broker-app.sh
```

O broker usa AMQP sem TLS no exemplo acadêmico. IPs de saída podem ser compartilhados com outros apps: autenticação forte continua obrigatória. Use só dados fictícios e adote AMQPS/rede privada antes de uso real.

O firewall SQL aceita os serviços Azure na demonstração; isso não elimina usuário/senha, mas é amplo. Autorize também o IP de saída da VM para executar SQL a partir dela:

```bash
az sql server firewall-rule create -g "$RESOURCE_GROUP" -s "$SQL_SERVER" -n VMClient --start-ip-address IP_PUBLICO_DA_VM --end-ip-address IP_PUBLICO_DA_VM
bash scripts/06-inicializar-banco.sh
bash scripts/07-configurar-app.sh
```

Execute `06` na VM com `sqlcmd` instalado ou em outra estação com cliente SQL. O DDL é para Azure SQL, não Oracle; a API de DevOps pode usar esse banco porque é uma das opções do enunciado. Se a entrega de banco exigir Oracle, mantenha esse trabalho em seu escopo próprio e combine a integração com a equipe.

## 6. Deploy

Na primeira validação, opcionalmente:

```bash
bash scripts/08-deploy-manual.sh
```

No vídeo, o deploy precisa vir da CI/Release automática: configure `docs/AZURE-DEVOPS.md`.

- Saúde: `https://SEU_WEBAPP.azurewebsites.net/actuator/health`
- Swagger: `https://SEU_WEBAPP.azurewebsites.net/swagger-ui/index.html`
- Versão: `https://SEU_WEBAPP.azurewebsites.net/api/v1/versao`

`/actuator/health` pode ficar DOWN se banco/broker estiverem indisponíveis. Não remova dependências do health para esconder falhas; corrija firewall, credenciais, host e esquema. Para logs: Portal Azure → App Service → Log stream (ative logs de aplicação quando necessário).

## 7. Validar em nuvem

1. Login e CRUD com dados fictícios, conforme Postman.
2. Rode `bash scripts/09-select-banco.sh` **após cada criação, atualização e exclusão** para comprovar banco, além dos GETs.
3. Verifique `evento_pendente=0` e `evento_recebido=1` após alguns segundos; confira fila/consumidor no RabbitMQ.
4. Ative `AI_ENABLED=true`, preencha chave/modelo válidos, rode `07` novamente e gere um plano real.
5. Teste outro cuidador: deve receber 403 ao acessar registros de terceiros.
6. Reinicie o App Service e confirme que os registros continuam no Azure SQL.

O primeiro gestor é inicializado via ambiente. Após validá-lo, remova `BOOTSTRAP_PASSWORD` e `BOOTSTRAP_EMAIL` do App Service (o usuário fica no banco).

## Problemas comuns

| Sintoma | Verificar |
|---|---|
| QuotaExceeded | Família da VM ou App Service sem quota; mudar apenas para opções autorizadas |
| InvalidResourceGroupLocation | Grupo existente com outra região; use o grupo novo e preserve sua localização |
| SSH timeout | IP público, chave, NSG e `MY_IP` atual |
| API não sobe | DDL aplicado, `prod`, JDBC, segredo JWT, credenciais e logs |
| SQL login timeout | Firewall do IP da VM/App, porta 1433 e DNS |
| Rabbit DOWN | Container/volume, NSG, IPs App Service, usuário e senha persistidos |
| LLM 503 | Ativação, chave, modelo, quota, endpoint ou resposta inválida |
| CI em fila | Habilitar paralelismo Microsoft-hosted ou configurar agente próprio |

## Remoção após avaliação

Somente quando não precisar mais das evidências/serviços:

```bash
bash scripts/99-limpar.sh
```

O script pede o nome exato do RG e remove tudo nele. Caso VM esteja em outro grupo, revise esse grupo separadamente. Não remova durante a avaliação.
