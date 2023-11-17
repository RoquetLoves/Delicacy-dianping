package com.hmdp.mq;

import com.hmdp.constants.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(MqConstants.HOTEL_EXCHANGE,true,false);
    }

    @Bean
    public Queue orderVoucherQueue(){
        return new Queue(MqConstants.ORDER_VOUCHER_QUEUE, true);
    }


    @Bean
    public Binding bindingQueue(){
        return BindingBuilder.bind(orderVoucherQueue()).to(topicExchange()).with(MqConstants.ORDER_KEY);
    }

}
