(function() {
    const API = '/api';
    let currentUser = null;
    let users = [];
    let allRooms = [];
    let todayStr = '';

    const $ = function(id) { return document.getElementById(id); };

    function showToast(msg, type) {
        var toast = $('toast');
        toast.textContent = msg;
        toast.className = 'toast ' + (type || 'success');
        setTimeout(function() { toast.classList.add('show'); }, 10);
        setTimeout(function() { toast.classList.remove('show'); }, 2500);
    }

    function ajax(url, options) {
        options = options || {};
        return fetch(url, Object.assign({
            headers: { 'Content-Type': 'application/json' }
        }, options)).then(function(r) { return r.json(); });
    }

    function getTodayStr() {
        var d = new Date();
        var y = d.getFullYear();
        var m = String(d.getMonth() + 1).padStart(2, '0');
        var day = String(d.getDate()).padStart(2, '0');
        return y + '-' + m + '-' + day;
    }

    function formatDateCN(dateStr) {
        var parts = dateStr.split('-');
        return parts[0] + '年' + parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日';
    }

    function isToday(dateStr) {
        return dateStr === todayStr;
    }

    function isOwnBooking(b) {
        return currentUser && b.userId === currentUser.id;
    }

    function canCancel(b) {
        if (!currentUser) return false;
        if (currentUser.role === 'ADMIN') return true;
        return isOwnBooking(b);
    }

    function loadUsers() {
        return ajax(API + '/users').then(function(res) {
            if (res.success) {
                users = res.data;
                renderUserSelect();
                renderForUserSelect();
                if (!currentUser && users.length > 0) {
                    selectUser(users[0].id);
                }
            }
        });
    }

    function renderUserSelect() {
        var sel = $('userSelect');
        sel.innerHTML = '';
        users.forEach(function(u) {
            var opt = document.createElement('option');
            opt.value = u.id;
            opt.textContent = u.name;
            sel.appendChild(opt);
        });
        sel.onchange = function() { selectUser(sel.value); };
    }

    function renderForUserSelect() {
        var sel = $('forUserSelect');
        sel.innerHTML = '';
        users.forEach(function(u) {
            var opt = document.createElement('option');
            opt.value = u.id;
            opt.textContent = u.name;
            sel.appendChild(opt);
        });
    }

    function selectUser(userId) {
        ajax(API + '/user?id=' + userId).then(function(res) {
            if (res.success) {
                currentUser = res.data;
                $('userSelect').value = userId;
                var roleEl = $('userRole');
                if (currentUser.role === 'ADMIN') {
                    roleEl.textContent = '管理员';
                    roleEl.className = 'user-role admin';
                    $('adminFilters').style.display = 'flex';
                    $('forUserGroup').style.display = 'block';
                } else {
                    roleEl.textContent = '普通员工';
                    roleEl.className = 'user-role';
                    $('adminFilters').style.display = 'none';
                    $('forUserGroup').style.display = 'none';
                }
                loadRooms();
            }
        });
    }

    function loadRooms() {
        if (!currentUser) return;
        ajax(API + '/rooms?userId=' + currentUser.id).then(function(res) {
            if (res.success) {
                allRooms = res.data;
                todayStr = res.today || getTodayStr();
                $('todayDate').textContent = '今天：' + formatDateCN(todayStr);
                renderRoomFilter();
                renderRooms();
            }
        });
    }

    function renderRoomFilter() {
        var sel = $('roomFilter');
        var cur = sel.value;
        sel.innerHTML = '<option value="">全部会议室</option>';
        allRooms.forEach(function(r) {
            var opt = document.createElement('option');
            opt.value = r.id;
            opt.textContent = r.name;
            sel.appendChild(opt);
        });
        sel.value = cur;
    }

    function renderRooms() {
        var container = $('roomsContainer');
        var filter = $('roomFilter').value;
        container.innerHTML = '';
        allRooms.forEach(function(room) {
            if (filter && room.id !== filter) return;
            container.appendChild(createRoomCard(room));
        });
    }

    function createRoomCard(room) {
        var card = document.createElement('div');
        card.className = 'room-card';

        var header = document.createElement('div');
        header.className = 'room-header';
        header.innerHTML = '<span class="room-name">' + escapeHtml(room.name) +
            '</span><span class="room-capacity">容纳 ' + room.capacity + ' 人</span>';
        card.appendChild(header);

        var body = document.createElement('div');
        body.className = 'room-body';

        var section = document.createElement('div');
        section.className = 'bookings-section';

        var titleText = currentUser.role === 'ADMIN' ? '全部预订' : '今日预订';
        section.innerHTML = '<div class="bookings-title">' + titleText + ' (' +
            room.bookings.length + ')</div>';

        var list = document.createElement('ul');
        list.className = 'booking-list';

        if (room.bookings.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'empty-bookings';
            empty.textContent = currentUser.role === 'ADMIN' ? '暂无预订记录' : '今日暂无预订';
            section.appendChild(empty);
        } else {
            room.bookings.forEach(function(b) {
                list.appendChild(createBookingItem(b));
            });
            section.appendChild(list);
        }

        body.appendChild(section);

        var btn = document.createElement('button');
        btn.className = 'btn btn-primary btn-block';
        btn.textContent = '＋ 预订此会议室';
        btn.onclick = function() { openBookingModal(room.id); };
        body.appendChild(btn);

        card.appendChild(body);
        return card;
    }

    function createBookingItem(b) {
        var li = document.createElement('li');
        var classes = ['booking-item'];
        if (isToday(b.date)) classes.push('today');
        if (isOwnBooking(b)) classes.push('own');
        li.className = classes.join(' ');

        var top = document.createElement('div');
        top.className = 'booking-top';
        top.innerHTML = '<span class="booking-time">' + b.startTime + ' - ' + b.endTime + '</span>' +
            '<span class="booking-date-tag">' + formatDateCN(b.date) + '</span>';
        li.appendChild(top);

        var info = document.createElement('div');
        info.className = 'booking-info';
        info.innerHTML = '<span class="booking-user">' + escapeHtml(b.userName) + '</span>' +
            '<br>事由：' + escapeHtml(b.purpose || '');
        li.appendChild(info);

        if (canCancel(b)) {
            var actions = document.createElement('div');
            actions.className = 'booking-actions';
            var btn = document.createElement('button');
            btn.className = 'btn btn-danger';
            btn.textContent = '取消预订';
            btn.onclick = function(e) {
                e.stopPropagation();
                cancelBooking(b.id);
            };
            actions.appendChild(btn);
            li.appendChild(actions);
        }

        return li;
    }

    function escapeHtml(s) {
        if (!s) return '';
        var div = document.createElement('div');
        div.textContent = s;
        return div.innerHTML;
    }

    function cancelBooking(id) {
        if (!confirm('确定要取消此预订吗？')) return;
        ajax(API + '/bookings/' + id + '?userId=' + currentUser.id, {
            method: 'DELETE'
        }).then(function(res) {
            if (res.success) {
                showToast('取消成功');
                loadRooms();
            } else {
                showToast(res.message || '取消失败', 'error');
            }
        });
    }

    function openBookingModal(roomId) {
        $('bookingModal').classList.add('show');
        $('bookingForm').reset();

        var roomSel = $('modalRoomSelect');
        roomSel.innerHTML = '';
        allRooms.forEach(function(r) {
            var opt = document.createElement('option');
            opt.value = r.id;
            opt.textContent = r.name + ' (' + r.capacity + '人)';
            roomSel.appendChild(opt);
        });
        if (roomId) roomSel.value = roomId;

        var dateInput = $('bookingDate');
        dateInput.value = todayStr;
        dateInput.min = todayStr;

        if (currentUser.role === 'ADMIN') {
            $('forUserGroup').style.display = 'block';
            $('forUserSelect').value = currentUser.id;
        } else {
            $('forUserGroup').style.display = 'none';
        }

        populateTimeSelects();
    }

    function populateTimeSelects() {
        var startSel = $('startTime');
        var endSel = $('endTime');
        startSel.innerHTML = '';
        endSel.innerHTML = '';
        for (var h = 8; h <= 21; h++) {
            for (var m = 0; m < 60; m += 30) {
                if (h === 21 && m > 0) break;
                var t = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
                var opt1 = document.createElement('option');
                opt1.value = t; opt1.textContent = t;
                startSel.appendChild(opt1);
                var opt2 = document.createElement('option');
                opt2.value = t; opt2.textContent = t;
                endSel.appendChild(opt2);
            }
        }
        startSel.value = '09:00';
        endSel.value = '10:00';
        startSel.onchange = adjustEndTime;
    }

    function adjustEndTime() {
        var startSel = $('startTime');
        var endSel = $('endTime');
        var sv = startSel.value;
        var ev = endSel.value;
        if (sv >= ev) {
            var parts = sv.split(':');
            var h = parseInt(parts[0]);
            var m = parseInt(parts[1]);
            m += 30;
            if (m >= 60) { h++; m = 0; }
            endSel.value = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
        }
    }

    function closeModal() {
        $('bookingModal').classList.remove('show');
    }

    function submitBooking(e) {
        e.preventDefault();
        var roomId = $('modalRoomSelect').value;
        var userId = currentUser.role === 'ADMIN' ? $('forUserSelect').value : currentUser.id;
        var date = $('bookingDate').value;
        var startTime = $('startTime').value;
        var endTime = $('endTime').value;
        var purpose = $('purpose').value.trim();

        ajax(API + '/bookings', {
            method: 'POST',
            body: JSON.stringify({
                roomId: roomId, userId: userId, date: date,
                startTime: startTime, endTime: endTime, purpose: purpose
            })
        }).then(function(res) {
            if (res.success) {
                showToast('预订成功');
                closeModal();
                loadRooms();
            } else {
                showToast(res.message || '预订失败', 'error');
            }
        });
    }

    function init() {
        $('closeModal').onclick = closeModal;
        $('cancelBtn').onclick = closeModal;
        $('bookingForm').onsubmit = submitBooking;
        $('roomFilter').onchange = renderRooms;

        $('bookingModal').onclick = function(e) {
            if (e.target.id === 'bookingModal') closeModal();
        };

        loadUsers();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
