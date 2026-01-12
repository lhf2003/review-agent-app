package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.request.CollectionItemRequest;
import com.review.agent.entity.request.CollectionRequest;
import com.review.agent.entity.vo.CollectionDetailVo;
import com.review.agent.entity.vo.CollectionVo;
import com.review.agent.service.CollectionService;
import jakarta.annotation.Resource;
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

    /**
     * 新增合集
     * @param request 合集请求
     */
    @PostMapping("/add")
    public BaseResponse<Long> addCollection(@RequestHeader("userId") Long userId, @RequestBody CollectionRequest request) {
        Long id = collectionService.createCollection(userId, request);
        return ResultUtil.success(id);
    }

    /**
     * 更新合集信息
     * @param userId 用户ID
     * @param id 合集ID
     * @param request 合集请求
     */
    @PutMapping("/update")
    public BaseResponse<Void> updateCollection(@RequestHeader("userId") Long userId, @RequestParam("id") Long id, @RequestBody CollectionRequest request) {
        collectionService.updateCollection(userId, id, request);
        return ResultUtil.success();
    }

    /**
     * 删除合集
     * @param userId 用户ID
     * @param id 合集ID
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> deleteCollection(@RequestHeader("userId") Long userId, @RequestParam("id") Long id) {
        collectionService.deleteCollection(userId, id);
        return ResultUtil.success();
    }

    /**
     * 归纳合集（相似结果）
     * @param request 合集请求
     */
    @PostMapping("/add-with-items")
    public BaseResponse<Long> addCollectionWithItems(@RequestHeader("userId") Long userId, @RequestBody CollectionRequest request) {
        Long id = collectionService.createCollectionWithItems(userId, request);
        return ResultUtil.success(id);
    }

    /**
     * 合集列表（带结果数量）
     * @param userId 用户ID
     * @return 合集列表
     */
    @GetMapping("/list")
    public BaseResponse<List<CollectionVo>> listCollection(@RequestHeader("userId") Long userId) {
        return ResultUtil.success(collectionService.listCollection(userId));
    }

    /**
     * 合集详情
     * @param userId 用户ID
     * @param id 合集ID
     * @return 合集详情
     */
    @GetMapping("/detail")
    public BaseResponse<CollectionDetailVo> detailCollection(@RequestHeader("userId") Long userId, @RequestParam("id") Long id) {
        return ResultUtil.success(collectionService.getCollectionDetail(userId, id));
    }

    /**
     * 修改合集内容 (Update - Add/Remove Items)
     * @param userId 用户ID
     * @param collectionId 合集ID
     * @param request 合集请求
     * @return 空响应
     */
    @PostMapping("/items")
    public BaseResponse<Void> updateCollectionItems(@RequestHeader("userId") Long userId,
                                                 @RequestParam(value = "collectionId", required = false) Long collectionId, 
                                                 @RequestBody CollectionItemRequest request) {
        
        if (collectionId == null) {
            // Check if it's in the request body? No, I defined the DTO without it.
            return ResultUtil.error("Collection ID is required");
        }
        
        collectionService.updateCollectionItems(userId, collectionId, request);
        return ResultUtil.success();
    }
    
    /**
     * 检查归属状态
     * @param userId 用户ID
     * @param analysisId 分析ID
     * @return 合集列表
     */
    @GetMapping("/check-contain")
    public BaseResponse<List<CollectionVo>> checkContain(@RequestHeader("userId") Long userId, @RequestParam("analysisId") Long analysisId) {
        return ResultUtil.success(collectionService.checkContain(userId, analysisId));
    }

}
