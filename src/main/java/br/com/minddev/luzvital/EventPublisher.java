package br.com.minddev.luzvital;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.messaging-enabled", havingValue = "true", matchIfMissing = true)
public class EventPublisher {
  private final OcorrenciaRepository incidents;
  private final RabbitTemplate rabbit;
  private final TransactionTemplate tx;

  public EventPublisher(
      OcorrenciaRepository incidents, RabbitTemplate rabbit, PlatformTransactionManager manager) {
    this.incidents = incidents;
    this.rabbit = rabbit;
    this.tx = new TransactionTemplate(manager);
  }

  // Transactional outbox flag in the occurrence itself. Confirmed delivery, at least once.
  @Scheduled(fixedDelay = 5000, initialDelay = 5000)
  public void publish() {
    for (var o : incidents.findTop20ByEventoPendenteTrueOrderByIdAsc())
      try {
        var correlation = new CorrelationData(java.util.UUID.randomUUID().toString());
        rabbit.convertAndSend(
            MessagingConfig.EXCHANGE,
            MessagingConfig.KEY,
            new MessagingConfig.EnergiaCaiu(o.id),
            correlation);
        if (correlation.getFuture().get(10, TimeUnit.SECONDS).isAck())
          tx.executeWithoutResult(
              s ->
                  incidents
                      .findById(o.id)
                      .ifPresent(
                          current -> {
                            current.eventoPendente = false;
                            incidents.save(current);
                          }));
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Evento pendente para ocorrencia {}: broker indisponivel", o.id);
      }
  }
}
