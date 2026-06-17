from flask import Blueprint, request, jsonify, send_file
import io
import json

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
    keyword = request.args.get("keyword", "").strip() or None
    member = request.args.get("member", "").strip() or None
    sort = request.args.get("sort", "").strip() or None
    only_expired = request.args.get("only_expired", "").lower() in ("1", "true", "yes")

    data, stats = medicine_service.get_medicines(
        purpose=purpose,
        location=location,
        keyword=keyword,
        member=member,
        sort=sort,
        only_expired=only_expired,
    )
    return success_response(data, stats=stats)


@medicine_bp.route("/filter-options", methods=["GET"])
def get_filter_options():
    options = medicine_service.get_filter_options()
    return success_response(options)


@medicine_bp.route("/logs", methods=["GET"])
def get_operation_logs():
    try:
        limit = int(request.args.get("limit", 50))
    except (ValueError, TypeError):
        limit = 50
    logs = medicine_service.get_operation_logs(limit)
    return success_response(logs)


@medicine_bp.route("/settings", methods=["GET"])
def get_settings():
    settings = medicine_service.get_settings()
    return success_response(settings)


@medicine_bp.route("/settings", methods=["PUT"])
def update_settings():
    try:
        data = request.get_json() or {}
        updated = medicine_service.update_settings(data)
        return success_response(updated, "设置已更新")
    except ValueError as e:
        return error_response(str(e), 400)


@medicine_bp.route("/export", methods=["GET"])
def export_data():
    export_data = medicine_service.export_data()
    json_str = json.dumps(export_data, ensure_ascii=False, indent=2)
    buf = io.BytesIO(json_str.encode("utf-8"))
    buf.seek(0)
    return send_file(
        buf,
        mimetype="application/json",
        as_attachment=True,
        download_name="medicine_backup.json",
    )


@medicine_bp.route("/import", methods=["POST"])
def import_data():
    try:
        if "file" in request.files:
            file = request.files["file"]
            content = file.read().decode("utf-8")
            data = json.loads(content)
        else:
            data = request.get_json() or {}

        result = medicine_service.import_data(data)
        return success_response(
            result, f"导入成功：{result['imported']} 条，跳过 {result['skipped']} 条"
        )
    except json.JSONDecodeError:
        return error_response("JSON 格式错误", 400)
    except ValueError as e:
        return error_response(str(e), 400)
    except Exception as e:
        return error_response(f"导入失败：{str(e)}", 500)


@medicine_bp.route("/expired", methods=["DELETE"])
def delete_expired_medicines():
    result = medicine_service.delete_expired_medicines()
    return success_response(result, f"已清理 {result['deleted']} 种过期药品")


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


@medicine_bp.route("/<medicine_id>/upload", methods=["POST"])
def upload_image(medicine_id):
    try:
        if "image" not in request.files:
            return error_response("未找到上传文件", 400)
        file = request.files["image"]
        image_url = medicine_service.upload_image(medicine_id, file)
        return success_response({"image_url": image_url}, "图片上传成功")
    except ValueError as e:
        return error_response(str(e), 400)
    except Exception as e:
        return error_response(f"上传失败：{str(e)}", 500)
