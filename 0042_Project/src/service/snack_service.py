"""
文件职责：
    业务服务层，包含零食库存的核心业务逻辑。
    维护内存中的零食列表，负责新增、查询、修改数量、删除等操作，
    每次操作后通过 snack_file_store 持久化到磁盘。

数据流：
    snack_api（HTTP请求） → snack_service（操作内存列表） → snack_file_store（保存到JSON）
                                                      ↓
    snack_api（HTTP响应） ←  返回结果/列表
"""

from typing import List, Optional

from src.model.snack import Snack
from src.storage.snack_file_store import SnackFileStore


class SnackService:
    """零食业务服务，管理内存中的零食库存并负责持久化"""

    def __init__(self, store: SnackFileStore = None):
        """
        初始化服务，从文件存储加载初始数据
        :param store: 文件存储器实例
        """
        self.store = store or SnackFileStore()
        self._snacks: List[Snack] = self.store.load_all()

    def _persist(self) -> None:
        """将当前内存数据持久化到文件"""
        self.store.save_all(self._snacks)

    def get_all(self, location: Optional[str] = None) -> List[Snack]:
        """
        获取所有零食，可按存放位置筛选"""
        if location and location != "all":
            return [s for s in self._snacks if s.location == location]
        return list(self._snacks)

    def get_by_id(self, snack_id: str) -> Optional[Snack]:
        """根据 ID 查找零食"""
        for snack in self._snacks:
            if snack.id == snack_id:
                return snack
        return None

    def get_locations(self) -> List[str]:
        """获取所有不重复的存放位置"""
        locations = sorted({s.location for s in self._snacks if s.location})
        return locations

    def count_needs_attention(self) -> int:
        """统计需要处理的零食数量（库存不足/临近保质期/已停用）"""
        return sum(1 for s in self._snacks if s.needs_attention())

    def add_snack(self, name: str, flavor: str, quantity: int, location: str, expiry_date: str) -> Snack:
        """
        新增一个零食
        :param name: 名称
        :param flavor: 口味
        :param quantity: 数量
        :param location: 存放位置
        :param expiry_date: 保质期
        :return: 新增的 Snack 对象
        """
        snack = Snack.create(name, flavor, quantity, location, expiry_date)
        self._snacks.append(snack)
        self._persist()
        return snack

    def consume(self, snack_id: str) -> Optional[Snack]:
        """
        吃掉一份，数量减 1（最低为 0）"""
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        snack.quantity = max(0, snack.quantity - 1)
        self._persist()
        return snack

    def restock(self, snack_id: str, amount: int = 1) -> Optional[Snack]:
        """
        补货，数量增加
        :param snack_id: 零食 ID
        :param amount: 增加的数量，默认 1
        """
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        snack.quantity = snack.quantity + max(0, amount)
        self._persist()
        return snack

    def toggle_disabled(self, snack_id: str) -> Optional[Snack]:
        """
        切换零食的"不再购买"标记"""
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        snack.disabled = not snack.disabled
        self._persist()
        return snack

    def delete_snack(self, snack_id: str) -> bool:
        """
        删除零食
        :return: 是否成功删除
        """
        for i, snack in enumerate(self._snacks):
            if snack.id == snack_id:
                del self._snacks[i]
                self._persist()
                return True
        return False
