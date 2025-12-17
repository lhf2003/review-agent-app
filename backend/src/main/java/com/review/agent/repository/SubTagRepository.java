package com.review.agent.repository;

import com.review.agent.entity.pojo.SubTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubTagRepository extends JpaRepository<SubTag, Long> {

    /**
     * 根据名称查询子标签是否存在
     * @param name 子标签名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据用户ID查询标签列表
     * @param userId 用户ID
     * @return 标签列表
     */
    @Query("select t from SubTag t where t.userId = ?1")
    List<SubTag> findAllByUserId(Long userId);
}
