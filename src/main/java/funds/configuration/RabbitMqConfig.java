package funds.configuration;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMqConfig {

    public static final String IMPORT_TRIGGER_QUEUE = "fund.import.trigger.queue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Listener factory configured specifically for fund import triggers.
     * Retries 3 times with exponential backoff before sending to DLQ.
     */
    @Bean(name = "importJobListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory importJobListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1); // Crucial for heavy batch jobs: process one message at a time

        // Attach retry interceptor with DLQ recoverer
        factory.setAdviceChain(importJobRetryInterceptor());

        return factory;
    }

    @Bean
    public RetryOperationsInterceptor importJobRetryInterceptor() {
        // When retries are exhausted (or a non-retryable exception is hit),
        // RejectAndDontRequeueRecoverer issues basic.reject(requeue=false),
        // triggering RabbitMQ to route the message to etl.events.dlx.
        RejectAndDontRequeueRecoverer recoverer = new RejectAndDontRequeueRecoverer();

        return RetryInterceptorBuilder.stateless()
                .retryOperations(createRetryTemplate())
                .recoverer(recoverer)
                .build();
    }

    private RetryTemplate createRetryTemplate() {
        // Define which exceptions should and should not be retried
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 8000) // 1s, 2s, 4s (max 8s)
                .notRetryOn(JobInstanceAlreadyCompleteException.class)
                .traversingCauses()
                .build();
    }
}