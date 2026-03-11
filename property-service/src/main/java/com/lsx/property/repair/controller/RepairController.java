package com.lsx.property.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.repair.dto.BatchUpdateStatusDTO;
import com.lsx.property.repair.dto.RepairDto;
import com.lsx.property.repair.entity.Repair;
import com.lsx.property.repair.service.RepairService;
import com.lsx.property.repair.vo.RepairResult;
import com.lsx.property.repair.vo.RepairStatsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/repair")
@Tag(name = "閹躲儰鎱ㄩ幒銉ュ經", description = "娑撴矮瀵岄幎銉ゆ叏閹绘劒姘﹂妴浣哄Ц閹焦鐓＄拠銏犲挤缁狅紕鎮婇崨妯侯槱閻炲棙甯撮崣?)
public class RepairController {

    @Resource
    private RepairService repairService;

    // 1. 娑撴矮瀵岄幓鎰唉閹躲儰鎱?    @Operation(summary = "閹绘劒姘﹂幎銉ゆ叏销毁涘牅绗熸稉浼欑礆", description = "娑撴矮瀵岄幓鎰唉閹村灝鐪块弫鍛存閹躲儰鎱ㄩ敍宀勬付娴肩姴鍙嗛悽銊﹀煕ID閵嗕焦鍩х仦瀣╀繆閹垬鈧焦鏅犻梾婊嗩嚊閹懐鐡?)
    @PostMapping("/submit")
    public Result<Boolean> submitRepair(
            @Parameter(description = "閹躲儰鎱ㄦ穱鈩冧紖DTO销毁涘苯瀵橀崥鐜約erId閵嗕攻uildingNo閵嗕弓ouseNo閵嗕公aultType缁?)
            @RequestBody RepairDto repairDto) {
        boolean success = repairService.submitRepair(repairDto);
        if (success) {
            return Result.success(true); // 閻?success(T data) 鏉╂柨娲栭幋鎰閻樿埖鈧?        } else {
            return Result.fail("閹绘劒姘︽径杈Е销毁涘矁顕Λ鈧弻銉や繆閹?); // 閻?fail 鏉╂柨娲栨径杈Е閸樼喎娲?        }
    }

    // 2. 缁狅紕鎮婇崨妯绘纯閺傜増濮ゆ穱顔惧Ц閹?    @Operation(summary = "閺囧瓨鏌婇幎銉ゆ叏閻樿埖鈧緤绱欑粻锛勬倞閸涙﹫绱?, description = "缁狅紕鎮婇崨妯侯槱閻炲棙濮ゆ穱顔煎礋销毁涘本娲块弬所有Ц閹椒璐熸径鍕倞娑?瀹告彃鐣幋?瀹告彃褰囧☉鍫礉閸欘垰锝為崘娆忣槱閻炲棗顦▔?)
    @PostMapping("/admin/updateStatus")
    @Log(title = "閹躲儰鎱ㄧ粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<Boolean> updateStatus(
            @Parameter(description = "閹躲儰鎱ㄧ拋鏉跨秿ID", required = true) @RequestParam Long repairId,
            @Parameter(description = "閻╊喗鐖ｉ悩鑸碘偓渚婄窗processing销毁涘牆顦╅悶鍡曡厬销毁涘鈧恭ompleted销毁涘牆鍑＄€瑰本鍨氶敍澶堚偓涔ncelled销毁涘牆鍑￠崣鏍ㄧХ销毁?, required = true) @RequestParam String status,
            @Parameter(description = "婢跺嫮鎮婃径鍥ㄦ暈销毁涘牆褰囧☉鍫熸韫囧懎锝為敍?) @RequestParam(required = false) String remark) {
        try {
            boolean success = repairService.updateRepairStatus(repairId, status, remark);
            return success ? Result.success(true) : Result.fail("閺囧瓨鏌婃径杈Е销毁涘本濮ゆ穱顔煎礋娑撳秴鐡ㄩ崷?);
        } catch (RuntimeException e) {
            // 閹规洝骞廠ervice鐏炲倹濮忛崙铏规畱娑撴艾濮熷鍌氱埗销毁涘牆顩ч悩鑸碘偓浣风瑝閸氬牊纭堕妴浣稿絿濞戝牊婀繅顐㈩槵濞夘煉绱?            return Result.fail(e.getMessage());
        }
    }

    // 3. 娑撴矮瀵岄弻銉嚄閼奉亜绻侀惃鍕Г娣囶喛顔囪ぐ鏇礄閸掑棝銆夐敍?    @Operation(summary = "娑撴矮瀵岄弻銉嚄閼奉亜绻侀惃鍕Г娣囶喛顔囪ぐ?, description = "閸掑棝銆夐弻銉嚄瑜版挸澧犳稉姘瘜閻ㄥ嫭澧嶉張澶嬪Г娣囶喛顔囪ぐ鏇礉閹稿褰佹禍銈嗘闂傛潙鈧帒绨?)
    @GetMapping("/user/my")
    public Result<IPage<RepairResult>> getMyRepairs(
            @Parameter(description = "娑撴矮瀵岄悽銊﹀煕ID", required = true) @RequestParam Long userId,
            @Parameter(description = "妞ょ數鐖滈敍鍫ョ帛鐠?销毁?) @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "濮ｅ繘銆夐弶鈩冩殶销毁涘牓绮拋?0销毁?) @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getMyRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage); // 閻╁瓨甯存潻鏂挎礀閸掑棝銆夐弫鐗堝祦
    }

    // 4. 缁狅紕鎮婇崨妯荤叀鐠囥垺澧嶉張澶嬪Г娣囶喛顔囪ぐ鏇礄閸掑棝銆?閻樿埖鈧胶鐡柅澶涚礆
    @GetMapping("/admin/all")
    public Result<IPage<RepairResult>> getAllRepairs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {  // 閴?濞ｈ濮?keyword

        IPage<RepairResult> resultPage = repairService.getAllRepairs(pageNum, pageSize, status, keyword);
        return Result.success(resultPage);
    }

    // 5. 缁狅紕鎮婇崨妯荤叀鐠囥垹宕熸稉顏冪瑹娑撹崵娈戦幎銉ゆ叏鐠佹澘缍嶉敍鍫濆瀻妞ょ绱?    @Operation(summary = "缁狅紕鎮婇崨妯荤叀鐠囥垹宕熸稉顏冪瑹娑撹崵娈戦幎銉ゆ叏鐠佹澘缍?, description = "閸掑棝銆夐弻銉嚄閹稿洤鐣炬稉姘瘜閻ㄥ嫭澧嶉張澶嬪Г娣囶喛顔囪ぐ鏇礉閹稿褰佹禍銈嗘闂傛潙鈧帒绨?)
    @GetMapping("/admin/user")
    public Result<IPage<RepairResult>> getUserRepairs(
            @Parameter(description = "娑撴矮瀵岄悽銊﹀煕ID", required = true) @RequestParam Long userId,
            @Parameter(description = "妞ょ數鐖滈敍鍫ョ帛鐠?销毁?) @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "濮ｅ繘銆夐弶鈩冩殶销毁涘牓绮拋?0销毁?) @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getUserRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage); // 閻╁瓨甯存潻鏂挎礀閸掑棝銆夐弫鐗堝祦
    }

    // 6. 缁狅紕鎮婇崨妯诲闁插繑娲块弬鐗堝Г娣囶喚濮搁幀?    @PostMapping("/admin/batchUpdateStatus")
    @Operation(summary = "閹靛綊鍣洪弴瀛樻煀閹躲儰鎱ㄩ悩鑸碘偓?, description = "缁狅紕鎮婇崨妯诲闁插繑娲块弬鏉款樋閺夆剝濮ゆ穱顔惧Ц閹?)
    @Log(title = "閹躲儰鎱ㄧ粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<Boolean> batchUpdateStatus(@RequestBody BatchUpdateStatusDTO dto) {
        boolean success = repairService.batchUpdateStatus(dto.getRepairIds(), dto.getStatus(), dto.getRemark());
        return success ? Result.success(true) : Result.fail("閹靛綊鍣洪弴瀛樻煀婢惰精瑙?);
    }

    // 7. 鐎电厧鍤幎銉ゆ叏閺佺増宓?    @GetMapping("/admin/export")
    @Operation(summary = "鐎电厧鍤幎銉ゆ叏閺佺増宓?, description = "鐎电厧鍤粭锕€鎮庨弶鈥叉閻ㄥ嫭濮ゆ穱顔芥殶閹诡喕璐烢xcel閹存渿SV")
    public void exportRepairs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) {
        
        repairService.exportRepairs(status, keyword, response);
    }

    // 8. 閼惧嘲褰囬幎銉ゆ叏缂佺喕顓搁弫鐗堝祦
    @GetMapping("/admin/stats")
    @Operation(summary = "閼惧嘲褰囬幎銉ゆ叏缂佺喕顓搁弫鐗堝祦", description = "閼惧嘲褰囬崥鍕潚閻樿埖鈧焦濮ゆ穱顔炬畱缂佺喕顓搁弫浼村櫤")
    public Result<RepairStatsResult> getRepairStats() {
        RepairStatsResult stats = repairService.getRepairStats();
        return Result.success(stats);
    }

}
