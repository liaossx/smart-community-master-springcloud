package com.lsx.property.notice.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lsx.property.notice.dto.BatchNoticeExpireDTO;
import com.lsx.property.notice.dto.ExpiringNoticeDTO;
import com.lsx.property.notice.dto.NoticeExpireDTO;
import com.lsx.property.notice.entity.SysNotice;
import com.lsx.property.notice.mapper.SysNoticeMapper;
import com.lsx.property.notice.service.ISysNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {

    @Autowired
    private SysNoticeMapper noticeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNoticeExpire(NoticeExpireDTO dto) {
        // 妤犲矁鐦夐崗顒€鎲￠弰顖氭儊鐎涙ê婀稉鏃€婀崚状态绘珟
        SysNotice notice = noticeMapper.selectById(dto.getNoticeId());
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸︺劍鍨ㄥ鑼额潶閸掔娀娅?);
        }

        // 鐠侊紕鐣绘潻鍥ㄦ埂閺冨爼妫?        LocalDateTime expireTime = calculateExpireTime(dto);

        // 閺囧瓨鏌婇弫鐗堝祦鎼?        SysNotice updateNotice = new SysNotice();
        updateNotice.setId(dto.getNoticeId());
        updateNotice.setExpireTime(expireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        int result = noticeMapper.updateById(updateNotice);
        if (result == 0) {
            throw new RuntimeException("鐠佸墽鐤嗘潻鍥ㄦ埂閺冨爼妫挎径杈Е");
        }

        log.info("鐠佸墽鐤嗛崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫块幋鎰, noticeId: {}, expireTime: {}", dto.getNoticeId(), expireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetNoticeExpire(BatchNoticeExpireDTO dto) {
        if (CollectionUtils.isEmpty(dto.getNoticeIds())) {
            throw new RuntimeException("閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?);
        }

        // 妤犲矁鐦夐幍鈧張澶婂彆閸涘﹥妲搁崥锕€鐡ㄩ崷銊ょ瑬閺堫亜鍨归梽?        List<SysNotice> notices = noticeMapper.selectList(
                new QueryWrapper<SysNotice>()
                        .in("id", dto.getNoticeIds())
                        .ne("deleted", 1)  // 娑撳秶鐡戞禍?鐏忚鲸妲搁張顏勫灩闂?        );

        if (notices.size() != dto.getNoticeIds().size()) {
            // 閹垫儳鍤稉宥呯摠閸︺劎娈慖D
            List<Long> foundIds = notices.stream()
                    .map(SysNotice::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundIds = dto.getNoticeIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new RuntimeException("娴犮儰绗呴崗顒€鎲℃稉宥呯摠閸︺劍鍨ㄥ鑼额潶閸掔娀娅? " + notFoundIds);
        }

        // 鐠侊紕鐣绘潻鍥ㄦ埂閺冨爼妫?        LocalDateTime expireTime = calculateBatchExpireTime(dto);

        // 閹靛綊鍣洪弴瀛樻煀
        SysNotice updateNotice = new SysNotice();
        updateNotice.setExpireTime(expireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        UpdateWrapper<SysNotice> wrapper = new UpdateWrapper<>();
        wrapper.in("id", dto.getNoticeIds())
                .ne("deleted", 1);  // 閺囧瓨鏌婇張顏勫灩闂勩倗娈戠拋鏉跨秿

        int result = noticeMapper.update(updateNotice, wrapper);

        log.info("閹靛綊鍣虹拋鍓х枂閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫块幋鎰, noticeIds: {}, count: {}, expireTime: {}",
                dto.getNoticeIds(), result, expireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearNoticeExpire(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸︺劍鍨ㄥ鑼额潶閸掔娀娅?);
        }

        // 娴ｈ法鏁pdateWrapper閺勫海鈥橀幐鍥х暰鐟曚焦娲块弬所有畱鐎涙顔?        UpdateWrapper<SysNotice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", noticeId)
                .eq("deleted", 0)
                .set("expire_time", null)  // 閺勫海鈥樼拋鍓х枂expire_time娑撶瘶ULL
                .set("update_time", LocalDateTime.now());

        int result = noticeMapper.update(null, wrapper);
        if (result == 0) {
            throw new RuntimeException("濞撳懘娅庢潻鍥ㄦ埂閺冨爼妫挎径杈Е");
        }

        log.info("濞撳懘娅庨崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫块幋鎰, noticeId: {}", noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extendNoticeExpire(Long noticeId, Integer days) {
        if (days == null || days <= 0) {
            throw new RuntimeException("瀵ゅ爼鏆遍弮鍫曟？韫囧懘銆忔径褌绨?婢?);
        }

        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸︺劍鍨ㄥ鑼额潶閸掔娀娅?);
        }

        LocalDateTime newExpireTime;
        if (notice.getExpireTime() == null) {
            // 婵″倹鐏夐崢鐔告降濮橀晲绗夋潻鍥ㄦ埂销毁涘苯姘ㄩ崺杞扮艾瑜版挸澧犻弮鍫曟？鐠侊紕鐣?            newExpireTime = LocalDateTime.now().plusDays(days);
        } else if (notice.getExpireTime().isBefore(LocalDateTime.now())) {
            // 婵″倹鐏夊鑼剁箖閺堢噦绱濈亸鍙樼矤瑜版挸澧犻弮鍫曟？瀵偓婵顓哥粻?            newExpireTime = LocalDateTime.now().plusDays(days);
        } else {
            // 婵″倹鐏夐張顏囩箖閺堢噦绱濈亸鍗炴闂€鎸庡瘹鐎规艾銇夐弫?            newExpireTime = notice.getExpireTime().plusDays(days);
        }

        SysNotice updateNotice = new SysNotice();
        updateNotice.setId(noticeId);
        updateNotice.setExpireTime(newExpireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        int result = noticeMapper.updateById(updateNotice);
        if (result == 0) {
            throw new RuntimeException("瀵ゅ爼鏆辨潻鍥ㄦ埂閺冨爼妫挎径杈Е");
        }

        log.info("瀵ゅ爼鏆遍崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫块幋鎰, noticeId: {}, days: {}, newExpireTime: {}",
                noticeId, days, newExpireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchExtendNoticeExpire(List<Long> noticeIds, Integer days) {
        if (CollectionUtils.isEmpty(noticeIds)) {
            throw new RuntimeException("閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?);
        }

        if (days == null || days <= 0) {
            throw new RuntimeException("瀵ゅ爼鏆遍弮鍫曟？韫囧懘銆忔径褌绨?婢?);
        }

        int successCount = 0;
        int failCount = 0;
        List<Long> failedIds = new java.util.ArrayList<>();

        for (Long noticeId : noticeIds) {
            try {
                extendNoticeExpire(noticeId, days);
                successCount++;
            } catch (Exception e) {
                log.warn("瀵ゅ爼鏆遍崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫挎径杈Е, noticeId: {}, error: {}", noticeId, e.getMessage());
                failCount++;
                failedIds.add(noticeId);
            }
        }

        if (failCount > 0) {
            log.warn("閹靛綊鍣哄鍫曟毐閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫块柈銊ュ瀻婢惰精瑙? total: {}, success: {}, fail: {}, failedIds: {}",
                    noticeIds.size(), successCount, failCount, failedIds);
        } else {
            log.info("閹靛綊鍣哄鍫曟毐閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫块崗銊╁劥閹存劕濮? total: {}, days: {}", noticeIds.size(), days);
        }
    }

    @Override
    public List<ExpiringNoticeDTO> getExpiringSoonNotices(Integer daysThreshold) {
        if (daysThreshold == null || daysThreshold <= 0) {
            daysThreshold = 7; // 姒涙顓?婢垛晛鍞存潻鍥ㄦ埂
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdTime = now.plusDays(daysThreshold);

        // 閺屻儴顕楅崡鍐茬殺鏉╁洦婀￠惃鍕彆閸?        List<SysNotice> notices = noticeMapper.selectList(
                new QueryWrapper<SysNotice>()
                        .eq("deleted", 0)
                        .eq("publish_status", "PUBLISHED")
                        .isNotNull("expire_time")
                        .gt("expire_time", now) // 閺堫亣绻冮張?                        .le("expire_time", thresholdTime) // 閸楀啿鐨㈡潻鍥ㄦ埂
                        .orderByAsc("expire_time") // 閹稿绻冮張鐔告闂傛潙宕屾惔蹇ョ礄閺堚偓鏉╂垼绻冮張鐔烘畱閸︺劌澧犻敍?        );

        // 鏉烆剚宕叉稉绡婽O销毁涘矁顓哥粻妤€澧挎担娆忋亯閺?        return notices.stream()
                .map(notice -> {
                    ExpiringNoticeDTO dto = new ExpiringNoticeDTO();
                    dto.setId(notice.getId());
                    dto.setTitle(notice.getTitle());
                    dto.setTargetType(notice.getTargetType());
                    dto.setPublishTime(notice.getPublishTime());
                    dto.setExpireTime(notice.getExpireTime());
                    dto.setTopFlag(notice.getTopFlag());

                    // 鐠侊紕鐣婚崜鈺€缍戞径鈺傛殶
                    if (notice.getExpireTime() != null) {
                        long daysLeft = java.time.Duration.between(now, notice.getExpireTime()).toDays();
                        dto.setDaysLeft(daysLeft);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
    /**
     * 鐠侊紕鐣绘潻鍥ㄦ埂閺冨爼妫?- 閸楁洑閲滈崗顒€鎲?     */
    private LocalDateTime calculateExpireTime(NoticeExpireDTO dto) {
        switch (dto.getExpireType()) {
            case NEVER:
                return null;

            case CUSTOM:
                if (dto.getCustomExpireTime() == null) {
                    throw new RuntimeException("閼奉亜鐣炬稊澶庣箖閺堢喐妞傞梻缈犵瑝閼虫垝璐熺粚?);
                }
                if (dto.getCustomExpireTime().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("鏉╁洦婀￠弮鍫曟？娑撳秷充值橀弮鈺€绨ぐ鎾冲閺冨爼妫?);
                }
                return dto.getCustomExpireTime();

            case DAYS_7:
                return LocalDateTime.now().plusDays(7);

            case DAYS_30:
                return LocalDateTime.now().plusDays(30);

            case MONTH_3:
                return LocalDateTime.now().plusMonths(3);

            default:
                // 婵″倹鐏夐張濉猘ys閸欏倹鏆熼敍灞煎▏閻⑩暊ays閸欏倹鏆?                if (dto.getDays() != null && dto.getDays() > 0) {
                    return LocalDateTime.now().plusDays(dto.getDays());
                }
                throw new RuntimeException("娑撳秵鏁幐浣烘畱鏉╁洦婀￠弮鍫曟？缁鐎? " + dto.getExpireType());
        }
    }

    /**
     * 鐠侊紕鐣绘潻鍥ㄦ埂閺冨爼妫?- 閹靛綊鍣虹拋鍓х枂
     */
    private LocalDateTime calculateBatchExpireTime(BatchNoticeExpireDTO dto) {
        NoticeExpireDTO singleDto = new NoticeExpireDTO();
        singleDto.setExpireType(dto.getExpireType());
        singleDto.setCustomExpireTime(dto.getCustomExpireTime());
        singleDto.setDays(dto.getDays());

        return calculateExpireTime(singleDto);
    }

    /**
     * 鏉堝懎濮弬瑙勭《销毁涙艾鍨介弬顓炲彆閸涘﹥妲搁崥锔芥箒閺?     */
    private boolean isValidNotice(SysNotice notice) {
        return notice != null &&
                (notice.getDeleted() == null || notice.getDeleted() == 0);
    }
}
