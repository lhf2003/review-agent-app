package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.pojo.MainTag;
import com.review.agent.entity.pojo.SubTag;
import com.review.agent.entity.pojo.TagRelation;
import com.review.agent.entity.request.TagRecommendRequest;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签接口
 */
@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private TagService tagService;

    /**
     * 添加推荐标签
     */
    @PostMapping("/recommand/add")
    public BaseResponse<Void> addRecommendTag(@RequestHeader("userId") Long userId, @RequestBody TagRecommendRequest request) {
        tagService.addRecommendTag(userId, request);
        return ResultUtil.success();
    }

    /**
     * 清空推荐标签
     */
    @GetMapping("/clear")
    public BaseResponse<Void> clearRecommendTag(@RequestHeader("userId") Long userId) {
        return ResultUtil.success();
    }

    // region 主标签

    /**
     * 获取主标签列表
     * @param userId 用户ID
     * @return 主标签列表
     */
    @GetMapping("/list")
    public BaseResponse<List<MainTag>> mainTagList(@RequestHeader("userId") Long userId) {
        return ResultUtil.success(tagService.findMainTagList(userId));
    }

    /**
     * 添加主标签
     * @param mainTag 主标签
     * @return 成功
     */
    @PostMapping("/add")
    public BaseResponse<Void> add(@Valid @RequestBody MainTag mainTag, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            mainTag.setUserId(userId);
        }
        tagService.addTag(mainTag);
        return ResultUtil.success();
    }

    /**
     * 更新主标签
     * @param mainTag 主标签
     * @return 成功
     */
    @PostMapping("/update")
    public BaseResponse<Void> update(@RequestBody MainTag mainTag, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            mainTag.setUserId(userId);
        }
        tagService.updateMainTag(mainTag);
        return ResultUtil.success();
    }

    /**
     * 删除主标签
     * @param userId 用户ID
     * @param id 主标签ID
     * @return 成功
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> delete(@RequestHeader("userId") Long userId, @RequestParam("id") Long id) {
        tagService.deleteMainTag(userId, id);
        return ResultUtil.success();
    }

    // endregion 主标签

    // region 子标签

    /**
     * 获取子标签列表
     * @param userId 用户ID
     * @return 子标签列表
     */
    @GetMapping("/sub/list")
    public BaseResponse<List<SubTag>> subTagList(@RequestHeader("userId") Long userId) {
        return ResultUtil.success(tagService.findSubTagList(userId));
    }

    /**
     * 添加子标签
     * @param subTag 子标签
     * @return 成功
     */
    @PostMapping("/add/sub")
    public BaseResponse<Void> addSub(@Valid @RequestBody SubTag subTag, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            subTag.setUserId(userId);
        }
        tagService.addSubTag(subTag);
        return ResultUtil.success();
    }

    /**
     * 更新子标签
     * @param subTag 子标签
     * @return 成功
     */
    @PostMapping("/update/sub")
    public BaseResponse<Void> updateSub(@RequestBody SubTag subTag, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            subTag.setUserId(userId);
        }
        tagService.updateSubTag(subTag);
        return ResultUtil.success();
    }

    /**
     * 删除子标签
     * @param userId 用户ID
     * @param id 子标签ID
     * @return 成功
     */
    @DeleteMapping("/delete/sub")
    public BaseResponse<Void> deleteSub(@RequestHeader("userId") Long userId, @RequestParam("id") Long id) {
        tagService.deleteSubTag(userId, id);
        return ResultUtil.success();
    }

    //endregion 子标签

    //region 关联关系

    /**
     * 获取主标签关联子标签关系列表
     * @param userId 用户ID
     * @param mainTagId 主标签ID
     * @return 主标签关联子标签关系列表
     */
    @GetMapping("/list/relation")
    public BaseResponse<List<SubTag>> tagRelationList(@RequestHeader("userId") Long userId, @RequestParam("mainTagId") Long mainTagId) {
        return ResultUtil.success(tagService.findSubTagListByMainTagId(userId, mainTagId));
    }

    /**
     * 绑定主标签关联子标签关系
     * @param tagRelation 主标签关联子标签关系
     * @return 成功
     */
    @PostMapping("/add/relation")
    public BaseResponse<Void> addRelation(@RequestHeader("userId") Long userId, @Valid @RequestBody TagRelation tagRelation) {
        tagRelation.setUserId(userId);
        tagService.addTagRelation(tagRelation);
        return ResultUtil.success();
    }

    /**
     * 解绑主标签关联子标签关系
     * @return 成功
     */
    @DeleteMapping("/delete/relation")
    public BaseResponse<Void> deleteRelation(@RequestBody TagRelation tagRelation) {
        tagService.deleteTagRelation(tagRelation);
        return ResultUtil.success();
    }
}
