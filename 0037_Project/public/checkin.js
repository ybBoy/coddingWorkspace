const API_BASE = '/api/checkin';
const STATS_API = '/api/stats';
const GOAL_API = '/api/settings/goal';
const IMPORT_API = '/api/checkin/import';

let allRecords = [];
let weeklyGoal = 150;
let timeFilter = '7';
let typeFilter = 'all';
let calendarDate = new Date();
let selectedDate = null;

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

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

function showFormError(formId, message) {
    const errorEl = document.getElementById(formId === 'edit' ? 'editError' : 'formError');
    if (message) {
        errorEl.textContent = message;
        errorEl.classList.add('show');
    } else {
        errorEl.classList.remove('show');
    }
}

function setButtonLoading(btnId, loading, originalText) {
    const btn = document.getElementById(btnId);
    if (loading) {
        btn.disabled = true;
        btn.dataset.originalText = btn.textContent;
        btn.textContent = '处理中...';
    } else {
        btn.disabled = false;
        btn.textContent = btn.dataset.originalText || originalText || '保存';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('checkinDate').value = getTodayStr();

    setupEventListeners();
    loadAllData();
    renderCalendar();
});

function setupEventListeners() {
    document.getElementById('checkinForm').addEventListener('submit', handleSubmit);
    document.getElementById('resetForm').addEventListener('click', resetForm);
    document.getElementById('editForm').addEventListener('submit', handleEditSubmit);
    document.getElementById('cancelEdit').addEventListener('click', closeEditModal);
    document.getElementById('goalEditBtn').addEventListener('click', openGoalModal);
    document.getElementById('goalForm').addEventListener('submit', handleGoalSubmit);
    document.getElementById('cancelGoal').addEventListener('click', closeGoalModal);

    document.querySelectorAll('#timeFilterButtons .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('#timeFilterButtons .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            timeFilter = this.dataset.time;
            selectedDate = null;
            renderRecords();
            updateRecordsTitle();
        });
    });

    document.querySelectorAll('#typeFilterButtons .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('#typeFilterButtons .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            typeFilter = this.dataset.type;
            renderRecords();
        });
    });

    document.getElementById('prevMonth').addEventListener('click', function() {
        calendarDate.setMonth(calendarDate.getMonth() - 1);
        renderCalendar();
    });

    document.getElementById('nextMonth').addEventListener('click', function() {
        calendarDate.setMonth(calendarDate.getMonth() + 1);
        renderCalendar();
    });

    document.getElementById('exportBtn').addEventListener('click', handleExport);
    document.getElementById('importBtn').addEventListener('click', function() {
        document.getElementById('importFile').click();
    });
    document.getElementById('importFile').addEventListener('change', handleImport);

    document.getElementById('editModal').addEventListener('click', function(e) {
        if (e.target === this) closeEditModal();
    });
    document.getElementById('goalModal').addEventListener('click', function(e) {
        if (e.target === this) closeGoalModal();
    });
}

async function loadAllData() {
    try {
        const [recordsResp, statsResp] = await Promise.all([
            fetch(API_BASE),
            fetch(STATS_API)
        ]);

        allRecords = await recordsResp.json();

        if (statsResp.ok) {
            const stats = await statsResp.json();
            weeklyGoal = stats.weeklyGoal || 150;
        }

        renderStats();
        renderRecords();
        renderCalendar();
        updateRecordsTitle();
    } catch (error) {
        console.error('加载数据失败:', error);
        showToast('加载数据失败', 'error');
    }
}

function renderStats() {
    const weekMinutes = calculateWeekMinutes();
    const streakDays = calculateStreak();

    document.getElementById('weekMinutes').textContent = weekMinutes;
    document.getElementById('weekGoal').textContent = weeklyGoal;
    document.getElementById('streakDays').textContent = streakDays;

    const percentage = Math.min(100, Math.round((weekMinutes / weeklyGoal) * 100));
    document.getElementById('progressFill').style.width = percentage + '%';
    document.getElementById('progressText').textContent = percentage + '%';
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

function getTimeRangeStart() {
    const today = new Date();
    const todayStr = getTodayStr();

    switch (timeFilter) {
        case '7':
            return getDaysAgoStr(6);
        case '30':
            return getDaysAgoStr(29);
        case 'week': {
            const dayOfWeek = today.getDay() || 7;
            const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (dayOfWeek - 1));
            return formatDate(monday);
        }
        case 'month': {
            return formatDate(new Date(today.getFullYear(), today.getMonth(), 1));
        }
        case 'all':
        default:
            return '0000-01-01';
    }
}

