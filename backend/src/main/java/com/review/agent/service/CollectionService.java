package com.review.agent.service;

import com.review.agent.common.exception.ErrorCode;
import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.request.CollectionItemRequest;
import com.review.agent.entity.request.CollectionRequest;
import com.review.agent.entity.vo.CollectionDetailVo;
import com.review.agent.entity.vo.CollectionVo;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.CollectionRelationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CollectionService {

    @Resource
    private AnalysisCollectionRepository analysisCollectionRepository;

    @Resource
    private CollectionRelationRepository collectionRelationRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    public Long createCollection(Long userId, CollectionRequest request) {
        AnalysisCollection collection = new AnalysisCollection();
        collection.setUserId(userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection = analysisCollectionRepository.save(collection);
        return collection.getId();
    }

    public void updateCollection(Long userId, Long collectionId, CollectionRequest request) {
        AnalysisCollection collection = analysisCollectionRepository.findByIdAndUserId(userId, collectionId);
        if (collection == null) {
            ExceptionUtils.throwDataNotFound("合集不存在或无权修改");
        }
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        analysisCollectionRepository.save(collection);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long userId, Long collectionId) {
        AnalysisCollection collection = analysisCollectionRepository.findByIdAndUserId(userId, collectionId);
        if (collection == null) {
            ExceptionUtils.throwDataNotFound("合集不存在或无权删除");
        }
        // 删除关联
        collectionRelationRepository.deleteByCollectionId(collectionId);
        // 删除合集
        analysisCollectionRepository.delete(collection);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCollectionWithItems(Long userId, CollectionRequest request) {
        // 1. Create Collection
        AnalysisCollection collection = new AnalysisCollection();
        collection.setUserId(userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection = analysisCollectionRepository.save(collection);

        // 2. Add Items
        if (!CollectionUtils.isEmpty(request.getAnalysisIds())) {
            List<CollectionRelation> relations = new ArrayList<>();
            for (Long analysisId : request.getAnalysisIds()) {
                CollectionRelation relation = new CollectionRelation();
                relation.setCollectionId(collection.getId());
                relation.setAnalysisResultId(analysisId);
                relations.add(relation);
            }
            collectionRelationRepository.saveAll(relations);
        }
        return collection.getId();
    }

    public List<CollectionVo> listCollection(Long userId) {
        List<AnalysisCollection> collections = analysisCollectionRepository.findByUserIdOrderByCreatedTimeDesc(userId);

        if (CollectionUtils.isEmpty(collections)) {
            return new ArrayList<>();
        }

        // Batch query counts for all collections at once to avoid N+1 queries
        List<Long> collectionIds = collections.stream().map(AnalysisCollection::getId).toList();
        List<Object[]> countResults = collectionRelationRepository.countByCollectionIds(collectionIds);

        // Build count map: collectionId -> count
        Map<Long, Long> countMap = countResults.stream()
            .collect(Collectors.toMap(
                result -> (Long) result[0],
                result -> (Long) result[1]
            ));

        return collections.stream().map(c -> {
            CollectionVo vo = new CollectionVo();
            BeanUtils.copyProperties(c, vo);
            vo.setCount(countMap.getOrDefault(c.getId(), 0L));
            return vo;
        }).toList();
    }

    public CollectionDetailVo getCollectionDetail(Long userId, Long collectionId) {
        AnalysisCollection collection = analysisCollectionRepository.findByIdAndUserId(userId,collectionId);
        if (collection == null) {
            ExceptionUtils.throwDataNotFound("合集不存在");
        }

        CollectionDetailVo vo = new CollectionDetailVo();
        BeanUtils.copyProperties(collection, vo);
        vo.setCount(collectionRelationRepository.countByCollectionId(collectionId));

        List<CollectionRelation> relations = collectionRelationRepository.findByCollectionId(collectionId);
        if (!CollectionUtils.isEmpty(relations)) {
            List<Long> analysisIds = relations.stream().map(CollectionRelation::getAnalysisResultId).toList();
            List<AnalysisResult> results = analysisResultRepository.findAllById(analysisIds);
            vo.setAnalysisResults(results);
        } else {
            vo.setAnalysisResults(new ArrayList<>());
        }

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCollectionItems(Long userId, Long collectionId, CollectionItemRequest request) {
        AnalysisCollection collection = analysisCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        if (CollectionUtils.isEmpty(request.getAnalysisIds())) {
            return;
        }

        if ("ADD".equalsIgnoreCase(request.getAction())) {
            List<CollectionRelation> newRelations = new ArrayList<>();
            for (Long analysisId : request.getAnalysisIds()) {
                // Check existence to avoid unique constraint violation
                if (collectionRelationRepository.existsByCollectionIdAndAnalysisResultId(collectionId, analysisId)) {
                    continue;
                }
                
                CollectionRelation relation = new CollectionRelation();
                relation.setCollectionId(collectionId);
                relation.setAnalysisResultId(analysisId);
                collectionRelationRepository.save(relation);
            }
        } else if ("REMOVE".equalsIgnoreCase(request.getAction())) {
            collectionRelationRepository.deleteByCollectionIdAndAnalysisIdIn(collectionId, request.getAnalysisIds());
        }
    }

    public List<CollectionVo> checkContain(Long userId, Long analysisId) {
        List<Long> collectionIds = collectionRelationRepository.findCollectionIdsByAnalysisResultId(analysisId);
        if (CollectionUtils.isEmpty(collectionIds)) {
            return new ArrayList<>();
        }
        
        List<AnalysisCollection> collections = analysisCollectionRepository.findAllById(collectionIds);

        return collections.stream()
                .filter(c -> c.getUserId().equals(userId))
                .map(c -> {
                    CollectionVo vo = new CollectionVo();
                    BeanUtils.copyProperties(c, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
