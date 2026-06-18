"""
app.py - Flask 应用入口
文件职责：初始化各层组件（存储层 → 服务层 → 路由层），
挂载静态资源，配置 CORS，启动 Web 服务。

整体架构流程：
  前端页面 (client/) → HTTP 请求 → Flask 路由 (routes/) → 业务服务 (services/) → JSON 存储 (store/)
                                                                                          ↓
                                                                                     data/candles.json
"""

import os
from flask import Flask, send_from_directory
from flask_cors import CORS

from src.store.candle_json_store import CandleJsonStore
from src.services.candle_service import CandleService
from src.routes.candle_routes import create_candle_blueprint


def create_app() -> Flask:
    """
    创建并配置 Flask 应用
    
    Returns:
        配置好的 Flask 应用实例
    """
    app = Flask(__name__, static_folder=None)
    CORS(app)  # 允许跨域，方便前端调试

    # 确定数据文件路径
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_file = os.path.join(base_dir, "data", "candles.json")

    # 初始化各层：存储层 → 服务层 → 路由层
    store = CandleJsonStore(data_file)
    service = CandleService(store)
    candle_bp = create_candle_blueprint(service)
    app.register_blueprint(candle_bp)

    # 静态文件路由：直接访问 / 时跳转到蜡烛页面
    client_dir = os.path.join(base_dir, "client")

    @app.route("/")
    def index():
        """首页：跳转到蜡烛管理页面"""
        return send_from_directory(client_dir, "candles.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        """提供前端静态文件（HTML/CSS/JS）"""
        return send_from_directory(client_dir, filename)

    return app


if __name__ == "__main__":
    app = create_app()
    print("[香薰蜡烛库存管理系统] 启动中...")
    print(f"数据文件: {os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'data', 'candles.json')}")
    print("访问地址: http://127.0.0.1:5002/")
    app.run(host="0.0.0.0", port=5002, debug=True)
