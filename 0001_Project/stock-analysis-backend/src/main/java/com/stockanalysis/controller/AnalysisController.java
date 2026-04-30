package com.stockanalysis.controller;

import com.stockanalysis.dto.AnalysisParams;
import com.stockanalysis.dto.AnalysisResult;
import com.stockanalysis.entity.TradeData;
import com.stockanalysis.service.AnalysisService;
import com.stockanalysis.service.FileParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired
    private FileParserService fileParserService;

    @Autowired
    private AnalysisService analysisService;

    private List<TradeData> cachedTradeData;
    private AnalysisResult cachedResult;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "文件为空");
                return ResponseEntity.badRequest().body(response);
            }

            cachedTradeData = fileParserService.parseFile(file);
            
            response.put("success", true);
            response.put("message", "文件上传成功，共解析 " + cachedTradeData.size() + " 条数据");
            response.put("dataCount", cachedTradeData.size());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "文件解析错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody(required = false) AnalysisParams params) {
        Map<String, Object> response = new HashMap<>();
        
        if (cachedTradeData == null || cachedTradeData.isEmpty()) {
            response.put("success", false);
            response.put("message", "请先上传文件");
            return ResponseEntity.badRequest().body(response);
        }

        if (params == null) {
            params = new AnalysisParams();
        }

        cachedResult = analysisService.analyze(cachedTradeData, params);
        
        response.put("success", true);
        response.put("message", "分析完成");
        response.put("result", cachedResult);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/default-params")
    public ResponseEntity<AnalysisParams> getDefaultParams() {
        return ResponseEntity.ok(new AnalysisParams());
    }

    @GetMapping("/cached-result")
    public ResponseEntity<Map<String, Object>> getCachedResult() {
        Map<String, Object> response = new HashMap<>();
        if (cachedResult == null) {
            response.put("success", false);
            response.put("message", "暂无分析结果");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", true);
        response.put("result", cachedResult);
        return ResponseEntity.ok(response);
    }
}
