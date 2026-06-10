const API_BASE = 'http://localhost:8080/api';

let state = {
    currentUserId: 1,
    users: [],
    userMap: {},
    tasks: [],
    currentFilter: 'ALL',
    currentAssigneeFilter: '',
    modalMode: 'create',
    editingTaskId: null
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
    try {
        const res = await fetch(`${API_BASE}${path}`, {
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
            ...options
        });
        return await res.json();
    } catch (e) {
        showToast('网络请求失败，请检查后端服务是否启动', 'error');
        return { success: false, message: e.message };
    }
}

function getInitial(name) {
    if (!name) return '?';
    return name.trim().charAt(0).toUpperCase();
}

function getAvatarColor(userId) {
    const user = state.userMap[userId];
    return user ? user.avatarColor : '#64748b';
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

function isOverdue(dateStr) {
    if (!dateStr) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(dateStr) < today;
}

function isSoon(dateStr, days = 2) {
    if (!dateStr) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dateStr);
    const diff = Math.ceil((due - today) / (1000 * 60 * 60 * 24));
    return diff >= 0 && diff <= days;
}

function getPriorityLabel(p) {
    return { HIGH: '高', MEDIUM: '中', LOW: '低' }[p] || p;
}

function getStatusLabel(s) {
    return { PENDING: '未开始', IN_PROGRESS: '进行中', COMPLETED: '已完成' }[s] || s;
}

function showToast(msg, type = 'success') {
    const toast = $('toast');
    const content = $('toastContent');
    content.textContent = msg;
    const colors = {
        success: 'bg-emerald-500',
        error: 'bg-red-500',
        info: 'bg-indigo-500'
    };
    content.className = `px-5 py-3 rounded-xl shadow-xl text-sm font-medium text-white ${colors[type] || colors.info}`;
    toast.classList.remove('translate-y-4', 'opacity-0');
    toast.classList.add('translate-y-0', 'opacity-100');
    setTimeout(() => {
        toast.classList.add('translate-y-4', 'opacity-0');
        toast.classList.remove('translate-y-0', 'opacity-100');
    }, 2500);
}

function isLeader() {
    const user = state.userMap[state.currentUserId];
    return user && user.role === 'LEADER';
}

function initUserSelect() {
    const select = $('userSelect');
    select.innerHTML = '';
    state.users.forEach(u => {
        const opt = document.createElement('option');
        opt.value = u.id;
        const roleLabel = u.role === 'LEADER' ? '（组长）' : '';
        opt.textContent = `${u.name}${roleLabel}`;
        if (u.id === state.currentUserId) opt.selected = true;
        select.appendChild(opt);
    });
}

function initAssigneeFilter() {
    const select = $('assigneeFilter');
    select.innerHTML = '<option value="">全部负责人</option>';
    state.users.forEach(u => {
        const opt = document.createElement('option');
        opt.value = u.id;
        opt.textContent = u.name;
        select.appendChild(opt);
    });
    select.value = state.currentAssigneeFilter;
}

function initModalAssignee() {
    const select = $('taskAssignee');
    select.innerHTML = '';
    if (isLeader()) {
        state.users.forEach(u => {
            const opt = document.createElement('option');
            opt.value = u.id;
            opt.textContent = u.name;
            select.appendChild(opt);
        });
        select.disabled = false;
    } else {
        const user = state.userMap[state.currentUserId];
        const opt = document.createElement('option');
        opt.value = user.id;
        opt.textContent = user.name;
        select.appendChild(opt);
        select.disabled = true;
    }
}

function renderHeader() {
    const user = state.userMap[state.currentUserId];
    if (!user) return;
    const avatar = $('userAvatar');
    avatar.style.background = user.avatarColor;
    avatar.innerHTML = `<span class="avatar-initial">${getInitial(user.name)}</span>`;
    $('userName').textContent = user.name;
    $('userRole').textContent = user.role === 'LEADER' ? '项目组长' : '普通成员';

    const leader = isLeader();
    if (leader) {
        $('pageTitle').textContent = '全部任务';
        $('pageSubtitle').textContent = '管理和追踪团队所有成员的任务';
        $('assigneeFilterContainer').classList.remove('hidden');
    } else {
        $('pageTitle').textContent = '我的任务';
        $('pageSubtitle').textContent = '管理和追踪你负责的所有任务';
        $('assigneeFilterContainer').classList.add('hidden');
    }
}

