package com.rabbit.consumer;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Created by lucky8987 on 17/6/28.
 */
@Component
public class Receiver {

    @RabbitListener(queues = "hello", containerFactory = "rabbitListenerContainerFactoryAutoAck")
    @RabbitHandler
    public void process(byte[] str) {
        System.out.println("Receiver: " + new String(str) + "; " + LocalDateTime.now().withNano(0).toString());
    }

    @RabbitListener(queues = "qx", containerFactory = "rabbitListenerContainerFactory")
    @RabbitHandler
    public String processQx(Message message, Channel channel) {
        String string = new String(message.getBody());
        JSONObject jsonObject = JSONObject.parseObject(string);
        try {
            if(jsonObject.getInteger("ex") == 1) {
                throw new Exception("手动抛出异常");
            } else {
                System.out.println("消费成功 Receiver: " + string + "; " + LocalDateTime.now().withNano(0).toString());
            }
            // 处理成功，扔掉消息
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            System.out.println("消费失败, msg: " + string);
            try {
                if (jsonObject.containsKey("recover")) {
                    // false：重发给当前消费者，true：退回到当前queue中派给其他消费者 （经测试该策略不生效，推荐使用channel.basicReject）
                    channel.basicRecover(jsonObject.getBoolean("recover"));
                } else {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return "success";
    }

    @RabbitListener(queues = "qx")
    @RabbitHandler
    public void processQx2(Message message, Channel channel) {
        try {
            String string = new String(message.getBody());
            System.err.println("再次消费 Receiver: " + string + "; " + LocalDateTime.now().withNano(0).toString());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
