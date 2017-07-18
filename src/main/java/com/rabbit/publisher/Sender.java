package com.rabbit.publisher;

import com.alibaba.fastjson.JSONObject;
import java.time.LocalDateTime;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by lucky8987 on 17/6/28.
 */
@Component
public class Sender {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("#{topicExchange.name}")
    private String exchangeName;

    public void  send(String name) {
        String str = "send: " + name.concat(",").concat(LocalDateTime.now().withNano(0).toString());
        MessageProperties msp = new MessageProperties();
        msp.setDelay(10000); // 延迟10s发送
        amqpTemplate.send(exchangeName, "hello.test", MessageBuilder.withBody(str.getBytes()).andProperties(msp).build());
        System.out.println("send success...");
    }

    public void sendAndReceive(String string) {
        // 采用应答方式(request/reply)
        JSONObject jsonObject = JSONObject.parseObject(string);
        jsonObject.put("currentTime", LocalDateTime.now().withNano(0).toString());
        string = jsonObject.toJSONString();
        Message replyMsg = amqpTemplate.sendAndReceive(exchangeName, "qx.test",  MessageBuilder.withBody(string.getBytes()).build());
        System.out.println("receive reply: " + String.valueOf(replyMsg.getBody()));
    }
}
