# server.py — 应用入口
# 负责创建 Flask 应用、注册路由蓝图、托管前端静态文件
# 数据流总览：
#   用户新增/修改/删除/筛选地点 → weekend.js 用 fetch 调 Python 后端接口
#   → place_api 路由接收请求 → place_manager 更新内存数据
#   → place_store 保存 JSON 文件 → 前端重新拉取地点列表并刷新统计和卡片

from flask import Flask, send_from_directory
import os

SITE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "site")


def create_app() -> Flask:
    app = Flask(__name__, static_folder=None)

    from app.routes.place_api import api_bp
    app.register_blueprint(api_bp)

    @app.route("/")
    def index():
        return send_from_directory(SITE_DIR, "index.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        return send_from_directory(SITE_DIR, filename)

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(debug=True, port=5000)
