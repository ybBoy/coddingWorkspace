const { createApp, ref, computed, watch, onMounted } = Vue;

const API = '/api';

createApp({
    setup() {
        const users = ref([]);
        const currentUserId = ref('');
        const currentView = ref('dashboard');
        const dashboard = ref(null);
        const myOvertime = ref([]);
        const myTimeoff = ref([]);
        const pendingOvertime = ref([]);
        const pendingTimeoff = ref([]);
        const deptReport = ref([]);

        const otForm = ref({ date: '', startTime: '', endTime: '', reason: '' });
        const toForm = ref({ date: '', type: 'FULL_DAY', reason: '' });

        const rejectOvertimeId = ref(null);
        const rejectOvertimeReason = ref('');
        const rejectTimeoffId = ref(null);
        const rejectTimeoffReason = ref('');

        const toast = ref({ show: false, message: '', type: 'success' });

        let toastTimer = null;

        function showToast(message, type) {
            if (toastTimer) clearTimeout(toastTimer);
            toast.value = { show: true, message, type };
            toastTimer = setTimeout(() => { toast.value.show = false; }, 2500);
        }

        const currentUser = computed(() => {
            return users.value.find(u => u.id === currentUserId.value) || null;
        });

        const isManager = computed(() => {
            return currentUser.value && currentUser.value.role === 'MANAGER';
        });

        const today = new Date().toISOString().split('T')[0];

        function statusText(s) {
            const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝' };
            return map[s] || s;
        }

        async function loadUsers() {
            const res = await fetch(API + '/users');
            users.value = await res.json();
        }

        async function loadDashboard() {
            if (!currentUserId.value) return;
            const res = await fetch(API + '/users/' + currentUserId.value + '/dashboard');
            dashboard.value = await res.json();
        }

        async function loadMyOvertime() {
            if (!currentUserId.value) return;
            const res = await fetch(API + '/overtime/my/' + currentUserId.value);
            myOvertime.value = await res.json();
        }

        async function loadMyTimeoff() {
            if (!currentUserId.value) return;
            const res = await fetch(API + '/timeoff/my/' + currentUserId.value);
            myTimeoff.value = await res.json();
        }

        async function loadPendingOvertime() {
            if (!currentUser.value) return;
            const dept = encodeURIComponent(currentUser.value.department);
            const res = await fetch(API + '/overtime/pending/' + dept);
            pendingOvertime.value = await res.json();
        }

        async function loadPendingTimeoff() {
            if (!currentUser.value) return;
            const dept = encodeURIComponent(currentUser.value.department);
            const res = await fetch(API + '/timeoff/pending/' + dept);
            pendingTimeoff.value = await res.json();
        }

        async function loadDeptReport() {
            if (!currentUser.value) return;
            const dept = encodeURIComponent(currentUser.value.department);
            const res = await fetch(API + '/users/department/' + dept + '/report');
            deptReport.value = await res.json();
        }

        async function onUserChange() {
            await refreshAll();
        }

        async function refreshAll() {
            await Promise.all([
                loadDashboard(),
                loadMyOvertime(),
                loadMyTimeoff()
            ]);
            if (isManager.value) {
                await Promise.all([
                    loadPendingOvertime(),
                    loadPendingTimeoff(),
                    loadDeptReport()
                ]);
            }
        }

        async function submitOvertime() {
            const form = otForm.value;
            if (!form.date || !form.startTime || !form.endTime || !form.reason.trim()) {
                showToast('请填写完整信息', 'error');
                return;
            }
            const res = await fetch(API + '/overtime/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: currentUserId.value,
                    date: form.date,
                    startTime: form.startTime,
                    endTime: form.endTime,
                    reason: form.reason
                })
            });
            const data = await res.json();
            if (data.success) {
                showToast('加班申请提交成功', 'success');
                otForm.value = { date: '', startTime: '', endTime: '', reason: '' };
                await Promise.all([loadMyOvertime(), loadDashboard()]);
            } else {
                showToast(data.message, 'error');
            }
        }

        async function submitTimeoff() {
            const form = toForm.value;
            if (!form.date || !form.reason.trim()) {
                showToast('请填写完整信息', 'error');
                return;
            }
            const res = await fetch(API + '/timeoff/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: currentUserId.value,
                    date: form.date,
                    type: form.type,
                    reason: form.reason
                })
            });
            const data = await res.json();
            if (data.success) {
                showToast('调休申请提交成功', 'success');
                toForm.value = { date: '', type: 'FULL_DAY', reason: '' };
                await Promise.all([loadMyTimeoff(), loadDashboard()]);
            } else {
                showToast(data.message, 'error');
            }
        }

        async function approveOvertime(id) {
            const res = await fetch(API + '/overtime/approve/' + id, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approverId: currentUserId.value })
            });
            const data = await res.json();
            if (data.success) {
                showToast('审批通过', 'success');
                await loadPendingOvertime();
            } else {
                showToast(data.message, 'error');
            }
        }

        function startRejectOvertime(id) {
            rejectOvertimeId.value = id;
            rejectOvertimeReason.value = '';
        }

        async function confirmRejectOvertime() {
            if (!rejectOvertimeReason.value.trim()) {
                showToast('请填写拒绝意见', 'error');
                return;
            }
            const res = await fetch(API + '/overtime/reject/' + rejectOvertimeId.value, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    approverId: currentUserId.value,
                    rejectReason: rejectOvertimeReason.value
                })
            });
            const data = await res.json();
            if (data.success) {
                showToast('已拒绝', 'success');
                rejectOvertimeId.value = null;
                await loadPendingOvertime();
            } else {
                showToast(data.message, 'error');
            }
        }

        async function approveTimeoff(id) {
            const res = await fetch(API + '/timeoff/approve/' + id, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approverId: currentUserId.value })
            });
            const data = await res.json();
            if (data.success) {
                showToast('审批通过', 'success');
                await loadPendingTimeoff();
            } else {
                showToast(data.message, 'error');
            }
        }

        function startRejectTimeoff(id) {
            rejectTimeoffId.value = id;
            rejectTimeoffReason.value = '';
        }

        async function confirmRejectTimeoff() {
            if (!rejectTimeoffReason.value.trim()) {
                showToast('请填写拒绝意见', 'error');
                return;
            }
            const res = await fetch(API + '/timeoff/reject/' + rejectTimeoffId.value, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    approverId: currentUserId.value,
                    rejectReason: rejectTimeoffReason.value
                })
            });
            const data = await res.json();
            if (data.success) {
                showToast('已拒绝', 'success');
                rejectTimeoffId.value = null;
                await loadPendingTimeoff();
            } else {
                showToast(data.message, 'error');
            }
        }

        watch(currentView, async (view) => {
            if (view === 'dashboard') await loadDashboard();
            if (view === 'overtime') await loadMyOvertime();
            if (view === 'timeoff') await loadMyTimeoff();
            if (view === 'approve-overtime') await loadPendingOvertime();
            if (view === 'approve-timeoff') await loadPendingTimeoff();
            if (view === 'report') await loadDeptReport();
        });

        onMounted(async () => {
            await loadUsers();
            if (users.value.length > 0) {
                currentUserId.value = users.value[0].id;
                await refreshAll();
            }
        });

        return {
            users, currentUserId, currentView, dashboard, currentUser, isManager, today,
            myOvertime, myTimeoff, pendingOvertime, pendingTimeoff, deptReport,
            otForm, toForm,
            rejectOvertimeId, rejectOvertimeReason, rejectTimeoffId, rejectTimeoffReason,
            toast, statusText,
            onUserChange, submitOvertime, submitTimeoff,
            approveOvertime, startRejectOvertime, confirmRejectOvertime,
            approveTimeoff, startRejectTimeoff, confirmRejectTimeoff
        };
    }
}).mount('#app');
