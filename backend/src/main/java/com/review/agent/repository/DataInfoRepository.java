package com.review.agent.repository;

import com.review.agent.entity.DataInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface DataInfoRepository extends JpaRepository<DataInfo, Long> {
    @Query(nativeQuery = true, value = """
                        select * from data_info 
                        where (user_id = :userId or :userId is null) 
                        and (processed_status = :processedStatus or :processedStatus is null)
                        and (created_time between :startTime and :endTime or (:startTime is null and :endTime is null))
            """)
    Page<DataInfo> findByPage(Pageable pageable, Long userId, Integer processedStatus, Date startTime, Date endTime);

    @Query("select d from DataInfo d where d.userId = :userId")
    List<DataInfo> findByUserId(Long userId);

    @Query("select d from DataInfo d where d.filePath = :filePath")
    DataInfo findByFilePath(String filePath);

    boolean existsByFilePath(String filePath);
}
