var API_BASE = 'http://localhost:8088/api';

var state = {
    currentUserId: '',
    members: [],
    tasks: [],
    isLeader: false,
    filter: '全部',
    editingTaskId: null,
    deleteTaskId: null
};

var dom = {
    userSelector: document.getElementById('user-selector'),
    userInfo: document.getElementById('user-info'),
    userAvatar: document.getElementById('user-avatar'),
    userName: document.getElementById('user-name'),
    userRole: document.getElementById('user-role'),
    taskList: document.getElementById('task-list'),
    btnNewTask: document.getElementById('btn-new-task'),
    assigneeFilterWrap: document.getElementById('assignee-filter-wrap'),
    assigneeFilter: document.getElementById('assignee-filter'),
    modalOverlay: document.getElementById('modal-overlay'),
    modalTitle: document.getElementById('modal-title'),
    modalClose: document.getElementById('modal-close'),
    taskTitle: document.getElementById('task-title'),
    taskDueDate: document.getElementById('task-due-date'),
    taskPriority: document.getElementById('task-priority'),
    taskAssignee: document.getElementById('task-assignee'),
    assigneeGroup: document.getElementById('assignee-group'),
    taskStatus: document.getElementById('task-status'),
    statusGroup: document.getElementById('status-group'),
    btnCancel: document.getElementById('btn-cancel'),
    btnSave: document.getElementById('btn-save'),
    confirmOverlay: document.getElementById('confirm-overlay'),
    confirmClose: document.getElementById('confirm-close'),
    confirmCancel: document.getElementById('confirm-cancel'),
    confirmOk: document.getElementById('confirm-ok'),
    filterBtns: document.querySelectorAll('.filter-btn')
};

function api(method, path, body) {
    var opts = {
        method: method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) opts.body = JSON.stringify(body);
    return fetch(API_BASE + path, opts).then(function(r) {
        if (!r.ok) throw new Error('请求失败: ' + r.status);
        return r.json();
    });
}

function loadMembers() {
    api('GET', '/members').then(function(data) {
        state.members = data;
        dom.userSelector.innerHTML = '<option value="">-- 选择用户 --</option>';
        data.forEach(function(m) {
            var opt = document.createElement('option');
            opt.value = m.id;
            opt.textContent = m.name + (m.role === 'leader' ? ' (组长)' : '');
            dom.userSelector.appendChild(opt);
        });
        dom.assigneeFilter.innerHTML = '<option value="">全部成员</option>';
        data.forEach(function(m) {
            var opt = document.createElement('option');
            opt.value = m.id;
            opt.textContent = m.name;
            dom.assigneeFilter.appendChild(opt);
        });
        dom.taskAssignee.innerHTML = '';
        data.forEach(function(m) {
            var opt = document.createElement('option');
            opt.value = m.id;
            opt.textContent = m.name;
            dom.taskAssignee.appendChild(opt);
        });
    });
}

function loadUserInfo(userId) {
    api('GET', '/members/' + userId).then(function(data) {
        state.currentUserId = userId;
        state.isLeader = data.isLeader;
        dom.userAvatar.textContent = data.avatar;
        dom.userName.textContent = data.name;
        dom.userRole.textContent = data.isLeader ? '组长' : '成员';
        dom.assigneeFilterWrap.style.display = data.isLeader ? 'block' : 'none';
        dom.btnNewTask.disabled = false;
        loadTasks();
    });
}

function loadTasks() {
    if (!state.currentUserId) return;
    var url = '/tasks?memberId=' + state.currentUserId;
    if (state.isLeader && dom.assigneeFilter.value) {
        url += '&assigneeId=' + dom.assigneeFilter.value;
    }
    if (state.filter === '未完成') {
        url += '&status=未完成';
    } else if (state.filter === '已完成') {
        url += '&status=已完成';
    }
    api('GET', url).then(function(data) {
        state.tasks = data.tasks;
        state.isLeader = data.isLeader;
        renderTasks();
    });
}

function renderTasks() {
    if (!state.currentUserId) {
        dom.taskList.innerHTML = '<div class="empty-state">请先选择用户</div>';
        return;
    }
    if (state.tasks.length === 0) {
        dom.taskList.innerHTML = '<div class="empty-state">暂无任务，点击右上角新建任务吧 ✨</div>';
        return;
    }
    var html = '';
    state.tasks.forEach(function(t) {
        html += '<div class="task-card priority-' + t.priority + ' status-' + t.status + '">';
        html += '<div class="task-status-dot ' + t.status + '"></div>';
        html += '<div class="task-content">';
        html += '<div class="task-title">' + escapeHtml(t.title) + '</div>';
        html += '<div class="task-meta">';
        if (state.isLeader) {
            html += '<span class="task-meta-item">👤 ' + escapeHtml(t.assigneeName) + '</span>';
        }
        html += '<span class="task-meta-item">📅 ' + escapeHtml(t.dueDate || '未设置') + '</span>';
        html += '<span class="priority-tag ' + t.priority + '">' + t.priority + '优先级</span>';
        html += '<span class="status-tag ' + t.status + '">' + t.status + '</span>';
        html += '</div></div>';
        html += '<div class="task-actions">';
        html += '<button class="action-btn edit-btn" data-id="' + t.id + '" title="编辑">✏️</button>';
        if (state.isLeader) {
            html += '<button class="action-btn delete-btn" data-id="' + t.id + '" title="删除">🗑️</button>';
        }
        html += '</div></div>';
    });
    dom.taskList.innerHTML = html;
}

