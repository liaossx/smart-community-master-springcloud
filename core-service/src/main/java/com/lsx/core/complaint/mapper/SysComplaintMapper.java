package com.lsx.core.complaint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.complaint.dto.ComplaintDTO;
import com.lsx.core.complaint.entity.SysComplaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysComplaintMapper extends BaseMapper<SysComplaint> {

    @Select("<script>" +
            "SELECT c.*, u.phone AS userPhone, u.real_name AS userName " +
            "FROM sys_complaint c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='status != null and status.length > 0'> AND c.status = #{status} </if>" +
            "<if test='communityId != null'> AND c.community_id = #{communityId} </if>" +
            "ORDER BY c.create_time DESC" +
            "</script>")
    IPage<ComplaintDTO> selectAdminList(Page<ComplaintDTO> page, 
                                        @Param("status") String status, 
                                        @Param("communityId") Long communityId);
}
