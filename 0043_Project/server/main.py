import os
import sys
from pathlib import Path
from flask import Flask, send_from_directory
from flask_cors import CORS

BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from server.routes.wardrobe_routes import wardrobe_bp

PAGE_DIR = BASE_DIR / "page"


def create_app() -> Flask:
    app = Flask(__name__, static_folder=None)
    CORS(app)

    app.register_blueprint(wardrobe_bp)

    @app.route("/")
    def index():
        return send_from_directory(PAGE_DIR, "wardrobe.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        return send_from_directory(PAGE_DIR, filename)

    return app


if __name__ == "__main__":
    app = create_app()
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port, debug=False, use_reloader=False)
