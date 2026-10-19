package com.privacy.repository;

import com.privacy.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByUserIdOrderByReportedAtDesc(Long userId);
    Optional<IncidentReport> findByIdAndUserId(Long id, Long userId);
    List<IncidentReport> findByStatusOrderByReportedAtDesc(IncidentReport.ReportStatus status);
    List<IncidentReport> findAllByOrderByReportedAtDesc();
}