function renderStats() {
    const tasks = state.tasks;
    const total = tasks.length;
    const completed = tasks.filter(t => t.status === 'COMPLETED').length;
    const pending = tasks.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS').length;
    const rate = total > 0 ? Math.round((completed / total) * 100) : 0;

    $('statsGrid').innerHTML = `
        <div class="stat-card stat-total">
            <p class="text-sm text-slate-500 mb-1">全部任务</p>
            <p class="text-3xl font-bold text-slate-800 mb-2">${total}</p>
            <div class="flex items-center text-xs text-slate-500">
                <span class="inline-block w-2 h-2 rounded-full bg-indigo-500 mr-2"></span>
                任务总数统计
            </div>
        </div>
        <div class="stat-card stat-pending">
            <p class="text-sm text-slate-500 mb-1">未完成</p>
            <p class="text-3xl font-bold text-slate-700 mb-2">${pending}</p>
            <div class="flex items-center text-xs text-slate-500">
                <span class="inline-block w-2 h-2 rounded-full bg-amber-500 mr-2"></span>
                待处理任务
            </div>
        </div>
        <div class="stat-card stat-completed">
            <p class="text-sm text-slate-500 mb-1">已完成</p>
            <p class="text-3xl font-bold text-emerald-600 mb-2">${completed}</p>
            <div class="flex items-center text-xs text-slate-500">
                <span class="inline-block w-2 h-2 rounded-full bg-emerald-500 mr-2"></span>
                完成率 ${rate}%
            </div>
        </div>
    `;
}

function createTaskCard(task) {
    const completed = task.status === 'COMPLETED';
    const overdue = !completed && isOverdue(task.dueDate);
    const soon = !completed && !overdue && isSoon(task.dueDate);
    const leader = isLeader();
    const isOwner = task.assigneeId === state.currentUserId;
    const canEdit = leader || isOwner;
    const canDelete = leader;
    const canChangeStatus = canEdit;

    let dueClass = 'due-date';
    if (overdue) dueClass += ' overdue';
    else if (soon) dueClass += ' soon';

    let statusActions = '';
    if (canChangeStatus && !completed) {
        const options = [
            { val: 'PENDING', label: '未开始' },
            { val: 'IN_PROGRESS', label: '进行中' },
            { val: 'COMPLETED', label: '已完成' }
        ];
        statusActions = `
            <select class="status-dropdown" data-id="${task.id}" title="修改状态">
                ${options.map(o => `<option value="${o.val}" ${o.val === task.status ? 'selected' : ''}>${o.label}</option>`).join('')}
            </select>
        `;
    } else {
        statusActions = `<span class="status-badge status-${task.status}">${getStatusLabel(task.status)}</span>`;
    }

    return `
        <div class="task-card ${completed ? 'completed' : ''}">
            <div class="flex items-start justify-between mb-3">
                <span class="priority-badge priority-${task.priority}">
                    ${task.priority === 'HIGH' ? '● ' : task.priority === 'MEDIUM' ? '● ' : '● '}${getPriorityLabel(task.priority)}优先级
                </span>
                <div class="card-actions">
                    ${canEdit ? `<button class="action-btn action-edit" data-edit="${task.id}" title="编辑任务">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    </button>` : ''}
                    ${canDelete ? `<button class="action-btn action-delete" data-delete="${task.id}" title="删除任务">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6M14 11v6"></path><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"></path></svg>
                    </button>` : ''}
                </div>
            </div>
            <h4 class="task-title text-base font-semibold text-slate-800 mb-3 leading-snug">${escapeHtml(task.title)}</h4>
            <div class="flex flex-wrap gap-2 mb-3">
                <span class="${dueClass}">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                    ${formatDate(task.dueDate)}${overdue ? '（已逾期）' : ''}
                </span>
                ${statusActions}
            </div>
            <div class="flex items-center justify-between pt-3 border-t border-slate-100">
                <span class="assignee-chip">
                    <span class="assignee-avatar-sm" style="background:${getAvatarColor(task.assigneeId)}">${getInitial(task.assigneeName)}</span>
                    ${escapeHtml(task.assigneeName)}
                </span>
                <span class="text-xs text-slate-400">#${task.id}</span>
            </div>
        </div>
    `;
}

