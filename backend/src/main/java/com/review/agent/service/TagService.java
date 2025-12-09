package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.MainTag;
import com.review.agent.entity.SubTag;
import com.review.agent.entity.TagRelation;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.repository.MainTagRepository;
import com.review.agent.repository.SubTagRepository;
import com.review.agent.repository.TagRelationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TagService {
    @Resource
    private MainTagRepository mainTagRepository;

    @Resource
    private SubTagRepository subTagRepository;

    @Resource
    private TagRelationRepository tagRelationRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    // region 主标签

    /**
     * 查询用户所有主标签（不包含子标签）
     * @param userId 用户id
     * @return 主标签列表
     */
    public List<MainTag> findMainTagList(Long userId) {
        return mainTagRepository.findAllByUserId(userId);
    }

    /**
     * 添加主标签
     * @param mainTag 标签
     */
    @Transactional
    public void addTag(MainTag mainTag) {
        if (mainTag.getName() == null || mainTag.getName().isEmpty()) {
            throw new IllegalArgumentException("mainTag name required");
        }
        if (mainTagRepository.existsByName(mainTag.getName())) {
            throw new IllegalArgumentException("mainTag name exists");
        }
        mainTag.setId(null);
        Date date = new Date();
        mainTag.setCreateTime(date);
        mainTag.setUpdateTime(date);
        mainTagRepository.save(mainTag);
    }

    @Transactional
    public void updateMainTag(MainTag mainTag) {
        MainTag mainTagInDb = mainTagRepository.findById(mainTag.getId()).orElse(null);
        if (mainTagInDb == null) {
            throw new IllegalArgumentException("mainTag not found");
        }
        BeanUtils.copyProperties(mainTag, mainTagInDb, ObjectTransformUtil.getNullPropertyNames(mainTag));
        mainTagRepository.save(mainTagInDb);
    }

    @Transactional
    public void deleteMainTag(Long userId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("tag id required");
        }
        long refCount = analysisTagRepository.countByTagId(id);
        // TODO 后续支持删除，但是会删除相关联的所有分析记录
        if (refCount > 0) {
            ExceptionUtils.throwDataInUse("主标签");
        }
        // 删除主标签
        mainTagRepository.deleteById(id);

        // 删除标签关联关系
        List<TagRelation> tagRelationList = tagRelationRepository.findByUserId(userId);
        tagRelationRepository.deleteAll(tagRelationList);
    }

    public List<MainTag> findByIdList(List<Long> tagIdList) {
        return mainTagRepository.findAllById(tagIdList);
    }


    // endregion 主标签
    // region 子标签

    /**
     * 查询用户所有子标签（不包含主标签）
     * @param userId 用户id
     * @return 子标签列表
     */
    public List<SubTag> findSubTagList(Long userId) {
        return subTagRepository.findAllByUserId(userId);
    }

    /**
     * 查询用户某个主标签下的子标签
     * @param userId 用户id
     * @param mainTagId 主标签id
     * @return 子标签列表
     */
    public List<SubTag> findSubTagListByMainTagId(Long userId, Long mainTagId) {
        List<TagRelation> tagRelationList = tagRelationRepository.findAllByMainTagIdAndUserId(userId, mainTagId);
        List<Long> subTagIdList = tagRelationList.stream().map(TagRelation::getSubTagId).toList();
        return subTagRepository.findAllById(subTagIdList);
    }

    /**
     * 添加子标签
     * @param subTag 标签
     */
    public void addSubTag(SubTag subTag) {
        if (subTagRepository.existsByName(subTag.getName())) {
            throw new IllegalArgumentException("subTag name exists");
        }
        subTag.setId(null);
        Date date = new Date();
        subTag.setCreateTime(date);
        subTag.setUpdateTime(date);
        subTagRepository.save(subTag);
    }

    /**
     * 更新子标签
     * @param subTag 子标签
     */
    @Transactional
    public void updateSubTag(SubTag subTag) {
        SubTag subTagInDb = subTagRepository.findById(subTag.getId()).orElse(null);
        if (subTagInDb == null) {
            ExceptionUtils.throwDataNotFound("子标签" + subTag.getId() + "不存在");
        }
        BeanUtils.copyProperties(subTag, subTagInDb, ObjectTransformUtil.getNullPropertyNames(subTag));
        subTagRepository.save(subTagInDb);
    }

    /**
     * 删除子标签
     * @param userId 用户id
     * @param id 子标签id
     */
    @Transactional
    public void deleteSubTag(Long userId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("subTag id required");
        }
        // 检查子标签是否被使用
        long refCount = analysisTagRepository.countBySubTagId(id);
        if (refCount > 0) {
            ExceptionUtils.throwDataInUse("子标签");
        }
        subTagRepository.deleteById(id);

        // 删除标签关联关系
        List<TagRelation> tagRelationList = tagRelationRepository.findByUserId(userId);
        tagRelationRepository.deleteAll(tagRelationList);
    }


    // endregion 子标签
    // region 关联关系
//    /**
//     * 查询用户所有主标签和关联的子标签
//     * @param userId 用户id
//     * @return 主标签列表
//     */
//    public List<TagRelationVo> findMainTagVo(Long userId) {
//        List<TagRelationVo> resultList = new ArrayList<>();
//        List<TagRelation> tagRelationList = tagRelationRepository.findByUserId(userId);
//        List<MainTag> mainTagList = mainTagRepository.findAllByUserId(userId);
//        List<SubTag> subTagList = subTagRepository.findAllByUserId(userId);
//
//        Map<Long, String> mainTagIdToNameMap = mainTagList.stream().collect(Collectors.toMap(MainTag::getId, MainTag::getName));
//        Map<Long, List<TagRelation>> mainTagIdToTagRelationMap = tagRelationList.stream().collect(Collectors.groupingBy(TagRelation::getMainTagId));
//        for (Map.Entry<Long, List<TagRelation>> entry : mainTagIdToTagRelationMap.entrySet()) {
//            Long mainTagId = entry.getKey();
//            // 封装子标签列表
//            List<TagRelation> tagRelationListForMainTag = entry.getValue();
//            List<Long> subTagIdList = tagRelationListForMainTag.stream().map(TagRelation::getSubTagId).toList();
//            List<SubTag> subTagListForMainTag = subTagList.stream().filter(subTag -> subTagIdList.contains(subTag.getId())).toList();
//            // 封装返回数据
//            TagRelationVo tagRelationVo = new TagRelationVo();
//            tagRelationVo.setMainTagId(mainTagId);
//            tagRelationVo.setName(mainTagIdToNameMap.get(mainTagId));
//            tagRelationVo.setSubTagList(subTagListForMainTag);
//        }
//        return resultList;
//    }

    /**
     * 添加主标签关联子标签关系
     * @param tagRelation 主标签关联子标签关系
     */
    @Transactional
    public void addTagRelation(TagRelation tagRelation) {
        tagRelation.setId(null);
        tagRelationRepository.save(tagRelation);
    }

    /**
     * 删除主标签关联子标签关系
     */
    @Transactional
    public void deleteTagRelation(TagRelation tagRelation) {
        TagRelation tagRelationInDb = tagRelationRepository.findRelation(tagRelation.getUserId(), tagRelation.getMainTagId(), tagRelation.getSubTagId());
        if (tagRelationInDb == null) {
            ExceptionUtils.throwDataAlreadyExists(tagRelation.getMainTagId() + "-" + tagRelation.getSubTagId());
        }
        tagRelationRepository.delete(tagRelationInDb);
    }
}
