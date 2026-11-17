# Roteiro de gravação e apresentação

Vídeo contínuo, sem cortes, pelo menos 720p, áudio claro, narração por voz. Antes, ensaie tudo, confira permissões/segredos e deixe Portal, DevOps, Swagger e terminal SQL abertos. Não exponha credenciais em tela.

## Sequência sugerida

1. **README e proposta.** “Somos o grupo Mind-Dev. O Luz Vital organiza pacientes eletrodependentes em apagões pela autonomia declarada dos aparelhos. Esta entrega hospeda a API Java e o banco na Azure.” Mostre escopo e arquitetura, com foco no caminho Boards → Repos → CI → artefato → Release → nuvem.
2. **Portal Azure.** Mostre RG, VM de apoio, App Service Plan/Web App, SQL Server/banco. Abra os scripts do repo e relacione os comandos aos recursos. A VM é apoio/broker; app e banco são PaaS.
3. **Políticas/acesso.** Mostre projeto privado, professor Basic/Project Administrators, main com revisor, Work Item e revisor padrão RM. Não filme dados sensíveis.
4. **Boards.** Crie uma tarefa nova para a gravação, atribuída a você. Descreva a alteração de mensagem de versão e anote o ID.
5. **Branch e código.** Crie branch vinculada. Altere a mensagem em `VersaoController.java` (por exemplo acrescente “GS 2026/2”). Mostre antes/depois. Use `AB#ID_REAL` no commit. Faça push da branch.
6. **PR e merge.** Abra PR para main, vincule tarefa, mostre revisor obrigatório e sua aprovação permitida. Conclua a PR sem bypass.
7. **CI completa.** Mostre disparo automático após merge, todas as tasks, testes, resultados e artefatos. Abra `drop` e mostre JAR e SQL. Mostre o artefato de JUnit e a cobertura.
8. **Release completa.** Mostre disparo pela CI, download do artefato correto, deploy e smoke test. Aguarde conclusão. Não dispare manualmente para simular automático.
9. **Mudança em nuvem.** Abra `/api/v1/versao` HTTPS e comprove a nova mensagem do código. Abra Swagger em nuvem.
10. **CRUD com persistência.** Use sequência abaixo. Mostre resultado HTTP e `SELECT` no Azure SQL após cada criação, alteração e exclusão. GET sozinho não comprova persistência.
11. **Operação da infraestrutura.** Mostre o health check e, se necessário, a conectividade do broker. Explique as variáveis de ambiente e a separação entre build e deploy sem exibir segredos. IA/Mobile não são etapas obrigatórias desta gravação de DevOps.
12. **Tarefa concluída.** Volte ao Boards, confirme links de branch/commit/PR e mova a tarefa para Done/Closed conforme processo. Finalize com app funcionando.

## CRUD das quatro tabelas

O projeto tem quatro tabelas, incluindo usuários. Demonstre todas para cobrir a redação mais exigente do enunciado:

| Ordem | Operação HTTP | Evidência SQL |
|---|---|---|
| 1 | POST usuário cuidador (gestor) | SELECT id,nome,email,papel FROM usuarios |
| 2 | GET e PUT usuário | Mesmo SELECT mostrando nome alterado |
| 3 | POST paciente vinculado ao cuidador | SELECT em pacientes |
| 4 | GET e PUT paciente | SELECT com alteração |
| 5 | POST aparelho vinculado ao paciente | SELECT em aparelhos |
| 6 | GET e PUT aparelho | SELECT com nova autonomia/modelo |
| 7 | POST ocorrência com Idempotency-Key | SELECT em ocorrencias |
| 8 | GET e PUT ocorrência (ENCERRADA) | SELECT com status/encerramento |
| 9 | DELETE ocorrência | SELECT sem o ID excluído |
| 10 | DELETE aparelho | SELECT sem o ID excluído |
| 11 | DELETE paciente | SELECT sem o ID excluído |
| 12 | DELETE cuidador temporário | SELECT sem o ID; mantenha o gestor inicial |

O arquivo `scripts/select-evidencias.sql` consulta as quatro tabelas sem hashes/senhas; `scripts/09-select-banco.sh` executa no Azure SQL. Não automatize todos os POST/PUT/DELETE de uma vez antes de mostrar os SELECTs: alterne as evidências durante a gravação.

## Antes de enviar

Assista ao vídeo publicado inteiro no YouTube: 720p, voz audível, sem cortes, sem segredos, políticas e acesso, alteração Java, pipeline automática, artefatos, SELECTs e tarefa final. Preencha links no README e gere PDF `Mind-Dev_rm563489.pdf` com os cinco integrantes/RMs, organização Azure DevOps e vídeo. O PDF final depende dos links reais.

## Presencial: pontos a explicar

- JWT autentica; papéis autorizam; checagem de propriedade impede acesso cruzado.
- Controller recebe DTO; serviço aplica regras; repositório persiste via JPA.
- API com três entidades de domínio relacionadas, mais usuários.
- Contagem usa instante da queda e autonomia congelada na abertura.
- Idempotência protege reenvios; SQL impõe unicidade; retry mantém a mesma chave.
- Evento assíncrono, fila durável, confirmação, outbox e deduplicação.
- IA é plano logístico integrado e persistido; nunca substitui decisão clínica.
- CI testa/empacota; Release implanta o mesmo pacote; políticas protegem main.
- VM apoia a operação, App Service hospeda aplicação e Azure SQL persiste dados.
