package com.lsx.property.notice.service;

import com.lsx.property.notice.dto.BatchNoticeExpireDTO;
import com.lsx.property.notice.dto.ExpiringNoticeDTO;
import com.lsx.property.notice.dto.NoticeExpireDTO;

import java.util.List;

public interface ISysNoticeService {

    /**
     * 鐠佸墽鐤嗛崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫?     */
    void setNoticeExpire(NoticeExpireDTO dto);

    /**
     * 閹靛綊鍣虹拋鍓х枂閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫?     */
    void batchSetNoticeExpire(BatchNoticeExpireDTO dto);

    /**
     * 濞撳懘娅庢潻鍥ㄦ埂閺冨爼妫块敍鍫ｎ啎娑撶儤妗堟稉宥堢箖閺堢噦绱?     */
    void clearNoticeExpire(Long noticeId);

    /**
     * 瀵ゅ爼鏆遍崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫?     */
    void extendNoticeExpire(Long noticeId, Integer days);

    /**
     * 閹靛綊鍣哄鍫曟毐閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫?     */
    void batchExtendNoticeExpire(List<Long> noticeIds, Integer days);

    /**
     * 閺屻儴顕楅崡鍐茬殺鏉╁洦婀￠惃鍕彆閸?     */
    List<ExpiringNoticeDTO> getExpiringSoonNotices(Integer daysThreshold);
}
