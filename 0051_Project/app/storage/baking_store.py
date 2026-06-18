import json
import os
from typing import List, Optional
from app.model.baking_trial import BakingTrial


class BakingStore:
    def __init__(self, data_file: str = "baking_trials.json"):
        self.data_file = data_file
        self._trials: List[BakingTrial] = []
        self._next_id: int = 1
        self._load_from_file()

    def _load_from_file(self):
        if os.path.exists(self.data_file):
            try:
                with open(self.data_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    for item in data:
                        trial = BakingTrial.from_dict(item)
                        self._trials.append(trial)
                        if trial.id is not None and trial.id >= self._next_id:
                            self._next_id = trial.id + 1
            except (json.JSONDecodeError, IOError):
                self._trials = []
                self._next_id = 1

    def _save_to_file(self):
        try:
            data = [t.to_dict() for t in self._trials]
            with open(self.data_file, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except IOError:
            pass

    def get_all(self) -> List[BakingTrial]:
        return list(self._trials)

    def get_by_id(self, trial_id: int) -> Optional[BakingTrial]:
        for t in self._trials:
            if t.id == trial_id:
                return t
        return None

    def add(self, trial: BakingTrial) -> BakingTrial:
        trial.id = self._next_id
        self._next_id += 1
        self._trials.append(trial)
        self._save_to_file()
        return trial

    def update(self, trial_id: int, updates: dict) -> Optional[BakingTrial]:
        trial = self.get_by_id(trial_id)
        if trial is None:
            return None
        for key, value in updates.items():
            if hasattr(trial, key) and key != "id":
                setattr(trial, key, value)
        self._save_to_file()
        return trial

    def delete(self, trial_id: int) -> bool:
        for i, t in enumerate(self._trials):
            if t.id == trial_id:
                self._trials.pop(i)
                self._save_to_file()
                return True
        return False
