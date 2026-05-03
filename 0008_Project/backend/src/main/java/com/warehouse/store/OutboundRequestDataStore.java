package com.warehouse.store;

import com.warehouse.entity.OutboundRequest;
import com.warehouse.entity.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 出库申请数据存储类
 * 管理出库申请的内存存储和查询
 */
@Component
public class OutboundRequestDataStore {

    /**
     * 申请数据存储Map
     * key: 申请编号
     * value: 申请对象
     */
    private Map<String, OutboundRequest> requestsMap = new ConcurrentHashMap<>();

    /**
     * 保存申请
     * @param request 申请对象
     */
    public void saveRequest(OutboundRequest request) {
        requestsMap.put(request.getId(), request);
    }

    /**
     * 根据编号获取申请
     * @param id 申请编号
     * @return 申请对象，不存在则返回null
     */
    public OutboundRequest getRequestById(String id) {
        return requestsMap.get(id);
    }

    /**
     * 获取所有申请列表
     * 按创建时间倒序排列
     * @return 所有申请列表
     */
    public List<OutboundRequest> getAllRequests() {
        List<OutboundRequest> requests = new ArrayList<>(requestsMap.values());
        requests.sort((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()));
        return requests;
    }

    /**
     * 根据状态获取申请列表
     * @param status 状态描述（如："待审核"）
     * @return 匹配的申请列表
     */
    public List<OutboundRequest> getRequestsByStatus(String status) {
        return requestsMap.values().stream()
                .filter(r -> r.getStatus().equals(status))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 获取待审核的申请列表
     * @return 待审核的申请列表
     */
    public List<OutboundRequest> getPendingRequests() {
        return getRequestsByStatus(RequestStatus.PENDING.getDescription());
    }

    /**
     * 获取已审核的申请列表（通过+拒绝）
     * @return 已审核的申请列表
     */
    public List<OutboundRequest> getReviewedRequests() {
        return requestsMap.values().stream()
                .filter(r -> !r.getStatus().equals(RequestStatus.PENDING.getDescription()))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 根据申请人IP获取申请列表
     * @param applicantIp 申请人IP地址
     * @return 该申请人的所有申请列表
     */
    public List<OutboundRequest> getRequestsByApplicantIp(String applicantIp) {
        return requestsMap.values().stream()
                .filter(r -> applicantIp.equals(r.getApplicantIp()))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 根据申请人IP和状态获取申请列表
     * @param applicantIp 申请人IP
     * @param status 状态
     * @return 匹配的申请列表
     */
    public List<OutboundRequest> getRequestsByApplicantIpAndStatus(String applicantIp, String status) {
        return requestsMap.values().stream()
                .filter(r -> applicantIp.equals(r.getApplicantIp()) && r.getStatus().equals(status))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 删除申请
     * @param id 申请编号
     */
    public void deleteRequest(String id) {
        requestsMap.remove(id);
    }

    /**
     * 检查申请是否存在
     * @param id 申请编号
     * @return true 表示存在
     */
    public boolean existsById(String id) {
        return requestsMap.containsKey(id);
    }

    /**
     * 设置申请数据Map
     * 用于从文件加载数据后初始化
     * @param requestsMap 申请数据Map
     */
    public void setRequestsMap(Map<String, OutboundRequest> requestsMap) {
        this.requestsMap = requestsMap;
    }

    /**
     * 获取申请数据Map的副本
     * 用于持久化到文件
     * @return 申请数据Map的副本
     */
    public Map<String, OutboundRequest> getRequestsMap() {
        return new ConcurrentHashMap<>(requestsMap);
    }

    /**
     * 获取待审核申请数量
     * @return 待审核申请数量
     */
    public int getPendingCount() {
        return (int) requestsMap.values().stream()
                .filter(r -> RequestStatus.PENDING.getDescription().equals(r.getStatus()))
                .count();
    }
}
