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

from flask import Blueprint, request, jsonify, make_response

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

    # ===== 查询类接口 =====

    @bp.route("/api/snacks", methods=["GET"])
    def get_snacks():
        """
        获取零食列表，支持按存放位置、分类筛选，或只看需要处理的
        Query 参数: location, category, only_attention
        """
        location = request.args.get("location", "all")
        category = request.args.get("category", "all")
        only_attention = request.args.get("only_attention", "0") == "1"
        snacks = service.get_all(location, category, only_attention)
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": [s.to_dict() for s in snacks],
            "statistics": stats,
        })

    @bp.route("/api/snacks/locations", methods=["GET"])
    def get_locations():
        """获取所有不重复的存放位置"""
        locations = service.get_locations()
        return jsonify({
            "success": True,
            "data": locations,
        })

    @bp.route("/api/snacks/categories", methods=["GET"])
    def get_categories():
        """获取所有不重复的分类"""
        categories = service.get_categories()
        return jsonify({
            "success": True,
            "data": categories,
        })

    @bp.route("/api/snacks/statistics", methods=["GET"])
    def get_statistics():
        """获取详细统计信息"""
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": stats,
        })

    @bp.route("/api/snacks/<snack_id>", methods=["GET"])
    def get_snack_detail(snack_id: str):
        """获取单个零食详情"""
        snack = service.get_by_id(snack_id)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
        })

    # ===== 操作记录接口 =====

    @bp.route("/api/logs", methods=["GET"])
    def get_logs():
        """获取操作记录，可传 limit 参数"""
        limit = _safe_int(request.args.get("limit", "20"), 20)
        limit = max(1, min(limit, 100))
        logs = service.get_logs(limit)
        return jsonify({
            "success": True,
            "data": [log.to_dict() for log in logs],
        })

    # ===== 新增接口 =====

    @bp.route("/api/snacks", methods=["POST"])
    def add_snack():
        """
        新增零食
        Body JSON: { name, flavor, quantity, location, expiry_date, category }
        """
        body = request.get_json(silent=True) or {}
        name = body.get("name", "").strip()
        flavor = body.get("flavor", "").strip()
        quantity = _safe_int(body.get("quantity"), 1)
        location = body.get("location", "").strip()
        expiry_date = body.get("expiry_date", "").strip()
        category = body.get("category", "").strip()

        if not name:
            return jsonify({"success": False, "message": "名称不能为空"}), 400

        snack = service.add_snack(name, flavor, quantity, location, expiry_date, category)
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "statistics": stats,
        }), 201

    # ===== 更新接口 =====

    @bp.route("/api/snacks/<snack_id>", methods=["PUT"])
    def update_snack(snack_id: str):
        """
        更新零食信息
        Body JSON: { name?, flavor?, quantity?, location?, expiry_date?, category?, disabled? }
        """
        body = request.get_json(silent=True) or {}
        updates = {}

        if "name" in body:
            updates["name"] = str(body.get("name", "")).strip()
            if not updates["name"]:
                return jsonify({"success": False, "message": "名称不能为空"}), 400
        if "flavor" in body:
            updates["flavor"] = str(body.get("flavor", "")).strip()
        if "quantity" in body:
            updates["quantity"] = _safe_int(body.get("quantity"), 0)
        if "location" in body:
            updates["location"] = str(body.get("location", "")).strip()
        if "expiry_date" in body:
            updates["expiry_date"] = str(body.get("expiry_date", "")).strip()
        if "category" in body:
            updates["category"] = str(body.get("category", "")).strip()
        if "disabled" in body:
            updates["disabled"] = bool(body.get("disabled", False))

        snack = service.update_snack(snack_id, **updates)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404

        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "statistics": stats,
        })

    @bp.route("/api/snacks/<snack_id>/consume", methods=["POST"])
    def consume_snack(snack_id: str):
        """吃掉一份，数量减 1"""
        snack = service.consume(snack_id)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "statistics": stats,
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
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "statistics": stats,
        })

    @bp.route("/api/snacks/<snack_id>/toggle-disabled", methods=["POST"])
    def toggle_disabled(snack_id: str):
        """切换"不再购买"标记"""
        snack = service.toggle_disabled(snack_id)
        if snack is None:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "data": snack.to_dict(),
            "statistics": stats,
        })

    # ===== 删除接口 =====

    @bp.route("/api/snacks/<snack_id>", methods=["DELETE"])
    def delete_snack(snack_id: str):
        """删除零食"""
        ok = service.delete_snack(snack_id)
        if not ok:
            return jsonify({"success": False, "message": "零食不存在"}), 404
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "statistics": stats,
        })

    # ===== 导入导出接口 =====

    @bp.route("/api/export/json", methods=["GET"])
    def export_json():
        """导出 JSON 文件"""
        data = service.export_json()
        resp = make_response(data)
        resp.headers["Content-Type"] = "application/json; charset=utf-8"
        resp.headers["Content-Disposition"] = "attachment; filename=snacks.json"
        return resp

    @bp.route("/api/export/csv", methods=["GET"])
    def export_csv():
        """导出 CSV 文件"""
        data = service.export_csv()
        resp = make_response(data)
        resp.headers["Content-Type"] = "text/csv; charset=utf-8"
        resp.headers["Content-Disposition"] = "attachment; filename=snacks.csv"
        resp.data = "\ufeff" + data
        return resp

    @bp.route("/api/import/json", methods=["POST"])
    def import_json():
        """
        导入 JSON 数据
        Body: 直接是 JSON 数组，或 multipart 上传文件
        """
        json_str = None

        if request.is_json:
            data = request.get_json(silent=True)
            if data is not None:
                import json
                json_str = json.dumps(data, ensure_ascii=False)

        if json_str is None and "file" in request.files:
            f = request.files["file"]
            if f.filename:
                json_str = f.read().decode("utf-8")

        if json_str is None:
            raw = request.get_data(as_text=True)
            if raw:
                json_str = raw

        if not json_str:
            return jsonify({"success": False, "message": "未接收到导入数据"}), 400

        count = service.import_json(json_str)
        stats = service.get_statistics()
        return jsonify({
            "success": True,
            "imported": count,
            "statistics": stats,
        })

    return bp
