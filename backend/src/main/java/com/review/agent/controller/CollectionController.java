package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.request.CollectionItemRequest;
import com.review.agent.entity.request.CollectionRequest;
import com.review.agent.entity.request.QuickCreateRequest;
import com.review.agent.entity.vo.CollectionCreateResultVo;
import com.review.agent.entity.vo.CollectionDetailVo;
import com.review.agent.entity.vo.CollectionRecommendationVO;
import com.review.agent.entity.vo.CollectionVo;
import com.review.agent.entity.vo.QuickCreateResultVO;
import com.review.agent.entity.vo.SmartRecommendationVO;
import com.review.agent.service.CollectionRecommendationService;
import com.review.agent.service.CollectionService;
import com.review.agent.service.SmartCollectionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合集接口
 */
@RestController
@RequestMapping("/collection")
public class CollectionController {
    @Resource
    private CollectionService collectionService;
    @Resource
    private CollectionRecommendationService collectionRecommendationService;
    @Resource
    private SmartCollectionService smartCollectionService;
    @Resource
    private SecurityUtils securityUtils;

    /**
     * 新增合集
     * @param request 合集请求
     */
    @PostMapping("/add")
    public BaseResponse<CollectionCreateResultVo> addCollection(@RequestBody CollectionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        CollectionCreateResultVo result = collectionService.createCollection(userId, request);
        return ResultUtil.success(result);
    }

    /**
     * 更新合集信息
     * @param id 合集ID
     * @param request 合集请求
     */
    @PutMapping("/update")
    public BaseResponse<Void> updateCollection(@RequestParam("id") Long id, @RequestBody CollectionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        collectionService.updateCollection(userId, id, request);
        return ResultUtil.success();
    }

    /**
     * 删除合集
     * @param id 合集ID
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> deleteCollection(@RequestParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        collectionService.deleteCollection(userId, id);
        return ResultUtil.success();
    }

    /**
     * 归纳合集（相似结果）
     * @param request 合集请求
     */
    @PostMapping("/add-with-items")
    public BaseResponse<Long> addCollectionWithItems(@RequestBody CollectionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Long id = collectionService.createCollectionWithItems(userId, request);
        return ResultUtil.success(id);
    }

    /**
     * 合集列表（带结果数量）
     * @return 合集列表
     */
    @GetMapping("/list")
    public BaseResponse<List<CollectionVo>> listCollection() {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(collectionService.listCollection(userId));
    }

    /**
     * 合集详情
     * @param id 合集ID
     * @return 合集详情
     */
    @GetMapping("/detail")
    public BaseResponse<CollectionDetailVo> detailCollection(@RequestParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(collectionService.getCollectionDetail(userId, id));
    }

    /**
     * 修改合集内容 (Update - Add/Remove Items)
     * @param collectionId 合集ID
     * @param request 合集请求
     * @return 空响应
     */
    @PostMapping("/items")
    public BaseResponse<Void> updateCollectionItems(@RequestParam(value = "collectionId", required = false) Long collectionId,
                                                 @RequestBody CollectionItemRequest request) {
        
        if (collectionId == null) {
            // Check if it's in the request body? No, I defined the DTO without it.
            return ResultUtil.error("Collection ID is required");
        }
        
        Long userId = securityUtils.getCurrentUserId();
        collectionService.updateCollectionItems(userId, collectionId, request);
        return ResultUtil.success();
    }
    
    /**
     * 检查归属状态
     * @param analysisId 分析ID
     * @return 合集列表
     */
    @GetMapping("/check-contain")
    public BaseResponse<List<CollectionVo>> checkContain(@RequestParam("analysisId") Long analysisId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(collectionService.checkContain(userId, analysisId));
    }

    /**
     * 获取合集推荐列表（基于薄弱知识点）
     *
     * @param limit 限制数量
     * @return 推荐列表
     */
    @GetMapping("/recommendations")
    public BaseResponse<List<CollectionRecommendationVO>> getCollectionRecommendations(
        @RequestParam(defaultValue = "8") int limit
    ) {
        Long userId = securityUtils.getCurrentUserId();
        List<CollectionRecommendationVO> recommendations = collectionRecommendationService.getRecommendations(userId, limit);
        return ResultUtil.success(recommendations);
    }

    // ==================== 智能推荐合集 API ====================

    /**
     * 获取智能推荐合集
     * 基于未归档的分析结果，按标签聚合生成推荐
     *
     * @return 智能推荐列表
     */
    @GetMapping("/smart-recommendations")
    public BaseResponse<SmartRecommendationVO> getSmartRecommendations() {
        Long userId = securityUtils.getCurrentUserId();
        SmartRecommendationVO recommendations = smartCollectionService.getSmartRecommendations(userId);
        return ResultUtil.success(recommendations);
    }

    /**
     * 快速创建合集
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/quick-create")
    public BaseResponse<QuickCreateResultVO> quickCreateCollection(@Valid @RequestBody QuickCreateRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        QuickCreateResultVO result = smartCollectionService.quickCreateCollection(userId, request);
        return ResultUtil.success(result);
    }

    /**
     * 忽略推荐
     *
     * @param recommendationId 推荐ID
     * @return 操作结果
     */
    @PostMapping("/recommendations/{recommendationId}/dismiss")
    public BaseResponse<Void> dismissRecommendation(@PathVariable String recommendationId) {
        Long userId = securityUtils.getCurrentUserId();
        smartCollectionService.dismissRecommendation(userId, recommendationId);
        return ResultUtil.success();
    }

}
