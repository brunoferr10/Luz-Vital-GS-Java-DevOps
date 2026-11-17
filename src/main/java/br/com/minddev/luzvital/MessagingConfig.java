package br.com.minddev.luzvital;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.messaging-enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {
  public static final String EXCHANGE = "luz-vital.eventos",
      QUEUE = "luz-vital.energia-caiu",
      KEY = "energia.caiu";

  @Bean
  DirectExchange exchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue queue() {
    return QueueBuilder.durable(QUEUE)
        .deadLetterExchange(EXCHANGE)
        .deadLetterRoutingKey("erro")
        .build();
  }

  @Bean
  Queue deadQueue() {
    return QueueBuilder.durable("luz-vital.erros").build();
  }

  @Bean
  Binding binding(Queue queue, DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(KEY);
  }

  @Bean
  Binding deadBinding(Queue deadQueue, DirectExchange exchange) {
    return BindingBuilder.bind(deadQueue).to(exchange).with("erro");
  }

  @Bean
  Jackson2JsonMessageConverter converter() {
    return new Jackson2JsonMessageConverter();
  }

  public record EnergiaCaiu(Long ocorrenciaId) {}
}
