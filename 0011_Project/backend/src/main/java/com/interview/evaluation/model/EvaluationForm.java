package com.interview.evaluation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationForm {
    private String formId;
    private List<DimensionScore> scores;
    private Long updatedAt;
    private String updatedBy;
}
