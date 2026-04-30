package com.stockanalysis.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.stockanalysis.entity.TradeData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileParserService {

    public List<TradeData> parseFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String lowerName = originalFilename.toLowerCase();
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return parseExcel(file);
        } else if (lowerName.endsWith(".csv")) {
            return parseCsv(file);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .xlsx, .xls, .csv");
        }
    }

    private List<TradeData> parseExcel(MultipartFile file) throws IOException {
        List<TradeData> dataList = new ArrayList<>();
        EasyExcel.read(file.getInputStream(), TradeData.class, new ReadListener<TradeData>() {
            @Override
            public void invoke(TradeData tradeData, AnalysisContext context) {
                if (isValidTradeData(tradeData)) {
                    dataList.add(tradeData);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // Do nothing
            }
        }).sheet().doRead();
        return dataList;
    }

    private List<TradeData> parseCsv(MultipartFile file) throws IOException {
        List<TradeData> dataList = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            boolean isFirstLine = true;
            while ((line = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    if (isHeaderLine(line)) {
                        continue;
                    }
                }
                TradeData tradeData = parseCsvLine(line);
                if (isValidTradeData(tradeData)) {
                    dataList.add(tradeData);
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV文件解析错误: " + e.getMessage());
        }
        return dataList;
    }

    private boolean isHeaderLine(String[] line) {
        if (line.length < 5) return false;
        String firstCell = line[0].trim();
        return firstCell.contains("时间") || firstCell.contains("日期") || firstCell.contains("date") || firstCell.contains("time");
    }

    private TradeData parseCsvLine(String[] line) {
        TradeData data = new TradeData();
        if (line.length > 0) data.setTradeTimeStr(line[0].trim());
        if (line.length > 1 && !line[1].trim().isEmpty()) {
            try {
                data.setTradePrice(Double.parseDouble(line[1].trim()));
            } catch (NumberFormatException e) {
                data.setTradePrice(null);
            }
        }
        if (line.length > 2 && !line[2].trim().isEmpty()) {
            try {
                data.setTradeQuantity(Integer.parseInt(line[2].trim()));
            } catch (NumberFormatException e) {
                data.setTradeQuantity(null);
            }
        }
        if (line.length > 3) data.setBuyAccount(line[3].trim());
        if (line.length > 4) data.setSellAccount(line[4].trim());
        return data;
    }

    private boolean isValidTradeData(TradeData data) {
        return data.getTradeTime() != null
                && data.getTradePrice() != null
                && data.getTradeQuantity() != null
                && data.getBuyAccount() != null
                && !data.getBuyAccount().isEmpty()
                && data.getSellAccount() != null
                && !data.getSellAccount().isEmpty();
    }
}
