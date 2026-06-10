package com.interview.evaluation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationVersion {
    private Integer versionId;
    private List<DimensionScore> scores;
    private Long createdAt;
    private String createdBy;
}
