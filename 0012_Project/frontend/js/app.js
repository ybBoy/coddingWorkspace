var API_BASE = 'http://localhost:8080/api';

var currentUser = null;
var allUsers = [];
var currentRequestId = null;
var currentApprovalAction = null;
var currentMyFilter = 'all';
var currentMgrFilter = 'all';

function api(method, path, body) {
    var opts = {
        method: method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) opts.body = JSON.stringify(body);
    return fetch(API_BASE + path, opts).then(function(r) { return r.json(); });
}

function $(id) { return document.getElementById(id); }

function init() {
    loadUsers().then(function() {
        setupTabs();
        setupFilters();
        setupForm();
        setupModal();
        setupUserSwitch();
        if (allUsers.length > 0) {
            $('userSelect').value = allUsers[0].id;
            switchUser(allUsers[0].id);
        }
    });
}

function loadUsers() {
    return api('GET', '/users').then(function(users) {
        allUsers = users;
        var sel = $('userSelect');
        sel.innerHTML = '';
        users.forEach(function(u) {
            var opt = document.createElement('option');
            opt.value = u.id;
            opt.textContent = u.name + ' - ' + u.department + ' (' + u.role + ')';
            sel.appendChild(opt);
        });
    });
}

function setupUserSwitch() {
    $('userSelect').addEventListener('change', function() {
        switchUser(this.value);
    });
}

function switchUser(userId) {
    api('GET', '/users/' + userId).then(function(user) {
        currentUser = user;
        updateUserInfo();
        refreshData();
    });
}

function updateUserInfo() {
    if (!currentUser) return;
    var initial = currentUser.name.charAt(0);
    $('avatarDisplay').textContent = initial;
    $('bigAvatar').textContent = initial;
    $('userNameDisplay').textContent = currentUser.name;
    $('userStatusDisplay').textContent = currentUser.status;
    $('empName').textContent = currentUser.name;
    $('empDept').textContent = currentUser.department;
    $('empRole').textContent = currentUser.role;

    var badge = $('statusBadge');
    var dot = badge.querySelector('.status-dot');
    var text = badge.querySelector('.status-text');
    text.textContent = currentUser.status;
    if (currentUser.status === '正常在岗') {
        badge.className = 'status-badge active';
    } else {
        badge.className = 'status-badge on-leave';
    }

    var isManager = currentUser.role === '部门主管';
    $('tabManager').style.display = isManager ? '' : 'none';
    if (!isManager) {
        $('tabEmployee').click();
    }
}

function refreshData() {
    loadMyRecords();
    if (currentUser && currentUser.role === '部门主管') {
        loadAllRecords();
    }
}

function setupTabs() {
    var btns = document.querySelectorAll('.tab-btn');
    btns.forEach(function(btn) {
        btn.addEventListener('click', function() {
            btns.forEach(function(b) { b.classList.remove('active'); });
            btn.classList.add('active');
            document.querySelectorAll('.tab-content').forEach(function(tc) { tc.classList.remove('active'); });
            var tab = btn.getAttribute('data-tab');
            $(tab + 'Tab').classList.add('active');
            if (tab === 'manager') loadAllRecords();
        });
    });
}

function setupFilters() {
    $('myRecords').addEventListener('click', function(e) {
        if (e.target.classList.contains('filter-btn')) {
            e.target.parentElement.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
            e.target.classList.add('active');
            currentMyFilter = e.target.getAttribute('data-filter');
            loadMyRecords();
        }
    });

    $('allRecords').addEventListener('click', function(e) {
        if (e.target.classList.contains('filter-btn')) {
            e.target.parentElement.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
            e.target.classList.add('active');
            currentMgrFilter = e.target.getAttribute('data-filter');
            loadAllRecords();
        }
    });
}

function setupForm() {
    $('leaveForm').addEventListener('submit', function(e) {
        e.preventDefault();
        submitLeave();
    });

    var today = new Date().toISOString().split('T')[0];
    $('startDate').setAttribute('min', today);
    $('endDate').setAttribute('min', today);

    $('startDate').addEventListener('change', function() {
        $('endDate').setAttribute('min', this.value);
        if ($('endDate').value && $('endDate').value < this.value) {
            $('endDate').value = this.value;
        }
    });
}

function submitLeave() {
    if (!currentUser) return;
    var data = {
        userId: currentUser.id,
        leaveType: $('leaveType').value,
        startDate: $('startDate').value,
        endDate: $('endDate').value,
        reason: $('leaveReason').value
    };

    $('submitBtn').disabled = true;
    api('POST', '/leaves/submit', data).then(function(res) {
        $('submitBtn').disabled = false;
        var msg = $('formMessage');
        if (res.success) {
            msg.className = 'form-message success';
            msg.textContent = res.message;
            $('leaveForm').reset();
            switchUser(currentUser.id);
        } else {
            msg.className = 'form-message error';
            msg.textContent = res.message || '提交失败';
        }
        setTimeout(function() { msg.className = 'form-message'; }, 4000);
    }).catch(function() {
        $('submitBtn').disabled = false;
        var msg = $('formMessage');
        msg.className = 'form-message error';
        msg.textContent = '网络错误，请稍后重试';
        setTimeout(function() { msg.className = 'form-message'; }, 4000);
    });
}

function loadMyRecords() {
    if (!currentUser) return;
    api('GET', '/leaves/my/' + currentUser.id).then(function(records) {
        renderRecords(records, $('myRecords'), currentMyFilter, false);
    });
}

function loadAllRecords() {
    api('GET', '/leaves/all').then(function(records) {
        renderRecords(records, $('allRecords'), currentMgrFilter, true);
    });
}

