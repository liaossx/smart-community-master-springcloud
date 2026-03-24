package com.lsx.community.activity.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.community.activity.dto.ActivitySignupMessageDTO;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.entity.SysActivitySignup;
import com.lsx.community.activity.mapper.SysActivityMapper;
import com.lsx.community.activity.mapper.SysActivitySignupMapper;
import com.lsx.core.common.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ActivitySignupListener {

    @Autowired
    private SysActivitySignupMapper signupMapper;

    @Autowired
    private SysActivityMapper activityMapper;

    @RabbitListener(queues = MqConstants.ACTIVITY_SIGNUP_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleActivitySignupMessage(ActivitySignupMessageDTO msg) {
        log.info("接收到活动报名异步处理消息: activityId={}, userId={}", msg.getActivityId(), msg.getUserId());

        try {
            Long activityId = msg.getActivityId();
            Long userId = msg.getUserId();

            // 1. 幂等性校验：检查数据库是否已存在该报名记录
            Long count = signupMapper.selectCount(new QueryWrapper<SysActivitySignup>()
                    .eq("activity_id", activityId)
                    .eq("user_id", userId));
            
            if (count > 0) {
                log.warn("该用户已报名此活动，忽略此消息: activityId={}, userId={}", activityId, userId);
                return;
            }

            // 2. 插入报名记录
            SysActivitySignup signup = new SysActivitySignup();
            signup.setActivityId(activityId);
            signup.setUserId(userId);
            signup.setSignupTime(LocalDateTime.now());
            signupMapper.insert(signup);

            // 3. 更新活动报名人数
            SysActivity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                activity.setSignupCount((activity.getSignupCount() == null ? 0 : activity.getSignupCount()) + 1);
                activityMapper.updateById(activity);
            }

            log.info("活动报名异步处理成功: activityId={}, userId={}", activityId, userId);
        } catch (Exception e) {
            log.error("活动报名异步处理异常: ", e);
            // 这里可以考虑将失败消息写入死信队列或死信表，人工介入补偿
            throw e; // 抛出异常让消息重试
        }
    }
}