function updateRecordsTitle() {
    const titleEl = document.getElementById('recordsTitle');
    let title = '📋 打卡记录';

    if (selectedDate) {
        title = `📋 ${selectedDate} 的记录`;
    } else {
        const timeLabels = {
            '7': '最近7天',
            '30': '最近30天',
            'week': '本周',
            'month': '本月',
            'all': '全部'
        };
        title = `📋 ${timeLabels[timeFilter] || ''}打卡记录`;
    }

    titleEl.textContent = title;
}

function renderRecords() {
    const recordsList = document.getElementById('recordsList');
    const emptyState = document.getElementById('emptyState');
    const emptyText = document.getElementById('emptyText');

    let filtered = [...allRecords];

    if (selectedDate) {
        filtered = filtered.filter(r => r.checkinDate === selectedDate);
    } else {
        const startDate = getTimeRangeStart();
        filtered = filtered.filter(r => r.checkinDate >= startDate);
    }

    if (typeFilter !== 'all') {
        filtered = filtered.filter(r => r.exerciseType === typeFilter);
    }

    filtered.sort((a, b) => b.checkinDate.localeCompare(a.checkinDate));

    document.getElementById('filteredCount').textContent = filtered.length;

    if (filtered.length === 0) {
        recordsList.innerHTML = '';
        emptyState.style.display = 'block';

        if (selectedDate) {
            emptyText.textContent = '这天还没运动，快去打卡吧！';
        } else if (typeFilter !== 'all') {
            emptyText.textContent = `当前类型还没有${timeFilter !== 'all' ? '符合条件的' : ''}记录`;
        } else if (timeFilter !== 'all') {
            emptyText.textContent = '这段时间还没有打卡记录';
        } else {
            emptyText.textContent = '还没有打卡记录，开始今天的第一次运动吧！';
        }
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

function renderCalendar() {
    const calendarDays = document.getElementById('calendarDays');
    const calendarTitle = document.getElementById('calendarTitle');

    const year = calendarDate.getFullYear();
    const month = calendarDate.getMonth();

    calendarTitle.textContent = `${year}年${month + 1}月`;

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    let firstWeekday = firstDay.getDay();
    firstWeekday = firstWeekday === 0 ? 6 : firstWeekday - 1;

    const recordDateSet = new Set(allRecords.map(r => r.checkinDate));
    const todayStr = getTodayStr();

    let html = '';

    const prevMonthLastDay = new Date(year, month, 0).getDate();
    for (let i = firstWeekday - 1; i >= 0; i--) {
        const day = prevMonthLastDay - i;
        html += `<div class="calendar-day other-month">${day}</div>`;
    }

    for (let day = 1; day <= lastDay.getDate(); day++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        let classes = 'calendar-day';

        if (recordDateSet.has(dateStr)) classes += ' has-record';
        if (dateStr === todayStr) classes += ' today';
        if (dateStr === selectedDate) classes += ' selected';

        html += `<div class="${classes}" data-date="${dateStr}" onclick="selectCalendarDate('${dateStr}')">${day}</div>`;
    }

    const totalCells = firstWeekday + lastDay.getDate();
    const remainingCells = Math.ceil(totalCells / 7) * 7 - totalCells;
    for (let day = 1; day <= remainingCells; day++) {
        html += `<div class="calendar-day other-month">${day}</div>`;
    }

    calendarDays.innerHTML = html;
}

function selectCalendarDate(dateStr) {
    if (selectedDate === dateStr) {
        selectedDate = null;
    } else {
        selectedDate = dateStr;
    }
    renderRecords();
    renderCalendar();
    updateRecordsTitle();
}

async function handleSubmit(e) {
    e.preventDefault();
    showFormError('add', '');

    const data = {
        checkinDate: document.getElementById('checkinDate').value,
        exerciseType: document.getElementById('exerciseType').value,
        duration: parseInt(document.getElementById('duration').value),
        mood: document.getElementById('mood').value,
        note: document.getElementById('note').value.trim()
    };

    if (!data.exerciseType) {
        showFormError('add', '请选择运动类型');
        return;
    }
    if (!data.duration || data.duration <= 0) {
        showFormError('add', '请输入有效的运动时长');
        return;
    }

    setButtonLoading('submitBtn', true, '保存打卡');

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            await loadAllData();
            resetForm();
            showToast('打卡成功！', 'success');
        } else {
            let errorMsg = '提交失败';
            try {
                const error = await response.json();
                errorMsg = error.message || errorMsg;
            } catch (e) {}
            showFormError('add', errorMsg);
        }
    } catch (error) {
        console.error('提交失败:', error);
        showFormError('add', '网络错误，请稍后重试');
    } finally {
        setButtonLoading('submitBtn', false, '保存打卡');
    }
}

