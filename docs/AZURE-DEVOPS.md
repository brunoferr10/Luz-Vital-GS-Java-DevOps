# Azure DevOps · configuração para avaliação

## 1. Projeto e professor

Crie um projeto **privado**, Git, chamado `Luz-Vital-GS`. Confirme com o professor seu e-mail atual: não copie e-mails de slides como se fossem dados confirmados. Em Organization Settings → Users, adicione-o com **Basic**, projeto Luz-Vital-GS e grupo **Project Administrators**. Confirme também em Project Settings → Permissions. O convite depende de autorização do usuário e do e-mail confirmado.

## 2. Azure Repos

Importe o repositório inicial privado do GitHub usando acesso temporário mínimo ou envie sua cópia local para o remoto Azure Repos:

```bash
git remote add azure https://dev.azure.com/ORGANIZACAO/Luz-Vital-GS/_git/luz-vital
git push -u azure main
```

Use a URL real oferecida em Repos → Clone. O Azure Repos precisa conter **o código completo**, não só links para GitHub. Depois use `azure` nas branches/PRs. Não configure proteção antes da primeira carga, porque ainda não haverá branch principal para proteger.

## 3. Boards e identidade RM

Crie uma tarefa inicial com título `[RM563489] Implantar backend Luz Vital`, atribua a Bruno e adicione descrição e critérios de aceite. Anote o ID real (ex.: 42). A identidade do Azure DevOps é uma conta; o RM deve constar no título/descrição e nome identificável da conta, não como um ID fictício de usuário.

Crie a branch pela tarefa, opção **Create branch**, ou vincule manualmente na seção Development. Exemplo:

```bash
git switch -c feature/42-mensagem-versao
# Altere src/main/java/br/com/minddev/luzvital/VersaoController.java
git add src/main/java/br/com/minddev/luzvital/VersaoController.java
git commit -m "feat: atualizar mensagem de versao AB#42"
git push -u azure feature/42-mensagem-versao
```

Troque 42 pelo ID real. `AB#42` cria vínculo do commit no Azure DevOps; não cria uma tarefa automaticamente. Abra PR para `main`, vincule o Work Item e atribua o revisor padrão.

## 4. Políticas obrigatórias da main

Repos → Branches → main → Branch policies:

- Minimum number of reviewers: **1**, obrigatório.
- **Allow requestors to approve their own changes: ativado**, conforme simulação permitida.
- Se houver opção de impedir aprovação do último autor do push, deixe desativada nessa simulação individual.
- Check for linked work items: **Required**.
- Automatically included reviewers: sua conta identificada com RM 563489, **Required**, escopo geral.
- Limite políticas de merge se desejado; squash mantém histórico da PR e vínculo da tarefa.

Em Branch security, confirme que sua conta/grupos **não possuem Allow para Bypass policies when pushing/completing pull requests**. Não faça push direto na main depois disso. Não adicione Build validation automática de PR se a exigência for executar build somente após merge. O teste local `mvn verify` precede a PR; CI é disparada pela atualização protegida da main.

Políticas são configurações do servidor e precisam ser demonstradas na interface. Arquivos YAML sozinhos não protegem a branch. Um administrador com bypass pode contornar políticas: confira permissões efetivas.

## 5. Conexão Azure

Project Settings → Service connections → Azure Resource Manager. Use **Workload identity federation**, escopo no RG do Luz Vital, nome exato **`sc-luz-vital`**. Autorize somente as pipelines necessárias. A criação pode depender de permissões da assinatura/Entra; não coloque segredo de service principal no repositório.

Pipelines → Library → Variable groups → **`luz-vital-prod`**:

| Variável | Valor |
|---|---|
| `RESOURCE_GROUP` | RG real criado pelo script |
| `WEBAPP` | Nome real do App Service |

Autorize a pipeline Release a usar o grupo. Os segredos do runtime são configurados pelo script 07 como App Settings; se os guardar também em Library, marque como **secret** e nunca imprima. As pipelines usam variáveis de ambiente e não embutem senha.

## 6. Pipeline CI

Pipelines → New pipeline → Azure Repos Git → seu repo → Existing YAML → `/azure-pipeline.yml`.

**Renomeie para `luz-vital-ci`**, exatamente como `source` na Release. A primeira execução manual valida permissões e disponibilidade de agente. Nas execuções seguintes, push da main após merge dispara CI.

Evidências: Maven `clean verify`, aba Tests com JUnit, Code Coverage JaCoCo, artefatos `drop` e `testes-junit`. Um build falho não dispara Release.

## 7. Pipeline Release

Crie uma **segunda pipeline YAML** apontando para `/azure-release.yml`; nome `luz-vital-release`. Não é necessário Classic Release: o enunciado aceita fluxo de deploy automático. Deixe a branch padrão das duas definições em `refs/heads/main` (pipeline settings → Default branch for manual and scheduled builds).

Autorize acesso ao artefato da CI, ao grupo `luz-vital-prod` e à conexão `sc-luz-vital`. Faça o cadastro das duas definições antes do merge que vai demonstrar. A Release dispara por conclusão bem-sucedida da CI na main e baixa o artefato da execução correspondente. O JAR não é recompilado.

A primeira autorização pode interromper o automático. Resolva as permissões durante o ensaio; a gravação final deve mostrar o fluxo sem acionamentos manuais.

## 8. Agentes

O YAML usa `ubuntu-latest` Microsoft-hosted com JDK 17. Se a organização não tiver paralelismo gratuito, solicite sua habilitação ou instale um agente próprio na VM conforme as instruções oferecidas pelo Azure DevOps. Para agente próprio:

1. Crie Agent pool, baixe o agente Linux oficial e configure em usuário dedicado, fora da pasta do projeto.
2. Instale JDK 17, Maven, Azure CLI e `curl`; mantenha o agente atualizado.
3. Substitua `pool: vmImage: ubuntu-latest` nas duas definições por `pool: name: NOME_DO_POOL`.
4. Remova `JavaToolInstaller@0`; configure o agente para encontrar `JAVA_HOME`/JDK17 e Maven. Em `Maven@4`, use `javaHomeOption: Path` e `jdkDirectory` do JDK17 ou configure as capacidades de JDK17 exigidas pela task.
5. Nunca rode o agente como root, e nunca exponha PAT em logs/README. Limite o PAT a Agent Pools e revogue após configurar.

Não apresente CI manual ou erro de agente como fluxo automático concluído.

## 9. Ensaio e conclusão

Faça a tarefa → branch → alteração Java → commit vinculado → PR vinculada → autoaprovação permitida → merge → CI → Release → URL pública. Depois conclua a tarefa e confira os links em Development. Faça SELECT após cada operação de CRUD no banco remoto.
