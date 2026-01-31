package com.review.agent.repository;

import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.projection.DataInfoVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface DataInfoRepository extends JpaRepository<DataInfo, Long> {
    @Query(nativeQuery = true, value = """
                         select d.id, d.user_id as userId, d.file_name as fileName, d.file_content as fileContent,
                         d.processed_status as processedStatus, d.created_time as createdTime, d.source as source, COUNT(ar.id) as sessionCount
                         from data_info d
                         left join analysis_result as ar on d.id = ar.file_id
                         where (d.user_id = :userId or :userId is null)
                                 and (d.file_name like concat('%', :fileName, '%') or :fileName is null)
                                 and (d.processed_status = :processedStatus or :processedStatus is null)
                                 and (d.source = :source or :source is null)
                                 and (d.created_time  between :startTime and :endTime or (:startTime is null and :endTime is null))
                                 and d.deleted = 0
                         GROUP BY
                                 d.id,
                                 d.user_id,
                                 d.file_name,
                                 d.file_content,
                                 d.processed_status,
                                 d.created_time,
                                 d.update_time,
                                 d.source;
             """)
    Page<DataInfoVo> findByPage(Pageable pageable, Long userId, String fileName, Integer processedStatus, Date startTime, Date endTime, Integer source);

    @Query("select d from DataInfo d where d.userId = :userId and d.deleted = 0")
    List<DataInfo> findByUserId(Long userId);

    @Query("select d from DataInfo d where d.userId = :userId and d.fileName = :fileName and d.deleted = 0")
    DataInfo findByUserIdAndFileName(Long userId, String fileName);

    /**
     * 统计用户的文件数量（只统计未删除的）
     * @param userId 用户ID
     * @return 文件数量
     */
    @Query("select count(d) from DataInfo d where d.userId = :userId and d.deleted = 0")
    long countByUserId(Long userId);

    /**
     * 软删除（设置 deleted = true 和 deleted_at = 当前时间）
     * @param id 数据ID
     * @param deletedAt 删除时间
     */
    @Modifying
    @Query("update DataInfo d set d.deleted = 1, d.deletedAt = :deletedAt where d.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") Date deletedAt);

    /**
     * 物理删除（直接删除记录，慎用）
     * @param id 数据ID
     */
    @Modifying
    @Query("delete from DataInfo d where d.id = :id")
    void hardDelete(@Param("id") Long id);
}
