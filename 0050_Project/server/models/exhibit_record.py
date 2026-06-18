from dataclasses import dataclass, field, asdict
from datetime import date
import uuid


@dataclass
class ExhibitRecord:
    MIN_RATING = 1
    MAX_RATING = 5

    id: str
    name: str
    location: str
    visit_date: str
    exhibit_type: str
    rating: int
    comment: str

    def __post_init__(self) -> None:
        try:
            self.rating = int(self.rating)
        except (ValueError, TypeError):
            self.rating = self.MIN_RATING
        if self.rating < self.MIN_RATING:
            self.rating = self.MIN_RATING
        if self.rating > self.MAX_RATING:
            self.rating = self.MAX_RATING

    @classmethod
    def validate_rating(cls, rating: int) -> int:
        try:
            rating = int(rating)
        except (ValueError, TypeError):
            raise ValueError("评分必须是整数")
        if rating < cls.MIN_RATING or rating > cls.MAX_RATING:
            raise ValueError(f"评分必须在 {cls.MIN_RATING} 到 {cls.MAX_RATING} 之间")
        return rating

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
