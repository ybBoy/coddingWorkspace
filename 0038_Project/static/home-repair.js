const API_BASE = '/api/repairs';
let currentFilter = 'all';
let currentTypeFilter = '';
let currentKeyword = '';
let currentEditId = null;
let itemTypes = [];
let searchTimer = null;

document.addEventListener('DOMContentLoaded', function() {
    loadTypes().then(() => {
        populateTypeSelects();
        loadStatistics();
        loadRepairs();
        setupEventListeners();
    });
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

    document.getElementById('typeFilter').addEventListener('change', function() {
        currentTypeFilter = this.value;
        loadRepairs();
    });

    document.getElementById('searchInput').addEventListener('input', function() {
        clearTimeout(searchTimer);
        const val = this.value;
        searchTimer = setTimeout(() => {
            currentKeyword = val.trim();
            loadRepairs();
        }, 200);
    });

    document.getElementById('imageUpload').addEventListener('change', handleImageUpload);

    window.onclick = function(event) {
        const modal = document.getElementById('editModal');
        if (event.target === modal) {
            closeModal();
        }
    };
}

async function loadTypes() {
    try {
        const resp = await fetch(API_BASE + '/types');
        if (resp.ok) {
            itemTypes = await resp.json();
        }
    } catch (e) {
        console.error('加载类型失败', e);
    }
    if (!itemTypes || itemTypes.length === 0) {
        itemTypes = [
            { value: 'APPLIANCE', label: '电器' },
            { value: 'FURNITURE', label: '家具' },
            { value: 'PLUMBING', label: '水电' },
            { value: 'OTHER', label: '其他' }
        ];
    }
}

function populateTypeSelects() {
    const addSelect = document.getElementById('itemType');
    const filterSelect = document.getElementById('typeFilter');
    const editSelect = document.getElementById('editItemType');

    addSelect.innerHTML = itemTypes.map(t => `<option value="${t.value}">${t.label}</option>`).join('');

    filterSelect.innerHTML = '<option value="">全部类型</option>' +
        itemTypes.map(t => `<option value="${t.value}">${t.label}</option>`).join('');

    editSelect.innerHTML = itemTypes.map(t => `<option value="${t.value}">${t.label}</option>`).join('');
}

function getTypeLabel(value) {
    const t = itemTypes.find(x => x.value === value);
    return t ? t.label : (value || '其他');
}

async function loadStatistics() {
    try {
        const response = await fetch(API_BASE + '/statistics');
        const data = await response.json();
        document.getElementById('totalCost').textContent = '¥' + formatMoney(data.totalCost);
        document.getElementById('pendingCount').textContent = data.pendingCount || 0;
        document.getElementById('overdueCount').textContent = data.overdueCount || 0;
        document.getElementById('totalCount').textContent = data.totalCount || 0;
        renderChart('chartByMonth', data.costByMonth || {}, formatMonthLabel);
        renderChart('chartByType', data.costByType || {});
        renderChart('chartByStatus', data.costByStatus || {});
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}

function formatMonthLabel(ym) {
    if (!ym) return ym;
    const parts = ym.split('-');
    if (parts.length >= 2) {
        return parts[0].slice(-2) + '/' + parts[1];
    }
    return ym;
}

function renderChart(containerId, data, labelFormatter) {
    const container = document.getElementById(containerId);
    const entries = Object.entries(data);
    if (entries.length === 0) {
        container.innerHTML = '<div style="color:#AAA;font-size:13px;">暂无数据</div>';
        return;
    }
    let max = 0;
    entries.forEach(([k, v]) => {
        const n = typeof v === 'number' ? v : parseFloat(v) || 0;
        if (n > max) max = n;
    });
    if (max === 0) max = 1;
    container.innerHTML = entries.map(([k, v]) => {
        const n = typeof v === 'number' ? v : parseFloat(v) || 0;
        const pct = Math.round((n / max) * 100);
        const label = labelFormatter ? labelFormatter(k) : k;
        return `
            <div class="chart-row">
                <span class="chart-label">${escapeHtml(label)}</span>
                <div class="chart-bar-bg"><div class="chart-bar" style="width:${pct}%"></div></div>
                <span class="chart-value">¥${formatMoney(n)}</span>
            </div>
        `;
    }).join('');
}

async function loadRepairs() {
    try {
        const params = new URLSearchParams();
        if (currentKeyword) params.append('keyword', currentKeyword);
        if (currentFilter !== 'all') params.append('status', currentFilter);
        if (currentTypeFilter) params.append('type', currentTypeFilter);
        const qs = params.toString();
        const url = qs ? (API_BASE + '/search?' + qs) : API_BASE;
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
                <p>📋 暂无匹配的维修记录</p>
                <p class="empty-tip">尝试修改筛选条件或添加新记录</p>
            </div>
        `;
        return;
    }

    container.innerHTML = items.map(item => {
        const isOverdue = item.overdue === true;
        const overdueDays = item.overdueDays || 0;
        const pendingDays = item.pendingDays || 0;

        const statusClass = item.status === 'PENDING' ? 'pending' :
                           item.status === 'IN_PROGRESS' ? 'in-progress' : 'completed';
        const statusText = item.status === 'PENDING' ? '待处理' :
                          item.status === 'IN_PROGRESS' ? '维修中' : '已完成';
        const badgeClass = item.status === 'PENDING' ? 'status-pending' :
                          item.status === 'IN_PROGRESS' ? 'status-in-progress' : 'status-completed';

        let overdueText = '';
        if (isOverdue && overdueDays > 0) {
            overdueText = `⚠ 已逾期 ${overdueDays} 天`;
        } else if (item.status === 'PENDING' && pendingDays > 0) {
            overdueText = `⏳ 待处理 ${pendingDays} 天`;
        }

        const imagesHtml = renderCardImages(item.images);

        return `
            <div class="repair-card ${statusClass} ${isOverdue ? 'overdue' : ''}"
                 data-id="${item.id}"
                 data-overdue-text="${overdueText || ' '}">
                <div class="card-header">
                    <div class="card-title-wrap">
                        <span class="card-title">
                            <span class="type-badge">${escapeHtml(getTypeLabel(item.itemType))}</span>
                            ${escapeHtml(item.itemName)}
                        </span>
                    </div>
                    <span class="status-badge ${badgeClass}">${statusText}</span>
                </div>
                <div class="card-body">
                    <p class="card-description">${escapeHtml(item.problemDescription)}</p>
                    <div class="card-meta">
                        <span class="meta-item">📅 ${formatDate(item.reportDate)}</span>
                        <span class="meta-item meta-cost">💰 ¥${formatMoney(item.cost)}</span>
                        ${item.status === 'PENDING' ? `<span class="meta-item">⏱ 待处理 ${pendingDays} 天</span>` : ''}
                    </div>
                    ${imagesHtml}
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
            const id = this.closest('.repair-card').dataset.id;
            openEditModal(id);
        });
    });

    container.querySelectorAll('.btn-danger').forEach(btn => {
        btn.addEventListener('click', function() {
            const id = this.closest('.repair-card').dataset.id;
            deleteItem(id);
        });
    });

    container.querySelectorAll('.card-thumb, .card-more-img').forEach(el => {
        el.addEventListener('click', function() {
            if (this.dataset.src) {
                openLightbox(this.dataset.src);
            }
        });
    });
}

