package com.lsx.property.repair.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.property.client.HouseServiceClient;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.dto.external.HouseDTO;
import com.lsx.property.repair.dto.RepairDto;
import com.lsx.property.repair.entity.Repair;
import com.lsx.property.repair.mapper.RepairMapper;
import com.lsx.property.repair.service.RepairService;
import com.lsx.property.repair.vo.RepairResult;
import com.lsx.property.repair.vo.RepairStatsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {
    
    @Autowired
    private HouseServiceClient houseServiceClient;
    
    @Override
    public boolean submitRepair(RepairDto repairdto) {
        String buildingNo = repairdto.getBuildingNo();
        String houseNo = repairdto.getHouseNo();
        // 1. 闂堢偟鈹栭弽锟犵崣
        if (buildingNo == null || buildingNo.trim().isEmpty()) {
            throw new RuntimeException("濡ゅ吋鐖ч崣铚傜瑝閼虫垝璐熺粚?);
        }
        if (houseNo == null || houseNo.trim().isEmpty()) {
            throw new RuntimeException("閹村灝鐪块崣铚傜瑝閼虫垝璐熺粚?);
        }

        // 閹村灝鐪块崣销毁嬵劀閸?        String housePattern = "^\\d{3,4}$"; 
        if (!ReUtil.isMatch(housePattern, houseNo.trim())) {
            throw new RuntimeException("閹村灝鐪块崣销毁嬬壐瀵繘鏁婄拠顖ょ礉鎼存柧璐?-4娴ｅ秵鏆熺€涙绱欐俊?101'销毁?);
        }

        // 閺嶈宓佸Δ充值肩埀閸欏嘲鎷伴幋鍨溈缂傛牕褰块弻銉嚄閹村灝鐪縄D
        HouseDTO house = houseServiceClient.getHouseByInfo(buildingNo, houseNo);
        if (house == null) {
            throw new RuntimeException("閹村灝鐪挎稉宥呯摠閸︻煉绱濈拠销毁嬵梾閺屻儲銈奸弽瀣娇閸滃本鍩х仦瀣娇");
        }
        Long houseId = house.getId();
        
        Repair repair = new Repair();
        BeanUtil.copyProperties(repairdto, repair);
        repair.setHouseId(houseId);
        repair.setStatus("pending");
        if (UserContext.getCommunityId() != null) {
            repair.setCommunityId(UserContext.getCommunityId());
        }
        return baseMapper.insert(repair) > 0;
    }

    @Override
    public boolean updateRepairStatus(Long repairId, String status, String remark) {
        // 1. 閺屻儲濮ゆ穱顔煎礋閺勵垰鎯佺€涙ê婀?        Repair repair = baseMapper.selectById(repairId);
        if (repair == null) {
            return false;
        }
        // 2. 閺囧瓨鏌婇悩鑸碘偓浣告嫲婢跺洦鏁?        repair.setStatus(status);
        repair.setHandleRemark(remark);
        return baseMapper.updateById(repair) > 0;
    }

    //閻劍鍩涢弻銉嚄閼奉亜绻侀惃鍕Г娣囶喛顓归崡?    @Override
    public IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "閻劍鍩汭D娑撳秷充值樻稉铏光敄");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        // 閸掑棝銆夐弻銉嚄瑜版挸澧犻悽銊﹀煕閻ㄥ嫭濮ゆ穱顔款唶瑜?        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        // 鏉烆剚宕叉稉鍝勫缁旑垰鐫嶇粈铏规畱VO
        return repairPage.convert(this::convertToRepairResult);
    }

    //缁狅紕鎮婇崨妯荤叀鐠囥垹鍙忛柈銊ф畱閹躲儰鎱ㄧ拋銏犲礋
    @Override
    public IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<Repair> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 閺夊啴妾烘潻鍥ㄦ姢闁槒绶?---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 鐡掑懐楠囩粻锛勬倞閸涙﹫绱版稉宥呬粵闂勬劕鍩?        } else {
             if (currentCommunityId == null) {
                  // 閺咁噣鈧氨顓搁悶鍡楁喅/娑撴矮瀵屾俊鍌涚亯濞屸剝婀佺粈鎯у隘ID销毁涘本鐓℃稉宥呭煂閺佺増宓?                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------

        // 閻樿埖鈧胶鐡柅?        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }

        // 閸忔娊鏁拠宥嗘偝缁?        if (StringUtils.hasText(keyword)) {
            // 婢跺嫮鎮婇崗鎶芥暛鐠囧稄绱濋弨顖涘瘮"1閺?01"閺嶇厧绱?            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);

            queryWrapper.and(wrapper -> {
                // 娑撶儤鐦℃稉顏呮偝缁便垽銆嶅ǎ璇插閺夆€叉
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }

                // 濞ｈ濮為幋鍨溈ID閺夆€叉
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }

        IPage<Repair> repairPage = baseMapper.selectPage(page, queryWrapper);
        return repairPage.convert(this::convertToRepairResult);
    }

    // 婢跺嫮鎮婇幖婊呭偍閸忔娊鏁拠?    private List<String> processSearchKeyword(String keyword) {
        List<String> searchTerms = new ArrayList<>();
        searchTerms.add(keyword); // 閸樼喎顫愰崗鎶芥暛鐠?
        // 婢跺嫮鎮?1閺?01"閺嶇厧绱?        if (keyword.contains("閺?)) {
            // 閹绘劕褰囧Δ充值肩埀闁劌鍨?            String[] parts = keyword.split("閺?);
            if (parts.length > 0) {
                String buildingPart = parts[0] + "閺?;
                searchTerms.add(buildingPart);
            }

            // 閹绘劕褰囬幋鍨娇闁劌鍨?            if (parts.length > 1 && !parts[1].isEmpty()) {
                String housePart = parts[1];
                // 缁夊娅?鐎?鐎涙绱欐俊鍌涚亯閺堝绱?                housePart = housePart.replace("鐎?, "");
                if (!housePart.isEmpty()) {
                    searchTerms.add(housePart);
                }
            }
        }

        return searchTerms.stream().distinct().collect(Collectors.toList());
    }

    // 閼惧嘲褰囬幍鈧張澶婂爱闁板秶娈戦幋鍨溈ID
    private List<Long> getAllMatchingHouseIds(List<String> searchTerms) {
        Set<Long> houseIds = new HashSet<>();

        for (String term : searchTerms) {
            houseIds.addAll(getHouseIdsByKeyword(term));
        }

        return new ArrayList<>(houseIds);
    }

    
    // 閺嶈宓侀崡鏇氶嚋閸忔娊鏁拠宥嗙叀鐠囥垺鍩х仦濠璂
    private List<Long> getHouseIdsByKeyword(String keyword) {
        return houseServiceClient.searchHouseIds(keyword);
    }


    /**
     * 缁狅紕鎮婇崨妯荤叀鐠囥垹宕熸稉顏冪瑹娑撹崵娈戦幎銉ゆ叏鐠佹澘缍嶉敍鍫濆瀻妞ょ绱?     */
    @Override
    public IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "閻劍鍩汭D娑撳秷充值樻稉铏光敄");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        return repairPage.convert(this::convertToRepairResult);
    }


    /**
     * 閸忣剙鍙℃潪顒佸床閺傝纭堕敍姝奺pair 閳?RepairResult销毁涘牆鍙ч懕鏃€鍩х仦瀣╀繆閹垽绱?     */
    private RepairResult convertToRepairResult(Repair repair) {
        if (repair == null) return null;

        RepairResult result = new RepairResult();

        // 1. 婢跺秴鍩楅崺鐑樻拱閹躲儰鎱ㄦ穱鈩冧紖
        result.setId(repair.getId());
        result.setFaultType(repair.getFaultType());
        result.setFaultDesc(repair.getFaultDesc());
        result.setStatus(repair.getStatus());
        result.setHandleRemark(repair.getHandleRemark());
        result.setCreateTime(repair.getCreateTime());
        result.setUpdateTime(repair.getUpdateTime());

        // 2. 婢跺嫮鎮婇崶鍓у閸掓銆?        if (StringUtils.hasText(repair.getFaultImgs())) {
            List<String> imgs = Arrays.asList(repair.getFaultImgs().split(","));
            result.setFaultImgs(imgs);
        } else {
            result.setFaultImgs(new ArrayList<>());
        }

        // 3. 閺屻儴顕楅獮鎯邦啎缂冾喗鍩х仦瀣╀繆閹?        if (repair.getHouseId() != null) {
            HouseDTO house = houseServiceClient.getHouseById(repair.getHouseId());
            if (house != null) {
                result.setCommunityName(house.getCommunityName());
                result.setBuildingNo(house.getBuildingNo());
                result.setHouseNo(house.getHouseNo());
            }
        }

        // 4. 鐠佸墽鐤嗛悩鑸碘偓浣疯厬閺傚洦开始挎潻甯礄閸欘垶鈧绱?        result.setStatusDesc(getStatusDesc(repair.getStatus()));

        return result;
    }

    // 閼惧嘲褰囬悩鑸碘偓浣疯厬閺傚洦开始挎潻?    private String getStatusDesc(String status) {
        switch (status) {
            case "pending":
                return "瀵板懎顦╅悶?;
            case "processing":
                return "婢跺嫮鎮婃稉?;
            case "completed":
                return "瀹告彃鐣幋?;
            case "cancelled":
                return "瀹告彃褰囧☉?;
            default:
                return status;
        }
    }

    /**
     * 閻樿埖鈧浇瀚抽弬鍥祮娑擃厽鏋冮幓蹇氬牚
     */
    private String convertStatusToDesc(String status) {
        if (status == null) {
            return "閺堫亞鐓￠悩鑸碘偓?;
        }
        // JDK 8 閺€顖涘瘮閻ㄥ嫪绱剁紒?switch 鐠囶厼褰?        switch (status) {
            case "pending":
                return "瀵板懎顦╅悶?;
            case "processing":
                return "婢跺嫮鎮婃稉?;
            case "completed":
                return "瀹告彃鐣幋?;
            case "cancelled":
                return "瀹告彃褰囧☉?;
            default:
                return "閺堫亞鐓￠悩鑸碘偓?;
        }
    }

    @Override
    public boolean batchUpdateStatus(List<Long> repairIds, String status, String remark) {
        if (repairIds == null || repairIds.isEmpty()) {
            return false;
        }
        
        // 妤犲矁鐦夐悩鑸碘偓浣告値濞夋洘鈧?        List<String> validStatuses = Arrays.asList("pending", "processing", "completed", "cancelled");
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("閺冪姵鏅ラ惃鍕Ц閹礁鈧?);
        }
        
        // 閹靛綊鍣洪弴瀛樻煀
        List<Repair> repairs = new ArrayList<>();
        for (Long repairId : repairIds) {
            Repair repair = new Repair();
            repair.setId(repairId);
            repair.setStatus(status);
            repair.setHandleRemark(remark);
            repairs.add(repair);
        }
        
        // 娴ｈ法鏁yBatis Plus閻ㄥ嫭澹掗柌蹇旀纯閺傜増鏌熷▔?        return this.updateBatchById(repairs);
    }

    @Override
    public void exportRepairs(String status, String keyword, HttpServletResponse response) {
        // 閺屻儴顕楃粭锕€鎮庨弶鈥叉閻ㄥ嫭濮ゆ穱顔芥殶閹?        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 閺夊啴妾烘潻鍥ㄦ姢闁槒绶?---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 鐡掑懐楠囩粻锛勬倞閸涙﹫绱版稉宥呬粵闂勬劕鍩?        } else {
             if (currentCommunityId == null) {
                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------
        
        // 閻樿埖鈧胶鐡柅?        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }
        
        // 閸忔娊鏁拠宥嗘偝缁?        if (StringUtils.hasText(keyword)) {
            // 婢跺嫮鎮婇崗鎶芥暛鐠囧稄绱濋弨顖涘瘮"1閺?01"閺嶇厧绱?            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);
            
            queryWrapper.and(wrapper -> {
                // 娑撶儤鐦℃稉顏呮偝缁便垽銆嶅ǎ璇插閺夆€叉
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }
                
                // 濞ｈ濮為幋鍨溈ID閺夆€叉
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }
        
        List<Repair> repairs = this.list(queryWrapper);
        
        // 鏉烆剚宕叉稉绡焑pairResult
        List<RepairResult> repairResults = repairs.stream()
                .map(this::convertToRepairResult)
                .collect(Collectors.toList());
        
        // 鏉╂瑩鍣风€圭偟骞囩粻鈧崡鏇犳畱CSV鐎电厧鍤敍灞筋洤闂団偓Excel鐎电厧鍤崣顖涘潑閸旂嚛OI娓氭繆绂?        try {
            // 鐠佸墽鐤嗛崫宥呯安婢?            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=repairs.csv");
            
            // 閸愭瑥鍙咰SV閸愬懎顔?            try (PrintWriter writer = response.getWriter()) {
                // 閸愭瑥鍙嗙悰銊ャ仈
                writer.println("ID,缁€鎯у隘閸氬秶袨,濡ゅ吋鐖ч崣?閹村灝鐪块崣?閺佸懘娈扮猾璇茬€?閺佸懘娈伴幓蹇氬牚,閻樿埖鈧?婢跺嫮鎮婃径鍥ㄦ暈,閸掓稑缂撻弮鍫曟？");
                
                // 閸愭瑥鍙嗛弫鐗堝祦鐞?                for (RepairResult result : repairResults) {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            result.getId(),
                            result.getCommunityName(),
                            result.getBuildingNo(),
                            result.getHouseNo(),
                            result.getFaultType(),
                            result.getFaultDesc(),
                            result.getStatusDesc(),
                            result.getHandleRemark() != null ? result.getHandleRemark() : "",
                            result.getCreateTime());
                }
                
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("鐎电厧鍤径杈Е销毁? + e.getMessage());
        }
    }

    @Override
    public RepairStatsResult getRepairStats() {
        RepairStatsResult stats = new RepairStatsResult();
        
        // --- 閺夊啴妾烘潻鍥ㄦ姢闁槒绶?---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        LambdaQueryWrapper<Repair> baseWrapper = Wrappers.lambdaQuery(Repair.class);
        
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId != null) {
                baseWrapper.eq(Repair::getCommunityId, currentCommunityId);
            } else {
                baseWrapper.eq(Repair::getId, -1L);
            }
        }

        // 閺屻儴顕楅幀缁樻殶
        stats.setTotal(Math.toIntExact(this.count(baseWrapper)));
        
        // 閺屻儴顕楅崥鍕Ц閹焦鏆熼柌?        LambdaQueryWrapper<Repair> pendingQuery = baseWrapper.clone().eq(Repair::getStatus, "pending");
        stats.setPending(Math.toIntExact(this.count(pendingQuery)));
        
        LambdaQueryWrapper<Repair> processingQuery = baseWrapper.clone().eq(Repair::getStatus, "processing");
        stats.setProcessing(Math.toIntExact(this.count(processingQuery)));
        
        LambdaQueryWrapper<Repair> completedQuery = baseWrapper.clone().eq(Repair::getStatus, "completed");
        stats.setCompleted(Math.toIntExact(this.count(completedQuery)));
        
        LambdaQueryWrapper<Repair> cancelledQuery = baseWrapper.clone().eq(Repair::getStatus, "cancelled");
        stats.setCancelled(Math.toIntExact(this.count(cancelledQuery)));
        
        return stats;
    }
}

