# Validação do pacote · 17/11/2026

Executado com JDK 17 e Maven 3.9.9:

```text
mvn -B -ntp verify
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Suíte | Testes | Cobertura funcional |
|---|---:|---|
| ApiIntegrationTest | 7 | JWT/papéis, propriedade, CRUD das 4 tabelas, validação, paginação, dependências, idempotência, prioridade e consumidor repetido |
| PlanoIntegrationTest | 3 | ChatClient real com servidor LLM simulado; plano salvo sem dados pessoais no prompt; resposta inválida preserva plano; falha do provedor |
| SchemaCompatibilityTest | 1 | Geração de metadados SQLServerDialect e conferência dos tipos temporais/colunas críticas contra DDL |

Verificação do JAR empacotado com `java -jar`:

- `/api/v1/versao`: HTTP 200.
- `/swagger-ui/index.html`: HTTP 200.
- `/v3/api-docs`: HTTP 200, esquema Bearer e rota de plano presentes.
- `/actuator/health`: HTTP 200 com H2 e indicador Rabbit explicitamente desativado neste smoke test isolado.
- Login do gestor inicial: token emitido, sem imprimir o token.
- Perfil `application-prod.yml` presente no JAR.

Scripts shell conferidos com `bash -n`. JSON, XML/POM, SVG e YAML analisados sem erro de sintaxe. Fontes Java formatados.

## Limites da verificação

Não houve provisionamento na conta Azure do aluno nem acesso ao seu Azure DevOps. Não foram testados o transporte real RabbitMQ, a chamada real ao Gemini, a execução no app Mobile ou os comandos Azure em uma assinatura. O teste de esquema confere metadados, não substitui `ddl-auto=validate` com Azure SQL conectado. Essas verificações são obrigatórias no ensaio da implantação e vídeo; consulte os guias.

O ZIP contém fontes e configuração, sem diretório `target`, credenciais ou repositório `.git`. Gere o JAR com Maven no seu ambiente ou pela CI. Relatórios JUnit e JaCoCo serão gerados em `target` e publicados pela pipeline.

## Revisão de foco da entrega

Documentação e arquitetura reorganizadas para DevOps Tools & Cloud Computing. Fontes Java, testes, scripts de infraestrutura e YAML de execução foram preservados. A revisão alterou a apresentação do pacote; os 11 testes acima referem-se à mesma aplicação.
