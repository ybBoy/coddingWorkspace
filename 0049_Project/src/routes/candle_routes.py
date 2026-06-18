"""
candle_routes.py - 蜡烛 API 路由层
文件职责：定义所有与蜡烛相关的 HTTP 接口（Flask Blueprint）。
负责请求参数解析、调用业务服务层、返回 JSON 响应。

接口调用流程：
  前端 fetch 请求 → Flask 路由 → candle_service 业务处理 → 返回 JSON 结果

新增接口（v2）：
  - PUT /api/candles/:id/light - 点燃（支持可选 note 和 burn_hours）
  - GET /api/scents - 获取所有不重复香型
  - GET /api/candles/low - 获取快用完的蜡烛列表
  - GET /api/export - 导出数据
  - POST /api/import - 导入数据
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
        请求体：{ name, scent, capacity, remaining_ratio?, purchase_date?, note?, total_burn_hours? }

        流程：解析 JSON → 调用 service.add_candle() → 持久化 → 返回新对象
        """
        data = request.get_json() or {}
        name = data.get("name", "").strip()
        scent = data.get("scent", "").strip()

        if not name or not scent:
            return jsonify({"error": "名称和香型不能为空"}), 400

        try:
            capacity = float(data.get("capacity", 0))
        except (TypeError, ValueError):
            return jsonify({"error": "容量必须为数字"}), 400

        if capacity <= 0:
            return jsonify({"error": "容量必须大于 0"}), 400

        try:
            remaining_ratio = int(data.get("remaining_ratio", 100))
        except (TypeError, ValueError):
            return jsonify({"error": "剩余比例必须为整数"}), 400

        try:
            total_burn_hours = float(data.get("total_burn_hours", 0))
        except (TypeError, ValueError):
            return jsonify({"error": "总燃烧小时数必须为数字"}), 400

        candle = service.add_candle(
            name=name,
            scent=scent,
            capacity=capacity,
            remaining_ratio=remaining_ratio,
            purchase_date=data.get("purchase_date", ""),
            note=data.get("note", ""),
            total_burn_hours=total_burn_hours,
        )
        return jsonify(candle.to_dict()), 201

    @bp.route("/api/candles/<candle_id>/light", methods=["PUT"])
    def light_candle(candle_id: str):
        """
        记录点燃一次，使用次数 +1，追加使用历史记录
        请求体：{ note?, burn_hours? }
        - note: 本次点燃备注（可选）
        - burn_hours: 本次燃烧时长（小时，可选），如果蜡烛设置了 total_burn_hours 会自动估算剩余比例

        流程：接收 ID + 可选参数 → 调用 service.light_candle() → 持久化 → 返回更新后对象
        """
        data = request.get_json() or {}
        note = data.get("note", "").strip()

        try:
            burn_hours = float(data.get("burn_hours", 0))
        except (TypeError, ValueError):
            return jsonify({"error": "燃烧时长必须为数字"}), 400

        if burn_hours < 0:
            return jsonify({"error": "燃烧时长不能为负数"}), 400

        candle = service.light_candle(candle_id, note=note, burn_hours=burn_hours)
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

        try:
            remaining_ratio = int(remaining_ratio)
        except (TypeError, ValueError):
            return jsonify({"error": "剩余比例必须为整数"}), 400

        candle = service.update_remaining(candle_id, remaining_ratio)
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

    @bp.route("/api/scents", methods=["GET"])
    def get_scents():
        """
        获取所有不重复的香型列表，用于前端渲染快速筛选标签

        流程：调用 service.get_all_scents() → 返回香型列表
        """
        scents = service.get_all_scents()
        return jsonify(scents), 200

    @bp.route("/api/low-candles", methods=["GET"])
    def get_low_candles():
        """
        获取快用完的蜡烛列表（剩余比例低于 15%），用于前端低库存提醒区

        流程：调用 service.get_low_candles() → 返回蜡烛列表
        """
        candles = service.get_low_candles()
        return jsonify([c.to_dict() for c in candles]), 200

    @bp.route("/api/export", methods=["GET"])
    def export_data():
        """
        导出所有蜡烛数据为 JSON，方便备份或迁移

        流程：调用 service.export_data() → 返回蜡烛列表 JSON
        """
        data = service.export_data()
        return jsonify(data), 200

    @bp.route("/api/import", methods=["POST"])
    def import_data():
        """
        导入蜡烛数据
        请求体：{ data: [...], merge?: boolean }
        - data: 蜡烛数组
        - merge: True 为合并模式（追加不覆盖），False 为覆盖模式（默认）

        流程：解析 JSON → 校验 → 调用 service.import_data() → 持久化 → 返回结果
        """
        body = request.get_json() or {}
        data = body.get("data")

        if data is None:
            return jsonify({"error": "缺少 data 字段"}), 400

        merge = bool(body.get("merge", False))

        try:
            result = service.import_data(data, merge=merge)
            return jsonify(result), 200
        except ValueError as e:
            return jsonify({"error": str(e)}), 400

    return bp
