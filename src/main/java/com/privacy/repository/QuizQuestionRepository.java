package com.privacy.repository;

import com.privacy.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByActiveTrue();
    List<QuizQuestion> findByCategoryAndActiveTrue(QuizQuestion.Category category);
}
