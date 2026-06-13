import type { Role } from './types'

/**
 * 简易角色切换器（前端模拟权限）
 *   - 实际生产中应该走后端登录态 / JWT；这里简化成前端 localStorage 模拟
 *   - 三种角色：OWNER / FRONT / KITCHEN
 *
 * 权限边界：
 *   - FRONT（前台）：只能下单/改单/撤单，不能操作菜品制作，看不到分析导出
 *   - KITCHEN（后厨）：只能在看板操作（开始制作/完成/重做），不能进入下单页、不能撤单
 *   - OWNER（店主）：所有操作都允许 + 能看历史归档、菜品分析、CSV 导出
 */
interface Props {
  role: Role
  onChange: (r: Role) => void
}

const ROLES: { k: Role; label: string; icon: string; hint: string }[] = [
  { k: 'OWNER',   label: '店主',   icon: '👑', hint: '全部权限 + 统计/分析/导出' },
  { k: 'FRONT',   label: '前台',   icon: '📝', hint: '下单/改单/撤单/打印' },
  { k: 'KITCHEN', label: '后厨',   icon: '👨‍🍳', hint: '接单/完成/重做/打印' },
]

export default function RoleSelector({ role, onChange }: Props) {
  return (
    <div className="role-selector" title="点击切换当前登录角色（演示）">
      {ROLES.map((r) => (
        <button
          key={r.k}
          className={`role-chip ${role === r.k ? 'active' : ''}`}
          onClick={() => onChange(r.k)}
          title={r.hint}
        >
          <span style={{ fontSize: 16 }}>{r.icon}</span>
          <span style={{ marginLeft: 4 }}>{r.label}</span>
        </button>
      ))}
    </div>
  )
}
