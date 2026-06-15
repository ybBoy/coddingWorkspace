const API_BASE = '/api/repairs';
let currentFilter = 'all';
let currentEditId = null;

document.addEventListener('DOMContentLoaded', function() {
    loadStatistics();
    loadRepairs();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('repairForm').addEventListener('submit', handleSubmit);
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.dataset.status;
            loadRepairs();
        });
    });
}

async function loadStatistics() {
    try {
        const response = await fetch(API_BASE + '/statistics');
        const data = await response.json();
        document.getElementById('totalCost').textContent = '¥' + formatMoney(data.totalCost);
        document.getElementById('pendingCount').textContent = data.pendingCount;
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}

async function loadRepairs() {
    try {
        let url = API_BASE;
        if (currentFilter !== 'all') {
            url = API_BASE + '/filter?status=' + currentFilter;
        }
        const response = await fetch(url);
        const data = await response.json();
        renderRepairs(data);
    } catch (error) {
        console.error('加载维修记录失败:', error);
        document.getElementById('repairList').innerHTML = 
            '<div class="empty-state"><p>❌ 加载失败</p><p class="empty-tip">请确保后端服务已启动</p></div>';
    }
}

function renderRepairs(items) {
    const container = document.getElementById('repairList');
    
    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <p>📋 暂无维修记录</p>
                <p class="empty-tip">添加第一条记录开始使用吧！</p>
            </div>
        `;
        return;
    }

    container.innerHTML = items.map(item => {
        const isOverdue = item.overdue === true;
        
        const statusClass = item.status === 'PENDING' ? 'pending' : 
                           item.status === 'IN_PROGRESS' ? 'in-progress' : 'completed';
        const statusText = item.status === 'PENDING' ? '待处理' : 
                          item.status === 'IN_PROGRESS' ? '维修中' : '已完成';
        const badgeClass = item.status === 'PENDING' ? 'status-pending' : 
                          item.status === 'IN_PROGRESS' ? 'status-in-progress' : 'status-completed';

        return `
            <div class="repair-card ${statusClass} ${isOverdue ? 'overdue' : ''}" data-id="${item.id}" data-status="${item.status}" data-remark="${escapeHtml(item.remark || '')}">
                <div class="card-header">
                    <span class="card-title">${escapeHtml(item.itemName)}</span>
                    <span class="status-badge ${badgeClass}">${statusText}</span>
                </div>
                <div class="card-body">
                    <p class="card-description">${escapeHtml(item.problemDescription)}</p>
                    <div class="card-meta">
                        <span class="meta-item">📅 ${formatDate(item.reportDate)}</span>
                        <span class="meta-item meta-cost">💰 ¥${formatMoney(item.cost)}</span>
                    </div>
                    ${item.remark ? `<div class="card-remark">📝 ${escapeHtml(item.remark)}</div>` : ''}
                </div>
                <div class="card-footer">
                    <button class="btn btn-edit" data-action="edit">编辑</button>
                    <button class="btn btn-danger" data-action="delete">删除</button>
                </div>
            </div>
        `;
    }).join('');

    container.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', function() {
            const card = this.closest('.repair-card');
            const id = card.dataset.id;
            const status = card.dataset.status;
            const remark = card.dataset.remark;
            openEditModal(id, status, remark);
        });
    });

    container.querySelectorAll('.btn-danger').forEach(btn => {
        btn.addEventListener('click', function() {
            const card = this.closest('.repair-card');
            const id = card.dataset.id;
            deleteItem(id);
        });
    });
}

async function handleSubmit(event) {
    event.preventDefault();
    
    const formData = {
        itemName: document.getElementById('itemName').value.trim(),
        problemDescription: document.getElementById('problemDescription').value.trim(),
        cost: parseFloat(document.getElementById('cost').value) || 0,
        remark: document.getElementById('remark').value.trim()
    };

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            document.getElementById('repairForm').reset();
            loadRepairs();
            loadStatistics();
        } else {
            alert('添加失败，请重试');
        }
    } catch (error) {
        console.error('添加记录失败:', error);
        alert('添加失败，请确保后端服务已启动');
    }
}

function openEditModal(id, status, remark) {
    currentEditId = id;
    document.getElementById('editStatus').value = status;
    document.getElementById('editRemark').value = remark || '';
    document.getElementById('editModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('editModal').classList.add('hidden');
    currentEditId = null;
}

async function saveEdit() {
    if (!currentEditId) return;

    const updates = {
        status: document.getElementById('editStatus').value,
        remark: document.getElementById('editRemark').value.trim()
    };

    try {
        const response = await fetch(API_BASE + '/' + currentEditId, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updates)
        });

        if (response.ok) {
            closeModal();
            loadRepairs();
            loadStatistics();
        } else {
            alert('更新失败，请重试');
        }
    } catch (error) {
        console.error('更新记录失败:', error);
        alert('更新失败，请确保后端服务已启动');
    }
}

async function deleteItem(id) {
    if (!confirm('确定要删除这条记录吗？')) return;

    try {
        const response = await fetch(API_BASE + '/' + id, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadRepairs();
            loadStatistics();
        } else {
            alert('删除失败，请重试');
        }
    } catch (error) {
        console.error('删除记录失败:', error);
        alert('删除失败，请确保后端服务已启动');
    }
}

function formatDate(dateStr) {
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function formatMoney(value) {
    if (value === null || value === undefined) return '0.00';
    const num = typeof value === 'number' ? value : parseFloat(value);
    return num.toFixed(2);
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

window.onclick = function(event) {
    const modal = document.getElementById('editModal');
    if (event.target === modal) {
        closeModal();
    }
}
