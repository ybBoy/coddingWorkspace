from flask import Blueprint, request, jsonify

from api.services.medicine_service import medicine_service

medicine_bp = Blueprint("medicine", __name__, url_prefix="/api/medicines")


def success_response(data=None, message="success", code=200, **kwargs):
    response = {"code": code, "message": message, "data": data}
    response.update(kwargs)
    return jsonify(response), code


def error_response(message, code=400):
    return jsonify({"code": code, "message": message, "data": None}), code


@medicine_bp.route("", methods=["GET"])
def get_medicines():
    purpose = request.args.get("purpose", "").strip() or None
    location = request.args.get("location", "").strip() or None

    data, stats = medicine_service.get_medicines(purpose, location)
    return success_response(data, stats=stats)


@medicine_bp.route("/filter-options", methods=["GET"])
def get_filter_options():
    options = medicine_service.get_filter_options()
    return success_response(options)


@medicine_bp.route("/<medicine_id>", methods=["GET"])
def get_medicine(medicine_id):
    data = medicine_service.get_medicine_by_id(medicine_id)
    if not data:
        return error_response("药品不存在", 404)
    return success_response(data)


@medicine_bp.route("", methods=["POST"])
def add_medicine():
    try:
        data = request.get_json() or {}
        created = medicine_service.add_medicine(data)
        return success_response(created, "药品添加成功", 201)
    except ValueError as e:
        return error_response(str(e), 400)


@medicine_bp.route("/<medicine_id>", methods=["PUT"])
def update_medicine(medicine_id):
    try:
        data = request.get_json() or {}
        updated = medicine_service.update_medicine(medicine_id, data)
        if not updated:
            return error_response("药品不存在", 404)
        return success_response(updated, "药品更新成功")
    except ValueError as e:
        return error_response(str(e), 400)


@medicine_bp.route("/<medicine_id>", methods=["DELETE"])
def delete_medicine(medicine_id):
    success = medicine_service.delete_medicine(medicine_id)
    if not success:
        return error_response("药品不存在", 404)
    return success_response(None, "药品删除成功")


@medicine_bp.route("/<medicine_id>/use", methods=["POST"])
def use_medicine(medicine_id):
    try:
        data = request.get_json() or {}
        amount = data.get("amount", 1)
        updated = medicine_service.use_medicine(medicine_id, amount)
        if not updated:
            return error_response("药品不存在", 404)
        return success_response(updated, "使用记录已更新")
    except ValueError as e:
        return error_response(str(e), 400)


@medicine_bp.route("/<medicine_id>/replenish", methods=["POST"])
def replenish_medicine(medicine_id):
    try:
        data = request.get_json() or {}
        amount = data.get("amount", 1)
        updated = medicine_service.replenish_medicine(medicine_id, amount)
        if not updated:
            return error_response("药品不存在", 404)
        return success_response(updated, "库存补充成功")
    except ValueError as e:
        return error_response(str(e), 400)
