# Referência opcional · integração React Native / Expo

Este guia pertence à aplicação de referência. Integração Mobile não é etapa da entrega de DevOps deste repositório.

Use o backend deste repositório como origem única dos cadastros Java. Configure `EXPO_PUBLIC_API_URL=https://SEU_WEBAPP.azurewebsites.net/api/v1`. Não coloque chaves LLM, senhas SQL nem JWT_SECRET no app.

Dependências do exemplo:

```bash
npx expo install expo-secure-store @react-native-async-storage/async-storage
```

Copie `examples/mobile/api.ts` para o projeto Mobile. O exemplo implementa login, registro, listagem/cadastro e fila local de solicitações. Conecte os métodos às telas do grupo; ele não constitui um aplicativo completo e não foi executado em dispositivo neste pacote.

Fluxo: registro → guardar o ID de cuidador → login → JWT em SecureStore → paciente com cuidadorId → aparelho com pacienteId → ocorrência com data UTC/chave estável → acompanhamento. Gestor usa fila e encerramento. O app deve tratar 401 para novo login; 403 não deve causar retry infinito.

O POST de ocorrência aceita `Idempotency-Key`. Gere a chave e o corpo **antes de persistir** na fila local, envie novamente sem modificar. 2xx confirma processamento e permite retirar da fila. Falha de rede/5xx mantém pendente. 409 exige conferir o motivo e a ocorrência existente; não crie nova chave automaticamente. O mesmo paciente só pode ter uma aberta.

O campo `planoContingencia` no GET paciente é uma string contendo JSON estruturado. Salve a última versão revisada no aparelho para consulta sem rede. Endereço e plano contêm dados sensíveis: a fila de demonstração usa AsyncStorage sem criptografia; use dados fictícios e adote armazenamento cifrado antes de uso real.

Em Android físico, `localhost` aponta para o celular, não o computador. Prefira o HTTPS em nuvem na integração final. Para demonstrar o requisito, grave o app efetuando chamadas reais e o Swagger/banco refletindo os mesmos IDs.
