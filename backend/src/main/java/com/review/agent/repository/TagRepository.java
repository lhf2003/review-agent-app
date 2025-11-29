package com.review.agent.repository;

import com.review.agent.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    boolean existsByName(String name);
}
