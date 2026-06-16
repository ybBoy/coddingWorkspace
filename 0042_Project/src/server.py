"""
文件职责：
    Flask 应用入口，负责创建并配置 Web 服务器。
    组装各层组件（Service + API），挂载前端静态文件，
    并启动 HTTP 服务供浏览器访问。

数据流总览：
    浏览器打开 / → Flask 返回 snacks.html
    浏览器加载 snacks.css, snacks.js → Flask 返回静态资源
    snacks.js 通过 fetch 调用 /api/snacks/* → snack_api 处理 → snack_service → snack_file_store
                                                                                ↓
    snacks.js 收到 JSON 响应 → 更新 DOM 渲染零食卡片
"""

import os
import sys

from flask import Flask, send_from_directory
from flask_cors import CORS

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

from src.api.snack_api import create_snack_blueprint
from src.service.snack_service import SnackService
from src.storage.snack_file_store import SnackFileStore


def create_app() -> Flask:
    """创建并配置 Flask 应用"""
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    web_dir = os.path.join(project_root, "web")

    app = Flask(
        __name__,
        static_folder=web_dir,
        static_url_path="",
    )

    CORS(app)

    store = SnackFileStore()
    service = SnackService(store)
    blueprint = create_snack_blueprint(service)
    app.register_blueprint(blueprint)

    @app.route("/")
    def index():
        """根路径返回前端页面"""
        return send_from_directory(web_dir, "snacks.html")

    return app


if __name__ == "__main__":
    app = create_app()
    print("=" * 50)
    print("  零食库存小柜子启动啦！")
    print("  访问地址: http://127.0.0.1:5001")
    print("=" * 50)
    app.run(host="0.0.0.0", port=5001, debug=False)
