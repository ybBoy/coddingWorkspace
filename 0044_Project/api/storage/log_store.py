import json
from pathlib import Path
from datetime import datetime


class LogStore:
    def __init__(self, log_file: str = "data/operation_logs.json"):
        self.log_file = Path(log_file)
        self._logs: list[dict] = []
        self._ensure_log_file()
        self._load_from_file()

    def _ensure_log_file(self):
        self.log_file.parent.mkdir(parents=True, exist_ok=True)
        if not self.log_file.exists():
            self.log_file.write_text(
                json.dumps({"logs": []}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )

    def _load_from_file(self):
        try:
            with open(self.log_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            self._logs = data.get("logs", [])
        except (json.JSONDecodeError, FileNotFoundError):
            self._logs = []

    def _save_to_file(self):
        with open(self.log_file, "w", encoding="utf-8") as f:
            json.dump({"logs": self._logs}, f, ensure_ascii=False, indent=2)

    def add_log(
        self,
        operation_type: str,
        medicine_name: str,
        quantity: int,
        unit: str = "",
    ):
        log_entry = {
            "id": str(int(datetime.now().timestamp() * 1000)),
            "operation_type": operation_type,
            "medicine_name": medicine_name,
            "quantity": quantity,
            "unit": unit,
            "created_at": datetime.now().isoformat(),
        }
        self._logs.insert(0, log_entry)
        if len(self._logs) > 500:
            self._logs = self._logs[:500]
        self._save_to_file()
        return log_entry

    def get_logs(self, limit: int = 50) -> list[dict]:
        return self._logs[:limit]

    def import_logs(self, logs: list[dict]):
        if not isinstance(logs, list):
            return
        existing_ids = {log.get("id") for log in self._logs}
        for log in logs:
            if isinstance(log, dict) and log.get("id") not in existing_ids:
                self._logs.append(log)
        self._logs.sort(key=lambda x: x.get("created_at", ""), reverse=True)
        if len(self._logs) > 500:
            self._logs = self._logs[:500]
        self._save_to_file()


log_store = LogStore()
