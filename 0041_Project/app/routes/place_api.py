# place_api.py — REST API 路由层
# 负责定义 HTTP 接口，接收前端 fetch 请求并调用 place_manager 处理
# 数据流：weekend.js fetch 请求 → place_api 路由 → place_manager 操作 → 返回 JSON 响应

from flask import Blueprint, request, jsonify
from app.core.place_manager import PlaceManager

api_bp = Blueprint("place_api", __name__, url_prefix="/api/places")

_manager = PlaceManager()


@api_bp.route("", methods=["GET"])
def get_places():
    place_type = request.args.get("type")
    sort_by_cost = request.args.get("sort_by_cost", "0") == "1"
    keyword = request.args.get("keyword") or None
    max_cost_str = request.args.get("max_cost")
    max_cost = None
    if max_cost_str:
        try:
            max_cost = float(max_cost_str)
        except (ValueError, TypeError):
            return jsonify({"error": "max_cost 必须是有效的数字"}), 400
    places = _manager.list_places(
        place_type=place_type,
        sort_by_cost=sort_by_cost,
        keyword=keyword,
        max_cost=max_cost,
    )
    stats = _manager.get_stats(
        place_type=place_type, keyword=keyword, max_cost=max_cost
    )
    return jsonify({"places": [p.to_dict() for p in places], "stats": stats})


@api_bp.route("/recommend", methods=["GET"])
def get_recommend():
    places = _manager.get_weekend_recommend()
    all_stats = _manager.get_stats()
    return jsonify({"places": [p.to_dict() for p in places], "stats": all_stats})


@api_bp.route("", methods=["POST"])
def add_place():
    data = request.get_json(force=True)
    if not data.get("name"):
        return jsonify({"error": "地点名称不能为空"}), 400
    place = _manager.add_place(data)
    return jsonify(place.to_dict()), 201


@api_bp.route("/<place_id>", methods=["PUT"])
def update_place(place_id):
    data = request.get_json(force=True)
    place = _manager.update_place(place_id, data)
    if place is None:
        return jsonify({"error": "地点不存在"}), 404
    return jsonify(place.to_dict())


@api_bp.route("/<place_id>/toggle-visited", methods=["POST"])
def toggle_visited(place_id):
    place = _manager.toggle_visited(place_id)
    if place is None:
        return jsonify({"error": "地点不存在"}), 404
    return jsonify(place.to_dict())


@api_bp.route("/<place_id>", methods=["DELETE"])
def delete_place(place_id):
    ok = _manager.delete_place(place_id)
    if not ok:
        return jsonify({"error": "地点不存在"}), 404
    return jsonify({"message": "已删除"})
