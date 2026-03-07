package com.review.agent.repository;

import com.review.agent.entity.pojo.UserParadigmMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户思维范式掌握度Repository
 */
@Repository
public interface UserParadigmMasteryRepository extends JpaRepository<UserParadigmMastery, Long> {

    /**
     * 根据用户ID和范式编码查询掌握度
     */
    Optional<UserParadigmMastery> findByUserIdAndParadigmCode(Long userId, String paradigmCode);

    /**
     * 根据用户ID查询掌握度列表，按掌握度降序排列
     */
    List<UserParadigmMastery> findByUserIdOrderByMasteryScoreDesc(Long userId);
}
