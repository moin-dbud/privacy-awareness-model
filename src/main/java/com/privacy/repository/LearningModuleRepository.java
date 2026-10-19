package com.privacy.repository;

import com.privacy.entity.LearningModule;
import com.privacy.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningModuleRepository extends JpaRepository<LearningModule, Long> {
    List<LearningModule> findByActiveTrueOrderByDisplayOrderAsc();
    List<LearningModule> findByCategoryAndActiveTrueOrderByDisplayOrderAsc(QuizQuestion.Category category);
}
