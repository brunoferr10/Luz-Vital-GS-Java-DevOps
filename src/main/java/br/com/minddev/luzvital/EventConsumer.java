package br.com.minddev.luzvital;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.messaging-enabled", havingValue = "true", matchIfMissing = true)
public class EventConsumer {
  private final OcorrenciaRepository incidents;

  public EventConsumer(OcorrenciaRepository incidents) {
    this.incidents = incidents;
  }

  @RabbitListener(queues = MessagingConfig.QUEUE)
  @Transactional
  public void consume(MessagingConfig.EnergiaCaiu event) {
    incidents
        .findById(event.ocorrenciaId())
        .ifPresent(
            o -> {
              if (!o.eventoRecebido) {
                o.eventoRecebido = true;
                incidents.save(o);
              }
            });
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info("energia.caiu consumido; ocorrencia={}", event.ocorrenciaId());
  }
}
