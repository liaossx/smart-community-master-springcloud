package com.lsx.property.repair.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairResult {
    private Long id; // 閹躲儰鎱ㄧ拋鏉跨秿ID销毁涘牅绗熸稉璇插讲閼充粙娓剁憰浣圭壌閹圭攢D閸溿劏顕楁潻娑樺销毁?
    // 閹村灝鐪挎穱鈩冧紖销毁涘牆鍙ч懕鏂跨潔缁€鐚寸礉閺傞€涚┒娑撴矮瀵岀拠鍡楀焼閸忚渹缍嬮幋鍨溈销毁?    private String communityName; // 鐏忓繐灏崥宥囆為敍鍫熸降閼?sys_house.community_name销毁?    private String buildingNo;    // 濡ゅ吋鐖ч崣鍑ょ礄閺夈儴鍤?sys_house.building_no销毁?    private String houseNo;       // 閹村灝鐪跨紓鏍у娇销毁涘牊娼甸懛?sys_house.house_no销毁?
    // 閹躲儰鎱ㄩ弽绋跨妇娣団剝浼?    private String faultType;     // 閺佸懘娈扮猾璇茬€烽敍鍫濐洤閳ユ粍鎸夌粻鈩冪础濮樼补鈧績鈧粎鏁哥捄顖滅叚鐠侯垪鈧繐绱?    private String faultDesc;     // 閺佸懘娈伴幓蹇氬牚销毁涘牅绗熸稉璇诧綖閸愭瑧娈戦崗铚傜秼闂傤噣顣介敍?    private List<String> faultImgs; // 閺佸懘娈伴崶鍓уURL閸掓銆冮敍鍫熷閸掑棝鈧褰块崚鍡涙閻ㄥ嫬鐡х粭锔胯销毁涘本鏌熸笟鍨缁旑垰鐫嶇粈鐚寸礆

    // 鏉╂稑瀹抽惄绋垮彠娣団剝浼?    private String status;        // 閹躲儰鎱ㄩ悩鑸碘偓渚婄礄鏉烆兛鑵戦弬鍥ㄦ▔缁€鐑樻纯閸欏銈介敍灞筋洤閳ユ粌绶熸径鍕倞閳ユ績鈧粌鍑＄€瑰本鍨氶垾婵撶礆
    private String statusDesc;    // 閻樿埖鈧椒鑵戦弬鍥ㄥ伎鏉╁府绱欓崣顖炩偓澶涚礉婵?pending閳巻鈧粌绶熼悧鈺€绗熸径鍕倞閳ユ繐绱?    private String handleRemark;  // 婢跺嫮鎮婃径鍥ㄦ暈销毁涘牏澧挎稉姘綖閸愭瑧娈戞径鍕倞缂佹挻鐏夐敍?
    // 閺冨爼妫挎穱鈩冧紖
    private LocalDateTime createTime; // 閹躲儰鎱ㄩ幓鎰唉閺冨爼妫?    private LocalDateTime updateTime; // 閺堚偓鏉╂垶娲块弬鐗堟闂傝揪绱欐俊鍌氼槱閻炲棗鐣幋鎰闂傝揪绱?}