function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function openModal(task) {
    state.editingTaskId = task ? task.id : null;
    dom.modalTitle.textContent = task ? '编辑任务' : '新建任务';
    dom.taskTitle.value = task ? task.title : '';
    dom.taskDueDate.value = task ? task.dueDate : '';
    dom.taskPriority.value = task ? task.priority : '中';
    dom.taskStatus.value = task ? task.status : '未开始';

    if (state.isLeader) {
        dom.assigneeGroup.style.display = 'block';
        dom.taskAssignee.value = task ? task.assigneeId : state.currentUserId;
        dom.statusGroup.style.display = 'block';
    } else {
        dom.assigneeGroup.style.display = 'none';
        if (task) {
            dom.statusGroup.style.display = 'block';
        } else {
            dom.statusGroup.style.display = 'none';
            dom.taskStatus.value = '未开始';
        }
    }

    dom.modalOverlay.classList.add('show');
    dom.taskTitle.focus();
}

function closeModal() {
    dom.modalOverlay.classList.remove('show');
    state.editingTaskId = null;
}

function saveTask() {
    var title = dom.taskTitle.value.trim();
    if (!title) {
        dom.taskTitle.style.borderColor = '#ef4444';
        dom.taskTitle.focus();
        return;
    }
    dom.taskTitle.style.borderColor = '';

    var payload = {
        title: title,
        dueDate: dom.taskDueDate.value,
        priority: dom.taskPriority.value,
        status: dom.taskStatus.value,
        assigneeId: state.isLeader ? dom.taskAssignee.value : state.currentUserId
    };

    if (state.editingTaskId) {
        api('PUT', '/tasks/' + state.editingTaskId + '?memberId=' + state.currentUserId, payload)
            .then(function() { closeModal(); loadTasks(); })
            .catch(function(e) { alert('更新失败: ' + e.message); });
    } else {
        api('POST', '/tasks', payload)
            .then(function() { closeModal(); loadTasks(); })
            .catch(function(e) { alert('创建失败: ' + e.message); });
    }
}

function confirmDelete(taskId) {
    state.deleteTaskId = taskId;
    dom.confirmOverlay.classList.add('show');
}

function closeConfirm() {
    dom.confirmOverlay.classList.remove('show');
    state.deleteTaskId = null;
}

function doDelete() {
    if (!state.deleteTaskId) return;
    api('DELETE', '/tasks/' + state.deleteTaskId + '?memberId=' + state.currentUserId)
        .then(function(data) {
            if (data.success) {
                closeConfirm();
                loadTasks();
            } else {
                alert(data.message || '删除失败');
            }
        })
        .catch(function(e) { alert('删除失败: ' + e.message); });
}

dom.userSelector.addEventListener('change', function() {
    var userId = this.value;
    if (userId) {
        loadUserInfo(userId);
    } else {
        state.currentUserId = '';
        state.isLeader = false;
        dom.userAvatar.textContent = '';
        dom.userName.textContent = '';
        dom.userRole.textContent = '';
        dom.btnNewTask.disabled = true;
        dom.assigneeFilterWrap.style.display = 'none';
        renderTasks();
    }
});

dom.filterBtns.forEach(function(btn) {
    btn.addEventListener('click', function() {
        dom.filterBtns.forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');
        state.filter = btn.getAttribute('data-filter');
        loadTasks();
    });
});

dom.assigneeFilter.addEventListener('change', loadTasks);

dom.btnNewTask.addEventListener('click', function() {
    if (!state.currentUserId) return;
    openModal(null);
});

dom.taskList.addEventListener('click', function(e) {
    var editBtn = e.target.closest('.edit-btn');
    var deleteBtn = e.target.closest('.delete-btn');
    if (editBtn) {
        var taskId = editBtn.getAttribute('data-id');
        var task = state.tasks.find(function(t) { return t.id === taskId; });
        if (task) openModal(task);
    }
    if (deleteBtn) {
        confirmDelete(deleteBtn.getAttribute('data-id'));
    }
});

dom.modalClose.addEventListener('click', closeModal);
dom.btnCancel.addEventListener('click', closeModal);
dom.btnSave.addEventListener('click', saveTask);
dom.modalOverlay.addEventListener('click', function(e) {
    if (e.target === dom.modalOverlay) closeModal();
});

dom.confirmClose.addEventListener('click', closeConfirm);
dom.confirmCancel.addEventListener('click', closeConfirm);
dom.confirmOk.addEventListener('click', doDelete);
dom.confirmOverlay.addEventListener('click', function(e) {
    if (e.target === dom.confirmOverlay) closeConfirm();
});

dom.taskTitle.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') saveTask();
});

loadMembers();
