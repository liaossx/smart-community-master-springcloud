package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
@Data
@Schema(description = "閸忣剙鎲℃潻鍥ㄦ埂閺冨爼妫跨拋鍓х枂閸欏倹鏆?)
public class NoticeExpireDTO {

    @Schema(description = "閸忣剙鎲D", required = true)
    @NotNull(message = "閸忣剙鎲D娑撳秷充值樻稉铏光敄")
    private Long noticeId;

    @Schema(description = "鏉╁洦婀￠弮鍫曟？缁鐎? NEVER(濮橀晲绗夋潻鍥ㄦ埂), CUSTOM(閼奉亜鐣炬稊?, DAYS_7(7婢?, DAYS_30(30婢?, MONTH_3(3娑擃亝婀€)", required = true)
    @NotNull(message = "鏉╁洦婀￠弮鍫曟？缁鐎锋稉宥堝厴娑撹櫣鈹?)
    private ExpireType expireType;

    @Schema(description = "閼奉亜鐣炬稊澶庣箖閺堢喐妞傞梻杈剧礄瑜版徍xpireType=CUSTOM閺冭泛绻€婵夘偓绱?)
    private LocalDateTime customExpireTime;

    @Schema(description = "婢垛晜鏆熼敍鍫濈秼expireType=DAYS_*閺冭泛褰查柅澶涚礆")
    private Integer days;

    public enum ExpireType {
        NEVER,      // 濮橀晲绗夋潻鍥ㄦ埂
        CUSTOM,     // 閼奉亜鐣炬稊澶嬫闂?        DAYS_7,     // 7婢垛晛鎮?        DAYS_30,    // 30婢垛晛鎮?        MONTH_3     // 3娑擃亝婀€閸?    }
}
