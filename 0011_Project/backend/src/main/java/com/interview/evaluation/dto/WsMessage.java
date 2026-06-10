package com.interview.evaluation.dto;

import com.interview.evaluation.enums.Role;
import com.interview.evaluation.model.DimensionScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private String type;
    private String formId;
    private String userId;
    private String userName;
    private Role role;
    private DimensionScore score;
    private List<DimensionScore> scores;
    private Integer versionId;
    private Long timestamp;
}
