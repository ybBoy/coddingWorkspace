"""
接口入口层：灵感相关的 HTTP 路由
接收前端请求，调用业务服务，返回 JSON 响应
"""
from flask import Blueprint, request, jsonify

from backend.services.idea_service import IdeaService

idea_bp = Blueprint("idea", __name__, url_prefix="/api/ideas")

service = IdeaService()


@idea_bp.route("", methods=["GET"])
def list_ideas():
    """获取灵感列表，支持搜索、标签筛选、只看收藏"""
    keyword = request.args.get("keyword", "")
    tag = request.args.get("tag", "")
    only_favorite = request.args.get("only_favorite", "false").lower() == "true"
    ideas = service.search(keyword=keyword, tag=tag, only_favorite=only_favorite)
    result = [idea.to_dict() for idea in ideas]
    return jsonify(result)


@idea_bp.route("/<idea_id>", methods=["GET"])
def get_idea(idea_id):
    """获取单条灵感详情"""
    idea = service.get_by_id(idea_id)
    if not idea:
        return jsonify({"error": "灵感不存在"}), 404
    return jsonify(idea.to_dict())


@idea_bp.route("", methods=["POST"])
def create_idea():
    """新增一条灵感"""
    data = request.get_json() or {}
    title = data.get("title", "").strip()
    content = data.get("content", "").strip()
    tags = data.get("tags", [])
    source = data.get("source", "").strip()
    if not content:
        return jsonify({"error": "内容不能为空"}), 400
    idea = service.create(title=title, content=content, tags=tags, source=source)
    return jsonify(idea.to_dict()), 201


@idea_bp.route("/<idea_id>", methods=["PUT"])
def update_idea(idea_id):
    """更新灵感信息"""
    data = request.get_json() or {}
    idea = service.update(
        idea_id,
        title=data.get("title"),
        content=data.get("content"),
        tags=data.get("tags"),
        source=data.get("source"),
    )
    if not idea:
        return jsonify({"error": "灵感不存在"}), 404
    return jsonify(idea.to_dict())


@idea_bp.route("/<idea_id>", methods=["DELETE"])
def delete_idea(idea_id):
    """删除一条灵感"""
    success = service.delete(idea_id)
    if not success:
        return jsonify({"error": "灵感不存在"}), 404
    return jsonify({"message": "删除成功"})


@idea_bp.route("/<idea_id>/favorite", methods=["POST"])
def toggle_favorite(idea_id):
    """切换收藏状态"""
    idea = service.toggle_favorite(idea_id)
    if not idea:
        return jsonify({"error": "灵感不存在"}), 404
    return jsonify(idea.to_dict())


@idea_bp.route("/tags", methods=["GET"])
def list_tags():
    """获取所有标签"""
    tags = service.get_all_tags()
    return jsonify(tags)


@idea_bp.route("/stats", methods=["GET"])
def get_stats():
    """获取统计信息"""
    stats = service.get_stats()
    return jsonify(stats)
