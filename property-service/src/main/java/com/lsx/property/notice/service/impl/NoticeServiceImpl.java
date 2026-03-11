package com.lsx.property.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.property.client.HouseServiceClient;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.dto.external.HouseDTO;
import com.lsx.property.notice.dto.NoticeCreateDTO;
import com.lsx.property.notice.dto.NoticeVO;
import com.lsx.property.notice.dto.NoticeReadStatDTO;
import com.lsx.property.notice.entity.SysNotice;
import com.lsx.property.notice.entity.SysNoticeRead;
import com.lsx.property.notice.mapper.SysNoticeMapper;
import com.lsx.property.notice.mapper.SysNoticeReadMapper;
import com.lsx.property.notice.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements NoticeService  {

    @Autowired
    private SysNoticeMapper noticeMapper;
    @Autowired
    private SysNoticeReadMapper noticeReadMapper;
    
    @Autowired
    private HouseServiceClient houseServiceClient;

    // 鏉堝懎濮弬瑙勭《销毁涙艾鍨介弬顓熸Ц閸氾箑鍑￠崚状态绘珟
    private boolean isDeleted(SysNotice notice) {
        return notice != null && notice.getDeleted() != null && notice.getDeleted() == 1;
    }

    // 鏉堝懎濮弬瑙勭《销毁涙艾鍨介弬顓熸Ц閸氾附婀崚状态绘珟
    private boolean isNotDeleted(SysNotice notice) {
        return notice != null && (notice.getDeleted() == null || notice.getDeleted() == 0);
    }

    @Override
    @Transactional
    public Long createNotice(NoticeCreateDTO dto, Long adminId) {
        SysNotice notice = new SysNotice();
        BeanUtils.copyProperties(dto, notice);
        notice.setCreatorId(adminId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());

        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long adminCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (adminCommunityId == null) {
                throw new RuntimeException("缁狅紕鎮婇崨妯绘弓缂佹垵鐣剧粈鎯у隘销毁涘本妫ゅ▔鏇炲絺鐢啫鍙曢崨?);
            }
            // 娣囶喖顦查敍姘洤閺嬫粍妲?USER 缁鐎烽敍鍫濐洤閸岊剛鍗抽柅姘辩叀销毁涘绱濋崚娆庣箽閻?USER 缁鐎烽敍灞芥儊閸掓瑩绮拋銈勮礋 COMMUNITY
            if ("USER".equalsIgnoreCase(dto.getTargetType())) {
                notice.setTargetType("USER");
                notice.setTargetUserId(dto.getTargetUserId());
            } else {
                notice.setTargetType("COMMUNITY");
            }
            
            notice.setCommunityId(adminCommunityId);
            String communityName = houseServiceClient.getCommunityNameById(adminCommunityId);
            if (communityName != null) {
                notice.setCommunityName(communityName);
            }
        } else {
            if (dto.getCommunityId() != null) {
                notice.setTargetType("COMMUNITY");
                notice.setCommunityId(dto.getCommunityId());
                String communityName = houseServiceClient.getCommunityNameById(dto.getCommunityId());
                if (communityName != null) {
                    notice.setCommunityName(communityName);
                }
            }
        }

        if ("PUBLISHED".equalsIgnoreCase(dto.getPublishStatus())) {
            notice.setPublishStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
        } else {
            notice.setPublishStatus("DRAFT");
        }

        if (notice.getTopFlag() == null) {
            notice.setTopFlag(false);
        }
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public Page<NoticeVO> listNotices(Long userId, Integer pageNum, Integer pageSize) {
        /**
         * 1. 閺屻儴顕楅悽銊﹀煕缂佹垵鐣鹃幋鍨溈
         */
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);

        Set<String> communityNames = new HashSet<>();
        Set<String> buildingNos = new HashSet<>();

        if (userHouses != null) {
            for (HouseDTO h : userHouses) {
                if (h.getCommunityName() != null) communityNames.add(h.getCommunityName());
                if (h.getBuildingNo() != null) buildingNos.add(h.getBuildingNo());
            }
        }

        /**
         * 2. 閺嬪嫰鈧姴鍨庢い?         */
        Page<SysNotice> page = new Page<>(pageNum, pageSize);

        /**
         * 3. 閺屻儴顕楅崗顒€鎲￠敍鍫濆彠闁款喚鍋ｉ敍姝凴 闁槒绶箛鍛淬€忛弨鎯ф躬閸氬瞼楠囬敍渚婄礆
         */
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<>();
        wrapper.eq("publish_status", "PUBLISHED")
                .eq("deleted", 0)  // 鏉╂瑩鍣烽弰顖氼嚠閻ㄥ嫸绱濋崶状态辫礋deleted閺勭枠nteger缁鐎?                .and(w -> w
                        .eq("target_type", "ALL")
                        .or(!communityNames.isEmpty(),
                                c -> c.eq("target_type", "COMMUNITY")
                                        .in("community_name", communityNames))
                        .or(!buildingNos.isEmpty(),
                                b -> b.eq("target_type", "BUILDING")
                                        .in("building_no", buildingNos))
                        .or(u -> u.eq("target_type", "USER")
                                .eq("target_user_id", userId))
                )
                // 閺堫亣绻冮張?                .and(w -> w.isNull("expire_time")
                        .or()
                        .gt("expire_time", LocalDateTime.now())
                );

        Page<SysNotice> noticePage = noticeMapper.selectPage(page, wrapper);

        /**
         * 4. 閺屻儴顕楀鑼额嚢閻樿埖鈧?         */
        List<Long> noticeIds = noticePage.getRecords().stream()
                .map(SysNotice::getId)
                .collect(Collectors.toList());

        Map<Long, Boolean> readMap = new HashMap<>();
        if (!noticeIds.isEmpty()) {
            List<SysNoticeRead> readList = noticeReadMapper.selectList(
                    new QueryWrapper<SysNoticeRead>()
                            .eq("user_id", userId)
                            .in("notice_id", noticeIds)
            );

            readList.forEach(r -> readMap.put(r.getNoticeId(), true));
        }

        /**
         * 5. 鏉烆剚鍨?VO
         */
        List<NoticeVO> voList = noticePage.getRecords().stream()
                .map(n -> {
                    NoticeVO vo = new NoticeVO();
                    BeanUtils.copyProperties(n, vo);
                    vo.setRead(readMap.getOrDefault(n.getId(), false));   // 閺堫亣顕?= false
                    return vo;
                })
                .collect(Collectors.toList());

        /**
         * 6. 閹烘帒绨敍鍫熸弓鐠囪绱崗?閳?缂冾噣銆婃导妯哄帥 閳?閺冨爼妫块崐鎺戠碍销毁?         */
        voList.sort(Comparator
                .comparing(NoticeVO::getRead)                                   // false(閺堫亣顕? 閸︺劌澧?                .thenComparing(NoticeVO::getTopFlag, Comparator.reverseOrder())  // top=1 閸︺劌澧?                .thenComparing(NoticeVO::getPublishTime, Comparator.reverseOrder()) // 閺冨爼妫块崐鎺戠碍
        );

        /**
         * 7. 鏉╂柨娲栭崚鍡涖€?         */
        Page<NoticeVO> result = new Page<>(pageNum, pageSize, noticePage.getTotal());
        result.setRecords(voList);

        return result;
    }

    @Override
    public long countUnread(Long userId) {
        // 1. 閼惧嘲褰囬悽銊﹀煕缂佹垵鐣鹃惃鍕仦閸栧搫鎷板Δ充值肩埀
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);
        
        Set<String> communityNames = new HashSet<>();
        Set<String> buildingNos = new HashSet<>();
        
        if (userHouses != null) {
            for (HouseDTO h : userHouses) {
                if (h.getCommunityName() != null) communityNames.add(h.getCommunityName());
                if (h.getBuildingNo() != null) buildingNos.add(h.getBuildingNo());
            }
        }

        // 2. 閺屻儴顕楃拠銉ф暏閹村嘲褰茬憴浣烘畱閹碘偓閺堝婀侀弫鍫濆彆閸涘D
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .eq("publish_status", "PUBLISHED")
                .eq("deleted", 0)
                .and(w -> w
                        .eq("target_type", "ALL")
                        .or(!communityNames.isEmpty(), c -> c.eq("target_type", "COMMUNITY").in("community_name", communityNames))
                        .or(!buildingNos.isEmpty(), b -> b.eq("target_type", "BUILDING").in("building_no", buildingNos))
                        .or(u -> u.eq("target_type", "USER").eq("target_user_id", userId))
                )
                .and(w -> w.isNull("expire_time").or().gt("expire_time", LocalDateTime.now()));
        
        List<SysNotice> notices = noticeMapper.selectList(wrapper);
        if (notices.isEmpty()) {
            return 0;
        }
        List<Long> noticeIds = notices.stream().map(SysNotice::getId).collect(Collectors.toList());

        // 3. 閺屻儴顕楀鑼额嚢鐠佹澘缍嶉弫?        Long readCount = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                .eq("user_id", userId)
                .in("notice_id", noticeIds)
                .eq("status", "READ"));

        // 4. 閺堫亣顕伴弫?= 閹褰茬憴浣规殶 - 瀹歌尪顕伴弫?        return notices.size() - (readCount == null ? 0 : readCount);
    }

    @Override
    @Transactional
    public void markAsRead(Long noticeId, Long userId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (isDeleted(notice)) {  // 娴ｈ法鏁ゆ潏鍛И閺傝纭?            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸?);
        }

        QueryWrapper<SysNoticeRead> wrapper = new QueryWrapper<>();
        wrapper.eq("notice_id", noticeId).eq("user_id", userId);
        SysNoticeRead readRecord = noticeReadMapper.selectOne(wrapper);
        if (readRecord == null) {
            SysNoticeRead record = new SysNoticeRead();
            record.setNoticeId(noticeId);
            record.setUserId(userId);
            record.setStatus("READ");
            record.setReadTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            noticeReadMapper.insert(record);
        } else if (!"READ".equals(readRecord.getStatus())) {
            readRecord.setStatus("READ");
            readRecord.setReadTime(LocalDateTime.now());
            noticeReadMapper.updateById(readRecord);
        }
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId, Long adminId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸︺劍鍨ㄥ鎻掑灩闂?);
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("閺冪姵娼堥幙宥勭稊閸忔湹绮粈鎯у隘閸忣剙鎲?);
            }
        }

        // 閴?閻╁瓨甯寸拫鍐暏 deleteById
        int rows = noticeMapper.deleteById(noticeId);
        if (rows == 0) {
            throw new RuntimeException("閸掔娀娅庢径杈Е");
        }

        log.info("缁狅紕鎮婇崨姒寋}]瀹告彃鍨归梽銈呭彆閸涘カ{}]", adminId, noticeId);
    }

    @Override
    public SysNotice getById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public void updateNotice(Long id, NoticeCreateDTO dto, Long adminId) {
        // 1. 閺屻儴顕楅崗顒€鎲￠弰顖氭儊鐎涙ê婀?        SysNotice notice = this.getById(id);
        if (notice == null) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸︺劍鍨ㄥ鑼额潶閸掔娀娅?);
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("閺冪姵娼堥幙宥勭稊閸忔湹绮粈鎯у隘閸忣剙鎲?);
            }
        }

        // 2. 閺囧瓨鏌婇崺鐑樻拱娣団剝浼?        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTopFlag(dto.getTopFlag());
        notice.setTargetType(dto.getTargetType());
        // 婵″倹鐏?DTO 閸栧懎鎯堥悩鑸碘偓渚婄礉娑旂喎褰叉禒銉︽纯閺?        if (dto.getPublishStatus() != null) {
            notice.setPublishStatus(dto.getPublishStatus());
        }

        // 3. 閺囧瓨鏌婃潻鍥ㄦ埂閺冨爼妫?        // 閸撳秶顏韫叏婢跺秳璐熸导状态烩偓?ISO 閺嶅洤鍣弮鍫曟？销毁涘矁绻栭柌灞藉讲娴犮儳娲块幒銉ㄧゴ閸?        if (dto.getExpireTime() != null) {
            notice.setExpireTime(dto.getExpireTime());
        } else {
            // 婵″倹鐏夐棁鈧憰浣规暜閹?濞撳懘娅庢潻鍥ㄦ埂閺冨爼妫?销毁涘苯褰叉禒銉ュ絿濞戝牅绗呴棃銏ｇ箹鐞涘瞼娈戝▔銊╁櫞
            // notice.setExpireTime(null);
        }

        // 4. 閺囧瓨鏌婃穱顔芥暭娴滆桨淇婇幁顖ょ礄婵″倹鐏夐張澶屾祲閸忓啿鐡у▓纰夌礆
        // notice.setUpdateBy(adminId);
        // notice.setUpdateTime(LocalDateTime.now());

        // 5. 閹笛嗩攽閺囧瓨鏌?        this.updateById(notice);
    }

    @Override
    @Transactional
    public void publishNotice(Long id, Long adminId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸?);
        }
        notice.setPublishStatus("PUBLISHED");
        notice.setPublishTime(LocalDateTime.now());
        updateById(notice);
    }

    @Override
    @Transactional
    public void publishNotice(Long id, Long adminId, Long communityId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("充值憡不存在?);
        }

        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            Long targetCommunityId = communityId != null ? communityId : adminCommunityId;
            if (adminCommunityId == null) {
                throw new RuntimeException("管理员樻湭绑定社区锛屾棤娉曞彂甯冨叕鍛?);
            }
            if (targetCommunityId == null || !adminCommunityId.equals(targetCommunityId)) {
                // 濡傛灉鏄秴绾х鐞嗗憳锛屽彲鑳藉厑璁搞€備絾杩欓噷鏄櫘閫氱鐞嗗憳銆?                // 閫昏緫未读夌偣缁曪紝绠€鍖栦竴涓嬶細鏅€氱鐞嗗憳鍙兘发布鍒拌嚜宸辩ぞ鍖?                targetCommunityId = adminCommunityId;
            }
            notice.setTargetType("COMMUNITY");
            notice.setCommunityId(targetCommunityId);
            String communityName = houseServiceClient.getCommunityNameById(targetCommunityId);
            if (communityName != null) {
                notice.setCommunityName(communityName);
            }
        } else {
            if (communityId != null) {
                notice.setTargetType("COMMUNITY");
                notice.setCommunityId(communityId);
                String communityName = houseServiceClient.getCommunityNameById(communityId);
                if (communityName != null) {
                    notice.setCommunityName(communityName);
                }
            }
        }

        notice.setPublishStatus("PUBLISHED");
        notice.setPublishTime(LocalDateTime.now());
        updateById(notice);
    }


    @Override
    @Transactional
    public void offlineNotice(Long id, Long adminId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸?);
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("閺冪姵娼堥幙宥勭稊閸忔湹绮粈鎯у隘閸忣剙鎲?);
            }
        }
        notice.setPublishStatus("OFFLINE");
        updateById(notice);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> noticeIds, Long adminId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new RuntimeException("閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?);
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            List<SysNotice> list = noticeMapper.selectBatchIds(noticeIds);
            for (SysNotice n : list) {
                if (n != null) {
                    if (n.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(n.getCommunityId())) {
                        throw new RuntimeException("鐎涙ê婀棃鐐存拱缁€鎯у隘閸忣剙鎲￠敍灞炬￥濞夋洘澹掗柌蹇撳灩闂?);
                    }
                }
            }
        }
        noticeMapper.deleteBatchIds(noticeIds);
    }

    @Override
    @Transactional
    public void batchOffline(List<Long> noticeIds, Long adminId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new RuntimeException("閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?);
        }
        List<SysNotice> notices = noticeMapper.selectBatchIds(noticeIds);
        for (SysNotice n : notices) {
            if (n != null && isNotDeleted(n)) {
                String role = UserContext.getRole();
                Long adminCommunityId = UserContext.getCommunityId();
                if (!"super_admin".equalsIgnoreCase(role)) {
                    if (n.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(n.getCommunityId())) {
                        throw new RuntimeException("鐎涙ê婀棃鐐存拱缁€鎯у隘閸忣剙鎲￠敍灞炬￥濞夋洑绗呴弸?);
                    }
                }
                n.setPublishStatus("OFFLINE");
                updateById(n);
            }
        }
    }

    @Override
    public NoticeReadStatDTO getReadStat(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("閸忣剙鎲℃稉宥呯摠閸?);
        }
        int totalUsers = countTargetUsers(notice);
        Long readCountLong = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                .eq("notice_id", noticeId)
                .eq("status", "READ"));
        int readCount = readCountLong == null ? 0 : readCountLong.intValue();
        int unreadCount = Math.max(totalUsers - readCount, 0);
        double rate = totalUsers == 0 ? 0.0 : Math.round((readCount * 10000.0 / totalUsers)) / 100.0;
        NoticeReadStatDTO dto = new NoticeReadStatDTO();
        dto.setNoticeId(noticeId);
        dto.setTitle(notice.getTitle());
        dto.setTotalUsers(totalUsers);
        dto.setReadCount(readCount);
        dto.setUnreadCount(unreadCount);
        dto.setReadRate(rate);
        return dto;
    }

    private int countTargetUsers(SysNotice notice) {
        String target = notice.getTargetType();
        
        // 婵″倹鐏夐弰顖氬弿閸涙ê鍙曢崨濠忕礉閺嗗倹妞傞弮状态崇《缁墽鈥樼紒鐔活吀销毁涘牓娓剁憰浣稿弿鐞涖劍澹傞幓蹇ョ礆销毁涘矁绻栭柌灞戒海鐠侀箖鈧俺绻冮弶鈥叉閺屻儴顕?        // 閹存牞鈧?house-service 閹绘劒绶垫稉鈧稉顏嗙埠鐠佲€冲弿閸涙娈戦幒銉ュ經閵?        // 闁寸繝绨琈VP闂冭埖顔岄敍宀冪箹闁插瞼鐣濋崠鏍ь槱閻炲棴绱版俊鍌涚亯閺勭枆LL销毁涘瞼绮虹拋鈩冨閺堝绱辨俊鍌涚亯閺勵垱娼禒璁圭礉閹稿娼禒鍓佺埠鐠伮扳偓?        
        if ("ALL".equalsIgnoreCase(target)) {
            // 闂団偓鐟曚椒绔存稉顏呮煀閻ㄥ嫭甯撮崣锝嗘降缂佺喕顓搁幍鈧張澶夌秶閹村嚖绱濋幋鏍偓鍛炊缁岀儤娼禒?            Integer count = houseServiceClient.countUsersByCondition(null, null);
            return count != null ? count : 0;
        } else if ("COMMUNITY".equalsIgnoreCase(target)) {
            Integer count = houseServiceClient.countUsersByCondition(notice.getCommunityName(), null);
            return count != null ? count : 0;
        } else if ("BUILDING".equalsIgnoreCase(target)) {
            Integer count = houseServiceClient.countUsersByCondition(notice.getCommunityName(), notice.getBuildingNo());
            return count != null ? count : 0;
        } else if ("USER".equalsIgnoreCase(target)) {
            return 1;
        }
        return 0;
    }
}

