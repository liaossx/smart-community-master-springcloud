package com.lsx.property.visitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.visitor.dto.VisitorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/visitor")
@Tag(name = "鐠佸灝顓圭粻锛勬倞閹恒儱褰?)
public class VisitorController {

    private static final Logger log = LoggerFactory.getLogger(VisitorController.class);

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/apply")
    @Operation(summary = "閹绘劒姘︽０鍕")
    public Result<Long> apply(@RequestBody SysVisitor body) {
        Long id = visitorService.apply(body);
        return Result.success(id);
    }

    @GetMapping("/my")
    @Operation(summary = "閹存垹娈戠拋鍨吂妫板嫮瀹?)
    public Result<IPage<SysVisitor>> my(@RequestParam("userId") Long userId,
                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysVisitor> page = visitorService.myList(userId, pageNum, pageSize);
        return Result.success(page);
    }


    @GetMapping("/list")
    @Operation(summary = "缁狅紕鎮婇崨妯侯吀閺嶇鍨悰?)
    public Result<IPage<VisitorDTO>> adminList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            log.info("閺€璺哄煂鐠佸灝顓归崚妤勩€冪拠销毁嬬湴: pageNum={}, pageSize={}, status={}, keyword={}", pageNum, pageSize, status, keyword);
            IPage<VisitorDTO> page = visitorService.adminList(pageNum, pageSize, status, keyword);
            log.info("鐠佸灝顓归崚妤勩€冮弻銉嚄閹存劕濮? total={}", page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            log.error("鐠佸灝顓归崚妤勩€冮弻銉嚄閸欐垹鏁撳鍌氱埗!", e);
            throw e;
        }
    }

    @PutMapping("/audit")
    @Operation(summary = "缁狅紕鎮婇崨妯侯吀閺?)
    @Log(title = "鐠佸灝顓圭粻锛勬倞", businessType = BusinessType.UPDATE)
    public Result<Boolean> audit(@RequestParam("id") Long id,
                                 @RequestParam("status") String status,
                                 @RequestParam(value = "remark", required = false) String remark) {
        boolean ok = visitorService.audit(id, status, remark);
        return ok ? Result.success(true) : Result.fail("鐎光剝鐗虫径杈Е");
    }
}

