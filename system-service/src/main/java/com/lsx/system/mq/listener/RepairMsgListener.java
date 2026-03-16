package com.lsx.system.mq.listener;

import com.lsx.core.common.constant.MqConstants;
import com.lsx.core.common.dto.mq.RepairMsgDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RepairMsgListener {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.REPAIR_SUBMIT_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.REPAIR_EXCHANGE),
            key = MqConstants.REPAIR_SUBMIT_ROUTING_KEY
    ))
    public void listenRepairSubmit(RepairMsgDTO msg) {
        log.info("收到报修异步通知：报修单ID={}, 故障类型={}, 提交人ID={}", 
                msg.getRepairId(), msg.getFaultType(), msg.getUserId());
        
        // 这里可以执行实际的异步逻辑，例如：
        // 1. 发送短信/邮件给维修员
        // 2. 写入系统通知表
        // 3. 推送 WebSocket 消息到管理后台
        
        log.info("报修异步通知处理完成");
    }
}
