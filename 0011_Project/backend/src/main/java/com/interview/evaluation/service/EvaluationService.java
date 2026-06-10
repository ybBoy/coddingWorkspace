package com.interview.evaluation.service;

import com.interview.evaluation.model.DimensionScore;
import com.interview.evaluation.model.EvaluationForm;
import com.interview.evaluation.model.EvaluationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EvaluationService {

    @Value("${app.version.max-count:3}")
    private int maxVersionCount;

    private final Map<String, EvaluationForm> formStore = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<EvaluationVersion>> versionStore = new ConcurrentHashMap<>();
    private int versionCounter = 0;

    private static final List<String> DEFAULT_DIMENSIONS = Arrays.asList(
            "沟通能力", "专业技能", "项目经验", "学习能力", "文化契合"
    );

    public EvaluationForm getOrCreateForm(String formId) {
        return formStore.computeIfAbsent(formId, id -> {
            List<DimensionScore> scores = new ArrayList<>();
            for (String dim : DEFAULT_DIMENSIONS) {
                scores.add(new DimensionScore(dim, 3, ""));
            }
            EvaluationForm form = new EvaluationForm();
            form.setFormId(id);
            form.setScores(scores);
            form.setUpdatedAt(System.currentTimeMillis());
            form.setUpdatedBy("system");
            return form;
        });
    }

    public EvaluationForm updateScore(String formId, DimensionScore score, String userName) {
        EvaluationForm form = getOrCreateForm(formId);
        for (DimensionScore s : form.getScores()) {
            if (s.getDimension().equals(score.getDimension())) {
                s.setScore(score.getScore());
                s.setComment(score.getComment());
                break;
            }
        }
        form.setUpdatedAt(System.currentTimeMillis());
        form.setUpdatedBy(userName);
        return form;
    }

    public EvaluationVersion saveVersion(String formId, String userName) {
        EvaluationForm form = getOrCreateForm(formId);
        List<DimensionScore> snapshot = new ArrayList<>();
        for (DimensionScore s : form.getScores()) {
            snapshot.add(new DimensionScore(s.getDimension(), s.getScore(), s.getComment()));
        }
        EvaluationVersion version = new EvaluationVersion();
        version.setVersionId(++versionCounter);
        version.setScores(snapshot);
        version.setCreatedAt(System.currentTimeMillis());
        version.setCreatedBy(userName);
        LinkedList<EvaluationVersion> versions = versionStore.computeIfAbsent(formId, k -> new LinkedList<>());
        versions.addFirst(version);
        while (versions.size() > maxVersionCount) {
            versions.removeLast();
        }
        return version;
    }

    public List<EvaluationVersion> getVersions(String formId) {
        getOrCreateForm(formId);
        return versionStore.getOrDefault(formId, new LinkedList<>());
    }

    public EvaluationForm rollbackToVersion(String formId, Integer versionId, String userName) {
        LinkedList<EvaluationVersion> versions = versionStore.get(formId);
        if (versions == null) {
            return getOrCreateForm(formId);
        }
        EvaluationVersion target = null;
        for (EvaluationVersion v : versions) {
            if (v.getVersionId().equals(versionId)) {
                target = v;
                break;
            }
        }
        if (target == null) {
            return getOrCreateForm(formId);
        }
        EvaluationForm form = getOrCreateForm(formId);
        List<DimensionScore> restored = new ArrayList<>();
        for (DimensionScore s : target.getScores()) {
            restored.add(new DimensionScore(s.getDimension(), s.getScore(), s.getComment()));
        }
        form.setScores(restored);
        form.setUpdatedAt(System.currentTimeMillis());
        form.setUpdatedBy(userName);
        return form;
    }
}
