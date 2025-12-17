package com.review.agent.repository;

import com.review.agent.entity.pojo.ReportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReportDataRepository extends JpaRepository<ReportData, Long> {

    @Query(nativeQuery = true,
            value = """
                        select * from report_data rd
                        where rd.user_id = :userId
                        and rd.type = :type
                        and (:date is null or :date = '' or rd.start_date = str_to_date(:date, '%Y-%m-%d'))
                        order by rd.start_date desc
                        limit 10
                    """)
    List<ReportData> findByCondition(Long userId, Integer type, String date);
}