package com.warehouse.store;

import com.warehouse.entity.Part;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PartDataStore {

    private Map<String, Part> partsMap = new HashMap<>();

    public void savePart(Part part) {
        partsMap.put(part.getId(), part);
    }

    public Part getPartById(String id) {
        return partsMap.get(id);
    }

    public List<Part> getAllParts() {
        return new ArrayList<>(partsMap.values());
    }

    public void deletePart(String id) {
        partsMap.remove(id);
    }

    public boolean existsById(String id) {
        return partsMap.containsKey(id);
    }

    public List<Part> searchParts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllParts();
        }
        String lowerKeyword = keyword.toLowerCase().trim();
        return partsMap.values().stream()
                .filter(part -> 
                    part.getId().toLowerCase().contains(lowerKeyword) ||
                    part.getName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    public List<Part> searchPartsByKeywordAndCategory(String keyword, String category) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty();
        
        if (!hasKeyword && !hasCategory) {
            return getAllParts();
        }
        
        String lowerKeyword = hasKeyword ? keyword.toLowerCase().trim() : null;
        
        return partsMap.values().stream()
                .filter(part -> {
                    boolean matchKeyword = true;
                    boolean matchCategory = true;
                    
                    if (hasKeyword) {
                        matchKeyword = part.getId().toLowerCase().contains(lowerKeyword) ||
                                       part.getName().toLowerCase().contains(lowerKeyword);
                    }
                    
                    if (hasCategory) {
                        matchCategory = category.equals(part.getCategory());
                    }
                    
                    return matchKeyword && matchCategory;
                })
                .collect(Collectors.toList());
    }

    public List<Part> getPartsNeedRestock() {
        return partsMap.values().stream()
                .filter(Part::needsRestock)
                .collect(Collectors.toList());
    }

    public void setPartsMap(Map<String, Part> partsMap) {
        this.partsMap = partsMap;
    }

    public Map<String, Part> getPartsMap() {
        return new HashMap<>(partsMap);
    }
}
