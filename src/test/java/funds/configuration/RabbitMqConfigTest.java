package funds.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RabbitMqConfigTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    private RabbitMqConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMqConfig();
    }

    @Test
    @DisplayName("jsonMessageConverter should return Jackson2JsonMessageConverter instance")
    void testJsonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    @DisplayName("importJobListenerContainerFactory should create container with AUTO ack mode")
    void testImportJobListenerContainerFactory() {
        MessageConverter converter = config.jsonMessageConverter();
        SimpleRabbitListenerContainerFactory factory =
                config.importJobListenerContainerFactory(connectionFactory, converter);

        assertNotNull(factory);

        SimpleMessageListenerContainer container = factory.createListenerContainer();
        assertNotNull(container);
        assertEquals(AcknowledgeMode.AUTO, container.getAcknowledgeMode());
        assertEquals(connectionFactory, container.getConnectionFactory());
    }

    @Test
    @DisplayName("importJobRetryInterceptor should retry standard exceptions up to 3 times")
    void testRetryInterceptor_RetriesStandardExceptions() {
        RetryOperationsInterceptor interceptor = config.importJobRetryInterceptor();
        assertNotNull(interceptor);

        AtomicInteger executionAttempts = new AtomicInteger(0);

        // Implementing MessageListener matches the exact signature Spring AMQP advises
        MessageListener target = message -> {
            executionAttempts.incrementAndGet();
            throw new RuntimeException("Simulated transient connection error");
        };

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.setInterfaces(MessageListener.class);
        proxyFactory.addAdvice(interceptor);

        MessageListener proxy = (MessageListener) proxyFactory.getProxy();
        Message dummyMessage = new Message("test payload".getBytes());

        assertThrows(Throwable.class, () -> proxy.onMessage(dummyMessage));
        assertEquals(3, executionAttempts.get());
    }

    @Test
    @DisplayName("importJobRetryInterceptor should not retry JobInstanceAlreadyCompleteException (short-circuits at 1 attempt)")
    void testRetryInterceptor_DoesNotRetryJobInstanceAlreadyCompleteException() {
        RetryOperationsInterceptor interceptor = config.importJobRetryInterceptor();
        assertNotNull(interceptor);

        AtomicInteger executionAttempts = new AtomicInteger(0);

        MessageListener target = message -> {
            executionAttempts.incrementAndGet();
            // JobInstanceAlreadyCompleteException is a checked exception, rethrow as cause
            throw new RuntimeException(new JobInstanceAlreadyCompleteException("Quarter already processed"));
        };

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.setInterfaces(MessageListener.class);
        proxyFactory.addAdvice(interceptor);

        MessageListener proxy = (MessageListener) proxyFactory.getProxy();
        Message dummyMessage = new Message("test payload".getBytes());

        assertThrows(Throwable.class, () -> proxy.onMessage(dummyMessage));
        // traversingCauses() inspects the wrapped JobInstanceAlreadyCompleteException and aborts after 1 attempt
        assertEquals(1, executionAttempts.get());
    }
}