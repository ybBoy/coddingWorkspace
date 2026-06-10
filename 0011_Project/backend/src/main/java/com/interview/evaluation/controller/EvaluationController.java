package com.interview.evaluation.controller;

import com.interview.evaluation.dto.WsMessage;
import com.interview.evaluation.model.DimensionScore;
import com.interview.evaluation.model.EvaluationForm;
import com.interview.evaluation.model.EvaluationVersion;
import com.interview.evaluation.service.EvaluationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Resource
    private EvaluationService evaluationService;

    @GetMapping("/{formId}")
    public EvaluationForm getForm(@PathVariable String formId) {
        return evaluationService.getOrCreateForm(formId);
    }

    @PostMapping("/{formId}/score")
    public EvaluationForm updateScore(@PathVariable String formId,
                                      @RequestBody DimensionScore score,
                                      @RequestParam(defaultValue = "anonymous") String userName) {
        return evaluationService.updateScore(formId, score, userName);
    }

    @PostMapping("/{formId}/commit")
    public EvaluationVersion commitVersion(@PathVariable String formId,
                                           @RequestParam(defaultValue = "anonymous") String userName) {
        return evaluationService.saveVersion(formId, userName);
    }

    @GetMapping("/{formId}/versions")
    public List<EvaluationVersion> getVersions(@PathVariable String formId) {
        return evaluationService.getVersions(formId);
    }

    @PostMapping("/{formId}/rollback/{versionId}")
    public EvaluationForm rollback(@PathVariable String formId,
                                   @PathVariable Integer versionId,
                                   @RequestParam(defaultValue = "anonymous") String userName) {
        return evaluationService.rollbackToVersion(formId, versionId, userName);
    }
}
