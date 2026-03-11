package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "閸楀啿鐨㈡潻鍥ㄦ埂閸忣剙鎲TO")
public class ExpiringNoticeDTO {

    @Schema(description = "閸忣剙鎲D")
    private Long id;

    @Schema(description = "閸忣剙鎲￠弽鍥暯")
    private String title;

    @Schema(description = "閻╊喗鐖ｇ猾璇茬€?)
    private String targetType;

    @Schema(description = "閸欐垵绔烽弮鍫曟？")
    private LocalDateTime publishTime;

    @Schema(description = "鏉╁洦婀￠弮鍫曟？")
    private LocalDateTime expireTime;

    @Schema(description = "閸撯晙缍戞径鈺傛殶")
    private Long daysLeft;

    @Schema(description = "閺勵垰鎯佺純顕€銆?)
    private Boolean topFlag;
}
