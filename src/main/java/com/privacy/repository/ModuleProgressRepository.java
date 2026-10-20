package com.privacy.repository;

import com.privacy.entity.ModuleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Long> {
    List<ModuleProgress> findByUserId(Long userId);
    Optional<ModuleProgress> findByUserIdAndModuleId(Long userId, Long moduleId);

    @Query("SELECT COUNT(mp) FROM ModuleProgress mp WHERE mp.user.id = :userId AND mp.completed = true")
    long countCompletedByUserId(@Param("userId") Long userId);

    boolean existsByModuleId(Long moduleId);
    long countByCompletedTrue();
    long countByModuleIdAndCompletedTrue(Long moduleId);
}
