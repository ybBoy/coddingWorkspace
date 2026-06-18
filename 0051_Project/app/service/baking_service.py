from typing import List, Optional
from datetime import datetime
from app.model.baking_trial import BakingTrial, SUCCESS_LEVELS
from app.storage.baking_store import BakingStore


class BakingService:
    def __init__(self, store: BakingStore):
        self.store = store

    def get_trials(
        self,
        dessert_type: Optional[str] = None,
        search: Optional[str] = None
    ) -> List[BakingTrial]:
        trials = self.store.get_all()

        if dessert_type and dessert_type != "全部":
            trials = [t for t in trials if t.dessert_type == dessert_type]

        if search:
            search_lower = search.lower()
            trials = [
                t for t in trials
                if search_lower in t.dessert_name.lower()
            ]

        trials.sort(key=lambda t: t.created_at, reverse=True)
        return trials

    def get_trial_by_id(self, trial_id: int) -> Optional[BakingTrial]:
        return self.store.get_by_id(trial_id)

    def create_trial(self, data: dict) -> BakingTrial:
        trial = BakingTrial.from_dict(data)
        trial.created_at = datetime.now().isoformat()
        if trial.success_level not in SUCCESS_LEVELS:
            trial.success_level = "一般"
        return self.store.add(trial)

    def update_success_level(self, trial_id: int, success_level: str) -> Optional[BakingTrial]:
        if success_level not in SUCCESS_LEVELS:
            success_level = "一般"
        return self.store.update(trial_id, {"success_level": success_level})

    def delete_trial(self, trial_id: int) -> bool:
        return self.store.delete(trial_id)

    def get_statistics(self, filtered_count: Optional[int] = None) -> dict:
        all_trials = self.store.get_all()
        total = len(all_trials)

        good_levels = {"良好", "优秀", "完美"}
        success_count = sum(1 for t in all_trials if t.success_level in good_levels)
        success_rate = round((success_count / total * 100), 1) if total > 0 else 0.0

        return {
            "total": total,
            "success_rate": success_rate,
            "filtered_count": filtered_count if filtered_count is not None else total
        }
