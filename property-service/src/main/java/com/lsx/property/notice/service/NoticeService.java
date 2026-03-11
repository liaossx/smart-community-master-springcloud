package com.lsx.property.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.property.notice.dto.NoticeCreateDTO;
import com.lsx.property.notice.dto.NoticeReadStatDTO;
import com.lsx.property.notice.dto.NoticeVO;
import com.lsx.property.notice.entity.SysNotice;
import java.util.List;

public interface NoticeService extends IService<SysNotice> {
    Long createNotice(NoticeCreateDTO dto, Long adminId);

    Page<NoticeVO> listNotices(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 缂佺喕顓搁悽銊﹀煕閺堫亣顕伴崗顒€鎲￠弫浼村櫤
     */
    long countUnread(Long userId);

    void markAsRead(Long noticeId, Long userId);

    void deleteNotice(Long noticeId, Long adminId);

    SysNotice getById(Long id);

    void updateNotice(Long id, NoticeCreateDTO dto, Long adminId);

    void publishNotice(Long id, Long adminId);

    void publishNotice(Long id, Long adminId, Long communityId);

    void offlineNotice(Long id, Long adminId);

    void batchDelete(List<Long> noticeIds, Long adminId);

    void batchOffline(List<Long> noticeIds, Long adminId);

    NoticeReadStatDTO getReadStat(Long noticeId);
}


