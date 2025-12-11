package com.review.agent.repository;

import com.review.agent.entity.ReportData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportDataRepository extends JpaRepository<ReportData, Long> {
}