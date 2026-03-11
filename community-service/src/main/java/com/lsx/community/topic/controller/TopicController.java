package com.lsx.community.topic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.community.topic.dto.TopicAuditDTO;
import com.lsx.community.topic.dto.TopicCommentDTO;
import com.lsx.community.topic.dto.TopicCreateDTO;
import com.lsx.community.topic.service.TopicService;
import com.lsx.community.topic.vo.TopicCommentVO;
import com.lsx.community.topic.vo.TopicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topic")
@Tag(name = "社区话题接口")
public class TopicController {

    @Autowired
    private TopicService topicService;

    @PostMapping
    @Operation(summary = "发布话题")
    public Result<Long> create(@RequestBody TopicCreateDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(topicService.createTopic(dto, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "话题列表")
    public Result<Page<TopicVO>> list(@RequestParam("communityId") Long communityId,
                                      @RequestParam(value = "userId", required = false) Long userId,
                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(topicService.listTopics(communityId, userId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "话题详情")
    public Result<TopicVO> detail(@PathVariable("id") Long topicId, @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(topicService.getTopicDetail(topicId, userId));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞/取消点赞")
    public Result<Boolean> like(@PathVariable("id") Long topicId, @RequestParam("userId") Long userId) {
        return Result.success(topicService.likeTopic(topicId, userId));
    }

    @PostMapping("/comment")
    @Operation(summary = "发表评论")
    public Result<Long> comment(@RequestBody TopicCommentDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(topicService.commentTopic(dto, userId));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "获取评论列表")
    public Result<List<TopicCommentVO>> getComments(@PathVariable("id") Long topicId) {
        return Result.success(topicService.getComments(topicId));
    }

    @PostMapping("/audit")
    @Operation(summary = "审核话题")
    public Result<Boolean> audit(@RequestBody TopicAuditDTO dto) {
        return Result.success(topicService.auditTopic(dto));
    }
}