function resetForm() {
    document.getElementById('checkinForm').reset();
    document.getElementById('checkinDate').value = getTodayStr();
    document.getElementById('mood').value = '很棒';
    showFormError('add', '');
}

function openEditModal(id) {
    const record = allRecords.find(r => r.id === id);
    if (!record) return;

    document.getElementById('editId').value = record.id;
    document.getElementById('editDate').value = record.checkinDate;
    document.getElementById('editExerciseType').value = record.exerciseType;
    document.getElementById('editDuration').value = record.duration;
    document.getElementById('editMood').value = record.mood;
    document.getElementById('editNote').value = record.note || '';
    showFormError('edit', '');

    document.getElementById('editModal').classList.remove('hidden');
}

function closeEditModal() {
    document.getElementById('editModal').classList.add('hidden');
}

async function handleEditSubmit(e) {
    e.preventDefault();
    showFormError('edit', '');

    const id = document.getElementById('editId').value;
    const data = {
        checkinDate: document.getElementById('editDate').value,
        exerciseType: document.getElementById('editExerciseType').value,
        duration: parseInt(document.getElementById('editDuration').value),
        mood: document.getElementById('editMood').value,
        note: document.getElementById('editNote').value.trim()
    };

    if (!data.exerciseType) {
        showFormError('edit', '请选择运动类型');
        return;
    }
    if (!data.duration || data.duration <= 0) {
        showFormError('edit', '请输入有效的运动时长');
        return;
    }

    setButtonLoading('editSubmitBtn', true, '保存修改');

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            await loadAllData();
            closeEditModal();
            showToast('修改成功！', 'success');
        } else {
            let errorMsg = '修改失败';
            try {
                const error = await response.json();
                errorMsg = error.message || errorMsg;
            } catch (e) {}
            showFormError('edit', errorMsg);
        }
    } catch (error) {
        console.error('修改失败:', error);
        showFormError('edit', '网络错误，请稍后重试');
    } finally {
        setButtonLoading('editSubmitBtn', false, '保存修改');
    }
}

async function deleteRecord(id) {
    if (!confirm('确定要删除这条记录吗？')) return;

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            await loadAllData();
            showToast('删除成功！', 'success');
        } else {
            showToast('删除失败', 'error');
        }
    } catch (error) {
        console.error('删除失败:', error);
        showToast('删除失败，请稍后重试', 'error');
    }
}

function openGoalModal() {
    document.getElementById('goalInput').value = weeklyGoal;
    document.getElementById('goalModal').classList.remove('hidden');
}

function closeGoalModal() {
    document.getElementById('goalModal').classList.add('hidden');
}

async function handleGoalSubmit(e) {
    e.preventDefault();

    const newGoal = parseInt(document.getElementById('goalInput').value);
    if (!newGoal || newGoal <= 0 || newGoal > 10080) {
        showToast('请输入有效的目标值（1-10080分钟）', 'error');
        return;
    }

    try {
        const response = await fetch(GOAL_API, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ weeklyGoal: newGoal })
        });

        if (response.ok) {
            weeklyGoal = newGoal;
            renderStats();
            closeGoalModal();
            showToast('目标已更新！', 'success');
        } else {
            showToast('更新目标失败', 'error');
        }
    } catch (error) {
        console.error('更新目标失败:', error);
        showToast('更新目标失败，请稍后重试', 'error');
    }
}

function handleExport() {
    if (allRecords.length === 0) {
        showToast('没有数据可导出', 'error');
        return;
    }

    const dataStr = JSON.stringify(allRecords, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = `fitness-checkins-${getTodayStr()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showToast('导出成功！', 'success');
}

async function handleImport(e) {
    const file = e.target.files[0];
    if (!file) return;

    const overwrite = confirm('点击"确定"覆盖现有数据，点击"取消"合并到现有数据');

    try {
        const text = await file.text();
        const data = JSON.parse(text);

        if (!Array.isArray(data)) {
            showToast('文件格式错误，需要是数组格式', 'error');
            return;
        }

        const url = `${IMPORT_API}${overwrite ? '?overwrite=true' : ''}`;
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const result = await response.json();
            await loadAllData();
            showToast(`成功导入 ${result.imported} 条记录`, 'success');
        } else {
            showToast('导入失败', 'error');
        }
    } catch (error) {
        console.error('导入失败:', error);
        showToast('导入失败，文件格式可能不正确', 'error');
    } finally {
        e.target.value = '';
    }
}
