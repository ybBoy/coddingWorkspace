(function() {
    var API_BASE = '/api';
    var UPLOAD_BASE = '/uploads';

    var state = {
        items: [],
        selectedIds: {},
        editItemId: null,
        tempImageBase64: null,
        filters: {
            keyword: '',
            category: 'ALL',
            disposePlan: 'ALL',
            status: 'ALL',
            minPrice: '',
            maxPrice: '',
            sortBy: 'createdAt',
            sortOrder: 'desc'
        }
    };

    var STATUS_OPTIONS = [
        { value: 'PENDING', label: '待处理', plan: null },
        { value: 'SELLING', label: '出售中', plan: 'SELL' },
        { value: 'SOLD', label: '已出售', plan: 'SELL' },
        { value: 'GIVEN_AWAY', label: '已送出', plan: 'GIVE_AWAY' },
        { value: 'DISCARDED', label: '已丢弃', plan: 'DISCARD' },
        { value: 'KEPT', label: '已保留', plan: 'KEEP' }
    ];

    var PLAN_COLORS = {
        'KEEP': '#1976d2',
        'GIVE_AWAY': '#f57c00',
        'SELL': '#388e3c',
        'DISCARD': '#757575'
    };

    var STATUS_COLORS = {
        'PENDING': '#ad1457',
        'SELLING': '#2e7d32',
        'SOLD': '#1b5e20',
        'GIVEN_AWAY': '#bf360c',
        'DISCARDED': '#424242',
        'KEPT': '#0d47a1'
    };

    var PLAN_LABELS = {
        'KEEP': '留下',
        'GIVE_AWAY': '送人',
        'SELL': '出售',
        'DISCARD': '丢弃'
    };

    var STATUS_LABELS = {};
    STATUS_OPTIONS.forEach(function(s) {
        STATUS_LABELS[s.value] = s.label;
    });

    function init() {
        bindEvents();
        populateStatusSelects();
        loadItems();
        loadStats();
        loadDetailedStats();
    }

    function bindEvents() {
        var form = document.getElementById('addItemForm');
        form.addEventListener('submit', handleAddItem);

        document.getElementById('itemDisposePlan').addEventListener('change', function(e) {
            var plan = e.target.value;
            var statusSelect = document.getElementById('itemStatus');
            statusSelect.innerHTML = '<option value="">（自动）</option>';
            STATUS_OPTIONS.filter(function(s) {
                return !s.plan || s.plan === plan;
            }).forEach(function(s) {
                var opt = document.createElement('option');
                opt.value = s.value;
                opt.textContent = s.label;
                statusSelect.appendChild(opt);
            });
        });

        document.getElementById('uploadImageBtn').addEventListener('click', function() {
            document.getElementById('imageFile').click();
        });
        document.getElementById('imageFile').addEventListener('change', handleImageFile);
        document.getElementById('clearImageBtn').addEventListener('click', clearImagePreview);

        document.getElementById('searchInput').addEventListener('input', debounce(handleSearch, 300));

        document.getElementById('categoryFilter').addEventListener('change', function(e) {
            state.filters.category = e.target.value;
            loadItems();
        });
        document.getElementById('disposePlanFilter').addEventListener('change', function(e) {
            state.filters.disposePlan = e.target.value;
            loadItems();
        });
        document.getElementById('statusFilter').addEventListener('change', function(e) {
            state.filters.status = e.target.value;
            loadItems();
        });
        document.getElementById('minPriceFilter').addEventListener('input', debounce(function(e) {
            state.filters.minPrice = e.target.value;
            loadItems();
        }, 400));
        document.getElementById('maxPriceFilter').addEventListener('input', debounce(function(e) {
            state.filters.maxPrice = e.target.value;
            loadItems();
        }, 400));
        document.getElementById('sortBySelect').addEventListener('change', function(e) {
            state.filters.sortBy = e.target.value;
            loadItems();
        });
        document.getElementById('sortOrderSelect').addEventListener('change', function(e) {
            state.filters.sortOrder = e.target.value;
            loadItems();
        });

        document.getElementById('selectAllCheckbox').addEventListener('change', handleSelectAll);

        document.getElementById('batchDisposePlan').addEventListener('change', function(e) {
            var plan = e.target.value;
            if (!plan) return;
            handleBatchUpdateDisposePlan(plan);
            e.target.value = '';
        });
        document.getElementById('batchStatus').addEventListener('change', function(e) {
            var status = e.target.value;
            if (!status) return;
            handleBatchUpdateStatus(status);
            e.target.value = '';
        });
        document.getElementById('batchDeleteBtn').addEventListener('click', handleBatchDelete);

        document.getElementById('exportCsvBtn').addEventListener('click', function() {
            window.location.href = API_BASE + '/export/csv';
        });
        document.getElementById('exportJsonBtn').addEventListener('click', function() {
            window.location.href = API_BASE + '/export/json';
        });

        document.getElementById('cancelEditBtn').addEventListener('click', closeEditModal);
        document.getElementById('confirmEditBtn').addEventListener('click', handleConfirmEdit);

        document.getElementById('editDisposePlan').addEventListener('change', function(e) {
            var plan = e.target.value;
            var statusSelect = document.getElementById('editStatus');
            statusSelect.innerHTML = '';
            STATUS_OPTIONS.filter(function(s) {
                return !s.plan || s.plan === plan;
            }).forEach(function(s) {
                var opt = document.createElement('option');
                opt.value = s.value;
                opt.textContent = s.label;
                statusSelect.appendChild(opt);
            });
        });
    }

    function populateStatusSelects() {
        var statusFilter = document.getElementById('statusFilter');
        STATUS_OPTIONS.forEach(function(s) {
            var opt = document.createElement('option');
            opt.value = s.value;
            opt.textContent = s.label;
            statusFilter.appendChild(opt);
        });
    }

    function handleSearch(e) {
        state.filters.keyword = e.target.value;
        loadItems();
    }

    function handleImageFile(e) {
        var file = e.target.files[0];
        if (!file) return;
        var reader = new FileReader();
        reader.onload = function(event) {
            state.tempImageBase64 = event.target.result;
            var preview = document.getElementById('imagePreview');
            preview.innerHTML = '<img src="' + event.target.result + '" alt="预览">';
            document.getElementById('itemImageUrl').value = '';
        };
        reader.readAsDataURL(file);
    }

    function clearImagePreview() {
        state.tempImageBase64 = null;
        document.getElementById('imagePreview').innerHTML = '<span class="image-placeholder">📷</span>';
        document.getElementById('imageFile').value = '';
        document.getElementById('itemImageUrl').value = '';
    }

    function handleAddItem(e) {
        e.preventDefault();
        var form = e.target;

        var data = {
            name: form.name.value.trim(),
            category: form.category.value.trim(),
            disposePlan: form.disposePlan.value,
            estimatedPrice: parseFloat(form.estimatedPrice.value) || 0,
            location: form.location.value.trim(),
            remark: form.remark.value.trim()
        };

        if (form.status.value) {
            data.status = form.status.value;
        }

        var imageUrl = form.imageUrl.value.trim();
        if (imageUrl) {
            data.imageUrl = imageUrl;
        }

        if (state.tempImageBase64) {
            uploadImageAndAddItem(state.tempImageBase64, data);
        } else {
            addItem(data);
        }
    }

    function uploadImageAndAddItem(base64, itemData) {
        fetch(API_BASE + '/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ imageBase64: base64 })
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success && data.url) {
                itemData.imageUrl = data.url;
                addItem(itemData);
            } else {
                addItem(itemData);
            }
        })
        .catch(function() {
            addItem(itemData);
        });
    }

    function addItem(data) {
        fetch(API_BASE + '/items', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(function(res) { return res.json(); })
        .then(function(item) {
            showToast('添加成功！', 'success');
            document.getElementById('addItemForm').reset();
            clearImagePreview();
            loadItems();
            loadStats();
            loadDetailedStats();
        })
        .catch(function(err) {
            showToast('添加失败：' + err.message, 'error');
        });
    }

    function loadItems() {
        var params = [];
        if (state.filters.keyword) params.push('keyword=' + encodeURIComponent(state.filters.keyword));
        if (state.filters.category !== 'ALL') params.push('category=' + encodeURIComponent(state.filters.category));
        if (state.filters.disposePlan !== 'ALL') params.push('disposePlan=' + state.filters.disposePlan);
        if (state.filters.status !== 'ALL') params.push('status=' + state.filters.status);
        if (state.filters.minPrice) params.push('minPrice=' + state.filters.minPrice);
        if (state.filters.maxPrice) params.push('maxPrice=' + state.filters.maxPrice);
        params.push('sortBy=' + state.filters.sortBy);
        params.push('sortOrder=' + state.filters.sortOrder);

        var url = API_BASE + '/items' + (params.length ? '?' + params.join('&') : '');

        fetch(url)
        .then(function(res) { return res.json(); })
        .then(function(data) {
            state.items = data.items || data || [];
            renderItems();
            updateCategoryFilter();
            updateBatchBar();
        })
        .catch(function(err) {
            console.error('加载失败', err);
        });
    }

    function renderItems() {
        var grid = document.getElementById('itemsGrid');

        if (!state.items.length) {
            grid.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><p>暂无匹配的物品</p><p class="empty-hint">试试调整筛选条件吧~</p></div>';
            return;
        }

        var html = '';
        state.items.forEach(function(item) {
            var imageHtml = '';
            if (item.imageUrl) {
                var imgSrc = item.imageUrl;
                if (imgSrc.indexOf('/uploads/') === 0) {
                    imgSrc = imgSrc;
                }
                imageHtml = '<img src="' + imgSrc + '" alt="' + escapeHtml(item.name) + '">';
            } else {
                imageHtml = '<span class="item-image-placeholder">📦</span>';
            }

            var planClass = 'tag-plan-' + item.disposePlan.toLowerCase().replace('_', '');
            var statusLower = item.status ? item.status.toLowerCase().replace(/_/g, '-') : '';
            var statusClass = 'tag-status-' + statusLower;

            var checked = state.selectedIds[item.id] ? 'checked' : '';

            html += '<div class="item-card" data-id="' + item.id + '">'
                + '<div class="item-checkbox-bar"><label class="checkbox-label">'
                + '<input type="checkbox" class="item-checkbox" value="' + item.id + '" ' + checked + '>'
                + '</label></div>'
                + '<div class="item-image-area">' + imageHtml + '</div>'
                + '<div class="item-body">'
                + '<h3 class="item-name">' + escapeHtml(item.name) + '</h3>'
                + '<div class="item-tags">';

            if (item.category) {
                html += '<span class="tag tag-category">' + escapeHtml(item.category) + '</span>';
            }
            html += '<span class="tag ' + planClass + '">' + (item.disposePlanDisplayName || PLAN_LABELS[item.disposePlan] || item.disposePlan) + '</span>';
            if (item.status) {
                html += '<span class="tag ' + statusClass + '">' + (item.statusDisplayName || STATUS_LABELS[item.status] || item.status) + '</span>';
            }
            html += '</div><div class="item-info">';

            if (item.location) {
                html += '<div class="info-line"><span class="info-label">位置</span><span>' + escapeHtml(item.location) + '</span></div>';
            }
            html += '<div class="info-line"><span class="info-label">价格</span><span class="item-price">¥ ' + formatPrice(item.estimatedPrice) + '</span></div>';
            if (item.remark) {
                html += '<div class="item-remark">' + escapeHtml(item.remark) + '</div>';
            }
            html += '</div></div><div class="item-actions">'
                + '<button class="btn btn-secondary btn-sm btn-edit">编辑</button>'
                + '<button class="btn btn-danger btn-sm btn-delete">删除</button>'
                + '</div></div>';
        });

        grid.innerHTML = html;

        var checkboxes = grid.querySelectorAll('.item-checkbox');
        checkboxes.forEach(function(cb) {
            cb.addEventListener('change', function(e) {
                var id = e.target.value;
                if (e.target.checked) {
                    state.selectedIds[id] = true;
                } else {
                    delete state.selectedIds[id];
                }
                updateBatchBar();
            });
        });

        var editBtns = grid.querySelectorAll('.btn-edit');
        editBtns.forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                var card = e.target.closest('.item-card');
                var id = card.getAttribute('data-id');
                openEditModal(id);
            });
        });

        var deleteBtns = grid.querySelectorAll('.btn-delete');
        deleteBtns.forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                var card = e.target.closest('.item-card');
                var id = card.getAttribute('data-id');
                handleDeleteItem(id);
            });
        });
    }

    function updateCategoryFilter() {
        var filter = document.getElementById('categoryFilter');
        var datalist = document.getElementById('categoryList');
        var currentFilter = state.filters.category;

        var categories = {};
        state.items.forEach(function(item) {
            if (item.category) {
                categories[item.category] = true;
            }
        });

        var allCategories = Object.keys(categories).sort();

        filter.innerHTML = '<option value="ALL">全部分类</option>';
        allCategories.forEach(function(c) {
            var opt = document.createElement('option');
            opt.value = c;
            opt.textContent = c;
            filter.appendChild(opt);
        });
        filter.value = currentFilter;

        datalist.innerHTML = '';
        allCategories.forEach(function(c) {
            var opt = document.createElement('option');
            opt.value = c;
            datalist.appendChild(opt);
        });
    }

    function updateBatchBar() {
        var bar = document.getElementById('batchBar');
        var count = Object.keys(state.selectedIds).length;
        var selectAll = document.getElementById('selectAllCheckbox');

        if (count > 0) {
            bar.style.display = 'flex';
        } else {
            bar.style.display = 'none';
        }

        document.getElementById('selectedCount').textContent = count;

        selectAll.checked = count > 0 && count === state.items.length;
        selectAll.indeterminate = count > 0 && count < state.items.length;
    }

    function handleSelectAll(e) {
        var checked = e.target.checked;
        state.selectedIds = {};
        if (checked) {
            state.items.forEach(function(item) {
                state.selectedIds[item.id] = true;
            });
        }
        renderItems();
        updateBatchBar();
    }

    function openEditModal(id) {
        var item = state.items.find(function(i) { return i.id === id; });
        if (!item) return;

        state.editItemId = id;
        document.getElementById('editItemName').textContent = item.name;
        document.getElementById('editDisposePlan').value = item.disposePlan;

        var statusSelect = document.getElementById('editStatus');
        statusSelect.innerHTML = '';
        STATUS_OPTIONS.filter(function(s) {
            return !s.plan || s.plan === item.disposePlan;
        }).forEach(function(s) {
            var opt = document.createElement('option');
            opt.value = s.value;
            opt.textContent = s.label;
            statusSelect.appendChild(opt);
        });
        if (item.status) {
            statusSelect.value = item.status;
        }

        document.getElementById('editModal').style.display = 'flex';
    }

    function closeEditModal() {
        state.editItemId = null;
        document.getElementById('editModal').style.display = 'none';
    }

    function handleConfirmEdit() {
        var id = state.editItemId;
        if (!id) return;

        var plan = document.getElementById('editDisposePlan').value;
        var status = document.getElementById('editStatus').value;

        var completed = 0;
        var total = 2;
        var hasError = false;

        function done() {
            completed++;
            if (completed >= total) {
                if (!hasError) {
                    showToast('修改成功！', 'success');
                    closeEditModal();
                    loadItems();
                    loadStats();
                    loadDetailedStats();
                }
            }
        }

        fetch(API_BASE + '/items/' + id + '/dispose-plan', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ disposePlan: plan })
        })
        .then(function() { done(); })
        .catch(function() { hasError = true; showToast('修改处理方式失败', 'error'); done(); });

        if (status) {
            fetch(API_BASE + '/items/' + id + '/status', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: status })
            })
            .then(function() { done(); })
            .catch(function() { hasError = true; showToast('修改状态失败', 'error'); done(); });
        } else {
            done();
        }
    }

    function handleDeleteItem(id) {
        if (!confirm('确定要删除这个物品吗？')) return;

        fetch(API_BASE + '/items/' + id, { method: 'DELETE' })
        .then(function() {
            showToast('删除成功！', 'success');
            delete state.selectedIds[id];
            loadItems();
            loadStats();
            loadDetailedStats();
        })
        .catch(function() {
            showToast('删除失败', 'error');
        });
    }

    function handleBatchUpdateDisposePlan(plan) {
        var ids = Object.keys(state.selectedIds);
        if (!ids.length) return;
        if (!confirm('确定要将选中的 ' + ids.length + ' 件物品的处理方式改为 ' + PLAN_LABELS[plan] + ' 吗？')) return;

        fetch(API_BASE + '/items/batch/dispose-plan', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids, disposePlan: plan })
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            showToast('已批量修改 ' + (data.updated || 0) + ' 件物品', 'success');
            loadItems();
            loadStats();
            loadDetailedStats();
        })
        .catch(function() {
            showToast('批量操作失败', 'error');
        });
    }

    function handleBatchUpdateStatus(status) {
        var ids = Object.keys(state.selectedIds);
        if (!ids.length) return;
        if (!confirm('确定要将选中的 ' + ids.length + ' 件物品的状态改为 ' + STATUS_LABELS[status] + ' 吗？')) return;

        fetch(API_BASE + '/items/batch/status', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids, status: status })
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            showToast('已批量修改 ' + (data.updated || 0) + ' 件物品', 'success');
            loadItems();
            loadStats();
            loadDetailedStats();
        })
        .catch(function() {
            showToast('批量操作失败', 'error');
        });
    }

    function handleBatchDelete() {
        var ids = Object.keys(state.selectedIds);
        if (!ids.length) return;
        if (!confirm('确定要删除选中的 ' + ids.length + ' 件物品吗？此操作不可恢复！')) return;

        fetch(API_BASE + '/items/batch/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids })
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            showToast('已删除 ' + (data.deleted || 0) + ' 件物品', 'success');
            state.selectedIds = {};
            loadItems();
            loadStats();
            loadDetailedStats();
        })
        .catch(function() {
            showToast('批量删除失败', 'error');
        });
    }

    function loadStats() {
        fetch(API_BASE + '/stats')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            document.getElementById('expectedRevenue').textContent = formatPrice(data.expectedRevenue || 0);
            document.getElementById('soldRevenue').textContent = formatPrice(data.soldRevenue || 0);
            document.getElementById('totalItems').textContent = data.totalItems || 0;
        })
        .catch(function() {});
    }

    function loadDetailedStats() {
        fetch(API_BASE + '/stats/detailed')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            renderPlanStats(data.disposePlanCounts || {}, data.totalItems || 0);
            renderStatusStats(data.statusCounts || {}, data.totalItems || 0);
            renderCategoryStats(data.categoryCounts || {});

            var sellingCount = (data.statusCounts && data.statusCounts.SELLING) || 0;
            document.getElementById('sellingCount').textContent = sellingCount;

            var doneCount = 0;
            if (data.statusCounts) {
                doneCount += (data.statusCounts.SOLD || 0)
                    + (data.statusCounts.GIVEN_AWAY || 0)
                    + (data.statusCounts.DISCARDED || 0)
                    + (data.statusCounts.KEPT || 0);
            }
            document.getElementById('doneCount').textContent = doneCount;
        })
        .catch(function() {});
    }

    function renderPlanStats(counts, total) {
        var container = document.getElementById('planStats');
        var html = '';

        Object.keys(PLAN_LABELS).forEach(function(plan) {
            var count = counts[plan] || 0;
            var percent = total ? Math.round(count * 100 / total) : 0;
            var color = PLAN_COLORS[plan] || '#999';
            var label = PLAN_LABELS[plan];

            html += '<div class="stat-bar-item">'
                + '<span class="stat-bar-label">' + label + '</span>'
                + '<div class="stat-bar-track"><div class="stat-bar-fill" style="width:' + percent + '%;background:' + color + '"></div></div>'
                + '<span class="stat-bar-count">' + count + '</span>'
                + '</div>';
        });

        container.innerHTML = html;
    }

    function renderStatusStats(counts, total) {
        var container = document.getElementById('statusStats');
        var html = '';

        STATUS_OPTIONS.forEach(function(s) {
            var count = counts[s.value] || 0;
            if (count === 0 && total > 20) return;
            var percent = total ? Math.round(count * 100 / total) : 0;
            var color = STATUS_COLORS[s.value] || '#999';

            html += '<div class="stat-bar-item">'
                + '<span class="stat-bar-label">' + s.label + '</span>'
                + '<div class="stat-bar-track"><div class="stat-bar-fill" style="width:' + percent + '%;background:' + color + '"></div></div>'
                + '<span class="stat-bar-count">' + count + '</span>'
                + '</div>';
        });

        if (!html) {
            html = '<div style="font-size:12px;color:#aaa;text-align:center;padding:8px 0;">暂无数据</div>';
        }

        container.innerHTML = html;
    }

    function renderCategoryStats(counts) {
        var container = document.getElementById('categoryStats');
        var keys = Object.keys(counts || {});

        if (!keys.length) {
            container.innerHTML = '<div style="font-size:12px;color:#aaa;text-align:center;padding:8px 0;">暂无分类数据</div>';
            return;
        }

        keys.sort(function(a, b) { return counts[b] - counts[a]; });

        var html = '';
        keys.slice(0, 8).forEach(function(cat) {
            html += '<div class="stat-list-item">'
                + '<span class="stat-list-name">' + escapeHtml(cat) + '</span>'
                + '<span class="stat-list-count">' + counts[cat] + ' 件</span>'
                + '</div>';
        });

        container.innerHTML = html;
    }

    function formatPrice(price) {
        if (price === null || price === undefined) return '0.00';
        return parseFloat(price).toFixed(2);
    }

    function escapeHtml(str) {
        if (!str) return '';
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function debounce(fn, delay) {
        var timer = null;
        return function() {
            var args = arguments;
            var context = this;
            if (timer) clearTimeout(timer);
            timer = setTimeout(function() {
                fn.apply(context, args);
            }, delay);
        };
    }

    function showToast(message, type) {
        var toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = 'toast show ' + (type || '');
        setTimeout(function() {
            toast.className = 'toast';
        }, 2500);
    }

    document.addEventListener('DOMContentLoaded', init);
})();
