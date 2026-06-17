const API_BASE = '/api';

let currentClothes = [];
let allClothes = [];
let currentFilters = { type: 'all', color: 'all', season: 'all' };
let selectedOutfitIds = [];
let currentRecommendation = null;

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function safeImageUrl(url) {
    if (!url) return '';
    const str = String(url).trim();
    if (str.startsWith('http://') || str.startsWith('https://') || str.startsWith('/') || str.startsWith('data:image/')) {
        return str;
    }
    return '';
}

async function apiFetch(endpoint, options = {}) {
    try {
        const res = await fetch(API_BASE + endpoint, {
            headers: { 'Content-Type': 'application/json', ...options.headers },
            ...options
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || data.message || '请求失败');
        }
        return data;
    } catch (err) {
        showToast(err.message, 'error');
        throw err;
    }
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    setTimeout(() => {
        toast.className = 'toast';
    }, 2500);
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '—';
    const month = d.getMonth() + 1;
    const day = d.getDate();
    return `${month}月${day}日`;
}

function formatDateFull(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '—';
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function getTypeEmoji(type) {
    const map = {
        '上衣': '👕', 't恤': '👕', 'T恤': '👕', '衬衫': '👔',
        '裤子': '👖', '牛仔裤': '👖', '短裤': '🩳',
        '外套': '🧥', '大衣': '🧥', '风衣': '🧥',
        '裙子': '👗', '连衣裙': '👗',
        '鞋子': '👟', '运动鞋': '👟', '皮鞋': '👞',
        '袜子': '🧦', '帽子': '🎩', '包': '👜'
    };
    return map[type] || '👚';
}

function getColorHex(color) {
    const map = {
        '白色': '#f5f5f5', '黑色': '#2c2c2c', '灰色': '#8c8c8c',
        '红色': '#e74c3c', '蓝色': '#3498db', '绿色': '#27ae60',
        '黄色': '#f1c40f', '紫色': '#9b59b6', '粉色': '#ff9f9f',
        '棕色': '#a0522d', '米色': '#f5deb3', '牛仔蓝': '#4a6fa5',
        '藏青': '#2c3e50', '卡其': '#c3b091'
    };
    return map[color] || '#cccccc';
}

async function loadStats() {
    try {
        const stats = await apiFetch('/stats');
        document.getElementById('totalCount').textContent = stats.total || 0;
        renderTopWorn(stats.top_worn || []);
        renderCharts(stats);
    } catch (e) {
        console.error('加载统计失败:', e);
    }
}

function renderTopWorn(topWorn) {
    const list = document.getElementById('topWornList');
    if (!topWorn || topWorn.length === 0) {
        list.innerHTML = '<div class="top-worn-empty">暂无记录</div>';
        return;
    }
    list.innerHTML = topWorn.map((item, i) => `
        <div class="top-worn-item">
            <span class="top-worn-rank rank-${i + 1}">${i + 1}</span>
            <span class="top-worn-name">${escapeHtml(item.name)}</span>
            <span class="top-worn-count">${item.wear_count} 次</span>
        </div>
    `).join('');
}

function renderCharts(stats) {
    drawTypeChart(stats.type_counts || {});
    drawColorChart(stats.color_counts || {});
    drawTrendChart(stats.recent_7_days || []);
}

function drawTypeChart(typeCounts) {
    const canvas = document.getElementById('typeChart');
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = 180 * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = '180px';
    ctx.scale(dpr, dpr);

    const w = rect.width;
    const h = 180;
    const entries = Object.entries(typeCounts);

    if (entries.length === 0) {
        ctx.fillStyle = '#9aa8b6';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('暂无数据', w / 2, h / 2);
        return;
    }

    const total = entries.reduce((s, [, v]) => s + v, 0);
    const colors = ['#7393b3', '#d4a574', '#73b3a3', '#b37393', '#a3b373', '#9b73b3'];

    const barW = Math.min(60, (w - 40) / entries.length - 10);
    const maxVal = Math.max(...entries.map(([, v]) => v));
    const chartH = h - 50;
    const startX = (w - entries.length * (barW + 10) + 10) / 2;

    entries.forEach(([name, count], i) => {
        const barH = (count / maxVal) * chartH;
        const x = startX + i * (barW + 10);
        const y = h - 30 - barH;

        ctx.fillStyle = colors[i % colors.length];
        ctx.beginPath();
        ctx.roundRect(x, y, barW, barH, 4);
        ctx.fill();

        ctx.fillStyle = '#2c3e50';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(count, x + barW / 2, y - 6);

        ctx.fillStyle = '#6b7c8d';
        ctx.font = '11px sans-serif';
        ctx.fillText(name, x + barW / 2, h - 12);
    });
}

function drawColorChart(colorCounts) {
    const canvas = document.getElementById('colorChart');
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = 180 * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = '180px';
    ctx.scale(dpr, dpr);

    const w = rect.width;
    const h = 180;
    const entries = Object.entries(colorCounts);

    if (entries.length === 0) {
        ctx.fillStyle = '#9aa8b6';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('暂无数据', w / 2, h / 2);
        return;
    }

    const total = entries.reduce((s, [, v]) => s + v, 0);
    const cx = w * 0.35;
    const cy = h / 2;
    const r = Math.min(60, h * 0.35);
    const rInner = r * 0.6;

    let startAngle = -Math.PI / 2;

    entries.forEach(([name, count]) => {
        const angle = (count / total) * Math.PI * 2;
        const endAngle = startAngle + angle;

        ctx.fillStyle = getColorHex(name);
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, r, startAngle, endAngle);
        ctx.closePath();
        ctx.fill();

        startAngle = endAngle;
    });

    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(cx, cy, rInner, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#2c3e50';
    ctx.font = 'bold 16px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(total, cx, cy + 6);
    ctx.fillStyle = '#6b7c8d';
    ctx.font = '10px sans-serif';
    ctx.fillText('总件数', cx, cy + 20);

    const legendX = w * 0.65;
    const legendY = 30;
    const legendItemH = 22;

    entries.slice(0, 6).forEach(([name, count], i) => {
        const y = legendY + i * legendItemH;

        ctx.fillStyle = getColorHex(name);
        ctx.fillRect(legendX, y, 12, 12);

        ctx.fillStyle = '#2c3e50';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(`${name} ${count}`, legendX + 18, y + 10);
    });
}

function drawTrendChart(recentDays) {
    const canvas = document.getElementById('trendChart');
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = 180 * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = '180px';
    ctx.scale(dpr, dpr);

    const w = rect.width;
    const h = 180;

    if (!recentDays || recentDays.length === 0) {
        ctx.fillStyle = '#9aa8b6';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('暂无数据', w / 2, h / 2);
        return;
    }

    const paddingL = 30;
    const paddingR = 10;
    const paddingT = 20;
    const paddingB = 30;
    const chartW = w - paddingL - paddingR;
    const chartH = h - paddingT - paddingB;

    const maxVal = Math.max(...recentDays.map(d => d.count), 1);
    const stepX = chartW / (recentDays.length - 1 || 1);

    ctx.strokeStyle = '#e8e8e0';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = paddingT + (chartH / 4) * i;
        ctx.beginPath();
        ctx.moveTo(paddingL, y);
        ctx.lineTo(w - paddingR, y);
        ctx.stroke();
    }

    ctx.strokeStyle = '#7393b3';
    ctx.lineWidth = 2.5;
    ctx.beginPath();

    recentDays.forEach((d, i) => {
        const x = paddingL + i * stepX;
        const y = paddingT + chartH - (d.count / maxVal) * chartH;

        if (i === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    });
    ctx.stroke();

    ctx.fillStyle = 'rgba(115, 147, 179, 0.15)';
    ctx.beginPath();
    recentDays.forEach((d, i) => {
        const x = paddingL + i * stepX;
        const y = paddingT + chartH - (d.count / maxVal) * chartH;
        if (i === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    });
    ctx.lineTo(paddingL + (recentDays.length - 1) * stepX, paddingT + chartH);
    ctx.lineTo(paddingL, paddingT + chartH);
    ctx.closePath();
    ctx.fill();

    recentDays.forEach((d, i) => {
        const x = paddingL + i * stepX;
        const y = paddingT + chartH - (d.count / maxVal) * chartH;

        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#7393b3';
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.fillStyle = '#2c3e50';
        ctx.font = 'bold 10px sans-serif';
        ctx.textAlign = 'center';
        if (d.count > 0) {
            ctx.fillText(d.count, x, y - 8);
        }

        const date = new Date(d.date);
        const label = `${date.getMonth() + 1}/${date.getDate()}`;
        ctx.fillStyle = '#6b7c8d';
        ctx.font = '10px sans-serif';
        ctx.fillText(label, x, h - 12);
    });
}

async function loadFilters() {
    try {
        const filters = await apiFetch('/filters');
        populateSelect('filterType', filters.types || []);
        populateSelect('filterColor', filters.colors || []);
        populateSelect('filterSeason', filters.seasons || []);
    } catch (e) {
        console.error('加载筛选选项失败:', e);
    }
}

function populateSelect(selectId, options) {
    const select = document.getElementById(selectId);
    const currentVal = select.value;
    select.innerHTML = '<option value="all">全部</option>' +
        options.map(opt => `<option value="${opt}">${opt}</option>`).join('');
    if (options.includes(currentVal)) {
        select.value = currentVal;
    }
}

async function loadClothes() {
    try {
        const all = await apiFetch('/clothes');
        allClothes = all;

        const params = new URLSearchParams();
        if (currentFilters.type !== 'all') params.set('type', currentFilters.type);
        if (currentFilters.color !== 'all') params.set('color', currentFilters.color);
        if (currentFilters.season !== 'all') params.set('season', currentFilters.season);

        let clothes = all;
        if (currentFilters.type !== 'all') {
            clothes = clothes.filter(c => c.type.toLowerCase() === currentFilters.type.toLowerCase());
        }
        if (currentFilters.color !== 'all') {
            clothes = clothes.filter(c => c.color.toLowerCase() === currentFilters.color.toLowerCase());
        }
        if (currentFilters.season !== 'all') {
            clothes = clothes.filter(c => c.season.toLowerCase() === currentFilters.season.toLowerCase());
        }

        currentClothes = clothes;
        document.getElementById('filteredCount').textContent = clothes.length;
        renderClothes(clothes);
    } catch (e) {
        console.error('加载衣物失败:', e);
    }
}

function renderClothes(clothes) {
    const grid = document.getElementById('clothesGrid');

    if (clothes.length === 0) {
        grid.innerHTML = `
            <div class="empty-state">
                <p>衣橱空空如也～</p>
                <p class="empty-sub">添加你的第一件衣物吧！</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = clothes.map(item => {
        const longNoWear = item.is_long_time_no_wear;
        const hasBeenWorn = item.has_been_worn;
        const cardClass = longNoWear ? 'clothing-card long-time-no-wear' : 'clothing-card';

        let warningBadge = '';
        if (longNoWear && hasBeenWorn) {
            warningBadge = '<div class="warning-badge">⚠️ 很久没穿了</div>';
        } else if (!hasBeenWorn) {
            warningBadge = '<div class="warning-badge" style="background:#95a5a6;">🆕 从未穿过</div>';
        }

        const imgUrl = safeImageUrl(item.image_url);
        const imgHtml = imgUrl
            ? `<img src="${escapeHtml(imgUrl)}" alt="${escapeHtml(item.name)}" onerror="this.style.display='none';this.parentElement.textContent='${escapeHtml(getTypeEmoji(item.type))}';">`
            : escapeHtml(getTypeEmoji(item.type));

        return `
            <div class="${cardClass}" data-id="${escapeHtml(item.id)}">
                <div class="card-image">${imgHtml}</div>
                <div class="card-body">
                    <div class="card-header">
                        <div class="card-name">${escapeHtml(item.name)}</div>
                    </div>
                    <span class="card-tag type">${escapeHtml(item.type)}</span>
                    <span class="card-tag color">${escapeHtml(item.color)}</span>
                    <span class="card-tag season">${escapeHtml(item.season)}</span>
                    <div class="card-info">
                        <div class="card-info-row">
                            <span>穿着次数</span>
                            <span class="wear-count-badge">${item.wear_count} 次</span>
                        </div>
                        <div class="card-info-row">
                            <span>最后穿着</span>
                            <span>${hasBeenWorn ? formatDate(item.last_worn_at) : '从未'}</span>
                        </div>
                        <div class="card-info-row">
                            <span>创建时间</span>
                            <span>${formatDate(item.created_at)}</span>
                        </div>
                    </div>
                    ${warningBadge}
                    <div class="card-remark">${escapeHtml(item.remark || '')}</div>
                    <div class="card-actions">
                        <button class="btn btn-secondary btn-sm" onclick="openEditModal('${escapeHtml(item.id)}')">编辑</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteClothing('${escapeHtml(item.id)}')">删除</button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function addClothing(e) {
    e.preventDefault();
    const data = {
        name: document.getElementById('name').value.trim(),
        type: document.getElementById('type').value.trim(),
        color: document.getElementById('color').value.trim(),
        season: document.getElementById('season').value.trim(),
        image_url: document.getElementById('imageUrl').value.trim(),
        remark: document.getElementById('remark').value.trim()
    };

    if (!data.name || !data.type || !data.color || !data.season) {
        showToast('请填写必填项', 'warning');
        return;
    }

    try {
        await apiFetch('/clothes', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        showToast('添加成功！', 'success');
        e.target.reset();
        refreshAll();
    } catch (e) {
        console.error('添加失败:', e);
    }
}

async function deleteClothing(id) {
    if (!confirm('确定要删除这件衣物吗？')) return;

    try {
        await apiFetch(`/clothes/${id}`, { method: 'DELETE' });
        showToast('删除成功', 'success');
        refreshAll();
    } catch (e) {
        console.error('删除失败:', e);
    }
}

function openEditModal(id) {
    const item = currentClothes.find(c => c.id === id);
    if (!item) return;

    document.getElementById('editId').value = item.id;
    document.getElementById('editName').value = item.name;
    document.getElementById('editType').value = item.type;
    document.getElementById('editColor').value = item.color;
    document.getElementById('editSeason').value = item.season;
    document.getElementById('editImageUrl').value = item.image_url || '';
    document.getElementById('editRemark').value = item.remark || '';
    document.getElementById('editWearCount').value = item.wear_count;
    document.getElementById('editLastWorn').value = item.has_been_worn ? formatDateFull(item.last_worn_at) : '';

    document.getElementById('editModal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

async function saveEdit() {
    const id = document.getElementById('editId').value;
    const lastWornVal = document.getElementById('editLastWorn').value;

    const data = {
        name: document.getElementById('editName').value.trim(),
        type: document.getElementById('editType').value.trim(),
        color: document.getElementById('editColor').value.trim(),
        season: document.getElementById('editSeason').value.trim(),
        image_url: document.getElementById('editImageUrl').value.trim(),
        remark: document.getElementById('editRemark').value.trim(),
        wear_count: parseInt(document.getElementById('editWearCount').value) || 0
    };

    if (lastWornVal) {
        data.last_worn_at = lastWornVal;
    }

    if (!data.name || !data.type || !data.color || !data.season) {
        showToast('请填写必填项', 'warning');
        return;
    }

    try {
        await apiFetch(`/clothes/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        showToast('保存成功！', 'success');
        closeEditModal();
        refreshAll();
    } catch (e) {
        console.error('保存失败:', e);
    }
}

function openOutfitModal() {
    selectedOutfitIds = [];
    renderOutfitSelectList();
    document.getElementById('outfitNote').value = '';
    document.getElementById('outfitModal').style.display = 'flex';
}

function closeOutfitModal() {
    document.getElementById('outfitModal').style.display = 'none';
}

function renderOutfitSelectList() {
    const list = document.getElementById('outfitSelectList');
    const source = allClothes.length > 0 ? allClothes : currentClothes;

    if (source.length === 0) {
        list.innerHTML = '<div class="empty-state small">暂无衣物可选择</div>';
        return;
    }

    list.innerHTML = source.map(item => {
        const checked = selectedOutfitIds.includes(item.id) ? 'checked' : '';
        const itemClass = checked ? 'outfit-select-item selected' : 'outfit-select-item';

        const imgUrl = safeImageUrl(item.image_url);
        const imgHtml = imgUrl
            ? `<img src="${escapeHtml(imgUrl)}" alt="" style="width:40px;height:40px;border-radius:6px;object-fit:cover;">`
            : `<span style="font-size:28px;">${escapeHtml(getTypeEmoji(item.type))}</span>`;

        return `
            <div class="${itemClass}" data-id="${escapeHtml(item.id)}" onclick="toggleOutfitItem('${escapeHtml(item.id)}')">
                <input type="checkbox" ${checked} onclick="event.stopPropagation(); toggleOutfitItem('${escapeHtml(item.id)}');">
                ${imgHtml}
                <div class="outfit-select-info">
                    <div class="outfit-select-name">${escapeHtml(item.name)}</div>
                    <div class="outfit-select-meta">${escapeHtml(item.type)} · ${escapeHtml(item.color)}</div>
                </div>
            </div>
        `;
    }).join('');
}

function toggleOutfitItem(id) {
    const idx = selectedOutfitIds.indexOf(id);
    if (idx > -1) {
        selectedOutfitIds.splice(idx, 1);
    } else {
        selectedOutfitIds.push(id);
    }
    renderOutfitSelectList();
}

async function confirmOutfit() {
    if (selectedOutfitIds.length === 0) {
        showToast('请至少选择一件衣物', 'warning');
        return;
    }

    const note = document.getElementById('outfitNote').value.trim();

    try {
        await apiFetch('/outfit', {
            method: 'POST',
            body: JSON.stringify({
                clothing_ids: selectedOutfitIds,
                note: note
            })
        });
        showToast('穿搭记录已保存！', 'success');
        closeOutfitModal();
        refreshAll();
    } catch (e) {
        console.error('记录穿搭失败:', e);
    }
}

async function loadHistory() {
    try {
        const logs = await apiFetch('/outfit/logs?limit=20');
        renderHistory(logs);
    } catch (e) {
        console.error('加载历史失败:', e);
    }
}

function renderHistory(logs) {
    const list = document.getElementById('historyList');

    if (!logs || logs.length === 0) {
        list.innerHTML = '<div class="empty-state small"><p>还没有穿搭记录</p></div>';
        return;
    }

    list.innerHTML = logs.map(log => {
        const date = new Date(log.worn_at);
        const day = date.getDate();
        const month = date.getMonth() + 1;

        const clothesHtml = log.clothes.map(c => {
            const imgUrl = safeImageUrl(c.image_url);
            const imgHtml = imgUrl
                ? `<img src="${escapeHtml(imgUrl)}" alt="">`
                : `<span style="font-size:14px;">${escapeHtml(getTypeEmoji(c.type))}</span>`;
            return `<span class="history-clothing-tag">${imgHtml}${escapeHtml(c.name)}</span>`;
        }).join('');

        const noteHtml = log.note
            ? `<div class="history-note">📝 ${escapeHtml(log.note)}</div>`
            : '';

        return `
            <div class="history-item">
                <div class="history-date">
                    <div class="history-date-day">${day}</div>
                    <div class="history-date-month">${month}月</div>
                </div>
                <div class="history-content">
                    ${noteHtml}
                    <div class="history-clothes">${clothesHtml}</div>
                </div>
            </div>
        `;
    }).join('');
}

async function loadRecommendation() {
    const season = currentFilters.season !== 'all' ? currentFilters.season : '';
    const params = new URLSearchParams();
    if (season) params.set('season', season);

    try {
        const rec = await apiFetch('/recommend?' + params.toString());
        currentRecommendation = rec;
        renderRecommendation(rec);
        document.getElementById('recommendSection').style.display = 'block';
    } catch (e) {
        console.error('加载推荐失败:', e);
    }
}

function renderRecommendation(rec) {
    const tipEl = document.getElementById('recommendTip');
    const gridEl = document.getElementById('recommendGrid');

    const items = rec.items || {};
    const typeMap = [
        { key: '上衣', label: '上衣' },
        { key: '裤子', label: '下装' },
        { key: '鞋子', label: '鞋子' }
    ];

    let tipText = rec.tip || '为你推荐今日穿搭';
    tipEl.textContent = tipText;

    gridEl.innerHTML = typeMap.map(({ key, label }) => {
        const item = items[key];
        if (!item) {
            return `
                <div class="recommend-item recommend-item-empty">
                    <div style="font-size:32px;margin-bottom:8px;">📭</div>
                    <div>暂无${escapeHtml(label)}</div>
                </div>
            `;
        }

        const imgUrl = safeImageUrl(item.image_url);
        const imgHtml = imgUrl
            ? `<img src="${escapeHtml(imgUrl)}" alt="${escapeHtml(item.name)}">`
            : escapeHtml(getTypeEmoji(item.type));

        const tagHtml = item.is_long_time_no_wear
            ? '<span class="recommend-item-tag">很久没穿了</span>'
            : '';

        return `
            <div class="recommend-item">
                <div class="recommend-item-image">${imgHtml}</div>
                <div class="recommend-item-name">${escapeHtml(item.name)}</div>
                <div class="recommend-item-type">${escapeHtml(item.type)} · ${escapeHtml(item.color)}</div>
                ${tagHtml}
            </div>
        `;
    }).join('');
}

async function wearRecommendation() {
    if (!currentRecommendation) return;

    const items = currentRecommendation.items || {};
    const ids = [];
    for (const key of ['上衣', '裤子', '鞋子']) {
        if (items[key]) ids.push(items[key].id);
    }

    if (ids.length === 0) {
        showToast('没有可记录的衣物', 'warning');
        return;
    }

    try {
        await apiFetch('/outfit', {
            method: 'POST',
            body: JSON.stringify({
                clothing_ids: ids,
                note: '推荐穿搭'
            })
        });
        showToast('已记录今日穿搭！', 'success');
        refreshAll();
    } catch (e) {
        console.error('记录失败:', e);
    }
}

async function exportData() {
    try {
        const res = await fetch(API_BASE + '/export');
        if (!res.ok) throw new Error('导出失败');

        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const now = new Date();
        const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`;
        a.download = `wardrobe_backup_${dateStr}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('导出成功！', 'success');
    } catch (e) {
        showToast('导出失败：' + e.message, 'error');
    }
}

async function importData(file) {
    if (!file) return;

    if (!confirm('导入将合并到当前数据中，确定继续吗？')) return;

    try {
        const text = await file.text();
        const jsonData = JSON.parse(text);

        const res = await fetch(API_BASE + '/import?merge=true', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(jsonData)
        });

        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || '导入失败');
        }

        showToast(`导入成功！新增 ${data.clothes_added} 件衣物，${data.logs_added} 条记录`, 'success');
        refreshAll();
    } catch (e) {
        showToast('导入失败：' + e.message, 'error');
    }

    document.getElementById('importFile').value = '';
}

function refreshAll() {
    loadStats();
    loadFilters();
    loadClothes();
    loadHistory();
}

function initEvents() {
    document.getElementById('addClothingForm').addEventListener('submit', addClothing);

    document.getElementById('filterType').addEventListener('change', (e) => {
        currentFilters.type = e.target.value;
        loadClothes();
    });
    document.getElementById('filterColor').addEventListener('change', (e) => {
        currentFilters.color = e.target.value;
        loadClothes();
    });
    document.getElementById('filterSeason').addEventListener('change', (e) => {
        currentFilters.season = e.target.value;
        loadClothes();
    });

    document.getElementById('resetFilterBtn').addEventListener('click', () => {
        currentFilters = { type: 'all', color: 'all', season: 'all' };
        document.getElementById('filterType').value = 'all';
        document.getElementById('filterColor').value = 'all';
        document.getElementById('filterSeason').value = 'all';
        loadClothes();
    });

    document.getElementById('openOutfitModal').addEventListener('click', openOutfitModal);
    document.getElementById('closeOutfitModal').addEventListener('click', closeOutfitModal);
    document.getElementById('cancelOutfitBtn').addEventListener('click', closeOutfitModal);
    document.getElementById('confirmOutfitBtn').addEventListener('click', confirmOutfit);

    document.getElementById('closeEditModal').addEventListener('click', closeEditModal);
    document.getElementById('cancelEditBtn').addEventListener('click', closeEditModal);
    document.getElementById('saveEditBtn').addEventListener('click', saveEdit);

    document.getElementById('recommendBtn').addEventListener('click', loadRecommendation);
    document.getElementById('refreshRecommendBtn').addEventListener('click', loadRecommendation);
    document.getElementById('wearRecommendBtn').addEventListener('click', wearRecommendation);

    document.getElementById('exportBtn').addEventListener('click', exportData);
    document.getElementById('importFile').addEventListener('change', (e) => {
        importData(e.target.files[0]);
    });

    document.querySelectorAll('.modal-overlay').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    });

    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {
            loadStats();
        }, 200);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    initEvents();
    refreshAll();
});
