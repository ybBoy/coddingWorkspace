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

    @app.errorhandler(400)
    @app.errorhandler(404)
    @app.errorhandler(500)
    def _handle_error(e):
        from flask import jsonify
        status_code = e.code if hasattr(e, "code") else 500
        message = str(e) if str(e) else "Internal server error"
        return jsonify({"error": message}), status_code

    @app.errorhandler(ValueError)
    def _handle_value_error(e):
        from flask import jsonify
        return jsonify({"error": str(e)}), 400

    @app.errorhandler(RuntimeError)
    def _handle_runtime_error(e):
        from flask import jsonify
        return jsonify({"error": str(e)}), 500

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
