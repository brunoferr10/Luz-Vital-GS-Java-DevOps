package br.com.minddev.luzvital;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoService {
  private final DomainService domain;
  private final ChatClient chat;
  private final boolean enabled;
  private final TransactionTemplate tx;
  private final ObjectMapper mapper;

  public PlanoService(
      DomainService domain,
      ChatClient.Builder builder,
      @Value("${app.ai-enabled}") boolean enabled,
      PlatformTransactionManager manager,
      ObjectMapper mapper) {
    this.domain = domain;
    this.chat = builder.build();
    this.enabled = enabled;
    this.tx = new TransactionTemplate(manager);
    this.mapper = mapper;
  }

  public Dtos.Plano generate(Long id) {
    // Only technical device attributes are sent: no patient name, phone or address.
    String equipment =
        tx.execute(
            s -> {
              var p = domain.patient(id);
              var ds = domain.devicesRepository().findByPacienteId(p.id);
              if (ds.isEmpty())
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cadastre aparelhos antes do plano");
              return ds.stream()
                  .map(d -> d.tipo + ", autonomia declarada " + d.autonomiaMinutos + " minutos")
                  .collect(java.util.stream.Collectors.joining("; "));
            });
    if (!enabled)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "IA desativada: configure AI_ENABLED e AI_API_KEY");
    Dtos.Plano result;
    try {
      result =
          chat.prompt()
              .system(
                  "Voce redige um plano LOGISTICO para falta de energia. Use portugues simples e"
                      + " frases curtas. Nunca de orientacao clinica, ajuste de equipamento,"
                      + " dosagem, manobra ou garantia de autonomia. Se autonomia for zero,"
                      + " destaque necessidade de ajuda imediata. Oriente a seguir o plano aprovado"
                      + " pela equipe de saude; em risco imediato ligar 192 ou 193. Nao invente"
                      + " locais, telefones, disponibilidade ou promessas de socorro. Nao calcule"
                      + " condicoes clinicas. Retorne os campos primeirasAcoes, pedirAjuda,"
                      + " preparacao, aviso.")
              .user("Crie um plano preventivo para a familia com estes aparelhos: " + equipment)
              .call()
              .entity(Dtos.Plano.class);
      if (result == null
          || result.primeirasAcoes() == null
          || result.pedirAjuda() == null
          || result.preparacao() == null) throw new IllegalStateException("Resposta incompleta");
      result =
          new Dtos.Plano(
              result.primeirasAcoes(),
              result.pedirAjuda(),
              result.preparacao(),
              "Texto gerado por IA. Revise com a equipe de saude. Nao e orientacao clinica nem"
                  + " garantia de atendimento. Em risco imediato, ligue 192 ou 193.");
      String json = mapper.writeValueAsString(result);
      if (json.length() > 6000) throw new IllegalStateException("Resposta extensa");
      tx.executeWithoutResult(
          s -> {
            var p = domain.patient(id);
            p.plano = json;
            domain.patientsRepository().save(p);
          });
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "IA indisponivel ou resposta invalida. Use o ultimo plano revisado salvo.");
    }
    return result;
  }
}
