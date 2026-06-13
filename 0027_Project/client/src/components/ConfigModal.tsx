import { useState, useEffect } from 'react'
import type { Booth } from '../core/socket'
import { socket } from '../core/socket'

interface ConfigModalProps {
  open: boolean
  onClose: () => void
  allBooths: Booth[]
  projects: string[]
}

type Tab = 'booths' | 'projects'

function ConfigModal({ open, onClose, allBooths, projects }: ConfigModalProps) {
  const [tab, setTab] = useState<Tab>('booths')

  const [newBoothName, setNewBoothName] = useState('')
  const [newBoothDesc, setNewBoothDesc] = useState('')

  const [editingBooth, setEditingBooth] = useState<Booth | null>(null)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')

  const [newProjectName, setNewProjectName] = useState('')

  const [editingProject, setEditingProject] = useState<string | null>(null)
  const [editProjectName, setEditProjectName] = useState('')

  useEffect(() => {
    if (editingBooth) {
      setEditName(editingBooth.name)
      setEditDesc(editingBooth.description)
    }
  }, [editingBooth])

  useEffect(() => {
    if (editingProject) {
      setEditProjectName(editingProject)
    }
  }, [editingProject])

  if (!open) return null

  const handleAddBooth = () => {
    if (!newBoothName.trim()) return
    socket.addBooth(newBoothName.trim(), newBoothDesc.trim())
    setNewBoothName('')
    setNewBoothDesc('')
  }

  const handleSaveBooth = () => {
    if (!editingBooth) return
    socket.updateBooth(editingBooth.id, {
      name: editName.trim(),
      description: editDesc.trim(),
    })
    setEditingBooth(null)
  }

  const handleToggleDisabled = (booth: Booth) => {
    socket.updateBooth(booth.id, { disabled: !booth.disabled })
  }

  const handleDeleteBooth = (id: string) => {
    if (window.confirm('确定删除该展位吗？历史签到记录会保留但不再展示该展位信息。')) {
      socket.deleteBooth(id)
    }
  }

  const handleAddProject = () => {
    if (!newProjectName.trim()) return
    socket.addProject(newProjectName.trim())
    setNewProjectName('')
  }

  const handleSaveProject = () => {
    if (!editingProject || !editProjectName.trim()) return
    socket.updateProject(editingProject, editProjectName.trim())
    setEditingProject(null)
  }

  const handleDeleteProject = (name: string) => {
    if (window.confirm(`确定删除项目"${name}"吗？历史记录中的该项目也会被移除。`)) {
      socket.deleteProject(name)
    }
  }

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <div style={headerStyle}>
          <h2 style={{ margin: 0 }}>配置管理</h2>
          <button className="btn-secondary" onClick={onClose} style={{ padding: '6px 14px' }}>
            关闭
          </button>
        </div>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <button
            className={`range-btn ${tab === 'booths' ? 'active' : ''}`}
            onClick={() => setTab('booths')}
          >
            展位管理
          </button>
          <button
            className={`range-btn ${tab === 'projects' ? 'active' : ''}`}
            onClick={() => setTab('projects')}
          >
            项目管理
          </button>
        </div>

        {tab === 'booths' && (
          <div>
            <div className="card" style={{ marginBottom: '16px', padding: '16px' }}>
              <h3 style={{ fontSize: '15px', marginBottom: '12px' }}>新增展位</h3>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                <input
                  className="form-input"
                  style={{ flex: '1 1 180px', minWidth: '180px' }}
                  placeholder="展位名称 *"
                  value={newBoothName}
                  onChange={(e) => setNewBoothName(e.target.value)}
                />
                <input
                  className="form-input"
                  style={{ flex: '2 1 280px', minWidth: '220px' }}
                  placeholder="展位描述（可选）"
                  value={newBoothDesc}
                  onChange={(e) => setNewBoothDesc(e.target.value)}
                />
                <button
                  className="btn-primary"
                  style={{ width: 'auto', padding: '10px 18px' }}
                  onClick={handleAddBooth}
                  disabled={!newBoothName.trim()}
                >
                  新增
                </button>
              </div>
            </div>

            <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>ID</th>
                    <th style={thStyle}>名称</th>
                    <th style={thStyle}>描述</th>
                    <th style={thStyle}>状态</th>
                    <th style={thStyle}>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {allBooths.map((booth) => (
                    <tr key={booth.id}>
                      <td style={tdStyle}>{booth.id}</td>
                      <td style={tdStyle}>
                        {editingBooth?.id === booth.id ? (
                          <input
                            className="form-input"
                            value={editName}
                            onChange={(e) => setEditName(e.target.value)}
                          />
                        ) : (
                          <span style={{ fontWeight: 500 }}>
                            {booth.name}
                            {booth.disabled && (
                              <span
                                style={{
                                  marginLeft: '6px',
                                  color: '#9ca3af',
                                  fontSize: '12px',
                                }}
                              >
                                (已停用)
                              </span>
                            )}
                          </span>
                        )}
                      </td>
                      <td style={tdStyle}>
                        {editingBooth?.id === booth.id ? (
                          <input
                            className="form-input"
                            value={editDesc}
                            onChange={(e) => setEditDesc(e.target.value)}
                          />
                        ) : (
                          <span style={{ color: '#6b7280', fontSize: '13px' }}>
                            {booth.description || '-'}
                          </span>
                        )}
                      </td>
                      <td style={tdStyle}>
                        <button
                          className={`range-btn ${!booth.disabled ? 'active' : ''}`}
                          style={{ fontSize: '12px', padding: '4px 10px' }}
                          onClick={() => handleToggleDisabled(booth)}
                        >
                          {booth.disabled ? '已停用' : '启用中'}
                        </button>
                      </td>
                      <td style={tdStyle}>
                        {editingBooth?.id === booth.id ? (
                          <>
                            <button
                              className="btn-primary"
                              style={{
                                width: 'auto',
                                padding: '5px 12px',
                                fontSize: '13px',
                                marginRight: '6px',
                              }}
                              onClick={handleSaveBooth}
                            >
                              保存
                            </button>
                            <button
                              className="btn-secondary"
                              style={{
                                padding: '5px 12px',
                                fontSize: '13px',
                              }}
                              onClick={() => setEditingBooth(null)}
                            >
                              取消
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              className="btn-secondary"
                              style={{
                                padding: '5px 12px',
                                fontSize: '13px',
                                marginRight: '6px',
                              }}
                              onClick={() => setEditingBooth(booth)}
                            >
                              编辑
                            </button>
                            <button
                              className="btn-secondary"
                              style={{
                                padding: '5px 12px',
                                fontSize: '13px',
                                color: '#dc2626',
                                borderColor: '#fecaca',
                              }}
                              onClick={() => handleDeleteBooth(booth.id)}
                            >
                              删除
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {tab === 'projects' && (
          <div>
            <div className="card" style={{ marginBottom: '16px', padding: '16px' }}>
              <h3 style={{ fontSize: '15px', marginBottom: '12px' }}>新增项目</h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  className="form-input"
                  style={{ flex: 1 }}
                  placeholder="项目名称 *"
                  value={newProjectName}
                  onChange={(e) => setNewProjectName(e.target.value)}
                />
                <button
                  className="btn-primary"
                  style={{ width: 'auto', padding: '10px 18px' }}
                  onClick={handleAddProject}
                  disabled={!newProjectName.trim()}
                >
                  新增
                </button>
              </div>
            </div>

            <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>项目名称</th>
                    <th style={{ ...thStyle, width: '200px' }}>
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {projects.map((name) => (
                    <tr key={name}>
                      <td style={tdStyle}>
                        {editingProject === name ? (
                          <input
                            className="form-input"
                            value={editProjectName}
                            onChange={(e) => setEditProjectName(e.target.value)}
                          />
                        ) : (
                          <span style={{ fontWeight: 500 }}>{name}</span>
                        )}
                      </td>
                      <td style={tdStyle}>
                        {editingProject === name ? (
                          <>
                            <button
                              className="btn-primary"
                              style={{
                                width: 'auto',
                                padding: '5px 12px',
                                fontSize: '13px',
                                marginRight: '6px',
                              }}
                              onClick={handleSaveProject}
                            >
                              保存
                            </button>
                            <button
                              className="btn-secondary"
                              style={{ padding: '5px 12px', fontSize: '13px' }}
                              onClick={() => setEditingProject(null)}
                            >
                              取消
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              className="btn-secondary"
                              style={{
                                padding: '5px 12px',
                                fontSize: '13px',
                                marginRight: '6px',
                              }}
                              onClick={() => setEditingProject(name)}
                            >
                              编辑
                            </button>
                            <button
                              className="btn-secondary"
                              style={{
                                padding: '5px 12px',
                                fontSize: '13px',
                                color: '#dc2626',
                                borderColor: '#fecaca',
                              }}
                              onClick={() => handleDeleteProject(name)}
                            >
                              删除
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

const overlayStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: 'rgba(0,0,0,0.5)',
  zIndex: 1000,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '20px',
}

const modalStyle: React.CSSProperties = {
  background: '#fff',
  borderRadius: '12px',
  width: '100%',
  maxWidth: '760px',
  maxHeight: '90vh',
  overflow: 'auto',
  padding: '24px',
  boxShadow: '0 12px 40px rgba(0,0,0,0.2)',
}

const headerStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginBottom: '20px',
}

const tableStyle: React.CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: '14px',
}

const thStyle: React.CSSProperties = {
  textAlign: 'left',
  padding: '10px 12px',
  background: '#f8fafc',
  borderBottom: '1px solid #e5e7eb',
  fontWeight: 600,
  fontSize: '13px',
  color: '#374151',
}

const tdStyle: React.CSSProperties = {
  padding: '10px 12px',
  borderBottom: '1px solid #f1f5f9',
  verticalAlign: 'middle',
}

export default ConfigModal
