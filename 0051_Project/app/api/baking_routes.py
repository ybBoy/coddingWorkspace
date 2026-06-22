import csv
import io
import json
from flask import Blueprint, request, jsonify, Response
from app.service.baking_service import BakingService
from app.storage.baking_store import BakingStore
from app.model.baking_trial import (
    SUCCESS_LEVELS, DESSERT_TYPES, SORT_FIELDS, SORT_ORDERS,
)


bp = Blueprint("baking", __name__, url_prefix="/api/baking")

_store = BakingStore()
_service = BakingService(_store)


@bp.route("/trials", methods=["GET"])
def get_trials():
    dessert_type = request.args.get("dessert_type")
    success_level = request.args.get("success_level")
    search = request.args.get("search")
    sort_by = request.args.get("sort_by", "created_at")
    sort_order = request.args.get("sort_order", "desc")

    trials = _service.get_trials(
        dessert_type=dessert_type,
        success_level=success_level,
        search=search,
        sort_by=sort_by,
        sort_order=sort_order,
    )
    stats = _service.get_statistics(filtered_count=len(trials))

    return jsonify({
        "trials": [t.to_dict() for t in trials],
        "statistics": stats,
    })


@bp.route("/trials/<int:trial_id>", methods=["GET"])
def get_trial(trial_id):
    trial = _service.get_trial_by_id(trial_id)
    if trial is None:
        return jsonify({"error": "未找到该试验记录"}), 404
    return jsonify(trial.to_dict())


@bp.route("/trials", methods=["POST"])
def create_trial():
    data = request.get_json(force=True) or {}

    required = ["dessert_name", "dessert_type", "temperature", "duration_minutes"]
    for field in required:
        if field not in data:
            return jsonify({"error": f"缺少必填字段: {field}"}), 400

    try:
        data["temperature"] = int(data["temperature"])
        data["duration_minutes"] = int(data["duration_minutes"])
    except (ValueError, TypeError):
        return jsonify({"error": "温度和时间必须为数字"}), 400

    trial = _service.create_trial(data)
    return jsonify(trial.to_dict()), 201


@bp.route("/trials/<int:trial_id>", methods=["PUT"])
def update_trial(trial_id):
    data = request.get_json(force=True) or {}

    if "temperature" in data:
        try:
            data["temperature"] = int(data["temperature"])
        except (ValueError, TypeError):
            return jsonify({"error": "温度必须为数字"}), 400
    if "duration_minutes" in data:
        try:
            data["duration_minutes"] = int(data["duration_minutes"])
        except (ValueError, TypeError):
            return jsonify({"error": "时间必须为数字"}), 400

    trial = _service.update_trial(trial_id, data)
    if trial is None:
        return jsonify({"error": "未找到该试验记录"}), 404
    return jsonify(trial.to_dict())


@bp.route("/trials/<int:trial_id>/validate", methods=["POST"])
def validate_trial(trial_id):
    data = request.get_json(force=True) or {}
    warnings = _service.validate_trial(data, exclude_id=trial_id)
    return jsonify({"warnings": warnings})


@bp.route("/trials/validate", methods=["POST"])
def validate_new_trial():
    data = request.get_json(force=True) or {}
    warnings = _service.validate_trial(data)
    return jsonify({"warnings": warnings})


@bp.route("/trials/<int:trial_id>/success", methods=["PUT"])
def update_success(trial_id):
    data = request.get_json(force=True) or {}
    success_level = data.get("success_level")

    if success_level not in SUCCESS_LEVELS:
        return jsonify({"error": f"成功程度必须是以下之一: {SUCCESS_LEVELS}"}), 400

    trial = _service.update_success_level(trial_id, success_level)
    if trial is None:
        return jsonify({"error": "未找到该试验记录"}), 404
    return jsonify(trial.to_dict())


@bp.route("/trials/<int:trial_id>", methods=["DELETE"])
def delete_trial(trial_id):
    deleted = _service.delete_trial(trial_id)
    if not deleted:
        return jsonify({"error": "未找到该试验记录"}), 404
    return jsonify({"message": "删除成功"}), 200


@bp.route("/versions", methods=["GET"])
def get_versions():
    dessert_name = request.args.get("dessert_name")
    results = _service.get_version_comparison(dessert_name=dessert_name)
    return jsonify({"comparisons": results})


@bp.route("/export/json", methods=["GET"])
def export_json():
    trials = _service.get_trials()
    data = [t.to_dict() for t in trials]
    body = json.dumps(data, ensure_ascii=False, indent=2)
    return Response(
        body,
        mimetype="application/json",
        headers={"Content-Disposition": "attachment; filename=baking_trials.json"},
    )


@bp.route("/export/csv", methods=["GET"])
def export_csv():
    trials = _service.get_trials()
    fields = [
        "id", "dessert_name", "dessert_type", "recipe_version",
        "temperature", "duration_minutes", "success_level",
        "taste_score", "look_score", "texture_score",
        "notes", "created_at",
    ]
    buf = io.StringIO()
    writer = csv.DictWriter(buf, fieldnames=fields, extrasaction="ignore")
    writer.writeheader()
    for t in trials:
        writer.writerow(t.to_dict())
    csv_content = buf.getvalue()
    if not csv_content.startswith("\ufeff"):
        csv_content = "\ufeff" + csv_content
    return Response(
        csv_content,
        mimetype="text/csv; charset=utf-8-sig",
        headers={"Content-Disposition": "attachment; filename=baking_trials.csv"},
    )


@bp.route("/import", methods=["POST"])
def import_trials():
    data = request.get_json(force=True) or {}
    items = data.get("items") or []
    mode = data.get("mode", "append")

    if mode == "replace":
        all_trials = _service.get_trials()
        for t in list(all_trials):
            _service.delete_trial(t.id)

    result = _service.import_trials(items)
    stats = _service.get_statistics()
    return jsonify({**result, "statistics": stats})


@bp.route("/meta", methods=["GET"])
def get_meta():
    return jsonify({
        "success_levels": SUCCESS_LEVELS,
        "dessert_types": DESSERT_TYPES,
        "sort_fields": SORT_FIELDS,
        "sort_field_labels": {
            "created_at": "记录时间",
            "temperature": "烘焙温度",
            "duration_minutes": "烘焙时间",
            "success_level": "成功程度",
            "recipe_version": "配方版本",
        },
        "sort_orders": SORT_ORDERS,
        "sort_order_labels": {"desc": "降序", "asc": "升序"},
    })
