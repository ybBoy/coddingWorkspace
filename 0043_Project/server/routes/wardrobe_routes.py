import json
from flask import Blueprint, jsonify, request, send_file
from io import BytesIO
from datetime import datetime

from server.services.wardrobe_service import get_service


wardrobe_bp = Blueprint("wardrobe", __name__, url_prefix="/api")
service = get_service()


def _json_error(message: str, status_code: int = 400):
    return jsonify({"error": message}), status_code


def _enrich_clothing(c):
    data = c.to_dict()
    data["days_since_last_worn"] = c.days_since_last_worn()
    data["is_long_time_no_wear"] = c.is_long_time_no_wear()
    data["has_been_worn"] = c.has_been_worn()
    return data


@wardrobe_bp.route("/clothes", methods=["GET"])
def list_clothes():
    type_filter = request.args.get("type")
    color_filter = request.args.get("color")
    season_filter = request.args.get("season")
    tag_filter = request.args.get("tag")
    clothes = service.get_clothes(type_filter, color_filter, season_filter, tag_filter)
    return jsonify([_enrich_clothing(c) for c in clothes])


@wardrobe_bp.route("/clothes/<clothing_id>", methods=["GET"])
def get_clothing(clothing_id):
    c = service.get_clothing(clothing_id)
    if not c:
        return _json_error("Clothing not found", 404)
    return jsonify(_enrich_clothing(c))


@wardrobe_bp.route("/clothes", methods=["POST"])
def add_clothing():
    body = request.get_json(silent=True) or {}
    name = body.get("name", "").strip()
    type = body.get("type", "").strip()
    color = body.get("color", "").strip()
    season = body.get("season", "").strip()
    remark = body.get("remark", "").strip()
    image_url = body.get("image_url", "").strip()
    tags = body.get("tags", [])
    if not isinstance(tags, list):
        tags = []
    if not name or not type or not color or not season:
        return _json_error("name, type, color, season are required")
    try:
        c = service.add_clothing(name, type, color, season, remark, image_url, tags)
        return jsonify(c.to_dict()), 201
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/clothes/<clothing_id>", methods=["PUT"])
def update_clothing(clothing_id):
    body = request.get_json(silent=True) or {}
    try:
        c = service.update_clothing(
            clothing_id,
            name=body.get("name"),
            type=body.get("type"),
            color=body.get("color"),
            season=body.get("season"),
            remark=body.get("remark"),
            image_url=body.get("image_url"),
            tags=body.get("tags"),
            wear_count=body.get("wear_count"),
            last_worn_at=body.get("last_worn_at"),
        )
    except ValueError as e:
        return _json_error(str(e))
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
    limit = request.args.get("limit", 20, type=int)
    logs = service.get_outfit_logs_with_details(limit)
    return jsonify(logs)


@wardrobe_bp.route("/outfit/calendar", methods=["GET"])
def get_calendar():
    month_str = request.args.get("month")
    if not month_str:
        now = datetime.now()
        year = now.year
        month = now.month
    else:
        try:
            year, month = month_str.split("-")
            year = int(year)
            month = int(month)
        except (ValueError, TypeError):
            return _json_error("month must be in format YYYY-MM")
    try:
        data = service.get_calendar(year, month)
        return jsonify(data)
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/templates", methods=["GET"])
def list_templates():
    return jsonify(service.list_templates())


@wardrobe_bp.route("/templates/<template_id>", methods=["GET"])
def get_template(template_id):
    t = service.get_template(template_id)
    if not t:
        return _json_error("Template not found", 404)
    return jsonify(t)


@wardrobe_bp.route("/templates", methods=["POST"])
def add_template():
    body = request.get_json(silent=True) or {}
    name = body.get("name", "").strip()
    clothing_ids = body.get("clothing_ids", [])
    note = body.get("note", "").strip()
    if not name:
        return _json_error("name is required")
    if not clothing_ids or not isinstance(clothing_ids, list):
        return _json_error("clothing_ids is required and must be a non-empty array")
    try:
        t = service.add_template(name, clothing_ids, note)
        return jsonify(t), 201
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/templates/<template_id>", methods=["PUT"])
def update_template(template_id):
    body = request.get_json(silent=True) or {}
    try:
        t = service.update_template(
            template_id,
            name=body.get("name"),
            clothing_ids=body.get("clothing_ids"),
            note=body.get("note"),
        )
    except ValueError as e:
        return _json_error(str(e))
    if not t:
        return _json_error("Template not found", 404)
    return jsonify(t)


@wardrobe_bp.route("/templates/<template_id>", methods=["DELETE"])
def delete_template(template_id):
    ok = service.delete_template(template_id)
    if not ok:
        return _json_error("Template not found", 404)
    return jsonify({"message": "Deleted successfully"})


@wardrobe_bp.route("/templates/<template_id>/apply", methods=["POST"])
def apply_template(template_id):
    body = request.get_json(silent=True) or {}
    note = body.get("note", "").strip()
    try:
        result = service.apply_template(template_id, note)
        return jsonify(result), 201
    except ValueError as e:
        return _json_error(str(e))


@wardrobe_bp.route("/stats", methods=["GET"])
def get_stats():
    return jsonify(service.get_stats())


@wardrobe_bp.route("/filters", methods=["GET"])
def get_filters():
    return jsonify(service.get_filters())


@wardrobe_bp.route("/recommend", methods=["GET"])
def recommend_outfit():
    season = request.args.get("season")
    return jsonify(service.recommend_outfit(season))


@wardrobe_bp.route("/export", methods=["GET"])
def export_data():
    data = service.export_data()
    json_str = json.dumps(data, ensure_ascii=False, indent=2)
    buf = BytesIO(json_str.encode("utf-8"))
    buf.seek(0)
    filename = f"wardrobe_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    return send_file(
        buf,
        mimetype="application/json",
        as_attachment=True,
        download_name=filename,
    )


@wardrobe_bp.route("/import", methods=["POST"])
def import_data():
    merge = request.args.get("merge", "true").lower() != "false"
    raw = request.get_data(as_text=True)
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return _json_error("Invalid JSON format")
    try:
        result = service.import_data(data, merge=merge)
        return jsonify(result), 200
    except ValueError as e:
        return _json_error(str(e))
