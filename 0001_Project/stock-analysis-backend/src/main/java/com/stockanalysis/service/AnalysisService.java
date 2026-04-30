package com.stockanalysis.service;

import com.stockanalysis.dto.AnalysisParams;
import com.stockanalysis.dto.AnalysisResult;
import com.stockanalysis.dto.PositionResult;
import com.stockanalysis.dto.SellResult;
import com.stockanalysis.entity.TradeData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    public AnalysisResult analyze(List<TradeData> tradeDataList, AnalysisParams params) {
        AnalysisResult result = new AnalysisResult();
        result.setParams(params);

        if (tradeDataList == null || tradeDataList.isEmpty()) {
            result.setPositionResults(Collections.emptyList());
            result.setSellResults(Collections.emptyList());
            return result;
        }

        List<PositionResult> positionResults = filterPositionData(tradeDataList, params);
        result.setPositionResults(positionResults);

        List<SellResult> sellResults = filterSellData(tradeDataList, positionResults, params);
        result.setSellResults(sellResults);

        return result;
    }

    private List<PositionResult> filterPositionData(List<TradeData> tradeDataList, AnalysisParams params) {
        double priceThreshold = getPriceThreshold(tradeDataList, params.getPricePercentile());
        LocalDateTime timeThreshold = getTimeThreshold(tradeDataList, params.getTimePercentile());

        return tradeDataList.stream()
                .filter(data -> data.getTradePrice() <= priceThreshold)
                .filter(data -> !data.getTradeTime().isAfter(timeThreshold))
                .map(this::toPositionResult)
                .sorted(Comparator.comparing(PositionResult::getPositionTime))
                .collect(Collectors.toList());
    }

    private List<SellResult> filterSellData(List<TradeData> tradeDataList, 
                                             List<PositionResult> positionResults,
                                             AnalysisParams params) {
        Map<String, List<PositionResult>> positionMap = positionResults.stream()
                .collect(Collectors.groupingBy(PositionResult::getTrader));

        List<SellResult> sellResults = new ArrayList<>();

        for (TradeData trade : tradeDataList) {
            String sellAccount = trade.getSellAccount();
            if (positionMap.containsKey(sellAccount)) {
                List<PositionResult> positions = positionMap.get(sellAccount);
                for (PositionResult position : positions) {
                    if (trade.getTradePrice() >= position.getPositionPrice() * params.getSellPriceMultiple()) {
                        SellResult sellResult = toSellResult(trade, position);
                        sellResults.add(sellResult);
                    }
                }
            }
        }

        return sellResults.stream()
                .sorted(Comparator.comparing(SellResult::getSellTime))
                .collect(Collectors.toList());
    }

    private double getPriceThreshold(List<TradeData> tradeDataList, int percentile) {
        List<Double> prices = tradeDataList.stream()
                .map(TradeData::getTradePrice)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (prices.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(prices.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, prices.size() - 1));
        return prices.get(index);
    }

    private LocalDateTime getTimeThreshold(List<TradeData> tradeDataList, int percentile) {
        List<LocalDateTime> times = tradeDataList.stream()
                .map(TradeData::getTradeTime)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (times.isEmpty()) {
            return LocalDateTime.now();
        }

        int index = (int) Math.ceil(times.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, times.size() - 1));
        return times.get(index);
    }

    private PositionResult toPositionResult(TradeData data) {
        PositionResult result = new PositionResult();
        result.setTrader(data.getBuyAccount());
        result.setPositionTime(data.getTradeTime());
        result.setPositionPrice(data.getTradePrice());
        result.setPositionQuantity(data.getTradeQuantity());
        result.setBuyAccount(data.getBuyAccount());
        return result;
    }

    private SellResult toSellResult(TradeData data, PositionResult position) {
        SellResult result = new SellResult();
        result.setSeller(data.getSellAccount());
        result.setSellTime(data.getTradeTime());
        result.setSellPrice(data.getTradePrice());
        result.setSellQuantity(data.getTradeQuantity());
        result.setSellAccount(data.getSellAccount());
        result.setPositionPrice(position.getPositionPrice());
        
        double profit = (data.getTradePrice() - position.getPositionPrice()) * data.getTradeQuantity();
        double profitRate = (data.getTradePrice() - position.getPositionPrice()) / position.getPositionPrice() * 100;
        
        result.setProfit(Math.round(profit * 100.0) / 100.0);
        result.setProfitRate(Math.round(profitRate * 100.0) / 100.0);
        
        return result;
    }
}
