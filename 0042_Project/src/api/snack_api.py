"""
文件职责：
    API 层，定义零食相关的 HTTP 接口路由。
    接收前端 fetch 请求，调用 snack_service 完成业务逻辑，
    并将结果以 JSON 格式返回给前端。

数据流：
    前端 fetch 请求（snacks.js） → Flask 路由 → snack_api 函数 → snack_service 方法
                                                               ↓
    前端渲染界面 ← JSON 响应 ← snack_api 返回
"""

from flask import Blueprint, request, jsonify

from src.service.snack_service import SnackService


def _safe_int(value, default: int = 0) -> int:
    """安全解析整数，解析失败返回默认值"""
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


def create_snack_blueprint(service: SnackService) -> Blueprint:
    """
    创建零食 API 蓝图
    :param service: SnackService 实例
    :return: Flask Blueprint
    """
    bp = Blueprint("snack_api", __name__)

    @bp.route("/api/snacks", methods=["GET"])
    def get_snacks():
        """
        获取零食列表，支持按存放位置筛选
        Query 参数: location (可选)
        """
        location = request.args.get("location", "all")
        snacks = service.get_all(location)
        return jsonify({
            "success": True,
            "data": [s.to_dict() for s in snacks],
            "attention_count": service.count_needs_attention(),
        })

    @bp.route("/api/snacks/locations", methods=["GET"])
    def get_locations():
        """获取所有不重复的存放位置"""
        locations = service.get_locations()
        return jsonify({
            "success": True,
            "data": locations,
        })

    @bp.route("/api/snacks", methods=["POST"])
    def add_snack():
        """
        新增零食
        Body JSON: { name, flavor, quantity, location, expiry_date }
        """
        body = request.get_json(silent=True) or {}
        name = body.get("name", "").strip()
        flavor = body.get("flavor", "").strip()
        quantity = _safe_int(body.get("quantity"), 1)
        location = body.get("location", "").strip()
        expiry_date = body.get("expiry_date", "").strip()

        if not name:
            return jsonify({"success": False, "message": "名称不能为空"}), 400

        snack = service.add_snack(name, flavor, quantity, location, expiry_date)
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "attention_count": service.count_needs_attention(),
        }), 201

    @bp.route("/api/snacks/<snack_id>/consume", methods=["POST"])
    def consume_snack(snack_id: str):
        """吃掉一份，数量减 1"""
        snack = service.consume(snack_id)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "attention_count": service.count_needs_attention(),
        })

    @bp.route("/api/snacks/<snack_id>/restock", methods=["POST"])
    def restock_snack(snack_id: str):
        """
        补货，数量增加
        Body JSON: { amount } (可选，默认 1)
        """
        body = request.get_json(silent=True) or {}
        amount = _safe_int(body.get("amount"), 1)
        snack = service.restock(snack_id, amount)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "attention_count": service.count_needs_attention(),
        })

    @bp.route("/api/snacks/<snack_id>/toggle-disabled", methods=["POST"])
    def toggle_disabled(snack_id: str):
        """切换"不再购买"标记"""
        snack = service.toggle_disabled(snack_id)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "attention_count": service.count_needs_attention(),
        })

    @bp.route("/api/snacks/<snack_id>", methods=["DELETE"])
    def delete_snack(snack_id: str):
        """删除零食"""
        ok = service.delete_snack(snack_id)
        if not ok:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        return jsonify({
            "success": True,
            "attention_count": service.count_needs_attention(),
        })

    return bp
