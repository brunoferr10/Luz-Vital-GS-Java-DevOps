# Matriz de requisitos e evidências

## Java Advanced

| Requisito | Arquivo/implementação | Validação restante |
|---|---|---|
| 3 entidades relacionadas, CRUD/JPA/paginação | Paciente, Aparelho, Ocorrencia; DomainService e controllers | Demonstração em nuvem |
| DTO/Bean Validation/Advice | Dtos.java, ApiExceptionHandler.java | Testes automáticos incluídos |
| Security/JWT/2 papéis | SecurityConfig, AuthService e proteção nos controllers | Demonstrar dois logins reais |
| Spring AI com valor ao cidadão | PlanoService: plano preventivo dos aparelhos do paciente | Chave/modelo e chamada real |
| RabbitMQ assíncrono | MessagingConfig, EventPublisher e EventConsumer | Validar transporte no broker real |
| Swagger/README/Mobile | OpenApiConfig, README, docs/MOBILE.md e examples/mobile | Integrar e demonstrar no app do grupo |

## DevOps

| Requisito | Preparado no pacote | Etapa externa |
|---|---|---|
| Projeto privado Git | Guias de criação/importação | Criar no Azure DevOps |
| Professor Basic/Admin | Instruções | Confirmar e-mail e conceder acesso |
| Boards/commit/branch/PR | Fluxo documentado AB#ID | Criar tarefa e vínculos reais |
| Main protegida | Políticas detalhadas | Aplicar e confirmar bypass |
| CI após merge | azure-pipeline.yml, trigger main/pr none | Criar definição e proteger main |
| Testes/JUnit/cobertura | src/test + Maven/JaCoCo | Executar no agente e mostrar artefatos |
| Release automática | azure-release.yml, pipeline resource | Conexão/grupo/permissões e definição |
| App/banco em nuvem | Scripts App Service + Azure SQL | Provisionar, DDL e implantar |
| Infra Azure CLI | scripts/00 a 09 | Executar e mostrar Portal |
| DDL /scripts/script-bd.sql | Incluído | Aplicar no SQL real |
| Dockerfile | dockerfiles/Dockerfile opcional + Compose broker | Dockerfile da API não usado no deploy PaaS |
| YAML na raiz | azure-pipeline.yml + azure-release.yml | Usar caminho correto |
| API documentada | Swagger e Postman/JSON | Demonstrar |
| Variáveis/segredos | Env examples, Library e runtime | Preencher sem publicar senhas |
| Arquitetura | docs/arquitetura.svg + Mermaid | Mostrar no vídeo |
| CRUD/SELECT todas tabelas | Coleção e scripts SELECT, 4 tabelas | Gravação contínua com SELECTs |
| PDF e vídeo | Roteiro pronto | Gerar com links reais após execução |

A existência dos arquivos não comprova a conclusão das etapas externas. O app Mobile precisa consumir efetivamente esta API; o adaptador de exemplo não substitui essa evidência. A solução entregue é um protótipo acadêmico com dados fictícios.
