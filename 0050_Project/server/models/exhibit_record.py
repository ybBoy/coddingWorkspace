from dataclasses import dataclass, field, asdict
from datetime import date
import uuid


@dataclass
class ExhibitRecord:
    id: str
    name: str
    location: str
    visit_date: str
    exhibit_type: str
    rating: int
    comment: str

    @classmethod
    def create(cls, name: str, location: str, visit_date: str, exhibit_type: str,
               rating: int, comment: str) -> "ExhibitRecord":
        return cls(
            id=str(uuid.uuid4()),
            name=name,
            location=location,
            visit_date=visit_date,
            exhibit_type=exhibit_type,
            rating=rating,
            comment=comment,
        )

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "ExhibitRecord":
        return cls(**data)
