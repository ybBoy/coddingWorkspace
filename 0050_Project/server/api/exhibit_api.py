from flask import Blueprint, request, jsonify
from server.services.exhibit_service import ExhibitService


def create_exhibit_bp(service: ExhibitService) -> Blueprint:
    bp = Blueprint("exhibit", __name__)

    @bp.route("/api/exhibits", methods=["GET"])
    def get_exhibits():
        exhibit_type = request.args.get("type")
        location = request.args.get("location")
        records = service.get_filtered_records(exhibit_type, location)
        return jsonify([r.to_dict() for r in records])

    @bp.route("/api/exhibits/<record_id>", methods=["GET"])
    def get_exhibit(record_id):
        record = service.get_record_by_id(record_id)
        if not record:
            return jsonify({"error": "记录不存在"}), 404
        return jsonify(record.to_dict())

    @bp.route("/api/exhibits", methods=["POST"])
    def add_exhibit():
        data = request.get_json()
        if not data:
            return jsonify({"error": "请求体不能为空"}), 400

        required_fields = ["name", "location", "visit_date", "exhibit_type", "rating", "comment"]
        for field in required_fields:
            if field not in data:
                return jsonify({"error": f"缺少必填字段: {field}"}), 400

        try:
            rating = int(data["rating"])
        except (ValueError, TypeError):
            return jsonify({"error": "评分必须是数字"}), 400

        record = service.add_record(
            name=data["name"],
            location=data["location"],
            visit_date=data["visit_date"],
            exhibit_type=data["exhibit_type"],
            rating=rating,
            comment=data["comment"],
        )
        return jsonify(record.to_dict()), 201

    @bp.route("/api/exhibits/<record_id>/rating", methods=["PUT"])
    def update_rating(record_id):
        data = request.get_json()
        if not data or "rating" not in data:
            return jsonify({"error": "缺少 rating 字段"}), 400

        try:
            rating = int(data["rating"])
        except (ValueError, TypeError):
            return jsonify({"error": "评分必须是数字"}), 400

        try:
            record = service.update_rating(record_id, rating)
        except ValueError as e:
            return jsonify({"error": str(e)}), 400

        if not record:
            return jsonify({"error": "记录不存在"}), 404
        return jsonify(record.to_dict())

    @bp.route("/api/exhibits/<record_id>", methods=["DELETE"])
    def delete_exhibit(record_id):
        success = service.delete_record(record_id)
        if not success:
            return jsonify({"error": "记录不存在"}), 404
        return jsonify({"message": "删除成功"})

    @bp.route("/api/statistics", methods=["GET"])
    def get_statistics():
        exhibit_type = request.args.get("type")
        location = request.args.get("location")
        stats = service.get_statistics(exhibit_type, location)
        return jsonify(stats)

    @bp.route("/api/exhibit-types", methods=["GET"])
    def get_exhibit_types():
        types = service.get_all_types()
        return jsonify(types)

    return bp
