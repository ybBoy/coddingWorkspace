(function () {
    const API_BASE = '';

    const elements = {
        recordsList: document.getElementById('recordsList'),
        emptyState: document.getElementById('emptyState'),
        addForm: document.getElementById('addForm'),
        typeFilter: document.getElementById('typeFilter'),
        locationSearch: document.getElementById('locationSearch'),
        resetFilterBtn: document.getElementById('resetFilterBtn'),
        totalCount: document.getElementById('totalCount'),
        avgRating: document.getElementById('avgRating'),
        filteredCount: document.getElementById('filteredCount'),
        visitDate: document.getElementById('visitDate'),
    };

    function formatDate(dateStr) {
        const date = new Date(dateStr);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;
    }

    function renderStars(rating) {
        const full = '★';
        const empty = '☆';
        return full.repeat(rating) + empty.repeat(5 - rating);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function createRecordCard(record) {
        const card = document.createElement('div');
        card.className = 'record-card';
        card.dataset.id = record.id;

        card.innerHTML = `
            <div class="ticket-top">
                <div class="record-name">${escapeHtml(record.name)}</div>
                <div class="record-meta">
                    <span class="record-location">📍 ${escapeHtml(record.location)}</span>
                    <span class="record-type">${escapeHtml(record.exhibit_type)}</span>
                </div>
                <div class="record-date">${formatDate(record.visit_date)}</div>
                <div class="record-rating">${renderStars(record.rating)}</div>
                <div class="record-comment">"${escapeHtml(record.comment)}"</div>
            </div>
            <div class="ticket-bottom">
                <div class="card-actions">
                    <div class="rating-edit">
                        <select class="rating-select" title="修改评分">
                            <option value="1" ${record.rating === 1 ? 'selected' : ''}>1星</option>
                            <option value="2" ${record.rating === 2 ? 'selected' : ''}>2星</option>
                            <option value="3" ${record.rating === 3 ? 'selected' : ''}>3星</option>
                            <option value="4" ${record.rating === 4 ? 'selected' : ''}>4星</option>
                            <option value="5" ${record.rating === 5 ? 'selected' : ''}>5星</option>
                        </select>
                        <button class="btn btn-small btn-primary update-rating-btn">更新</button>
                    </div>
                    <button class="btn btn-small btn-danger delete-btn">删除</button>
                </div>
            </div>
        `;

        const deleteBtn = card.querySelector('.delete-btn');
        deleteBtn.addEventListener('click', () => handleDelete(record.id));

        const updateBtn = card.querySelector('.update-rating-btn');
        const ratingSelect = card.querySelector('.rating-select');
        updateBtn.addEventListener('click', () => {
            const newRating = parseInt(ratingSelect.value, 10);
            handleUpdateRating(record.id, newRating);
        });

        return card;
    }

    function renderRecords(records) {
        elements.recordsList.innerHTML = '';

        if (records.length === 0) {
            elements.emptyState.style.display = 'block';
            return;
        }

        elements.emptyState.style.display = 'none';

        records.forEach(record => {
            const card = createRecordCard(record);
            elements.recordsList.appendChild(card);
        });
    }

    function renderStatistics(stats) {
        elements.totalCount.textContent = stats.total_count;
        elements.avgRating.textContent = stats.average_rating.toFixed(1);
        elements.filteredCount.textContent = stats.filtered_count;
    }

    function renderTypeOptions(types) {
        const currentValue = elements.typeFilter.value;
        elements.typeFilter.innerHTML = '<option value="">全部类型</option>';
        types.forEach(type => {
            const option = document.createElement('option');
            option.value = type;
            option.textContent = type;
            if (type === currentValue) option.selected = true;
            elements.typeFilter.appendChild(option);
        });
    }

    function getFilterParams() {
        const params = new URLSearchParams();
        const typeValue = elements.typeFilter.value;
        const locationValue = elements.locationSearch.value.trim();
        if (typeValue) params.set('type', typeValue);
        if (locationValue) params.set('location', locationValue);
        return params;
    }

    async function fetchRecords() {
        try {
            const params = getFilterParams();
            const url = `${API_BASE}/api/exhibits${params.toString() ? '?' + params.toString() : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('获取记录失败');
            const data = await res.json();
            renderRecords(data);
        } catch (err) {
            console.error(err);
            alert('获取记录失败，请刷新页面重试');
        }
    }

    async function fetchStatistics() {
        try {
            const params = getFilterParams();
            const url = `${API_BASE}/api/statistics${params.toString() ? '?' + params.toString() : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('获取统计失败');
            const data = await res.json();
            renderStatistics(data);
        } catch (err) {
            console.error(err);
        }
    }

    async function fetchTypes() {
        try {
            const res = await fetch(`${API_BASE}/api/exhibit-types`);
            if (!res.ok) throw new Error('获取类型失败');
            const data = await res.json();
            renderTypeOptions(data);
        } catch (err) {
            console.error(err);
        }
    }

    async function handleAddRecord(e) {
        e.preventDefault();

        const name = document.getElementById('name').value.trim();
        const location = document.getElementById('location').value.trim();
        const visitDate = document.getElementById('visitDate').value;
        const exhibitType = document.getElementById('exhibitType').value;
        const rating = parseInt(document.getElementById('rating').value, 10);
        const comment = document.getElementById('comment').value.trim();

        if (!name || !location || !visitDate || !exhibitType || !rating || !comment) {
            alert('请填写所有必填项');
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/api/exhibits`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name,
                    location,
                    visit_date: visitDate,
                    exhibit_type: exhibitType,
                    rating,
                    comment,
                }),
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.error || '添加失败');
            }

            elements.addForm.reset();
            setDefaultDate();
            await refreshAll();
        } catch (err) {
            console.error(err);
            alert(err.message || '添加失败，请重试');
        }
    }

    async function handleUpdateRating(recordId, rating) {
        try {
            const res = await fetch(`${API_BASE}/api/exhibits/${recordId}/rating`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rating }),
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.error || '更新失败');
            }

            await refreshAll();
        } catch (err) {
            console.error(err);
            alert(err.message || '更新失败，请重试');
        }
    }

    async function handleDelete(recordId) {
        if (!confirm('确定要删除这条打卡记录吗？')) return;

        try {
            const res = await fetch(`${API_BASE}/api/exhibits/${recordId}`, {
                method: 'DELETE',
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.error || '删除失败');
            }

            await refreshAll();
        } catch (err) {
            console.error(err);
            alert(err.message || '删除失败，请重试');
        }
    }

    function handleFilterChange() {
        refreshAll();
    }

    function handleResetFilter() {
        elements.typeFilter.value = '';
        elements.locationSearch.value = '';
        refreshAll();
    }

    function setDefaultDate() {
        const today = new Date().toISOString().split('T')[0];
        elements.visitDate.value = today;
    }

    async function refreshAll() {
        await Promise.all([fetchRecords(), fetchStatistics(), fetchTypes()]);
    }

    function init() {
        setDefaultDate();

        elements.addForm.addEventListener('submit', handleAddRecord);
        elements.typeFilter.addEventListener('change', handleFilterChange);
        elements.locationSearch.addEventListener('input', debounce(handleFilterChange, 300));
        elements.resetFilterBtn.addEventListener('click', handleResetFilter);

        refreshAll();
    }

    function debounce(func, wait) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }

    document.addEventListener('DOMContentLoaded', init);
})();
