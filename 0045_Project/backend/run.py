import os
import sys
from flask import Flask, send_from_directory, jsonify
from flask_cors import CORS

from backend.repository.gear_json_repository import GearJsonRepository
from backend.core.gear_service import GearService
from backend.api.gear_api import gear_bp

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

data_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")
os.makedirs(data_dir, exist_ok=True)
json_path = os.path.join(data_dir, "gears.json")

repository = GearJsonRepository(json_path)
gear_service = GearService(repository)

if not repository.find_all():
    _seed_data = [
        {"name": "帐篷", "category": "住宿", "weight": 3.2, "essential": True, "notes": "双人帐篷"},
        {"name": "睡袋", "category": "住宿", "weight": 1.5, "essential": True, "notes": "零下10度款"},
        {"name": "头灯", "category": "照明", "weight": 0.1, "essential": True, "notes": ""},
        {"name": "登山杖", "category": "工具", "weight": 0.6, "essential": False, "notes": "碳纤维"},
        {"name": "炉具", "category": "炊具", "weight": 0.8, "essential": True, "notes": "气炉"},
        {"name": "急救包", "category": "安全", "weight": 0.4, "essential": True, "notes": ""},
        {"name": "水壶", "category": "炊具", "weight": 0.3, "essential": False, "notes": "1L保温壶"},
        {"name": "防潮垫", "category": "住宿", "weight": 0.9, "essential": False, "notes": "充气款"},
    ]
    from backend.model.camping_gear import CampingGear
    for item in _seed_data:
        gear = CampingGear(**item)
        repository.add(gear)


def create_app() -> Flask:
    app = Flask(__name__, static_folder=None)
    CORS(app)

    app.register_blueprint(gear_bp)

    public_dir = os.path.join(project_root, "public")

    @app.route("/")
    def index():
        return send_from_directory(public_dir, "camping.html")

    @app.route("/camping.css")
    def serve_css():
        return send_from_directory(public_dir, "camping.css")

    @app.route("/camping.js")
    def serve_js():
        return send_from_directory(public_dir, "camping.js")

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=5050, debug=False)
