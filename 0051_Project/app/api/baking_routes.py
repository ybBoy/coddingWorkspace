from flask import Blueprint, request, jsonify
from app.service.baking_service import BakingService
from app.storage.baking_store import BakingStore
from app.model.baking_trial import SUCCESS_LEVELS, DESSERT_TYPES


bp = Blueprint("baking", __name__, url_prefix="/api/baking")

_store = BakingStore()
_service = BakingService(_store)


@bp.route("/trials", methods=["GET"])
def get_trials():
    dessert_type = request.args.get("dessert_type")
    search = request.args.get("search")

    trials = _service.get_trials(dessert_type=dessert_type, search=search)
    stats = _service.get_statistics(filtered_count=len(trials))

    return jsonify({
        "trials": [t.to_dict() for t in trials],
        "statistics": stats
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


@bp.route("/meta", methods=["GET"])
def get_meta():
    return jsonify({
        "success_levels": SUCCESS_LEVELS,
        "dessert_types": DESSERT_TYPES
    })