function renderCardImages(images) {
    if (!images || images.length === 0) return '';
    const shown = images.slice(0, 3);
    const more = images.length - shown.length;
    let html = '<div class="card-images">';
    shown.forEach(img => {
        html += `<img class="card-thumb" src="${escapeHtml(img.filePath)}" alt="${escapeHtml(img.fileName)}" data-src="${escapeHtml(img.filePath)}">`;
    });
    if (more > 0) {
        html += `<div class="card-more-img">+${more}</div>`;
    }
    html += '</div>';
    return html;
}

async function handleSubmit(event) {
    event.preventDefault();

    const formData = {
        itemName: document.getElementById('itemName').value.trim(),
        problemDescription: document.getElementById('problemDescription').value.trim(),
        cost: parseFloat(document.getElementById('cost').value) || 0,
        remark: document.getElementById('remark').value.trim(),
        itemType: document.getElementById('itemType').value || 'OTHER'
    };

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            document.getElementById('repairForm').reset();
            document.getElementById('itemType').value = 'OTHER';
            loadRepairs();
            loadStatistics();
        } else {
            const err = await response.json().catch(() => ({}));
            alert('添加失败：' + (err.error || '请重试'));
        }
    } catch (error) {
        console.error('添加记录失败:', error);
        alert('添加失败，请确保后端服务已启动');
    }
}

