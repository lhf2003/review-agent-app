package com.review.agent.repository;

import com.review.agent.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByName(String name);

    /**
     * 根据用户ID查询标签列表
     * @param userId 用户ID
     * @return 标签列表
     */
    @Query("select t from Tag t where t.userId = ?1")
    List<Tag> findAllByUserId(Long userId);

    @Query(nativeQuery = true, value = """
        select * from tag t where (:tagName is null or t.user_id = :userId)\s
        and (:tagName is null or :tagName = '' or t.name like concat('%', :tagName, '%'))
        and (:parentId is null or t.parent_id = :parentId)
       \s""")
    Page<Tag> findAllByPage(Pageable pageable, @Param("userId") Long userId, @Param("tagName") String tagName, @Param("parentId") Integer parentId);

}
