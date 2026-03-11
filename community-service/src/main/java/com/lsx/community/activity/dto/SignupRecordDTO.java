package com.lsx.community.activity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SignupRecordDTO {
    private Long id;
    private String userName;
    private String userPhone;
    private LocalDateTime signupTime;
    private String status;
}
