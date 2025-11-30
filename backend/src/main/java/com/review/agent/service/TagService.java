package com.review.agent.service;

import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.request.TagRequest;
import com.review.agent.repository.TagRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.entity.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TagService {
    @Resource
    private TagRepository tagRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    public List<Tag> findAllByUserId(Long userId) {
        return tagRepository.findAllByUserId(userId);
    }

    @Transactional
    public void add(Tag tag) {
        if (tag.getName() == null || tag.getName().isEmpty()) {
            throw new IllegalArgumentException("tag name required");
        }
        if (tagRepository.existsByName(tag.getName())) {
            throw new IllegalArgumentException("tag name exists");
        }
        tag.setId(null);
        tagRepository.save(tag);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("tag id required");
        }
        long refCount = analysisTagRepository.countByTagId(id);
        if (refCount > 0) {
            throw new IllegalStateException("tag in use");
        }
        tagRepository.deleteById(id);
    }

    public Page<Tag> page(Pageable pageable, TagRequest tagRequest) {
        return tagRepository.findAllByPage(pageable, tagRequest.getUserId(), tagRequest.getTagName());
    }

    public void update(Tag tag) {
        Tag tagInDb = tagRepository.findById(tag.getId()).orElse(null);
        if (tagInDb == null) {
            throw new IllegalArgumentException("tag not found");
        }
        BeanUtils.copyProperties(tag, tagInDb, ObjectTransformUtil.getNullPropertyNames(tag));
        tagRepository.save(tagInDb);
    }
}
