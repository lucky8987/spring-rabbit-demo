package com.rabbit.config;

import com.rabbitmq.client.ConnectionFactory;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Created by lucky8987 on 17/7/17.
 */
@Configuration
@EnableRabbit
@ComponentScan({"com.rabbit"})
@PropertySource("classpath:common.properties")
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(env.getProperty("rabbitmq.host").trim());
        cf.setPort(Integer.valueOf(env.getProperty("rabbitmq.port").trim()));
        cf.setUsername(env.getProperty("rabbitmq.username").trim());
        cf.setPassword(env.getProperty("rabbitmq.password").trim());
        return cf;
    }

    @Bean
    public CachingConnectionFactory cachingConnectionFactory() {
        return new CachingConnectionFactory(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory());
        RetryTemplate retryTemplate = new RetryTemplate();
        // 重试机制
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(10.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        rabbitTemplate.setRetryTemplate(retryTemplate);
        return rabbitTemplate;
    }


    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cachingConnectionFactory());
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    /**
     * listener 容器 （consumer 需要手动确认消息）
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory listenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        listenerContainerFactory.setConnectionFactory(cachingConnectionFactory());
        // 设置手动 ACK
        listenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return listenerContainerFactory;
    }

    /**
     * listener 容器 （AcknowledgeMode：auto）
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory rabbitListenerContainerFactoryAutoAck() {
        SimpleRabbitListenerContainerFactory listenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        listenerContainerFactory.setConnectionFactory(cachingConnectionFactory());
        return listenerContainerFactory;
    }


    @Bean
    public Queue helloQueue() {
        Queue queue = new Queue("hello", false, false, true);
        return queue;
    }

    @Bean
    public TopicExchange topicExchange() {
        TopicExchange topicExchange = new TopicExchange("topic-exchange", false, true);
        topicExchange.setDelayed(true);
        return topicExchange;
    }

    @Bean
    public List<Binding> binding() {
        return new ArrayList<Binding>(){{
            add(BindingBuilder.bind(helloQueue()).to(topicExchange()).with("hello.#"));
            add(BindingBuilder.bind(qxQueue()).to(topicExchange()).with("qx.#"));
        }};
    }

    @Bean
    public Queue qxQueue() {
        Queue queue = new Queue("qx", false, false, true);
        return queue;
    }
}
