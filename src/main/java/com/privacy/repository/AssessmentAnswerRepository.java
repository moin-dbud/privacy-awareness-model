package com.privacy.repository;

import com.privacy.entity.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {
    boolean existsByQuestionId(Long questionId);
    List<AssessmentAnswer> findByQuestionId(Long questionId);
}
