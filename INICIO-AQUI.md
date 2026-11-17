# DevOps Tools & Cloud Computing · início da entrega

Este repositório é a entrega de **DevOps Tools & Cloud Computing**. A API Java é a aplicação usada para comprovar provisionamento, testes, artefatos e deploy. Comece pelo Git; depois configuraremos VM, serviços Azure, Boards, Repos e Pipelines. O roteiro não exige concluir a disciplina de Java ou o app Mobile.

## 1. Extrair

Extraia o ZIP. Abra a pasta que contém `pom.xml`, `README.md` e `azure-pipeline.yml`. Evite subir uma pasta extra envolvendo o projeto.

## 2. Criar repositório privado

No GitHub, crie um novo repositório privado chamado `luz-vital-gs-devops`, vazio (sem README/licença/gitignore gerados pelo site). No Git Bash, dentro da pasta extraída:

```bash
git init -b main
git add .
git commit -m "feat: backend Luz Vital e estrutura DevOps"
git remote add origin https://github.com/SEU_USUARIO/luz-vital-gs-devops.git
git push -u origin main
```

Troque `SEU_USUARIO`. Se o repositório já existir e tiver conteúdo, não use force push: confira histórico e remoto antes. O ZIP não inclui pasta `.git` nem credenciais.

GitHub é uma cópia inicial; **a entrega exige Azure Repos**. Depois vamos importar/copiar para o Azure Repos e usar esse remoto nas tarefas, branches e PRs do vídeo.

## 3. Testar antes da nuvem

Instale JDK 17 e Maven 3.6.3+ (validado com 3.9.9). Em Linux/Git Bash:

```bash
cp .env.example .env
```

Edite `.env` e preencha segredos. Exemplos `.example` não contêm senha utilizável. `openssl rand -hex 32` gera um segredo para JWT. Escolha uma senha própria para o gestor e outra para RabbitMQ.

```bash
set -a
source .env
set +a
docker compose up -d
mvn clean verify
mvn spring-boot:run
```

Se quiser apenas os testes, basta `mvn clean verify`: eles usam perfil `test`, H2 e provedor IA simulado; não precisam de Docker nem chave paga.

Acesse `http://localhost:8080/swagger-ui/index.html`. Login com o gestor configurado, copie o `accessToken` e autorize no Swagger. Cadastre um cuidador, depois paciente, aparelho e ocorrência.

## 4. Próxima etapa

Siga `docs/AZURE-E-VM.md`. Vamos conferir região, quota e política da assinatura **antes** de criar VM, evitando o problema de quota da Sprint 4. Depois configuraremos DevOps e só gravaremos com o fluxo completo validado.

Não execute `99-limpar.sh` antes da correção da GS: ele exclui os recursos e dados.
