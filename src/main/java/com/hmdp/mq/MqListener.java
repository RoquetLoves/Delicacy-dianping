package com.hmdp.mq;

import com.hmdp.constants.MqConstants;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Slf4j
public class MqListener {
    @Autowired
    private IVoucherOrderService voucherOrderService;


    @RabbitListener(queues = MqConstants.ORDER_VOUCHER_QUEUE)
    public void orderQueue(VoucherOrder voucherOrder){
        voucherOrderService.handleVoucherOrder(voucherOrder);
    }

}
