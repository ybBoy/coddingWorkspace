const API_BASE = '/api/checkin';
const RECENT_DAYS = 30;
let currentFilter = 'all';
let allRecords = [];

function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function parseDate(dateStr) {
    const [y, m, d] = dateStr.split('-').map(Number);
    return new Date(y, m - 1, d);
}

function getTodayStr() {
    return formatDate(new Date());
}

function getDaysAgoStr(days) {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return formatDate(date);
}

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('checkinDate').value = getTodayStr();

    loadRecords();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('checkinForm').addEventListener('submit', handleSubmit);
    document.getElementById('resetForm').addEventListener('click', resetForm);
    document.getElementById('editForm').addEventListener('submit', handleEditSubmit);
    document.getElementById('cancelEdit').addEventListener('click', closeEditModal);

    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.dataset.type;
            renderRecords();
        });
    });

    document.getElementById('editModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeEditModal();
        }
    });
}

async function loadRecords() {
    try {
        const response = await fetch(API_BASE);
        allRecords = await response.json();
        renderRecords();
        updateStats();
    } catch (error) {
        console.error('加载记录失败:', error);
    }
}

function renderRecords() {
    const recordsList = document.getElementById('recordsList');
    const emptyState = document.getElementById('emptyState');

    const cutoffDate = getDaysAgoStr(RECENT_DAYS - 1);
    let filtered = allRecords.filter(r => r.checkinDate >= cutoffDate);

    if (currentFilter !== 'all') {
        filtered = filtered.filter(r => r.exerciseType === currentFilter);
    }

    filtered.sort((a, b) => b.checkinDate.localeCompare(a.checkinDate));

    document.getElementById('filteredCount').textContent = filtered.length;

    if (filtered.length === 0) {
        recordsList.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    recordsList.innerHTML = filtered.map(record => `
        <div class="record-card">
            <div class="record-date">${record.checkinDate}</div>
            <span class="record-type">${getTypeEmoji(record.exerciseType)} ${record.exerciseType}</span>
            <div class="record-duration">${record.duration} 分钟</div>
            <span class="record-mood">${getMoodEmoji(record.mood)} ${record.mood}</span>
            <div class="record-note" title="${escapeHtml(record.note || '')}">${escapeHtml(record.note || '无')}</div>
            <div class="record-actions">
                <button class="btn-edit" onclick="openEditModal('${record.id}')">修改</button>
                <button class="btn-delete" onclick="deleteRecord('${record.id}')">删除</button>
            </div>
        </div>
    `).join('');
}

function getTypeEmoji(type) {
    const emojis = {
        '跑步': '🏃',
        '骑行': '🚴',
        '游泳': '🏊',
        '瑜伽': '🧘',
        '力量训练': '🏋️',
        '快走': '🚶',
        '跳绳': '⏱️',
        '其他': '🎯'
    };
    return emojis[type] || '🎯';
}

function getMoodEmoji(mood) {
    const emojis = {
        '很棒': '😄',
        '不错': '😊',
        '一般': '😐',
        '疲惫': '😫',
        '酸痛': '💪'
    };
    return emojis[mood] || '😊';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function updateStats() {
    const weekMinutes = calculateWeekMinutes();
    const streakDays = calculateStreak();

    document.getElementById('weekMinutes').textContent = weekMinutes;
    document.getElementById('streakDays').textContent = streakDays;
}

function calculateWeekMinutes() {
    const today = new Date();
    const dayOfWeek = today.getDay() || 7;
    const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (dayOfWeek - 1));
    const mondayStr = formatDate(monday);

    return allRecords
        .filter(r => r.checkinDate >= mondayStr)
        .reduce((sum, r) => sum + r.duration, 0);
}

function calculateStreak() {
    if (allRecords.length === 0) return 0;

    const dateSet = new Set(allRecords.map(r => r.checkinDate));
    const sortedDates = [...dateSet].sort((a, b) => b.localeCompare(a));

    const todayStr = getTodayStr();
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = formatDate(yesterday);

    if (sortedDates[0] !== todayStr && sortedDates[0] !== yesterdayStr) {
        return 0;
    }

    let streak = 0;
    let checkDateStr = sortedDates[0];

    while (dateSet.has(checkDateStr)) {
        streak++;
        const d = parseDate(checkDateStr);
        d.setDate(d.getDate() - 1);
        checkDateStr = formatDate(d);
    }

    return streak;
}

async function handleSubmit(e) {
    e.preventDefault();

    const data = {
        checkinDate: document.getElementById('checkinDate').value,
        exerciseType: document.getElementById('exerciseType').value,
        duration: parseInt(document.getElementById('duration').value),
        mood: document.getElementById('mood').value,
        note: document.getElementById('note').value.trim()
    };

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            await loadRecords();
            resetForm();
            alert('打卡成功！');
        } else {
            const error = await response.json();
            alert('提交失败: ' + (error.message || '未知错误'));
        }
    } catch (error) {
        console.error('提交失败:', error);
        alert('提交失败，请稍后重试');
    }
}

function resetForm() {
    document.getElementById('checkinForm').reset();
    document.getElementById('checkinDate').value = getTodayStr();
}

function openEditModal(id) {
    const record = allRecords.find(r => r.id === id);
    if (!record) return;

    document.getElementById('editId').value = record.id;
    document.getElementById('editMood').value = record.mood;
    document.getElementById('editNote').value = record.note || '';
    document.getElementById('editModal').classList.remove('hidden');
}

function closeEditModal() {
    document.getElementById('editModal').classList.add('hidden');
}

async function handleEditSubmit(e) {
    e.preventDefault();

    const id = document.getElementById('editId').value;
    const data = {
        mood: document.getElementById('editMood').value,
        note: document.getElementById('editNote').value.trim()
    };

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            await loadRecords();
            closeEditModal();
            alert('修改成功！');
        } else {
            alert('修改失败，请稍后重试');
        }
    } catch (error) {
        console.error('修改失败:', error);
        alert('修改失败，请稍后重试');
    }
}

async function deleteRecord(id) {
    if (!confirm('确定要删除这条记录吗？')) return;

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            await loadRecords();
            alert('删除成功！');
        } else {
            alert('删除失败，请稍后重试');
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('删除失败，请稍后重试');
    }
}
