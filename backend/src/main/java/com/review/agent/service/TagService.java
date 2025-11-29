package com.review.agent.service;

import com.review.agent.repository.TagRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.entity.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TagService {
    @Resource
    private TagRepository tagRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    public java.util.List<Tag> findAll() {
        return tagRepository.findAll();
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
    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("tag id required");
        }
        long refCount = analysisTagRepository.countByTagId(id);
        if (refCount > 0) {
            throw new IllegalStateException("tag in use");
        }
        tagRepository.deleteById(id);
    }
}
