package funds.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMqDlqConfigTest {

    private RabbitMqDlqConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMqDlqConfig();
    }

    @Test
    @DisplayName("mainExchange should be durable TopicExchange named etl.events.exchange")
    void testMainExchange() {
        TopicExchange exchange = config.mainExchange();
        assertNotNull(exchange);
        assertEquals("etl.events.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    @DisplayName("deadLetterExchange should be durable DirectExchange named etl.events.dlx")
    void testDeadLetterExchange() {
        DirectExchange exchange = config.deadLetterExchange();
        assertNotNull(exchange);
        assertEquals("etl.events.dlx", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    @DisplayName("deadLetterQueue should be durable and named fund.import.trigger.dlq")
    void testDeadLetterQueue() {
        Queue dlq = config.deadLetterQueue();
        assertNotNull(dlq);
        assertEquals("fund.import.trigger.dlq", dlq.getName());
        assertTrue(dlq.isDurable());
    }

    @Test
    @DisplayName("dlqBinding should bind DLQ to DLX with routing key fund.import.failed")
    void testDlqBinding() {
        Binding binding = config.dlqBinding();
        assertNotNull(binding);
        assertEquals("fund.import.trigger.dlq", binding.getDestination());
        assertEquals("etl.events.dlx", binding.getExchange());
        assertEquals("fund.import.failed", binding.getRoutingKey());
    }

    @Test
    @DisplayName("mainQueue should configure x-dead-letter-exchange and x-dead-letter-routing-key arguments")
    void testMainQueueArguments() {
        Queue queue = config.mainQueue();
        assertNotNull(queue);
        assertEquals("fund.import.trigger.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals("etl.events.dlx", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("fund.import.failed", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    @DisplayName("mainBinding should bind mainQueue to mainExchange with wildcard routing key fund.etl.#")
    void testMainBinding() {
        Binding binding = config.mainBinding();
        assertNotNull(binding);
        assertEquals("fund.import.trigger.queue", binding.getDestination());
        assertEquals("etl.events.exchange", binding.getExchange());
        assertEquals("fund.etl.#", binding.getRoutingKey());
    }
}