function escapeHtml(s) {
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
}

function renderTasks() {
    const pending = state.tasks.filter(t => t.status === 'PENDING');
    const inProgress = state.tasks.filter(t => t.status === 'IN_PROGRESS');
    const completed = state.tasks.filter(t => t.status === 'COMPLETED');

    $('pendingCount').textContent = pending.length;
    $('inProgressCount').textContent = inProgress.length;
    $('completedCount').textContent = completed.length;

    $('pendingTasks').innerHTML = pending.map(createTaskCard).join('') || emptyHint('暂无未开始任务');
    $('inProgressTasks').innerHTML = inProgress.map(createTaskCard).join('') || emptyHint('暂无进行中任务');
    $('completedTasks').innerHTML = completed.map(createTaskCard).join('') || emptyHint('暂无已完成任务');

    $('pendingSection').style.display = pending.length || showAllSections() ? '' : 'none';
    $('inProgressSection').style.display = inProgress.length || showAllSections() ? '' : 'none';
    $('completedSection').style.display = completed.length || showAllSections() ? '' : 'none';

    const totalEmpty = state.tasks.length === 0;
    $('tasksContainer').classList.toggle('hidden', totalEmpty);
    $('emptyState').classList.toggle('hidden', !totalEmpty);

    bindTaskEvents();
}

function emptyHint(msg) {
    return `<div class="col-span-full py-6 text-center text-sm text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200">${msg}</div>`;
}

function showAllSections() {
    return state.currentFilter === 'ALL';
}

function bindTaskEvents() {
    document.querySelectorAll('[data-edit]').forEach(btn => {
        btn.addEventListener('click', () => openEditModal(Number(btn.dataset.edit)));
    });
    document.querySelectorAll('[data-delete]').forEach(btn => {
        btn.addEventListener('click', () => deleteTask(Number(btn.dataset.delete)));
    });
    document.querySelectorAll('.status-dropdown').forEach(sel => {
        sel.addEventListener('change', (e) => {
            const id = Number(e.target.dataset.id);
            updateTaskStatus(id, e.target.value);
        });
    });
}

async function loadUsers() {
    const res = await api('/users');
    if (res.success) {
        state.users = res.data;
        state.userMap = {};
        res.data.forEach(u => { state.userMap[u.id] = u; });
        return true;
    }
    return false;
}

async function loadTasks() {
    let url = `/tasks?userId=${state.currentUserId}`;
    if (state.currentFilter) url += `&statusFilter=${state.currentFilter}`;
    if (state.currentAssigneeFilter) url += `&assigneeFilter=${state.currentAssigneeFilter}`;
    const res = await api(url);
    if (res.success) {
        state.tasks = res.data;
        return true;
    }
    return false;
}

async function refreshAll() {
    await loadTasks();
    renderHeader();
    renderStats();
    renderTasks();
}

function openCreateModal() {
    state.modalMode = 'create';
    state.editingTaskId = null;
    $('modalTitle').textContent = '新建任务';
    $('taskTitle').value = '';
    const today = new Date();
    const def = new Date(today.getTime() + 3 * 24 * 60 * 60 * 1000);
    $('taskDueDate').value = formatDate(def.toISOString().split('T')[0]);
    $('taskPriority').value = 'MEDIUM';
    $('taskStatus').value = 'PENDING';

    $('statusField').classList.add('hidden');
    initModalAssignee();
    if (!isLeader()) {
        $('taskAssignee').value = state.currentUserId;
    }

    openModal();
}

function openEditModal(id) {
    const task = state.tasks.find(t => t.id === id);
    if (!task) return;
    state.modalMode = 'edit';
    state.editingTaskId = id;
    $('modalTitle').textContent = '编辑任务';
    $('taskTitle').value = task.title;
    $('taskDueDate').value = formatDate(task.dueDate);
    $('taskPriority').value = task.priority;
    $('taskStatus').value = task.status;

    initModalAssignee();
    $('taskAssignee').value = task.assigneeId;

    const leader = isLeader();
    if (leader) {
        $('statusField').classList.remove('hidden');
        $('taskTitle').disabled = false;
        $('taskDueDate').disabled = false;
        $('taskPriority').disabled = false;
        $('taskAssignee').disabled = false;
    } else {
        $('statusField').classList.remove('hidden');
        $('taskTitle').disabled = true;
        $('taskDueDate').disabled = true;
        $('taskPriority').disabled = true;
        $('taskAssignee').disabled = true;
    }

    openModal();
}

