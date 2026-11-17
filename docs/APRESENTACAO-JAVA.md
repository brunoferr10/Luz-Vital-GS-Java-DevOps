# Como explicar o Java na apresentação

## Visão geral

“A API é o backend do Luz Vital, consumido pelo app Mobile. Ela separa os dados de cada cuidador, mantém pacientes e aparelhos, registra a falta de energia e oferece ao gestor uma fila de prioridade por autonomia restante. A IA gera um plano logístico com base nos aparelhos e a mensageria registra o processamento assíncrono da ocorrência.”

Somente diga que o Mobile está integrado depois de demonstrar essa integração no app real.

## Classes e responsabilidades

| Classes | Explicação |
|---|---|
| LuzVitalApplication | Inicia o Spring Boot e habilita agendamentos |
| Usuario | Conta de acesso: email único, hash de senha e papel |
| Paciente | Pessoa assistida, vinculada ao cuidador; guarda último plano |
| Aparelho | Equipamento do paciente e autonomia declarada |
| Ocorrencia | Queda de energia, instante, autonomia congelada, status e flags de evento |
| *Repository | Interfaces Spring Data JPA; persistência e consultas derivadas |
| Dtos | Entradas com validação e saídas sem senha/entidade JPA |
| AuthService/AuthController | Registro de cuidador, login e emissão JWT |
| SecurityConfig | BCrypt, validação de JWT, sessão stateless e leitura de papel |
| Bootstrap | Cria primeiro gestor a partir de variáveis, sem senha fixa |
| DomainService | Propriedade dos dados, CRUD, prioridade, idempotência e dependências |
| PacienteController/AparelhoController | Rotas REST paginadas e CRUD |
| OcorrenciaController | Abertura, consulta, fila e encerramento pelo gestor |
| UsuarioController | Administração de contas pelo gestor |
| PlanoService/PlanoController | Busca contexto técnico, chama Spring AI e persiste plano estruturado |
| MessagingConfig | Exchange, filas, binding, DLQ e serialização de eventos |
| EventPublisher | Reenvia eventos pendentes a cada 5 s e espera confirmação |
| EventConsumer | Consome evento assíncrono; repetição não duplica transição |
| ApiExceptionHandler | Padroniza erros, sem expor SQL ou stack trace ao cliente |
| OpenApiConfig | Swagger/OpenAPI e segurança Bearer |
| VersaoController | Endpoint público simples para demonstrar a alteração em nuvem |

## IDs e ordem correta

| Operação | ID já necessário |
|---|---|
| Registro/login | Nenhum ID; o registro devolve ID do cuidador |
| Criar paciente | `cuidadorId` existente e com papel CUIDADOR |
| Criar aparelho | `pacienteId` existente e acessível ao usuário |
| Gerar plano | ID do paciente na URL, com aparelho já cadastrado |
| Abrir ocorrência | `pacienteId`, aparelho existente e `Idempotency-Key` |
| Atualizar/excluir | ID do recurso na URL |
| Listar/consultar fila | Não precisa ID de outra entidade; exige login/permissão |

RM identifica o aluno, não substitui os IDs gerados pelo banco. Para excluir registros, respeite as FKs: ocorrência encerrada → aparelhos → paciente → cuidador temporário.

## O que explicar do pom.xml

- Parent Spring Boot gerencia versões compatíveis e plugins.
- Java 17 define a versão-alvo.
- BOM Spring AI alinha as dependências da IA.
- Web fornece REST; Validation valida DTOs; Data JPA/Hibernate fazem ORM.
- Security e Resource Server protegem rotas e validam JWT.
- AMQP integra RabbitMQ; Spring AI OpenAI Starter usa endpoint compatível Gemini.
- Driver SQL Server conecta ao Azure SQL; H2 é somente local/teste.
- Springdoc gera Swagger; Actuator oferece health.
- Test e Security Test suportam JUnit/MockMvc; JaCoCo mede cobertura.
- Plugin Boot gera JAR executável com dependências.

## Perguntas prováveis

**Por que DTO?** Para controlar entrada/saída, aplicar validação e evitar expor senha ou ciclos dos relacionamentos.

**JWT é criptografado?** Aqui ele é assinado, não criptografado. Não inclui senha nem dados clínicos. Validade de 60 minutos e validação de emissor/assinatura.

**Por que não confiar só no papel?** Dois cuidadores têm o mesmo papel; a regra de propriedade impede um acessar pacientes do outro.

**Qual a diferença entre 401 e 403?** 401 é autenticação ausente/inválida; 403 é acesso proibido a quem já está autenticado.

**O que garante idempotência?** Chave persistida por solicitante com restrição única; repetição do mesmo conteúdo retorna o mesmo ID. Conflitos concorrentes podem gerar 409 e exigem retry com a mesma chave.

**Se o broker cair?** A ocorrência fica no banco com evento pendente. O agendador tenta novamente. A fila do gestor continua consultável, mas o health acusa dependência indisponível.

**Por que “ao menos uma vez”?** Pode ocorrer envio confirmado antes de a flag ser limpa. A repetição é possível e o consumidor precisa ser idempotente.

**Como a IA agrega valor?** Ela adapta a redação do plano logístico ao tipo e à autonomia dos aparelhos do paciente, no mesmo fluxo de cadastro. Não é chat genérico, e a resposta é salva.

**IA sempre acerta?** Não. O plano tem aviso fixo e exige revisão; não pode ser apresentado como orientação clínica validada.

**Como provar persistência?** Consultando diretamente o Azure SQL com SELECT após cada operação, não só com GET.
