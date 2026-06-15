const API_BASE = '/api/checkin';
let currentFilter = 'all';
let allRecords = [];

document.addEventListener('DOMContentLoaded', function() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('checkinDate').value = today;

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

    let filtered = allRecords;
    if (currentFilter !== 'all') {
        filtered = allRecords.filter(r => r.exerciseType === currentFilter);
    }

    filtered.sort((a, b) => new Date(b.checkinDate) - new Date(a.checkinDate));

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
    const monday = new Date(today);
    monday.setDate(today.getDate() - (dayOfWeek - 1));
    monday.setHours(0, 0, 0, 0);

    return allRecords
        .filter(r => new Date(r.checkinDate) >= monday)
        .reduce((sum, r) => sum + r.duration, 0);
}

function calculateStreak() {
    if (allRecords.length === 0) return 0;

    const dates = [...new Set(allRecords.map(r => r.checkinDate))]
        .sort((a, b) => new Date(b) - new Date(a));

    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    const yesterdayStr = new Date(today.getTime() - 86400000).toISOString().split('T')[0];

    if (dates[0] !== todayStr && dates[0] !== yesterdayStr) {
        return 0;
    }

    let streak = 0;
    let checkDate = new Date(dates[0]);

    for (let i = 0; i < dates.length; i++) {
        const dateStr = checkDate.toISOString().split('T')[0];
        if (dates.includes(dateStr)) {
            streak++;
            checkDate.setDate(checkDate.getDate() - 1);
        } else {
            break;
        }
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
    document.getElementById('checkinDate').value = new Date().toISOString().split('T')[0];
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
