package com.review.agent.repository;

import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.projection.DataInfoVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface DataInfoRepository extends JpaRepository<DataInfo, Long> {
    @Query(nativeQuery = true, value = """
                        select d.id, d.user_id as userId, d.file_name as fileName, d.file_content as fileContent, 
                        d.processed_status as processedStatus, d.created_time as createdTime, COUNT(ar.id) as sessionCount
                        from data_info d
                        left join analysis_result as ar on d.id = ar.file_id
                        where (d.user_id = :userId or :userId is null) 
                                and (d.file_name like concat('%', :fileName, '%') or :fileName is null)
                                and (d.processed_status = :processedStatus or :processedStatus is null)
                                and (d.created_time  between :startTime and :endTime or (:startTime is null and :endTime is null))
                        GROUP BY
                                d.id,
                                d.user_id,
                                d.file_name,
                                d.file_content,
                                d.processed_status,
                                d.created_time,
                                d.update_time;
            """)
    Page<DataInfoVo> findByPage(Pageable pageable, Long userId, String fileName, Integer processedStatus, Date startTime, Date endTime);

    @Query("select d from DataInfo d where d.userId = :userId")
    List<DataInfo> findByUserId(Long userId);

    @Query("select d from DataInfo d where d.filePath = :filePath")
    DataInfo findByFilePath(String filePath);

    boolean existsByFilePath(String filePath);
}