async function openEditModal(id) {
    currentEditId = id;
    try {
        const resp = await fetch(API_BASE + '/' + id);
        if (!resp.ok) throw new Error('加载失败');
        const item = await resp.json();

        document.getElementById('editItemName').value = item.itemName || '';
        document.getElementById('editProblemDescription').value = item.problemDescription || '';
        document.getElementById('editCost').value = item.cost || 0;
        document.getElementById('editItemType').value = item.itemType || 'OTHER';
        document.getElementById('editStatus').value = item.status || 'PENDING';
        document.getElementById('editRemark').value = item.remark || '';
        document.getElementById('imageUpload').value = '';

        renderImageList(item.images || []);
        renderHistory(item.history || []);

        document.getElementById('editModal').classList.remove('hidden');
    } catch (e) {
        console.error(e);
        alert('加载记录详情失败');
    }
}

function renderImageList(images) {
    const container = document.getElementById('imageList');
    if (!images || images.length === 0) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = images.map(img => `
        <div class="img-wrapper" data-image-id="${img.id}">
            <img src="${escapeHtml(img.filePath)}" alt="${escapeHtml(img.fileName)}" data-src="${escapeHtml(img.filePath)}">
            <button class="img-del" title="删除图片" onclick="deleteImage('${img.id}', event)">×</button>
        </div>
    `).join('');

    container.querySelectorAll('img').forEach(img => {
        img.addEventListener('click', function() {
            if (this.dataset.src) openLightbox(this.dataset.src);
        });
    });
}

function renderHistory(history) {
    const container = document.getElementById('historyTimeline');
    if (!history || history.length === 0) {
        container.innerHTML = '';
        return;
    }
    const sorted = [...history].sort((a, b) => {
        return new Date(a.timestamp) - new Date(b.timestamp);
    }).reverse();
    const actionLabels = {
        'CREATED': '📝 创建记录',
        'STATUS_CHANGED': '🔄 状态变更',
        'REMARK_UPDATED': '📝 备注修改',
        'ITEM_NAME_CHANGED': '✏️ 物品名称',
        'ITEM_TYPE_CHANGED': '🏷️ 物品类型',
        'COST_CHANGED': '💰 费用调整',
        'DESCRIPTION_CHANGED': '📋 问题描述',
        'IMAGE_ADDED': '📷 图片上传',
        'IMAGE_REMOVED': '🗑 图片删除'
    };
    container.innerHTML = sorted.map(h => {
        const action = actionLabels[h.action] || h.action;
        let detail = '';
        if (h.action === 'STATUS_CHANGED' || h.action === 'REMARK_UPDATED'
                || h.action === 'ITEM_NAME_CHANGED' || h.action === 'ITEM_TYPE_CHANGED'
                || h.action === 'COST_CHANGED' || h.action === 'DESCRIPTION_CHANGED') {
            detail = `「${escapeHtml(h.oldValue || '空')}」 → 「${escapeHtml(h.newValue || '空')}」`;
        } else if (h.action === 'IMAGE_ADDED') {
            detail = `上传图片：${escapeHtml(h.newValue || '')}`;
            if (h.remark) detail += ` - ${escapeHtml(h.remark)}`;
        } else if (h.action === 'IMAGE_REMOVED') {
            detail = '删除图片';
        } else if (h.action === 'CREATED') {
            detail = escapeHtml(h.remark || '');
        } else if (h.remark) {
            detail = escapeHtml(h.remark);
        }
        return `
            <div class="timeline-item">
                <div class="timeline-time">${formatDateTime(h.timestamp)}</div>
                <span class="timeline-action">${action}</span>
                <span class="timeline-detail">${detail}</span>
            </div>
        `;
    }).join('');
}

