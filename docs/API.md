# Referência da aplicação usada no deploy

A entrega deste repositório é de **DevOps Tools & Cloud Computing**. Esta referência serve para executar os CRUDs e compreender a aplicação Java implantada. IA e integração Mobile pertencem ao projeto geral e não são condições da avaliação de DevOps.

## Contrato REST

Base: `/api/v1`. As listas recebem `page=0&size=20`, limite 100; retorno `content`, `totalElements`, `totalPages`, `number` e `size`. IDs são gerados pelo banco. Datas ISO-8601 em UTC (`Z`). IDs numéricos e chave idempotente persistidos pelo cliente.

| Método e rota | Finalidade |
|---|---|
| POST `/auth/registro` | Cadastro CUIDADOR |
| POST `/auth/login` | JWT |
| GET/POST `/usuarios` | Lista paginada/cria usuário (gestor) |
| GET/PUT/DELETE `/usuarios/{id}` | Consulta/atualiza/exclui usuário |
| GET/POST `/pacientes` | Lista paginada/cria paciente |
| GET/PUT/DELETE `/pacientes/{id}` | Consulta/atualiza/exclui paciente |
| POST `/pacientes/{id}/plano` | Gera e persiste plano com Spring AI |
| GET/POST `/aparelhos` | Lista paginada/cria aparelho |
| GET/PUT/DELETE `/aparelhos/{id}` | Consulta/atualiza/exclui aparelho |
| GET/POST `/ocorrencias` | Lista paginada/abre ocorrência |
| GET/PUT/DELETE `/ocorrencias/{id}` | Consulta/encerra/exclui ocorrência |
| GET `/ocorrencias/fila` | Prioridade recalculada (gestor) |
| GET `/versao` | Mensagem pública para simular alteração no vídeo |
| GET `/actuator/health` | Saúde da aplicação e dependências |

### JSONs essenciais

Registro/login:
```json
{"nome":"Cuidador fictício","email":"cuidador@example.com","senha":"PREENCHA_UMA_SENHA_FORTE"}
```
```json
{"email":"cuidador@example.com","senha":"PREENCHA_UMA_SENHA_FORTE"}
```

Paciente (`cuidadorId`: ID retornado pelo cadastro, não RM):
```json
{"nome":"Paciente fictício","endereco":"Rua de teste 100","telefone":"11999990000","cuidadorId":2}
```

Aparelho (`pacienteId`: ID retornado ao criar paciente):
```json
{"pacienteId":1,"modelo":"Ventilador de demonstração","tipo":"VENTILADOR","potenciaWatts":100,"autonomiaMinutos":120}
```
Tipos: `VENTILADOR`, `CONCENTRADOR`, `BOMBA`, `OUTRO`. Autonomia de 0 a 10080 minutos; potência de 1 a 100000 W.

Ocorrência (envie também `Idempotency-Key: UUID_GERADO_NO_CLIENTE`):
```json
{"pacienteId":1,"inicio":"2026-11-17T15:00:00Z","observacao":"Falta de energia informada pelo cuidador"}
```
Use o instante real declarado da queda, nunca uma data futura. Em reenvios, mantenha **o mesmo corpo e chave**; nova chave para novo pedido. O instante é normalizado a microssegundos, compatível com o banco. Repetição sequencial devolve o mesmo ID; chave com conteúdo diferente gera 409. Concorrência pode retornar 409: o cliente deve consultar e repetir com a mesma chave/corpo. O Azure SQL também impede duas ocorrências abertas para o mesmo paciente via índice filtrado.

Encerrar (gestor):
```json
{"status":"ENCERRADA","observacao":"Energia restabelecida; atendimento registrado"}
```
Não reabre ocorrência encerrada. Para excluir: encerrar e remover ocorrências → remover aparelhos → remover paciente → remover cuidador. Uma conta não pode excluir a si mesma.

Erros: 400 validação/JSON/cabeçalho; 401 JWT/credenciais; 403 papel ou propriedade; 404 registro; 409 dependência/duplicidade/conflito; 503 LLM indisponível. Exemplo:
```json
{"status":400,"codigo":"VALIDACAO","mensagem":"Revise os campos","timestamp":"2026-11-17T15:00:00Z","detalhes":["nome: não deve estar em branco"]}
```
Respostas do filtro de segurança usam os campos `status`, `codigo` e `mensagem`; não retornam stack trace ou detalhes internos.

## IA generativa aplicada ao fluxo

`POST /pacientes/{id}/plano` carrega os aparelhos daquele paciente e chama `ChatClient` do Spring AI. O LLM devolve um objeto `Plano` com `primeirasAcoes`, `pedirAjuda`, `preparacao` e `aviso`. O backend valida os campos e limite de tamanho, acrescenta aviso fixo e salva o JSON em `pacientes.plano_contingencia`. `GET /pacientes/{id}` devolve o último plano para o app armazenar offline. O backend não implementa o armazenamento no celular.

Configuração padrão: Gemini via endpoint compatível OpenAI, utilizando **Spring AI**, não uma chamada HTTP artesanal. Configure `AI_API_KEY`, `AI_ENABLED=true` e um `AI_MODEL` disponível para sua chave. Alternativa OpenAI: `AI_BASE_URL=https://api.openai.com`, `AI_COMPLETIONS_PATH=/v1/chat/completions` e modelo compatível com sua conta. Disponibilidade, quota e cobrança dependem do provedor. Nunca compartilhe a chave.

Sem chave/ativação, o endpoint retorna 503; não inventa resposta. Se o provedor falhar ou devolver estrutura inválida, o plano anterior permanece. Os testes usam um servidor LLM simulado e passam pelo `ChatClient` real, sem cobrança. O prompt reduz riscos, mas não garante correção factual: planos precisam de revisão humana.

## Mensageria e consistência

A ocorrência e a flag `eventoPendente=true` são persistidas na mesma transação. A cada 5 segundos o publisher tenta enviar `{"ocorrenciaId":123}` para `luz-vital.eventos`, routing key `energia.caiu`. Após confirmação do broker, limpa a flag. O consumidor assíncrono marca `eventoRecebido=true`; duplicatas não repetem a transição. A fila de erros `luz-vital.erros` recebe mensagens rejeitadas após tentativas.

Entrega **ao menos uma vez**, não exatamente uma. A fila do gestor consulta os dados já persistidos e não fica bloqueada pela indisponibilidade do broker. O evento registra processamento assíncrono para acompanhamento; não envia um chamado real à Defesa Civil. Uma transação concorrente do publisher/consumer pode ser repetida por controle otimista. O broker tem volume persistente na VM.

