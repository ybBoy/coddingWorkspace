import os
from flask import Flask, send_from_directory
from server.repositories.exhibit_repository import ExhibitRepository
from server.services.exhibit_service import ExhibitService
from server.api.exhibit_api import create_exhibit_bp


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(BASE_DIR)
WEB_DIR = os.path.join(PROJECT_DIR, "web")
DATA_FILE = os.path.join(BASE_DIR, "data", "exhibits.json")


def create_app() -> Flask:
    app = Flask(__name__, static_folder=None)

    repository = ExhibitRepository(DATA_FILE)
    service = ExhibitService(repository)
    exhibit_bp = create_exhibit_bp(service)

    app.register_blueprint(exhibit_bp)

    @app.route("/")
    def index():
        return send_from_directory(WEB_DIR, "exhibit.html")

    @app.route("/<path:filename>")
    def static_files(filename):
        return send_from_directory(WEB_DIR, filename)

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=5001, debug=True)