function openModal() {
    $('modalOverlay').classList.add('modal-overlay-visible');
    setTimeout(() => $('taskTitle').focus(), 100);
}

function closeModal() {
    $('modalOverlay').classList.remove('modal-overlay-visible');
    $('taskTitle').disabled = false;
    $('taskDueDate').disabled = false;
    $('taskPriority').disabled = false;
}

async function submitModal() {
    const title = $('taskTitle').value.trim();
    const dueDate = $('taskDueDate').value;
    const priority = $('taskPriority').value;
    const status = $('taskStatus').value;
    const assigneeId = Number($('taskAssignee').value);

    if (state.modalMode === 'create') {
        if (!title) return showToast('请输入任务标题', 'error');
        if (!dueDate) return showToast('请选择截止日期', 'error');

        const body = { title, dueDate, priority };
        if (isLeader()) body.assigneeId = assigneeId;

        const res = await api(`/tasks?userId=${state.currentUserId}`, {
            method: 'POST',
            body: JSON.stringify(body)
        });
        if (res.success) {
            showToast('任务创建成功', 'success');
            closeModal();
            await refreshAll();
        } else {
            showToast(res.message || '创建失败', 'error');
        }
    } else {
        const body = {};
        const leader = isLeader();
        if (leader) {
            body.title = title;
            body.dueDate = dueDate;
            body.priority = priority;
            body.assigneeId = assigneeId;
        }
        body.status = status;

        const res = await api(`/tasks/${state.editingTaskId}?userId=${state.currentUserId}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
        if (res.success) {
            showToast('任务更新成功', 'success');
            closeModal();
            await refreshAll();
        } else {
            showToast(res.message || '更新失败', 'error');
        }
    }
}

async function updateTaskStatus(id, status) {
    const res = await api(`/tasks/${id}?userId=${state.currentUserId}`, {
        method: 'PUT',
        body: JSON.stringify({ status })
    });
    if (res.success) {
        showToast('状态更新成功', 'success');
        await refreshAll();
    } else {
        showToast(res.message || '更新失败', 'error');
        await refreshAll();
    }
}

async function deleteTask(id) {
    if (!confirm('确定要删除这个任务吗？')) return;
    const res = await api(`/tasks/${id}?userId=${state.currentUserId}`, { method: 'DELETE' });
    if (res.success) {
        showToast('任务删除成功', 'success');
        await refreshAll();
    } else {
        showToast(res.message || '删除失败', 'error');
    }
}

function bindEvents() {
    $('userSelect').addEventListener('change', async (e) => {
        state.currentUserId = Number(e.target.value);
        state.currentAssigneeFilter = '';
        $('assigneeFilter').value = '';
        initModalAssignee();
        await refreshAll();
    });

    $('statusFilter').addEventListener('change', async (e) => {
        state.currentFilter = e.target.value;
        await refreshAll();
    });

    $('assigneeFilter').addEventListener('change', async (e) => {
        state.currentAssigneeFilter = e.target.value;
        await refreshAll();
    });

    $('createTaskBtn').addEventListener('click', openCreateModal);
    $('closeModalBtn').addEventListener('click', closeModal);
    $('cancelModalBtn').addEventListener('click', closeModal);
    $('submitModalBtn').addEventListener('click', submitModal);

    $('modalOverlay').addEventListener('click', (e) => {
        if (e.target.id === 'modalOverlay') closeModal();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeModal();
        if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA' && e.target.tagName !== 'BUTTON') {
            if ($('modalOverlay').classList.contains('modal-overlay-visible')) {
                submitModal();
            }
        }
    });
}

async function init() {
    bindEvents();
    const ok = await loadUsers();
    if (!ok) {
        $('emptyState').classList.remove('hidden');
        $('emptyState').querySelector('h3').textContent = '无法连接到后端服务';
        $('emptyState').querySelector('p').textContent = '请确认后端已启动（默认端口 8080）';
        return;
    }
    initUserSelect();
    initAssigneeFilter();
    initModalAssignee();
    await refreshAll();
}

init();
