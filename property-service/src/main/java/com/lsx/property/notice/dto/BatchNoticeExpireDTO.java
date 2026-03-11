package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "閹靛綊鍣虹拋鍓х枂閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫块崣鍌涙殶")
public class BatchNoticeExpireDTO {

    @Schema(description = "閸忣剙鎲D閸掓銆?, required = true)
    @NotEmpty(message = "閸忣剙鎲D閸掓銆冩稉宥堝厴娑撹櫣鈹?)
    private List<Long> noticeIds;

    @Schema(description = "鏉╁洦婀￠弮鍫曟？缁鐎?, required = true)
    @NotNull(message = "鏉╁洦婀￠弮鍫曟？缁鐎锋稉宥堝厴娑撹櫣鈹?)
    private NoticeExpireDTO.ExpireType expireType;

    @Schema(description = "閼奉亜鐣炬稊澶庣箖閺堢喐妞傞梻?)
    private LocalDateTime customExpireTime;

    @Schema(description = "婢垛晜鏆?)
    private Integer days;
}
