package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
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
    @Resource
    private SecurityUtils securityUtils;

    /**
     * 添加推荐标签
     */
    @PostMapping("/recommand/add")
    public BaseResponse<Void> addRecommendTag(@RequestBody TagRecommendRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        tagService.addRecommendTag(userId, request);
        return ResultUtil.success();
    }

    /**
     * 清空推荐标签
     */
    @GetMapping("/clear")
    public BaseResponse<Void> clearRecommendTag() {
        return ResultUtil.success();
    }

    // region 主标签

    /**
     * 获取主标签列表
     * @return 主标签列表
     */
    @GetMapping("/list")
    public BaseResponse<List<MainTag>> mainTagList() {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.findMainTagList(userId));
    }

    /**
     * 添加主标签
     * @param mainTag 主标签
     * @return 成功
     */
    @PostMapping("/add")
    public BaseResponse<Void> add(@Valid @RequestBody MainTag mainTag) {
        Long userId = securityUtils.getCurrentUserId();
        mainTag.setUserId(userId);
        tagService.addTag(mainTag);
        return ResultUtil.success();
    }

    /**
     * 更新主标签
     * @param mainTag 主标签
     * @return 成功
     */
    @PostMapping("/update")
    public BaseResponse<Void> update(@Valid @RequestBody MainTag mainTag) {
        Long userId = securityUtils.getCurrentUserId();
        mainTag.setUserId(userId);
        tagService.updateMainTag(mainTag);
        return ResultUtil.success();
    }

    /**
     * 删除主标签
     * @param id 主标签ID
     * @return 成功
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> delete(@RequestParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        tagService.deleteMainTag(userId, id);
        return ResultUtil.success();
    }

    // endregion 主标签

    // region 子标签

    /**
     * 获取子标签列表
     * @return 子标签列表
     */
    @GetMapping("/sub/list")
    public BaseResponse<List<SubTag>> subTagList() {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.findSubTagList(userId));
    }

    /**
     * 添加子标签
     * @param subTag 子标签
     * @return 成功
     */
    @PostMapping("/add/sub")
    public BaseResponse<Void> addSub(@Valid @RequestBody SubTag subTag) {
        Long userId = securityUtils.getCurrentUserId();
        subTag.setUserId(userId);
        tagService.addSubTag(subTag);
        return ResultUtil.success();
    }

    /**
     * 更新子标签
     * @param subTag 子标签
     * @return 成功
     */
    @PostMapping("/update/sub")
    public BaseResponse<Void> updateSub(@Valid @RequestBody SubTag subTag) {
        Long userId = securityUtils.getCurrentUserId();
        subTag.setUserId(userId);
        tagService.updateSubTag(subTag);
        return ResultUtil.success();
    }

    /**
     * 删除子标签
     * @param id 子标签ID
     * @return 成功
     */
    @DeleteMapping("/delete/sub")
    public BaseResponse<Void> deleteSub(@RequestParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        tagService.deleteSubTag(userId, id);
        return ResultUtil.success();
    }

    //endregion 子标签

    //region 关联关系

    /**
     * 获取主标签关联子标签关系列表
     * @param mainTagId 主标签ID
     * @return 主标签关联子标签关系列表
     */
    @GetMapping("/list/relation")
    public BaseResponse<List<SubTag>> tagRelationList(@RequestParam("mainTagId") Long mainTagId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.findSubTagListByMainTagId(userId, mainTagId));
    }

    /**
     * 绑定主标签关联子标签关系
     * @param tagRelation 主标签关联子标签关系
     * @return 成功
     */
    @PostMapping("/add/relation")
    public BaseResponse<Void> addRelation(@Valid @RequestBody TagRelation tagRelation) {
        Long userId = securityUtils.getCurrentUserId();
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
