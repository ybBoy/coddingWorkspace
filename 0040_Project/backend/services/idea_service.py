"""
业务服务层：处理灵感的核心业务逻辑
包含增删改查、搜索筛选、收藏切换、统计信息等功能
内存数据变更后会自动持久化到 JSON 文件
"""
from datetime import datetime
from typing import List, Optional

from backend.models.idea import Idea
from backend.storage.json_store import JsonStore


class IdeaService:
    """灵感业务服务"""

    def __init__(self, store: Optional[JsonStore] = None):
        self.store = store or JsonStore()
        self._ideas: List[Idea] = self.store.load_all()

    def _persist(self) -> None:
        """将内存数据保存到 JSON 文件"""
        self.store.save_all(self._ideas)

    def get_all(self) -> List[Idea]:
        """获取所有灵感，按创建时间倒序"""
        return sorted(self._ideas, key=lambda x: x.created_at, reverse=True)

    def get_by_id(self, idea_id: str) -> Optional[Idea]:
        """根据 ID 获取单个灵感"""
        for idea in self._ideas:
            if idea.id == idea_id:
                return idea
        return None

    def create(self, title: str, content: str, tags: Optional[List[str]] = None,
               source: str = "") -> Idea:
        """新增一条灵感"""
        idea = Idea(
            title=title,
            content=content,
            tags=tags or [],
            source=source,
        )
        self._ideas.append(idea)
        self._persist()
        return idea

    def update(self, idea_id: str, title: Optional[str] = None,
               content: Optional[str] = None, tags: Optional[List[str]] = None,
               source: Optional[str] = None) -> Optional[Idea]:
        """更新灵感信息"""
        idea = self.get_by_id(idea_id)
        if not idea:
            return None
        if title is not None:
            idea.title = title
        if content is not None:
            idea.content = content
        if tags is not None:
            idea.tags = tags
        if source is not None:
            idea.source = source
        idea.updated_at = datetime.now().isoformat()
        self._persist()
        return idea

    def delete(self, idea_id: str) -> bool:
        """删除一条灵感"""
        idea = self.get_by_id(idea_id)
        if not idea:
            return False
        self._ideas.remove(idea)
        self._persist()
        return True

    def toggle_favorite(self, idea_id: str) -> Optional[Idea]:
        """切换收藏状态"""
        idea = self.get_by_id(idea_id)
        if not idea:
            return None
        idea.is_favorite = not idea.is_favorite
        idea.updated_at = datetime.now().isoformat()
        self._persist()
        return idea

    def search(self, keyword: str = "", tag: str = "",
               only_favorite: bool = False) -> List[Idea]:
        """
        搜索和筛选灵感
        - keyword: 匹配标题或内容
        - tag: 按标签筛选
        - only_favorite: 只看收藏
        """
        result = self.get_all()
        if keyword:
            kw = keyword.lower()
            result = [i for i in result
                      if kw in i.title.lower() or kw in i.content.lower()]
        if tag:
            result = [i for i in result if tag in i.tags]
        if only_favorite:
            result = [i for i in result if i.is_favorite]
        return result

    def get_all_tags(self) -> List[str]:
        """获取所有不重复的标签"""
        tags_set = set()
        for idea in self._ideas:
            for tag in idea.tags:
                tags_set.add(tag)
        return sorted(list(tags_set))

    def get_stats(self) -> dict:
        """获取统计信息：总数、收藏数"""
        total = len(self._ideas)
        favorites = sum(1 for i in self._ideas if i.is_favorite)
        return {
            "total": total,
            "favorites": favorites,
        }
