import sys
import os

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Flask, send_from_directory
from flask_cors import CORS

from api.routes.medicine_routes import medicine_bp

app = Flask(__name__, static_folder="../client", static_url_path="")
CORS(app)

app.register_blueprint(medicine_bp)


@app.route("/")
def index():
    return send_from_directory("../client", "medicine.html")


@app.route("/health")
def health():
    return {"status": "ok", "message": "家庭药箱管理服务运行中"}


if __name__ == "__main__":
    print("🚀 家庭药箱管理应用启动中...")
    print("📂 前端页面: http://localhost:5000/")
    print("📊 健康检查: http://localhost:5000/health")
    app.run(host="0.0.0.0", port=5000, debug=True)
