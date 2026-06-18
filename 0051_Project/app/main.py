import os
from flask import Flask, send_from_directory
from app.api.baking_routes import bp as baking_bp


def create_app():
    app = Flask(
        __name__,
        static_folder=None
    )

    frontend_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "frontend")
    )

    @app.route("/")
    def index():
        return send_from_directory(frontend_dir, "baking.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        return send_from_directory(frontend_dir, filename)

    app.register_blueprint(baking_bp)

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host="127.0.0.1", port=5000, debug=True)