function getTypeClass(type) {
    if (type === '年假') return 'annual';
    if (type === '病假') return 'sick';
    return 'personal';
}

function getStatusClass(status) {
    if (status === '待审批') return 'pending';
    if (status === '已通过') return 'approved';
    return 'rejected';
}

function renderRecords(records, container, filter, showActions) {
    var filtered = filter === 'all' ? records : records.filter(function(r) { return r.status === filter; });
    filtered.sort(function(a, b) { return b.createTime > a.createTime ? 1 : -1; });

    if (filtered.length === 0) {
        container.innerHTML = '<div class="empty-state">暂无请假记录</div>';
        var header = container.parentElement.querySelector('.card-header');
        if (header) {
            var existing = header.querySelector('.filter-tabs');
            if (existing) existing.style.display = '';
        }
        return;
    }

    var html = '';
    var filterHtml = container.parentElement.querySelector('.card-header .filter-tabs');
    if (filterHtml) filterHtml.style.display = '';

    filtered.forEach(function(r) {
        var initial = r.userName ? r.userName.charAt(0) : '?';
        var typeClass = getTypeClass(r.leaveType);
        var statusClass = getStatusClass(r.status);

        html += '<div class="record-item">';
        html += '<div class="record-avatar">' + initial + '</div>';
        html += '<div class="record-content">';
        html += '<div class="record-header">';
        html += '<span class="record-title">' + (showActions ? r.userName + ' · ' : '') + r.department + '</span>';
        html += '<div style="display:flex;gap:6px;align-items:center;">';
        html += '<span class="record-type ' + typeClass + '">' + r.leaveType + '</span>';
        html += '<span class="record-status ' + statusClass + '">' + r.status + '</span>';
        html += '</div></div>';
        html += '<div class="record-meta">';
        html += '<span>📅 ' + r.startDate + ' ~ ' + r.endDate + '</span>';
        html += '<span>📝 提交于 ' + r.createTime + '</span>';
        html += '</div>';
        html += '<div class="record-reason">事由：' + escapeHtml(r.reason) + '</div>';
        if (r.approverComment) {
            html += '<div class="record-comment">审批意见：' + escapeHtml(r.approverComment) + '</div>';
        }
        if (showActions && r.status === '待审批') {
            html += '<div class="record-actions">';
            html += '<button class="btn btn-success" onclick="openApproval(\'' + r.id + '\', \'approve\')">通过</button>';
            html += '<button class="btn btn-danger" onclick="openApproval(\'' + r.id + '\', \'reject\')">拒绝</button>';
            html += '</div>';
        }
        html += '</div></div>';
    });

    var filterTabsHtml = '';
    if (container.parentElement.querySelector('.filter-tabs')) {
        filterTabsHtml = container.parentElement.querySelector('.filter-tabs').outerHTML;
    }

    container.innerHTML = html;

    if (filterTabsHtml && !container.parentElement.querySelector('.filter-tabs')) {
        container.parentElement.querySelector('.card-header').insertAdjacentHTML('beforeend', filterTabsHtml);
    }
}

function escapeHtml(text) {
    if (!text) return '';
    var d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
}

function openApproval(requestId, action) {
    currentRequestId = requestId;
    currentApprovalAction = action;
    $('modalTitle').textContent = action === 'approve' ? '通过审批' : '拒绝审批';
    $('approvalComment').value = '';
    $('commentHint').classList.remove('show');
    $('btnApprove').style.display = action === 'approve' ? '' : 'none';
    $('btnReject').style.display = action === 'reject' ? '' : 'none';

    api('GET', '/leaves/all').then(function(records) {
        var req = records.find(function(r) { return r.id === requestId; });
        if (req) {
            $('modalDetail').innerHTML =
                '<strong>申请人：</strong>' + req.userName + '<br>' +
                '<strong>部门：</strong>' + req.department + '<br>' +
                '<strong>请假类型：</strong>' + req.leaveType + '<br>' +
                '<strong>起止日期：</strong>' + req.startDate + ' ~ ' + req.endDate + '<br>' +
                '<strong>事由：</strong>' + escapeHtml(req.reason);
        }
    });

    $('approvalModal').classList.add('show');
}

function setupModal() {
    $('modalClose').addEventListener('click', closeModal);
    $('btnCancel').addEventListener('click', closeModal);
    $('approvalModal').addEventListener('click', function(e) {
        if (e.target === $('approvalModal')) closeModal();
    });

    $('btnApprove').addEventListener('click', function() {
        doApproval('approve');
    });

    $('btnReject').addEventListener('click', function() {
        doApproval('reject');
    });
}

function closeModal() {
    $('approvalModal').classList.remove('show');
    currentRequestId = null;
    currentApprovalAction = null;
}

function doApproval(action) {
    var comment = $('approvalComment').value.trim();

    if (action === 'reject' && !comment) {
        $('commentHint').textContent = '拒绝申请时必须填写审批意见';
        $('commentHint').classList.add('show');
        return;
    }

    var path = action === 'approve'
        ? '/leaves/approve/' + currentRequestId
        : '/leaves/reject/' + currentRequestId;

    api('POST', path, { comment: comment }).then(function(res) {
        if (res.success) {
            closeModal();
            refreshData();
        } else {
            $('commentHint').textContent = res.message || '操作失败';
            $('commentHint').classList.add('show');
        }
    }).catch(function() {
        $('commentHint').textContent = '网络错误，请稍后重试';
        $('commentHint').classList.add('show');
    });
}

document.addEventListener('DOMContentLoaded', init);
