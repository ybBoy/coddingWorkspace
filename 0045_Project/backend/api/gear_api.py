from flask import Blueprint, request, jsonify, current_app
from backend.core.gear_service import GearService

gear_bp = Blueprint("gear", __name__, url_prefix="/api/gears")


def get_service() -> GearService:
    return current_app.config["GEAR_SERVICE"]


@gear_bp.route("", methods=["GET"])
def list_gears():
    service = get_service()
    category = request.args.get("category", "全部")
    gears = service.list_gears(category if category != "全部" else None)
    return jsonify([g.to_dict() for g in gears])


@gear_bp.route("/stats", methods=["GET"])
def get_stats():
    service = get_service()
    return jsonify(service.get_stats())


@gear_bp.route("/categories", methods=["GET"])
def get_categories():
    service = get_service()
    return jsonify(service.get_categories())


@gear_bp.route("", methods=["POST"])
def add_gear():
    service = get_service()
    data = request.get_json()
    if not data or not data.get("name"):
        return jsonify({"error": "装备名称不能为空"}), 400
    try:
        weight = float(data.get("weight", 0))
    except (ValueError, TypeError):
        return jsonify({"error": "重量必须是数字"}), 400
    gear = service.add_gear(
        name=data["name"],
        category=data.get("category", ""),
        weight=weight,
        essential=bool(data.get("essential", False)),
        notes=data.get("notes", ""),
    )
    return jsonify(gear.to_dict()), 201


@gear_bp.route("/<gear_id>/toggle-packed", methods=["PATCH"])
def toggle_packed(gear_id):
    service = get_service()
    gear = service.toggle_packed(gear_id)
    if gear is None:
        return jsonify({"error": "装备不存在"}), 404
    return jsonify(gear.to_dict())


@gear_bp.route("/<gear_id>/toggle-essential", methods=["PATCH"])
def toggle_essential(gear_id):
    service = get_service()
    gear = service.toggle_essential(gear_id)
    if gear is None:
        return jsonify({"error": "装备不存在"}), 404
    return jsonify(gear.to_dict())


@gear_bp.route("/<gear_id>", methods=["DELETE"])
def delete_gear(gear_id):
    service = get_service()
    if service.delete_gear(gear_id):
        return jsonify({"message": "已删除"}), 200
    return jsonify({"error": "装备不存在"}), 404
