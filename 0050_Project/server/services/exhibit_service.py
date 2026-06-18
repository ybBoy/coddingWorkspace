from typing import List, Optional, Dict, Any
from server.models.exhibit_record import ExhibitRecord
from server.repositories.exhibit_repository import ExhibitRepository


class ExhibitService:
    def __init__(self, repository: ExhibitRepository):
        self._repository = repository

    def get_all_records(self) -> List[ExhibitRecord]:
        return self._repository.get_all()

    def get_filtered_records(self, exhibit_type: Optional[str] = None,
                             location_keyword: Optional[str] = None) -> List[ExhibitRecord]:
        records = self._repository.get_all()

        if exhibit_type:
            records = [r for r in records if r.exhibit_type == exhibit_type]

        if location_keyword:
            keyword = location_keyword.lower()
            records = [r for r in records if keyword in r.location.lower()]

        records.sort(key=lambda r: r.visit_date, reverse=True)
        return records

    def get_record_by_id(self, record_id: str) -> Optional[ExhibitRecord]:
        return self._repository.get_by_id(record_id)

    def add_record(self, name: str, location: str, visit_date: str,
                   exhibit_type: str, rating: int, comment: str) -> ExhibitRecord:
        record = ExhibitRecord.create(
            name=name,
            location=location,
            visit_date=visit_date,
            exhibit_type=exhibit_type,
            rating=rating,
            comment=comment,
        )
        return self._repository.add(record)

    def update_rating(self, record_id: str, rating: int) -> Optional[ExhibitRecord]:
        if rating < 1 or rating > 5:
            raise ValueError("评分必须在 1 到 5 之间")
        return self._repository.update_rating(record_id, rating)

    def delete_record(self, record_id: str) -> bool:
        return self._repository.delete(record_id)

    def get_statistics(self, exhibit_type: Optional[str] = None,
                       location_keyword: Optional[str] = None) -> Dict[str, Any]:
        all_records = self._repository.get_all()
        filtered_records = self.get_filtered_records(exhibit_type, location_keyword)

        total_count = len(all_records)
        filtered_count = len(filtered_records)

        if total_count > 0:
            avg_rating = sum(r.rating for r in all_records) / total_count
        else:
            avg_rating = 0.0

        return {
            "total_count": total_count,
            "average_rating": round(avg_rating, 1),
            "filtered_count": filtered_count,
        }

    def get_all_types(self) -> List[str]:
        records = self._repository.get_all()
        types = sorted(set(r.exhibit_type for r in records))
        return types
