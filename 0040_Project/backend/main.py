"""
后端入口文件：启动 Flask 应用，注册路由，配置静态页面
数据流：前端请求 → routes 路由 → services 业务 → storage 持久化 → 返回响应
"""
import os
import sys
from flask import Flask, send_from_directory

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from backend.routes.idea_routes import idea_bp


def create_app() -> Flask:
    """创建 Flask 应用"""
    app = Flask(__name__, static_folder=None)

    app.register_blueprint(idea_bp)

    public_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "public")

    @app.route("/")
    def index():
        """首页：返回灵感箱页面"""
        return send_from_directory(public_dir, "inbox.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        """提供静态文件（CSS、JS 等）"""
        return send_from_directory(public_dir, filename)

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=5000, debug=True)