async function handleImageUpload(event) {
    const files = event.target.files;
    if (!files || !files.length || !currentEditId) return;

    for (const file of files) {
        const formData = new FormData();
        formData.append('file', file);
        try {
            const resp = await fetch(API_BASE + '/' + currentEditId + '/images', {
                method: 'POST',
                body: formData
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                alert('图片上传失败：' + (err.error || file.name));
            }
        } catch (e) {
            console.error(e);
            alert('图片上传失败：' + file.name);
        }
    }

    event.target.value = '';

    const resp = await fetch(API_BASE + '/' + currentEditId);
    if (resp.ok) {
        const item = await resp.json();
        renderImageList(item.images || []);
        renderHistory(item.history || []);
    }
    loadStatistics();
}

async function deleteImage(imageId, event) {
    if (event) event.stopPropagation();
    if (!currentEditId) return;
    if (!confirm('确定删除该图片？')) return;

    try {
        const resp = await fetch(API_BASE + '/' + currentEditId + '/images/' + imageId, {
            method: 'DELETE'
        });
        if (resp.ok) {
            const r = await fetch(API_BASE + '/' + currentEditId);
            if (r.ok) {
                const item = await r.json();
                renderImageList(item.images || []);
                renderHistory(item.history || []);
            }
            loadStatistics();
        } else {
            alert('删除失败');
        }
    } catch (e) {
        console.error(e);
        alert('删除失败');
    }
}

function closeModal() {
    document.getElementById('editModal').classList.add('hidden');
    currentEditId = null;
}

async function saveEdit() {
    if (!currentEditId) return;

    const updates = {
        itemName: document.getElementById('editItemName').value.trim(),
        problemDescription: document.getElementById('editProblemDescription').value.trim(),
        cost: parseFloat(document.getElementById('editCost').value) || 0,
        itemType: document.getElementById('editItemType').value || 'OTHER',
        status: document.getElementById('editStatus').value,
        remark: document.getElementById('editRemark').value.trim()
    };

    try {
        const response = await fetch(API_BASE + '/' + currentEditId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });

        if (response.ok) {
            closeModal();
            loadRepairs();
            loadStatistics();
        } else {
            const err = await response.json().catch(() => ({}));
            alert('更新失败：' + (err.error || '请重试'));
        }
    } catch (error) {
        console.error('更新记录失败:', error);
        alert('更新失败，请确保后端服务已启动');
    }
}

async function deleteItem(id) {
    if (!confirm('确定要删除这条记录吗？')) return;

    try {
        const response = await fetch(API_BASE + '/' + id, { method: 'DELETE' });
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

function exportCsv() {
    window.location.href = API_BASE + '/export/csv';
}

function exportJson() {
    window.location.href = API_BASE + '/export/json';
}

function openLightbox(src) {
    if (!src) return;
    document.getElementById('lightboxImg').src = src;
    document.getElementById('lightbox').classList.remove('hidden');
}

function closeLightbox() {
    document.getElementById('lightbox').classList.add('hidden');
    document.getElementById('lightboxImg').src = '';
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function formatDateTime(dtStr) {
    if (!dtStr) return '';
    const d = new Date(dtStr);
    if (isNaN(d.getTime())) return dtStr;
    const y = d.getFullYear();
    const mo = String(d.getMonth() + 1).padStart(2, '0');
    const da = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${mo}-${da} ${h}:${mi}`;
}

function formatMoney(value) {
    if (value === null || value === undefined || value === '') return '0.00';
    const num = typeof value === 'number' ? value : parseFloat(value);
    if (isNaN(num)) return '0.00';
    return num.toFixed(2);
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
