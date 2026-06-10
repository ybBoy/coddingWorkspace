const API_BASE = 'http://localhost:8080/api';

let currentUser = null;
let allUsers = [];
let meetingRooms = [];
let currentScope = 'today';
let currentRoomFilter = '';

function apiRequest(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (currentUser) {
        headers['X-User-Id'] = currentUser.id;
    }

    return fetch(API_BASE + url, { ...options, headers })
        .then(response => response.json())
        .then(result => {
            if (result.code !== 200) {
                throw new Error(result.message || '请求失败');
            }
            return result.data;
        });
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function loadUsers() {
    apiRequest('/users')
        .then(users => {
            allUsers = users;
            const select = document.getElementById('userSelect');
            select.innerHTML = users.map(u => 
                `<option value="${u.id}">${u.name}</option>`
            ).join('');

            const bookerSelect = document.getElementById('bookerSelect');
            bookerSelect.innerHTML = users
                .filter(u => u.role === 'employee')
                .map(u => `<option value="${u.id}">${u.name}</option>`)
            .join('');

            const savedUserId = localStorage.getItem('currentUserId');
            if (savedUserId && users.find(u => u.id === savedUserId)) {
                switchUser(savedUserId);
            } else {
                switchUser(users[0].id);
            }
        })
        .catch(err => {
            showToast('加载用户列表失败: ' + err.message, 'error');
        });
}

function switchUser(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (user) {
        currentUser = user;
        localStorage.setItem('currentUserId', userId);
        document.getElementById('userSelect').value = userId;
        
        const roleBadge = document.getElementById('userRole');
        roleBadge.textContent = user.isAdmin ? '管理员' : '普通员工';
        roleBadge.className = 'role-badge ' + user.role;

        if (user.role === 'admin') {
            document.body.classList.add('is-admin');
        } else {
            document.body.classList.remove('is-admin');
        }

        loadMeetingRooms();
    }
}

function loadMeetingRooms() {
    const scope = currentUser.isAdmin ? currentScope : 'today';
    apiRequest('/meeting-rooms?scope=' + scope)
        .then(rooms => {
            meetingRooms = rooms;
            updateRoomFilterOptions(rooms);
            renderMeetingRooms();
        })
        .catch(err => {
            showToast('加载会议室列表失败: ' + err.message, 'error');
        });
}

function updateRoomFilterOptions(rooms) {
    const roomFilter = document.getElementById('roomFilter');
    const roomSelect = document.getElementById('roomSelect');
    
    const options = rooms.map(r => `<option value="${r.id}">${r.name}</option>`).join('');
    roomFilter.innerHTML = '<option value="">全部会议室</option>' + options;
    roomSelect.innerHTML = options;
}

function renderMeetingRooms() {
    const today = formatDate(new Date());
    document.getElementById('currentDate').textContent = 
        currentUser.isAdmin && currentScope === 'all' ? '全部预订记录' : `今日预订 - ${today}`;

    const container = document.getElementById('meetingRooms');
    let roomsToShow = meetingRooms;

    if (currentRoomFilter) {
        roomsToShow = meetingRooms.filter(r => r.id === currentRoomFilter);
    }

    container.innerHTML = roomsToShow.map(room => {
        const bookingsHtml = room.bookings && room.bookings.length > 0
            ? `<ul class="bookings-list">
                ${room.bookings.map(booking => renderBookingItem(booking, today)).join('')}
              </ul>`
            : '<div class="empty-bookings">暂无预订</div>';

        return `
            <div class="room-card">
                <div class="room-header">
                    <h3>${room.name}</h3>
                    <span class="room-capacity">容纳 ${room.capacity} 人</span>
                </div>
                <div class="room-body">
                    ${bookingsHtml}
                    <button class="btn btn-primary book-btn" onclick="openBookingModal('${room.id}')">
                        预订此会议室
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function renderBookingItem(booking, today) {
    const isToday = booking.date === today;
    const canCancel = currentUser && (
        currentUser.isAdmin || 
        currentUser.id === booking.bookerId
    );

    return `
        <li class="booking-item ${isToday ? 'today' : ''}">
            <div class="booking-time">${booking.startTime} - ${booking.endTime}</div>
            <div class="booking-info">
                <span>👤 ${booking.bookerName}</span>
            </div>
            <div class="booking-purpose">${escapeHtml(booking.purpose)}</div>
            <div class="booking-footer">
                ${currentUser.isAdmin ? `<span class="booking-date-tag">${booking.date}</span>` : ''}
                ${canCancel ? `<button class="btn btn-danger" onclick="cancelBooking('${booking.id}')">取消</button>` : ''}
            </div>
        </li>
    `;
}

function cancelBooking(bookingId) {
    if (!confirm('确定要取消这个预订吗？')) {
        return;
    }

    apiRequest('/bookings/' + bookingId, {
        method: 'DELETE'
    })
    .then(() => {
        showToast('取消预订成功', 'success');
        loadMeetingRooms();
    })
    .catch(err => {
        showToast('取消失败: ' + err.message, 'error');
    });
}

function openBookingModal(roomId) {
    document.getElementById('bookingModal').classList.add('show');
    document.getElementById('modalTitle').textContent = '预订会议室';
    
    if (roomId) {
        document.getElementById('roomSelect').value = roomId;
    }

    const todayInput = document.getElementById('bookingDate');
    const today = new Date();
    todayInput.value = formatDate(today);
    todayInput.min = formatDate(today);

    generateTimeOptions();
}

function closeBookingModal() {
    document.getElementById('bookingModal').classList.remove('show');
    document.getElementById('bookingForm').reset();
}

function generateTimeOptions() {
    const startSelect = document.getElementById('startTime');
    const endSelect = document.getElementById('endTime');
    const times = [];
    
    for (let hour = 8; hour <= 21; hour++) {
        times.push(`${padZero(hour)}:00`);
        if (hour < 21) {
            times.push(`${padZero(hour)}:30`);
        }
    }

    startSelect.innerHTML = times.map(t => `<option value="${t}">${t}</option>`).join('');
    updateEndTimeOptions();
}

function updateEndTimeOptions() {
    const startTime = document.getElementById('startTime').value;
    const endSelect = document.getElementById('endTime');
    const times = [];
    
    const [startHour, startMin] = startTime.split(':').map(Number);
    const startMinutes = startHour * 60 + startMin;

    for (let minutes = startMinutes + 30; minutes <= 21 * 60; minutes += 30) {
        const hour = Math.floor(minutes / 60);
        const min = minutes % 60;
        times.push(`${padZero(hour)}:${padZero(min)}`);
    }

    endSelect.innerHTML = times.map(t => `<option value="${t}">${t}</option>`).join('');
}

function submitBooking(event) {
    event.preventDefault();

    const roomId = document.getElementById('roomSelect').value;
    const date = document.getElementById('bookingDate').value;
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const purpose = document.getElementById('purpose').value;

    const body = {
        roomId,
        date,
        startTime,
        endTime,
        purpose
    };

    if (currentUser.isAdmin) {
        const bookerId = document.getElementById('bookerSelect').value;
        const originalUserId = currentUser.id;
        apiRequest('/bookings', {
            method: 'POST',
            headers: {
                'X-User-Id': bookerId
            },
            body: JSON.stringify(body)
        })
        .then(() => {
            currentUser = allUsers.find(u => u.id === originalUserId);
            showToast('预订成功', 'success');
            closeBookingModal();
            loadMeetingRooms();
        })
        .catch(err => {
            currentUser = allUsers.find(u => u.id === originalUserId);
            showToast('预订失败: ' + err.message, 'error');
        });
    } else {
        apiRequest('/bookings', {
            method: 'POST',
            body: JSON.stringify(body)
        })
        .then(() => {
            showToast('预订成功', 'success');
            closeBookingModal();
            loadMeetingRooms();
        })
        .catch(err => {
            showToast('预订失败: ' + err.message, 'error');
        });
    }
}

function changeScope(scope) {
    currentScope = scope;
    loadMeetingRooms();
}

function filterByRoom(roomId) {
    currentRoomFilter = roomId;
    renderMeetingRooms();
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = padZero(date.getMonth() + 1);
    const day = padZero(date.getDate());
    return `${year}-${month}-${day}`;
}

function padZero(num) {
    return num.toString().padStart(2, '0');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', function() {
    loadUsers();
});

document.getElementById('bookingModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeBookingModal();
    }
});
