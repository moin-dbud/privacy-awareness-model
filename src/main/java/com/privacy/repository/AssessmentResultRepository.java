package com.privacy.repository;

import com.privacy.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {
    List<AssessmentResult> findByUserIdOrderByTakenAtDesc(Long userId);
    List<AssessmentResult> findByUserIdOrderByTakenAtAsc(Long userId);
    Optional<AssessmentResult> findTopByUserIdOrderByTakenAtDesc(Long userId);

    @Query("SELECT COUNT(a) FROM AssessmentResult a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}