from flask import Blueprint, jsonify, request

from server.services.wardrobe_service import get_service


wardrobe_bp = Blueprint("wardrobe", __name__, url_prefix="/api")
service = get_service()


def _json_error(message: str, status_code: int = 400):
    return jsonify({"error": message}), status_code


@wardrobe_bp.route("/clothes", methods=["GET"])
def list_clothes():
    type_filter = request.args.get("type")
    color_filter = request.args.get("color")
    season_filter = request.args.get("season")
    clothes = service.get_clothes(type_filter, color_filter, season_filter)
    result = []
    for c in clothes:
        data = c.to_dict()
        data["days_since_last_worn"] = c.days_since_last_worn()
        data["is_long_time_no_wear"] = c.is_long_time_no_wear()
        result.append(data)
    return jsonify(result)


@wardrobe_bp.route("/clothes/<clothing_id>", methods=["GET"])
def get_clothing(clothing_id):
    c = service.get_clothing(clothing_id)
    if not c:
        return _json_error("Clothing not found", 404)
    data = c.to_dict()
    data["days_since_last_worn"] = c.days_since_last_worn()
    data["is_long_time_no_wear"] = c.is_long_time_no_wear()
    return jsonify(data)


@wardrobe_bp.route("/clothes", methods=["POST"])
def add_clothing():
    body = request.get_json(silent=True) or {}
    name = body.get("name", "").strip()
    type = body.get("type", "").strip()
    color = body.get("color", "").strip()
    season = body.get("season", "").strip()
    remark = body.get("remark", "").strip()
    if not name or not type or not color or not season:
        return _json_error("name, type, color, season are required")
    try:
        c = service.add_clothing(name, type, color, season, remark)
        return jsonify(c.to_dict()), 201
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/clothes/<clothing_id>", methods=["PUT"])
def update_clothing(clothing_id):
    body = request.get_json(silent=True) or {}
    c = service.update_clothing(
        clothing_id,
        name=body.get("name"),
        type=body.get("type"),
        color=body.get("color"),
        season=body.get("season"),
        remark=body.get("remark"),
    )
    if not c:
        return _json_error("Clothing not found", 404)
    return jsonify(c.to_dict())


@wardrobe_bp.route("/clothes/<clothing_id>", methods=["DELETE"])
def delete_clothing(clothing_id):
    ok = service.delete_clothing(clothing_id)
    if not ok:
        return _json_error("Clothing not found", 404)
    return jsonify({"message": "Deleted successfully"})


@wardrobe_bp.route("/outfit", methods=["POST"])
def record_outfit():
    body = request.get_json(silent=True) or {}
    clothing_ids = body.get("clothing_ids", [])
    note = body.get("note", "").strip()
    if not clothing_ids:
        return _json_error("clothing_ids is required and cannot be empty")
    try:
        result = service.record_outfit(clothing_ids, note)
        return jsonify(result), 201
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/outfit/logs", methods=["GET"])
def list_outfit_logs():
    limit = request.args.get("limit", 100, type=int)
    logs = service.get_outfit_logs(limit)
    return jsonify([l.to_dict() for l in logs])


@wardrobe_bp.route("/stats", methods=["GET"])
def get_stats():
    return jsonify(service.get_stats())


@wardrobe_bp.route("/filters", methods=["GET"])
def get_filters():
    return jsonify(service.get_filters())
