package com.privacy.dto;

import com.privacy.entity.LearningModule;
import lombok.Data;

@Data
public class LearningModuleResponse {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String content;
    private Integer estimatedMinutes;
    private Integer displayOrder;
    private boolean completed; // user-specific, populated separately

    public static LearningModuleResponse from(LearningModule m) {
        LearningModuleResponse r = new LearningModuleResponse();
        r.setId(m.getId());
        r.setTitle(m.getTitle());
        r.setCategory(m.getCategory().name());
        r.setDescription(m.getDescription());
        r.setContent(m.getContent());
        r.setEstimatedMinutes(m.getEstimatedMinutes());
        r.setDisplayOrder(m.getDisplayOrder());
        return r;
    }
}
