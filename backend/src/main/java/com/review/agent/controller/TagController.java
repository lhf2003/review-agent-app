package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.pojo.TagDimension;
import com.review.agent.entity.request.TagRecommendRequest;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签接口（重构版）
 * 使用新的Tag实体，支持多维度标签体系
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

    // region 维度管理

    /**
     * 获取标签维度列表
     * @return 维度列表
     */
    @GetMapping("/list")
    public BaseResponse<List<TagDimension>> dimensionList() {
        return ResultUtil.success(tagService.findAllDimensions());
    }

    // endregion 维度管理

    // region 标签管理

    /**
     * 获取技术领域标签列表（原主标签）
     * @return 标签列表
     */
    @GetMapping("/tech-domain/list")
    public BaseResponse<List<Tag>> techDomainTagList() {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.findTechDomainTags(userId));
    }

    /**
     * 获取标签树结构
     * @param dimensionId 维度ID（可选）
     * @return 标签树
     */
    @GetMapping("/tree")
    public BaseResponse<List<TagService.TagVO>> tagTree(@RequestParam(required = false) Long dimensionId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.getTagTree(userId, dimensionId));
    }

    /**
     * 添加标签
     * @param tag 标签
     * @return 成功
     */
    @PostMapping("/add")
    public BaseResponse<Long> add(@Valid @RequestBody Tag tag) {
        Long tagId = tagService.addTag(tag);
        return ResultUtil.success(tagId);
    }

    /**
     * 更新标签
     * @param tag 标签
     * @return 成功
     */
    @PostMapping("/update")
    public BaseResponse<Void> update(@Valid @RequestBody Tag tag) {
        tagService.updateTag(tag);
        return ResultUtil.success();
    }

    /**
     * 删除标签
     * @param id 标签ID
     * @return 成功
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> delete(@RequestParam("id") Long id) {
        tagService.deleteTag(id);
        return ResultUtil.success();
    }

    // endregion 标签管理

    // region 子标签

    /**
     * 获取子标签列表
     * @param parentId 父标签ID
     * @return 子标签列表
     */
    @GetMapping("/sub/list")
    public BaseResponse<List<Tag>> subTagList(@RequestParam("parentId") Long parentId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(tagService.findSubTagsByParentId(userId, parentId));
    }

    /**
     * 添加子标签（通过parentId字段）
     * @param tag 标签（需设置parentId）
     * @return 成功
     */
    @PostMapping("/add/sub")
    public BaseResponse<Long> addSub(@Valid @RequestBody Tag tag) {
        if (tag.getParentId() == null) {
            throw new IllegalArgumentException("子标签必须设置parentId");
        }
        Long tagId = tagService.addTag(tag);
        return ResultUtil.success(tagId);
    }

    // endregion 子标签
}
