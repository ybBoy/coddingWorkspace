(function () {
    'use strict';

    const API = {
        list: (category, disposePlan) => {
            let url = '/api/items';
            const params = [];
            if (category) params.push('category=' + encodeURIComponent(category));
            if (disposePlan) params.push('disposePlan=' + encodeURIComponent(disposePlan));
            if (params.length > 0) url += '?' + params.join('&');
            return fetch(url, { method: 'GET' });
        },
        add: (data) => fetch('/api/items', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }),
        updateDisposePlan: (id, plan) => fetch('/api/items/' + id + '/dispose-plan', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ disposePlan: plan })
        }),
        delete: (id) => fetch('/api/items/' + id, { method: 'DELETE' }),
        stats: () => fetch('/api/stats', { method: 'GET' })
    };

    const PLAN_DISPLAY = {
        KEEP: '留下',
        GIVE_AWAY: '送人',
        SELL: '出售',
        DISCARD: '丢弃'
    };

    const PLAN_CLASS = {
        KEEP: 'tag-plan-keep',
        GIVE_AWAY: 'tag-plan-giveaway',
        SELL: 'tag-plan-sell',
        DISCARD: 'tag-plan-discard'
    };

    let editingItem = null;

    function $(id) { return document.getElementById(id); }

    function showToast(message, type) {
        const toast = $('toast');
        toast.textContent = message;
        toast.className = 'toast ' + (type || 'info');
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 2500);
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatPrice(price) {
        if (price == null || price === '') return '0.00';
        const num = Number(price);
        if (isNaN(num)) return '0.00';
        return num.toFixed(2);
    }

    async function loadStats() {
        try {
            const resp = await API.stats();
            if (!resp.ok) throw new Error('加载统计失败');
            const data = await resp.json();
            $('expectedRevenue').textContent = formatPrice(data.expectedRevenue);
            $('totalItems').textContent = data.totalItems;
            $('sellCount').textContent = data.sellCount;
            $('discardCount').textContent = data.discardCount;

            const categoryList = $('categoryList');
            const categoryFilter = $('categoryFilter');
            const currentFilter = categoryFilter.value;

            categoryList.innerHTML = '';
            const allCategories = data.categories || [];
            allCategories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat;
                categoryList.appendChild(opt);
            });

            const currentOpts = categoryFilter.querySelectorAll('option:not([value="ALL"])');
            currentOpts.forEach(o => o.remove());
            allCategories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat;
                opt.textContent = cat;
                categoryFilter.appendChild(opt);
            });
            categoryFilter.value = currentFilter || 'ALL';
        } catch (e) {
            console.error('加载统计失败:', e);
        }
    }

    async function loadItems() {
        const category = $('categoryFilter').value;
        const disposePlan = $('disposePlanFilter').value;
        const catParam = category === 'ALL' ? '' : category;
        const planParam = disposePlan === 'ALL' ? '' : disposePlan;

        try {
            const resp = await API.list(catParam, planParam);
            if (!resp.ok) throw new Error('加载物品失败');
            const items = await resp.json();
            renderItems(items);
        } catch (e) {
            console.error('加载物品失败:', e);
            showToast('加载物品列表失败', 'error');
        }
    }

    function renderItems(items) {
        const grid = $('itemsGrid');
        grid.innerHTML = '';

        if (!items || items.length === 0) {
            grid.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📦</div>
                    <p>还没有整理任何物品</p>
                    <p class="empty-hint">从左侧表单开始添加你的第一件闲置物品吧~</p>
                </div>
            `;
            return;
        }

        const frag = document.createDocumentFragment();
        items.forEach(item => frag.appendChild(createItemCard(item)));
        grid.appendChild(frag);
    }

    function createItemCard(item) {
        const card = document.createElement('div');
        card.className = 'item-card';
        card.dataset.id = item.id;

        const planClass = PLAN_CLASS[item.disposePlan] || 'tag-plan-keep';
        const planDisplay = item.disposePlanDisplay || PLAN_DISPLAY[item.disposePlan] || item.disposePlan;

        const tagsHtml = [];
        tagsHtml.push(`<span class="tag ${planClass}">${escapeHtml(planDisplay)}</span>`);
        if (item.category) {
            tagsHtml.push(`<span class="tag tag-category">${escapeHtml(item.category)}</span>`);
        }

        const priceHtml = (item.disposePlan === 'SELL')
            ? `<span class="price-value">¥ ${formatPrice(item.estimatedPrice)}</span>`
            : `<span>${formatPrice(item.estimatedPrice)}</span>`;

        card.innerHTML = `
            <div class="item-header">
                <div class="item-name">${escapeHtml(item.name)}</div>
            </div>
            <div class="item-tags">${tagsHtml.join('')}</div>
            <div class="item-info">
                <div class="info-row">
                    <span class="info-label">处理方式</span>
                    <span class="info-value">${escapeHtml(planDisplay)}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">预估价格</span>
                    <span class="info-value">${priceHtml}</span>
                </div>
                ${item.location ? `
                <div class="info-row">
                    <span class="info-label">存放位置</span>
                    <span class="info-value">${escapeHtml(item.location)}</span>
                </div>` : ''}
                ${item.remark ? `
                <div class="info-row">
                    <span class="info-label">备注</span>
                    <span class="info-value">${escapeHtml(item.remark)}</span>
                </div>` : ''}
            </div>
            <div class="item-actions">
                <button class="btn btn-outline btn-edit">更改处理方式</button>
                <button class="btn btn-danger btn-delete">删除</button>
            </div>
        `;

        card.querySelector('.btn-edit').addEventListener('click', () => openEditModal(item));
        card.querySelector('.btn-delete').addEventListener('click', () => handleDelete(item));
        return card;
    }

    function openEditModal(item) {
        editingItem = item;
        $('editItemName').textContent = '物品：' + item.name;
        $('editDisposePlan').value = item.disposePlan;
        $('editModal').style.display = 'flex';
    }

    function closeEditModal() {
        editingItem = null;
        $('editModal').style.display = 'none';
    }

    async function handleConfirmEdit() {
        if (!editingItem) return;
        const newPlan = $('editDisposePlan').value;
        if (newPlan === editingItem.disposePlan) {
            closeEditModal();
            return;
        }
        try {
            const resp = await API.updateDisposePlan(editingItem.id, newPlan);
            if (!resp.ok) throw new Error('修改失败');
            showToast('处理方式已更新', 'success');
            closeEditModal();
            await Promise.all([loadItems(), loadStats()]);
        } catch (e) {
            console.error('修改失败:', e);
            showToast('修改失败：' + e.message, 'error');
        }
    }

    async function handleDelete(item) {
        if (!confirm(`确定要删除"${item.name}"吗？此操作不可撤销。`)) return;
        try {
            const resp = await API.delete(item.id);
            if (!resp.ok) throw new Error('删除失败');
            showToast('物品已删除', 'success');
            await Promise.all([loadItems(), loadStats()]);
        } catch (e) {
            console.error('删除失败:', e);
            showToast('删除失败：' + e.message, 'error');
        }
    }

    async function handleAddItem(e) {
        e.preventDefault();
        const form = e.target;
        const name = $('itemName').value.trim();
        if (!name) {
            showToast('请填写物品名称', 'error');
            return;
        }

        const data = {
            name: name,
            category: $('itemCategory').value.trim(),
            disposePlan: $('itemDisposePlan').value,
            estimatedPrice: parseFloat($('itemPrice').value) || 0,
            location: $('itemLocation').value.trim(),
            remark: $('itemRemark').value.trim()
        };

        try {
            const resp = await API.add(data);
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({ error: '添加失败' }));
                throw new Error(err.error || '添加失败');
            }
            showToast('物品添加成功！', 'success');
            form.reset();
            $('itemDisposePlan').value = 'KEEP';
            await Promise.all([loadItems(), loadStats()]);
        } catch (e) {
            console.error('添加失败:', e);
            showToast('添加失败：' + e.message, 'error');
        }
    }

    function init() {
        $('addItemForm').addEventListener('submit', handleAddItem);
        $('categoryFilter').addEventListener('change', loadItems);
        $('disposePlanFilter').addEventListener('change', loadItems);
        $('refreshBtn').addEventListener('click', () => {
            Promise.all([loadItems(), loadStats()]).then(() => showToast('已刷新', 'info'));
        });
        $('cancelEditBtn').addEventListener('click', closeEditModal);
        $('confirmEditBtn').addEventListener('click', handleConfirmEdit);
        document.querySelector('.modal-overlay').addEventListener('click', closeEditModal);
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && $('editModal').style.display === 'flex') {
                closeEditModal();
            }
        });

        Promise.all([loadItems(), loadStats()]);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
