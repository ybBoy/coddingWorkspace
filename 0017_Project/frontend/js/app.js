const API_BASE = 'http://localhost:8081/api';

const DEPARTMENT_LABELS = {
    TECH: '技术部',
    PRODUCT: '产品部',
    OPERATIONS: '运营部',
    HR: '人事部'
};

const ROLE_LABELS = {
    EMPLOYEE: '普通员工',
    MANAGER: '部门主管'
};

const STATUS_LABELS = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
};

const TIMEOFF_TYPE_LABELS = {
    HALF_DAY: '半天',
    FULL_DAY: '全天'
};

let currentUser = null;
let allUsers = [];
let pendingRejectData = null;

async function apiGet(url) {
    const res = await fetch(API_BASE + url);
    return await res.json();
}

async function apiPost(url, data) {
    const res = await fetch(API_BASE + url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    return await res.json();
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    setTimeout(() => {
        toast.className = `toast ${type}`;
    }, 3000);
}

function getAvatarInitial(name) {
    return name ? name.charAt(0) : '?';
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    return dateStr;
}

function formatTime(timeStr) {
    if (!timeStr) return '-';
    return timeStr.substring(0, 5);
}

function getStatusBadge(status) {
    const cls = status === 'APPROVED' ? 'status-approved' :
                status === 'REJECTED' ? 'status-rejected' : 'status-pending';
    return `<span class="status-badge ${cls}">${STATUS_LABELS[status] || status}</span>`;
}

function loadUsers() {
    return apiGet('/users').then(res => {
        if (res.success) {
            allUsers = res.data;
            populateUserSelect();
        }
        return res;
    });
}

function populateUserSelect() {
    const select = document.getElementById('userSelect');
    select.innerHTML = '';
    allUsers.forEach(user => {
        const opt = document.createElement('option');
        opt.value = user.id;
        const dept = DEPARTMENT_LABELS[user.department] || user.department;
        const role = ROLE_LABELS[user.role] || user.role;
        opt.textContent = `${user.name} - ${dept}（${role}）`;
        select.appendChild(opt);
    });
    if (allUsers.length > 0) {
        selectUser(allUsers[0].id);
        select.value = allUsers[0].id;
    }
}

function selectUser(userId) {
    const user = allUsers.find(u => u.id == userId);
    if (!user) return;
    currentUser = user;
    updateUIForUser();
    loadDashboard();
    loadOvertimeList();
    loadTimeoffList();
    if (user.role === 'MANAGER') {
        loadApprovalLists();
        loadDepartmentReport();
    }
}

function updateUIForUser() {
    document.querySelectorAll('.manager-only').forEach(el => {
        el.style.display = currentUser.role === 'MANAGER' ? '' : 'none';
    });
    if (currentUser.role !== 'MANAGER') {
        const activeNav = document.querySelector('.nav-item.active');
        if (activeNav && activeNav.classList.contains('manager-only')) {
            switchView('dashboard');
            document.querySelector('.nav-item[data-view="dashboard"]').classList.add('active');
        }
    }
    document.getElementById('userAvatar').textContent = getAvatarInitial(currentUser.name);
    document.getElementById('userName').textContent = currentUser.name;
    const dept = DEPARTMENT_LABELS[currentUser.department] || currentUser.department;
    const role = ROLE_LABELS[currentUser.role] || currentUser.role;
    document.getElementById('userDeptRole').textContent = `${dept} · ${role}`;
}

function switchView(viewName) {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelector(`.nav-item[data-view="${viewName}"]`).classList.add('active');
    document.querySelectorAll('.view').forEach(el => el.classList.remove('active'));
    document.getElementById(`view-${viewName}`).classList.add('active');
}

function loadDashboard() {
    apiGet(`/report/my/${currentUser.id}`).then(res => {
        if (res.success) {
            document.getElementById('totalOvertime').textContent = res.data.totalOvertimeHours.toFixed(1);
            document.getElementById('usedTimeoff').textContent = res.data.usedTimeoffHours.toFixed(1);
            document.getElementById('remainingTimeoff').textContent = res.data.remainingTimeoffHours.toFixed(1);
        }
    });
}

function loadOvertimeList() {
    apiGet(`/overtime/my/${currentUser.id}`).then(res => {
        const tbody = document.getElementById('overtimeTableBody');
        if (!res.success || res.data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                <p>暂无加班记录</p></div></td></tr>`;
            return;
        }
        tbody.innerHTML = res.data.map(r => `
            <tr>
                <td>${formatDate(r.overtimeDate)}</td>
                <td>${formatTime(r.startTime)} - ${formatTime(r.endTime)}</td>
                <td>${r.hours}小时</td>
                <td>${r.reason || '-'}</td>
                <td>${getStatusBadge(r.status)}</td>
                <td>${r.approvalComment || '-'}</td>
                <td>${formatDate(r.createTime)}</td>
            </tr>
        `).join('');
    });
}

function loadTimeoffList() {
    apiGet(`/timeoff/my/${currentUser.id}`).then(res => {
        const tbody = document.getElementById('timeoffTableBody');
        if (!res.success || res.data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                <p>暂无调休记录</p></div></td></tr>`;
            return;
        }
        tbody.innerHTML = res.data.map(r => `
            <tr>
                <td>${formatDate(r.timeoffDate)}</td>
                <td>${TIMEOFF_TYPE_LABELS[r.timeoffType] || r.timeoffType}</td>
                <td>${r.hours}小时</td>
                <td>${r.reason || '-'}</td>
                <td>${getStatusBadge(r.status)}</td>
                <td>${r.approvalComment || '-'}</td>
                <td>${formatDate(r.createTime)}</td>
            </tr>
        `).join('');
    });
}

async function getUserRemaining(userId) {
    try {
        const res = await apiGet(`/report/my/${userId}`);
        if (res.success) {
            return res.data.remainingTimeoffHours;
        }
    } catch (e) {}
    return 0;
}

async function loadApprovalLists() {
    const [otRes, toRes] = await Promise.all([
        apiGet(`/overtime/pending/${currentUser.id}`),
        apiGet(`/timeoff/pending/${currentUser.id}`)
    ]);

    const otTbody = document.getElementById('otApprovalTableBody');
    if (!otRes.success || otRes.data.length === 0) {
        otTbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
            </svg>
            <p>暂无待审批的加班申请</p></div></td></tr>`;
    } else {
        otTbody.innerHTML = (await Promise.all(otRes.data.map(async r => {
            const remaining = await getUserRemaining(r.userId);
            return `
            <tr>
                <td>${r.userName}</td>
                <td>${formatDate(r.overtimeDate)}</td>
                <td>${formatTime(r.startTime)} - ${formatTime(r.endTime)}</td>
                <td>${r.hours}小时</td>
                <td>${r.reason || '-'}</td>
                <td><strong>${remaining.toFixed(1)}</strong>小时</td>
                <td>
                    <div class="btn-group">
                        <button class="btn-success btn-sm" onclick="approveOvertime(${r.id})">通过</button>
                        <button class="btn-danger btn-sm" onclick="openRejectModal('overtime', ${r.id})">拒绝</button>
                    </div>
                </td>
            </tr>`;
        }))).join('');
    }

    const toTbody = document.getElementById('toApprovalTableBody');
    if (!toRes.success || toRes.data.length === 0) {
        toTbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
            </svg>
            <p>暂无待审批的调休申请</p></div></td></tr>`;
    } else {
        toTbody.innerHTML = (await Promise.all(toRes.data.map(async r => {
            const remaining = await getUserRemaining(r.userId);
            return `
            <tr>
                <td>${r.userName}</td>
                <td>${formatDate(r.timeoffDate)}</td>
                <td>${TIMEOFF_TYPE_LABELS[r.timeoffType] || r.timeoffType}</td>
                <td>${r.hours}小时</td>
                <td>${r.reason || '-'}</td>
                <td><strong>${remaining.toFixed(1)}</strong>小时</td>
                <td>
                    <div class="btn-group">
                        <button class="btn-success btn-sm" onclick="approveTimeoff(${r.id})">通过</button>
                        <button class="btn-danger btn-sm" onclick="openRejectModal('timeoff', ${r.id})">拒绝</button>
                    </div>
                </td>
            </tr>`;
        }))).join('');
    }
}

function loadDepartmentReport() {
    apiGet(`/report/department/${currentUser.id}`).then(res => {
        const tbody = document.getElementById('reportTableBody');
        if (!res.success || res.data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
                </svg>
                <p>暂无数据</p></div></td></tr>`;
            return;
        }
        tbody.innerHTML = res.data.map(r => `
            <tr>
                <td>${r.userName}</td>
                <td>${DEPARTMENT_LABELS[r.department] || r.department}</td>
                <td>${r.totalOvertimeHours.toFixed(1)}小时</td>
                <td>${r.usedTimeoffHours.toFixed(1)}小时</td>
                <td><strong>${r.remainingTimeoffHours.toFixed(1)}</strong>小时</td>
            </tr>
        `).join('');
    });
}

function openModal(modalId) {
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

function calculateOvertimeHours() {
    const start = document.getElementById('otStart').value;
    const end = document.getElementById('otEnd').value;
    if (!start || !end) {
        document.getElementById('otHours').value = '';
        return;
    }
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const startMin = sh * 60 + sm;
    const endMin = eh * 60 + em;
    if (endMin <= startMin) {
        document.getElementById('otHours').value = '';
        return;
    }
    const diffMin = endMin - startMin;
    const rawHours = diffMin / 60;
    const halfHours = Math.ceil(rawHours * 2) / 2;
    document.getElementById('otHours').value = halfHours + ' 小时';
}

function openOvertimeModal() {
    document.getElementById('overtimeForm').reset();
    document.getElementById('otHours').value = '';
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('otDate').max = today;
    openModal('overtimeModal');
}

async function submitOvertime() {
    const overtimeDate = document.getElementById('otDate').value;
    const startTime = document.getElementById('otStart').value;
    const endTime = document.getElementById('otEnd').value;
    const reason = document.getElementById('otReason').value.trim();

    if (!overtimeDate || !startTime || !endTime || !reason) {
        showToast('请填写所有必填项', 'error');
        return;
    }

    const res = await apiPost('/overtime', {
        userId: currentUser.id,
        overtimeDate,
        startTime,
        endTime,
        reason
    });

    if (res.success) {
        showToast('加班申请提交成功');
        closeModal('overtimeModal');
        loadOvertimeList();
    } else {
        showToast(res.message || '提交失败', 'error');
    }
}

async function openTimeoffModal() {
    document.getElementById('timeoffForm').reset();
    const statsRes = await apiGet(`/report/my/${currentUser.id}`);
    if (statsRes.success) {
        document.getElementById('currentRemaining').textContent = statsRes.data.remainingTimeoffHours.toFixed(1);
    }
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('toDate').min = today;
    openModal('timeoffModal');
}

async function submitTimeoff() {
    const timeoffDate = document.getElementById('toDate').value;
    const timeoffType = document.getElementById('toType').value;
    const reason = document.getElementById('toReason').value.trim();

    if (!timeoffDate || !timeoffType || !reason) {
        showToast('请填写所有必填项', 'error');
        return;
    }

    const res = await apiPost('/timeoff', {
        userId: currentUser.id,
        timeoffDate,
        timeoffType,
        reason
    });

    if (res.success) {
        showToast('调休申请提交成功');
        closeModal('timeoffModal');
        loadTimeoffList();
    } else {
        showToast(res.message || '提交失败', 'error');
    }
}

async function approveOvertime(requestId) {
    const res = await apiPost('/overtime/approve', {
        requestId,
        approverId: currentUser.id
    });
    if (res.success) {
        showToast('已通过审批');
        loadApprovalLists();
        loadDepartmentReport();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

async function approveTimeoff(requestId) {
    const res = await apiPost('/timeoff/approve', {
        requestId,
        approverId: currentUser.id
    });
    if (res.success) {
        showToast('已通过审批');
        loadApprovalLists();
        loadDepartmentReport();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

function openRejectModal(type, requestId) {
    pendingRejectData = { type, requestId };
    document.getElementById('rejectReason').value = '';
    openModal('rejectModal');
}

async function confirmReject() {
    const comment = document.getElementById('rejectReason').value.trim();
    if (!comment) {
        showToast('请填写拒绝原因', 'error');
        return;
    }
    if (!pendingRejectData) return;

    const url = pendingRejectData.type === 'overtime' ? '/overtime/reject' : '/timeoff/reject';
    const res = await apiPost(url, {
        requestId: pendingRejectData.requestId,
        approverId: currentUser.id,
        comment
    });

    if (res.success) {
        showToast('已拒绝申请');
        closeModal('rejectModal');
        pendingRejectData = null;
        loadApprovalLists();
        loadDepartmentReport();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

function bindEvents() {
    document.getElementById('userSelect').addEventListener('change', (e) => {
        selectUser(Number(e.target.value));
    });

    document.querySelectorAll('.nav-item').forEach(el => {
        el.addEventListener('click', (e) => {
            e.preventDefault();
            const view = el.dataset.view;
            if (el.classList.contains('manager-only') && currentUser.role !== 'MANAGER') {
                showToast('无权限访问', 'error');
                return;
            }
            switchView(view);
        });
    });

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
        });
    });

    document.getElementById('btnAddOvertime').addEventListener('click', openOvertimeModal);
    document.getElementById('btnAddTimeoff').addEventListener('click', openTimeoffModal);
    document.getElementById('btnSubmitOvertime').addEventListener('click', submitOvertime);
    document.getElementById('btnSubmitTimeoff').addEventListener('click', submitTimeoff);
    document.getElementById('btnConfirmReject').addEventListener('click', confirmReject);

    document.getElementById('otStart').addEventListener('change', calculateOvertimeHours);
    document.getElementById('otEnd').addEventListener('change', calculateOvertimeHours);

    document.querySelectorAll('[data-close]').forEach(el => {
        el.addEventListener('click', () => closeModal(el.dataset.close));
    });

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('active');
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    bindEvents();
    await loadUsers();
});
