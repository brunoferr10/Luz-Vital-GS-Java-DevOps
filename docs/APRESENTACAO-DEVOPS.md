# Apresentação presencial · DevOps Tools & Cloud Computing

## Abertura

“Minha entrega é de DevOps. Usamos a API Java Luz Vital para demonstrar um fluxo rastreável: tarefa no Boards, branch e commit no Repos, revisão por PR na main protegida, build com testes automáticos, artefato e Release automática para a Azure. A infraestrutura é criada por scripts Azure CLI.”

## Perguntas e respostas

| Pergunta | Resposta |
|---|---|
| O que é CI? | Integração contínua: compilar, testar e empacotar as mudanças integradas à main. |
| O que é a Release? | Fluxo que recebe o artefato aprovado na CI e o implanta em nuvem. |
| Por que duas pipelines? | Separar a construção do pacote da sua publicação; a Release usa o mesmo JAR, sem recompilar. |
| O que dispara cada uma? | CI: atualização protegida da main após merge. Release: conclusão bem-sucedida da CI na main. |
| Como provar rastreabilidade? | Abrir a tarefa e mostrar os links da branch, commit AB#ID e PR. |
| Como proteger a main? | Revisor obrigatório, vínculo com Work Item, revisor padrão e revisão das permissões de bypass. |
| Pode autoaprovar? | Sim, a simulação é permitida pelo enunciado; a política deve autorizar o solicitante. |
| Como cria a nuvem? | Scripts Azure CLI versionados em /scripts, com nomes e configurações via ambiente. |
| Onde ficam app e banco? | App Service Linux Java 17 e Azure SQL. A VM apoia a operação e hospeda RabbitMQ. |
| Por que não só VM? | O enunciado lista App Service/ACI para o app e PaaS/container para o banco; usamos as opções PaaS. |
| Quais artefatos são publicados? | JAR/SQL em drop e relatórios JUnit; também há cobertura JaCoCo. |
| O que acontece se o teste falhar? | A CI falha e a Release por conclusão bem-sucedida não é disparada. |
| Como a Release escolhe o pacote? | O recurso de pipeline vincula o download à execução da CI que a disparou. |
| Como comprovar o deploy? | Abrir /api/v1/versao em HTTPS e mostrar a mensagem alterada no código da PR. |
| Como comprovar persistência? | SELECT diretamente no Azure SQL após POST, PUT e DELETE de cada tabela. |
| Como tratar segredos? | Ambiente/App Settings, variáveis secretas quando usadas na Library e conexão federada; nunca senha no Git. |
| Se faltar agente? | Habilitar paralelismo hospedado ou configurar um agente próprio com as dependências necessárias. |
| Como validar a operação? | Smoke test após deploy e health check das dependências. |
| E se a Release falhar? | Inspecionar a task/log e corrigir configuração. Para reverter aplicação, reimplantar um artefato anterior compatível; rollback não está automatizado neste pacote. |

## Ordem da demonstração presencial

1. Aplicação em nuvem funcionando e CRUD com banco persistente.
2. Desenho macro e recursos criados pelos scripts.
3. Tarefa, PR, políticas, execução de CI, resultados dos testes e artefatos.
4. Release automática, execução disparadora e verificação do deploy.

Concentre a explicação no processo de entrega e operação. Explique o domínio Java apenas o suficiente para demonstrar os endpoints e os dados persistidos.
