"""
candle_routes.py - 蜡烛 API 路由层
文件职责：定义所有与蜡烛相关的 HTTP 接口（Flask Blueprint）。
负责请求参数解析、调用业务服务层、返回 JSON 响应。

接口调用流程：
  前端 fetch 请求 → Flask 路由 → candle_service 业务处理 → 返回 JSON 结果
"""

from flask import Blueprint, request, jsonify
from src.services.candle_service import CandleService


def create_candle_blueprint(service: CandleService) -> Blueprint:
    """
    创建蜡烛路由蓝图
    
    Args:
        service: CandleService 实例
        
    Returns:
        Flask Blueprint 对象
    """
    bp = Blueprint("candles", __name__)

    @bp.route("/api/candles", methods=["GET"])
    def list_candles():
        """
        获取蜡烛列表
        查询参数 scent 可选，用于按香型筛选
        
        流程：接收请求 → 调用 service.get_all() → 返回 JSON 列表
        """
        scent = request.args.get("scent", "")
        candles = service.get_all(scent)
        return jsonify([c.to_dict() for c in candles]), 200

    @bp.route("/api/candles/<candle_id>", methods=["GET"])
    def get_candle(candle_id: str):
        """
        获取单个蜡烛详情
        
        流程：接收 ID → 调用 service.get_by_id() → 返回 JSON 或 404
        """
        candle = service.get_by_id(candle_id)
        if not candle:
            return jsonify({"error": "蜡烛不存在"}), 404
        return jsonify(candle.to_dict()), 200

    @bp.route("/api/candles", methods=["POST"])
    def add_candle():
        """
        新增蜡烛
        请求体：{ name, scent, capacity, remaining_ratio?, purchase_date?, note? }
        
        流程：解析 JSON → 调用 service.add_candle() → 持久化 → 返回新对象
        """
        data = request.get_json() or {}
        name = data.get("name", "").strip()
        scent = data.get("scent", "").strip()
        capacity = data.get("capacity", 0)

        if not name or not scent or capacity <= 0:
            return jsonify({"error": "名称、香型和容量不能为空"}), 400

        candle = service.add_candle(
            name=name,
            scent=scent,
            capacity=float(capacity),
            remaining_ratio=data.get("remaining_ratio", 100),
            purchase_date=data.get("purchase_date", ""),
            note=data.get("note", ""),
        )
        return jsonify(candle.to_dict()), 201

    @bp.route("/api/candles/<candle_id>/light", methods=["PUT"])
    def light_candle(candle_id: str):
        """
        记录点燃一次，使用次数 +1
        
        流程：接收 ID → 调用 service.light_candle() → 持久化 → 返回更新后对象
        """
        candle = service.light_candle(candle_id)
        if not candle:
            return jsonify({"error": "蜡烛不存在"}), 404
        return jsonify(candle.to_dict()), 200

    @bp.route("/api/candles/<candle_id>/remaining", methods=["PUT"])
    def update_remaining(candle_id: str):
        """
        修改剩余比例
        请求体：{ remaining_ratio: 0-100 }
        
        流程：接收 ID 和新比例 → 调用 service.update_remaining() → 持久化 → 返回更新后对象
        """
        data = request.get_json() or {}
        remaining_ratio = data.get("remaining_ratio")

        if remaining_ratio is None:
            return jsonify({"error": "缺少 remaining_ratio 参数"}), 400

        candle = service.update_remaining(candle_id, int(remaining_ratio))
        if not candle:
            return jsonify({"error": "蜡烛不存在"}), 404
        return jsonify(candle.to_dict()), 200

    @bp.route("/api/candles/<candle_id>", methods=["DELETE"])
    def delete_candle(candle_id: str):
        """
        删除蜡烛
        
        流程：接收 ID → 调用 service.delete_candle() → 持久化 → 返回成功/失败
        """
        success = service.delete_candle(candle_id)
        if not success:
            return jsonify({"error": "蜡烛不存在"}), 404
        return jsonify({"message": "删除成功"}), 200

    @bp.route("/api/stats", methods=["GET"])
    def get_stats():
        """
        获取统计数据（总数、快用完的数量、筛选结果数量）
        查询参数 scent 可选
        
        流程：接收筛选参数 → 调用 service.get_stats() → 返回统计 JSON
        """
        scent = request.args.get("scent", "")
        stats = service.get_stats(scent)
        return jsonify(stats), 200

    return bp
