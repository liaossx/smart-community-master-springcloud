package com.lsx.property.notice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.client.UserServiceClient;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.notice.dto.*;
import com.lsx.property.notice.entity.SysNotice;
import com.lsx.property.notice.service.ISysNoticeService;
import com.lsx.property.notice.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.property.notice.mapper.SysNoticeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notice")
@Slf4j
@Tag(name = "閻椻晙绗熼崗顒€鎲￠幒銉ュ經", description = "閸忣剙鎲￠崣鎴濈閵嗕焦鐓＄拠顫偓浣稿嚒鐠囨眹鈧礁鍨归梽銈囨祲閸忚櫕甯撮崣?)
public class NoticeController {

    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ISysNoticeService sysNoticeService;
    @Autowired
    private SysNoticeMapper noticeMapper;
    
    // 濞夈劌鍙?UserServiceClient
    @Autowired
    private UserServiceClient userServiceClient;


    @PostMapping
    @Operation(summary = "缁狅紕鎮婇崨妯哄絺鐢啫鍙曢崨?, description = "閺€顖涘瘮閹稿洤鐣剧亸蹇撳隘/濡ゅ吋鐖ч幋鏍у弿娴ｆ挷绗熸稉?)
    @Log(title = "閸忣剙鎲＄粻锛勬倞", businessType = BusinessType.INSERT)
    public Result<Long> createNotice(@RequestBody NoticeCreateDTO dto,
                                     @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true) @RequestParam("adminId") Long adminId) {
        try {
            Long noticeId = noticeService.createNotice(dto, adminId);
            return Result.success(noticeId);
        } catch (Exception e) {
            log.error("閸欐垵绔烽崗顒€鎲℃径杈Е", e);
            return Result.fail("閸欐垵绔锋径杈Е销毁? + e.getMessage());
        }
    }

    @GetMapping("/admin/list")
    @Operation(summary = "閸氬骸褰撮崗顒€鎲＄粻锛勬倞閸掓銆?)
    public Result<Page<AdminNoticeListItemDTO>> adminList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "publishStatus", required = false) String publishStatus,
            @RequestParam(value = "topFlag", required = false) Boolean topFlag) {
        try {
            Page<SysNotice> page = new Page<>(pageNum, pageSize);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            QueryWrapper<SysNotice> qw = new QueryWrapper<>();
            if (title != null && !title.trim().isEmpty()) {
                qw.like("title", title.trim());
            }
            if (publishStatus != null && !publishStatus.trim().isEmpty()) {
                qw.eq("publish_status", publishStatus.trim());
            }
            if (topFlag != null) {
                qw.eq("top_flag", topFlag ? 1 : 0);
            }
            String role = UserContext.getRole();
            Long communityId = UserContext.getCommunityId();
            if (!"super_admin".equalsIgnoreCase(role)) {
                if (communityId != null) {
                    qw.eq("community_id", communityId);
                } else {
                    qw.eq("id", -1L);
                }
            }
            qw.eq("deleted", 0).orderByDesc("top_flag").orderByDesc("publish_time").orderByDesc("create_time");
            Page<SysNotice> noticePage = noticeMapper.selectPage(page, qw);
            List<AdminNoticeListItemDTO> records = noticePage.getRecords().stream().map(n -> {
                AdminNoticeListItemDTO dto = new AdminNoticeListItemDTO();
                dto.setId(n.getId());
                dto.setTitle(n.getTitle());
                dto.setPublishStatus(n.getPublishStatus());
                dto.setTopFlag(n.getTopFlag());
                dto.setTargetType(n.getTargetType());
                dto.setCommunityName(n.getCommunityName());
                dto.setBuildingNo(n.getBuildingNo());
                dto.setPublishTime(n.getPublishTime() == null ? null : n.getPublishTime().format(fmt));
                dto.setExpireTime(n.getExpireTime() == null ? null : n.getExpireTime().format(fmt));
                // User creator = n.getCreatorId() == null ? null : userMapper.selectById(n.getCreatorId());
                // dto.setCreatorName(creator == null ? null : creator.getRealName());
                // 閺嗗倹妞傛稉宥嗙叀鐠囥垹鍨卞楦库偓鍛倳缁夊府绱濋幋鏍偓鍛▏閻?UserServiceClient 閺屻儴顕楅敍鍫ユ付鐟曚礁顤冮崝?getById 閹恒儱褰涢敍?                dto.setCreatorName("Admin"); 
                
                NoticeReadStatDTO stat = noticeService.getReadStat(n.getId());
                int readCount = stat.getReadCount();
                int total = stat.getTotalUsers();
                dto.setReadCount(readCount);
                dto.setTotalCount(total);
                return dto;
            }).collect(Collectors.toList());
            Page<AdminNoticeListItemDTO> result = new Page<>(pageNum, pageSize);
            result.setRecords(records);
            result.setTotal(noticePage.getTotal());
            result.setPages(noticePage.getPages());
            result.setCurrent(noticePage.getCurrent());
            return Result.success(result);
        } catch (Exception e) {
            log.error("閸氬骸褰撮崗顒€鎲￠崚妤勩€冮弻銉嚄婢惰精瑙?, e);
            return Result.fail("閺屻儴顕楁径杈Е销毁? + e.getMessage());
        }
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "閸欐垵绔烽崗顒€鎲?)
    @Log(title = "閸忣剙鎲＄粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<Long> publish(@PathVariable("id") Long id,
                                @RequestParam("adminId") Long adminId,
                                @RequestParam(value = "communityId", required = false) Long communityId) {
        try {
            noticeService.publishNotice(id, adminId, communityId);
            return Result.success(id);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閸欐垵绔烽崗顒€鎲″鍌氱埗", e);
            return Result.fail("閸欐垵绔锋径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @PutMapping("/{id}/offline")
    @Operation(summary = "娑撳鐏﹂崗顒€鎲?)
    @Log(title = "閸忣剙鎲＄粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<String> offline(@PathVariable("id") Long id,
                                  @RequestParam("adminId") Long adminId) {
        try {
            noticeService.offlineNotice(id, adminId);
            return Result.success("娑撳鐏﹂幋鎰");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("娑撳鐏﹂崗顒€鎲″鍌氱埗", e);
            return Result.fail("娑撳鐏︽径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "閹靛綊鍣洪崚状态绘珟閸忣剙鎲?)
    public Result<String> batchDelete(@RequestBody IdListDTO body,
                                      @RequestParam("adminId") Long adminId) {
        try {
            noticeService.batchDelete(body.getNoticeIds(), adminId);
            return Result.success("閹靛綊鍣洪崚状态绘珟閹存劕濮?);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閹靛綊鍣洪崚状态绘珟閸忣剙鎲″鍌氱埗", e);
            return Result.fail("閹靛綊鍣洪崚状态绘珟婢惰精瑙﹂敍宀冾嚞缁嬪秴鎮楅崘宥堢槸");
        }
    }

    @PostMapping("/batch/offline")
    @Operation(summary = "閹靛綊鍣烘稉瀣仸閸忣剙鎲?)
    public Result<String> batchOffline(@RequestBody IdListDTO body,
                                       @RequestParam("adminId") Long adminId) {
        try {
            noticeService.batchOffline(body.getNoticeIds(), adminId);
            return Result.success("閹靛綊鍣烘稉瀣仸閹存劕濮?);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閹靛綊鍣烘稉瀣仸閸忣剙鎲″鍌氱埗", e);
            return Result.fail("閹靛綊鍣烘稉瀣仸婢惰精瑙﹂敍宀冾嚞缁嬪秴鎮楅崘宥堢槸");
        }
    }

    @GetMapping("/{id}/read-stat")
    @Operation(summary = "閸忣剙鎲￠梼鍛邦嚢缂佺喕顓?)
    public Result<NoticeReadStatDTO> readStat(@PathVariable("id") Long id) {
        try {
            NoticeReadStatDTO dto = noticeService.getReadStat(id);
            return Result.success(dto);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閺屻儴顕楅崗顒€鎲￠梼鍛邦嚢缂佺喕顓稿鍌氱埗", e);
            return Result.fail("閺屻儴顕楁径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }
    // 娣囶喗鏁糃ontroller閺傝纭?    @GetMapping("/list")
    @Operation(summary = "娑撴矮瀵岄弻銉嚄閸忣剙鎲?, description = "姒涙顓婚弮鍫曟？閸婃帒绨敍灞炬弓鐠囪绱崗?)
    public Result<Page<NoticeListDTO>> listNotices(
            @Parameter(description = "娑撴矮瀵孖D", required = true) @RequestParam("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Page<NoticeVO> result = noticeService.listNotices(userId, pageNum, pageSize);

            // 鏉烆剚宕叉稉鍝勫缁旑垶娓剁憰浣烘畱DTO閺嶇厧绱?            List<NoticeListDTO> dtoList = result.getRecords().stream()
                    .map(vo -> {
                        NoticeListDTO dto = new NoticeListDTO();
                        dto.setId(vo.getId());
                        dto.setTitle(vo.getTitle());
                        dto.setReadFlag(vo.getRead() ? 1 : 0); // 鐢啫鐨甸崐鑹版祮閺佹澘鐡?                        dto.setTargetType(vo.getTargetType());
                        dto.setContent(vo.getContent());

                        // 閺嶇厧绱￠崠鏍ㄦ闂傜繝璐焬yyy-MM-dd HH:mm:ss
                        if (vo.getPublishTime() != null) {
                            dto.setPublishTime(vo.getPublishTime()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            // 閺嬪嫬缂撻弬所有畱閸掑棝銆夌紒鎾寸亯
            Page<NoticeListDTO> pageResult = new Page<>(pageNum, pageSize);
            pageResult.setRecords(dtoList);
            pageResult.setTotal(result.getTotal());
            pageResult.setPages(result.getPages());

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("閺屻儴顕楅崗顒€鎲℃径杈Е", e);
            return Result.fail("閺屻儴顕楁径杈Е销毁? + e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    @Operation(summary = "閼惧嘲褰囪ぐ鎾冲閻劍鍩涢張顏囶嚢閸忣剙鎲￠弫浼村櫤")
    public Result<Long> getUnreadCount() {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.success(0L);
            }
            long count = noticeService.countUnread(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("閼惧嘲褰囬張顏囶嚢閺佷即鍣烘径杈Е", e);
            return Result.fail("閼惧嘲褰囨径杈Е");
        }
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "娑撴矮瀵岄弽鍥唶閸忣剙鎲″鑼额嚢")
    public Result<String> markNoticeRead(@PathVariable("id") Long noticeId,
                                         HttpServletRequest request) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.fail("閺堫亞娅ヨぐ?);
            }

            noticeService.markAsRead(noticeId, userId);
            return Result.success("閺嶅洩顔囬幋鎰");

        } catch (RuntimeException e) {
            log.warn("閺嶅洩顔囧鑼额嚢婢惰精瑙﹂敍姝縸", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閺嶅洩顔囬崗顒€鎲″鑼额嚢瀵倸鐖?, e);
            return Result.fail("閺嶅洩顔囨径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "缁狅紕鎮婇崨妯哄灩闂勩倕鍙曢崨?)
    @Log(title = "閸忣剙鎲＄粻锛勬倞", businessType = BusinessType.DELETE)
    public Result<String> deleteNotice(@PathVariable("id") Long noticeId,
                                       @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true) @RequestParam("adminId") Long adminId) {
        try {
            noticeService.deleteNotice(noticeId, adminId);
            return Result.success("閸掔娀娅庨幋鎰");
        } catch (RuntimeException e) {
            log.warn("閸掔娀娅庨崗顒€鎲℃径杈Е销毁涙}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閸掔娀娅庨崗顒€鎲″鍌氱埗", e);
            return Result.fail("閸掔娀娅庢径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }
    @PostMapping("/expire/set")
    @Operation(summary = "鐠佸墽鐤嗛崡鏇氶嚋閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫?, description = "缁狅紕鎮婇崨妯哄讲娴犮儴顔曠純顔煎彆閸涘﹦娈戞潻鍥ㄦ埂閺冨爼妫?)
    public Result<String> setNoticeExpire(@Validated @RequestBody NoticeExpireDTO dto,
                                          @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
                                          @RequestParam("adminId") Long adminId) {
        try {
            // 鏉╂瑩鍣烽崣顖欎簰濞ｈ濮炵粻锛勬倞閸涙ɑ娼堥梽鎰扮崣鐠?            sysNoticeService.setNoticeExpire(dto);
            log.info("缁狅紕鎮婇崨姒寋}]鐠佸墽鐤嗘禍鍡楀彆閸涘カ{}]閻ㄥ嫯绻冮張鐔告闂?, adminId, dto.getNoticeId());
            return Result.success("鐠佸墽鐤嗛幋鎰");
        } catch (RuntimeException e) {
            log.warn("鐠佸墽鐤嗘潻鍥ㄦ埂閺冨爼妫挎径杈Е销毁涙}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("鐠佸墽鐤嗛崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫垮鍌氱埗", e);
            return Result.fail("鐠佸墽鐤嗘径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @PostMapping("/expire/batch-set")
    @Operation(summary = "閹靛綊鍣虹拋鍓х枂閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫?, description = "缁狅紕鎮婇崨妯诲闁插繗顔曠純顔煎彆閸涘﹨绻冮張鐔告闂?)
    public Result<String> batchSetNoticeExpire(@Validated @RequestBody BatchNoticeExpireDTO dto,
                                               @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
                                               @RequestParam("adminId") Long adminId) {
        try {
            sysNoticeService.batchSetNoticeExpire(dto);
            log.info("缁狅紕鎮婇崨姒寋}]閹靛綊鍣虹拋鍓х枂娴滃敓}閺夆€冲彆閸涘﹦娈戞潻鍥ㄦ埂閺冨爼妫?, adminId, dto.getNoticeIds().size());
            return Result.success("閹靛綊鍣虹拋鍓х枂閹存劕濮?);
        } catch (RuntimeException e) {
            log.warn("閹靛綊鍣虹拋鍓х枂鏉╁洦婀￠弮鍫曟？婢惰精瑙﹂敍姝縸", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閹靛綊鍣虹拋鍓х枂閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫垮鍌氱埗", e);
            return Result.fail("閹靛綊鍣虹拋鍓х枂婢惰精瑙﹂敍宀冾嚞缁嬪秴鎮楅崘宥堢槸");
        }
    }

    @PostMapping("/expire/clear/{noticeId}")
    @Operation(summary = "濞撳懘娅庨崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫块敍鍫熸娑撳秷绻冮張鐕傜礆", description = "鐏忓棗鍙曢崨濠咁啎缂冾喕璐熷闀愮瑝鏉╁洦婀?)
    public Result<String> clearNoticeExpire(@PathVariable("noticeId") Long noticeId,
                                            @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
                                            @RequestParam("adminId") Long adminId) {
        try {
            sysNoticeService.clearNoticeExpire(noticeId);
            log.info("缁狅紕鎮婇崨姒寋}]濞撳懘娅庢禍鍡楀彆閸涘カ{}]閻ㄥ嫯绻冮張鐔告闂?, adminId, noticeId);
            return Result.success("濞撳懘娅庨幋鎰");
        } catch (RuntimeException e) {
            log.warn("濞撳懘娅庢潻鍥ㄦ埂閺冨爼妫挎径杈Е销毁涙}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("濞撳懘娅庨崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫垮鍌氱埗", e);
            return Result.fail("濞撳懘娅庢径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @PostMapping("/expire/extend/{noticeId}")
    @Operation(summary = "瀵ゅ爼鏆遍崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫?, description = "閸︺劌甯張澶婄唨绾偓娑撳﹤娆㈤梹鎸庡瘹鐎规艾銇夐弫?)
    public Result<String> extendNoticeExpire(@PathVariable("noticeId") Long noticeId,
                                             @Parameter(description = "瀵ゅ爼鏆遍惃鍕亯閺?, required = true)
                                             @RequestParam("days") Integer days,
                                             @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
                                             @RequestParam("adminId") Long adminId) {
        try {
            if (days == null || days <= 0) {
                return Result.fail("瀵ゅ爼鏆遍弮鍫曟？韫囧懘銆忔径褌绨?婢?);
            }

            sysNoticeService.extendNoticeExpire(noticeId, days);
            log.info("缁狅紕鎮婇崨姒寋}]鐏忓棗鍙曢崨濂珄}]鏉╁洦婀￠弮鍫曟？瀵ゅ爼鏆辨禍鍞焳婢?, adminId, noticeId, days);
            return Result.success("瀵ゅ爼鏆遍幋鎰");
        } catch (RuntimeException e) {
            log.warn("瀵ゅ爼鏆辨潻鍥ㄦ埂閺冨爼妫挎径杈Е销毁涙}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("瀵ゅ爼鏆遍崗顒€鎲℃潻鍥ㄦ埂閺冨爼妫垮鍌氱埗", e);
            return Result.fail("瀵ゅ爼鏆辨径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @PostMapping("/expire/batch-extend")
    @Operation(summary = "閹靛綊鍣哄鍫曟毐閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫?, description = "閹靛綊鍣哄鍫曟毐婢舵矮閲滈崗顒€鎲￠惃鍕箖閺堢喐妞傞梻?)
    public Result<String> batchExtendNoticeExpire(
            @Parameter(description = "閸忣剙鎲D閸掓銆冮敍宀€鏁ら柅妤€褰块崚鍡涙", required = true)
            @RequestParam("noticeIds") List<Long> noticeIds,
            @Parameter(description = "瀵ゅ爼鏆遍惃鍕亯閺?, required = true)
            @RequestParam("days") Integer days,
            @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
            @RequestParam("adminId") Long adminId) {
        try {
            if (noticeIds == null || noticeIds.isEmpty()) {
                return Result.fail("閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?);
            }

            if (days == null || days <= 0) {
                return Result.fail("瀵ゅ爼鏆遍弮鍫曟？韫囧懘銆忔径褌绨?婢?);
            }

            sysNoticeService.batchExtendNoticeExpire(noticeIds, days);
            log.info("缁狅紕鎮婇崨姒寋}]閹靛綊鍣哄鍫曟毐娴滃敓}閺夆€冲彆閸涘﹦娈戞潻鍥ㄦ埂閺冨爼妫块敍灞芥闂€鎸庢闂傝揪绱皗}婢?, adminId, noticeIds.size(), days);
            return Result.success("閹靛綊鍣哄鍫曟毐閹存劕濮?);
        } catch (RuntimeException e) {
            log.warn("閹靛綊鍣哄鍫曟毐鏉╁洦婀￠弮鍫曟？婢惰精瑙﹂敍姝縸", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("閹靛綊鍣哄鍫曟毐閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫垮鍌氱埗", e);
            return Result.fail("閹靛綊鍣哄鍫曟毐婢惰精瑙﹂敍宀冾嚞缁嬪秴鎮楅崘宥堢槸");
        }
    }

    @GetMapping("/expire/expiring-soon")
    @Operation(summary = "閺屻儴顕楅崡鍐茬殺鏉╁洦婀￠惃鍕彆閸?, description = "閺屻儴顕楅崷銊﹀瘹鐎规艾銇夐弫鏉垮敶閸楀啿鐨㈡潻鍥ㄦ埂閻ㄥ嫬鍙曢崨?)
    public Result<List<ExpiringNoticeDTO>> getExpiringSoonNotices(
            @Parameter(description = "鏉╁洦婀℃径鈺傛殶闂冨牆鈧》绱濇妯款吇7婢?)
            @RequestParam(value = "days", defaultValue = "7") Integer days,
            @Parameter(description = "缁狅紕鎮婇崨妤璂", required = true)
            @RequestParam("adminId") Long adminId) {
        try {
            List<ExpiringNoticeDTO> noticeIds = sysNoticeService.getExpiringSoonNotices(days);
            log.info("缁狅紕鎮婇崨姒寋}]閺屻儴顕楅崚鐨梷閺夆€冲祮鐏忓棗婀獅}婢垛晛鍞存潻鍥ㄦ埂閻ㄥ嫬鍙曢崨?, adminId, noticeIds.size(), days);
            return Result.success(noticeIds);
        } catch (Exception e) {
            log.error("閺屻儴顕楅崡鍐茬殺鏉╁洦婀￠崗顒€鎲″鍌氱埗", e);
            return Result.fail("閺屻儴顕楁径杈Е销毁涘矁顕粙宥呮倵閸愬秷鐦?);
        }
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "閸忣剙鎲＄拠锔藉剰")
    public Result<SysNotice> getNoticeDetail(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.fail("閸忣剙鎲℃稉宥呯摠閸?);
        }
        return Result.success(notice);
    }

    @PutMapping("/{id}")
    @Operation(summary = "缁狅紕鎮婇崨妯规叏閺€鐟板彆閸?)
    @Log(title = "閸忣剙鎲＄粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<String> updateNotice(@PathVariable("id") Long id,
                                       @RequestBody NoticeCreateDTO dto,
                                       @RequestParam("adminId") Long adminId) {
        try {
            // 闂団偓鐟曚礁婀?Service 鐏炲倸鐤勯悳?updateNotice 閺傝纭?            noticeService.updateNotice(id, dto, adminId);
            return Result.success("娣囶喗鏁奸幋鎰");
        } catch (Exception e) {
            log.error("娣囶喗鏁奸崗顒€鎲℃径杈Е", e);
            return Result.fail("娣囶喗鏁兼径杈Е销毁? + e.getMessage());
        }
    }

}


