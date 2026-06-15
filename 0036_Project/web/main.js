const API_BASE = '/api';
const TASTE_OPTIONS = ['清淡', '香辣', '甜口', '咸鲜', '酸辣', '麻辣', '酸甜'];
const CATEGORY_OPTIONS = ['常做', '想尝试', '已学会', '拿手菜', '招待客人'];

let state = {
    keyword: '',
    ingredients: '',
    taste: '',
    category: '',
    time: '',
    sortBy: 'createdAt',
    sortOrder: 'desc'
};

const recipeDataMap = {};
let searchDebounceTimer = null;
let importFileContent = '';

document.addEventListener('DOMContentLoaded', function() {
    setupGlobalEvents();
    loadAll();
});

function setupGlobalEvents() {
    document.getElementById('recipeForm').addEventListener('submit', handleAddRecipe);
    document.getElementById('retryBtn').addEventListener('click', loadAll);

    document.getElementById('searchInput').addEventListener('input', function(e) {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            state.keyword = e.target.value.trim();
            loadAll();
        }, 250);
    });

    document.getElementById('ingredientInput').addEventListener('input', function(e) {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            state.ingredients = e.target.value.trim();
            loadAll();
        }, 300);
    });

    bindFilterButtons('#tasteFilters', 'taste');
    bindFilterButtons('#categoryFilters', 'category');
    bindFilterButtons('#timeFilters', 'time');

    document.getElementById('sortBy').addEventListener('change', function(e) {
        const [sortBy, sortOrder] = e.target.value.split('_');
        state.sortBy = sortBy;
        state.sortOrder = sortOrder;
        loadAll();
    });

    document.getElementById('recipesGrid').addEventListener('click', handleCardClick);

    document.getElementById('closeModal').addEventListener('click', () => closeModal('editModal'));
    document.getElementById('cancelEdit').addEventListener('click', () => closeModal('editModal'));
    document.getElementById('saveEdit').addEventListener('click', handleSaveEdit);

    document.getElementById('btnShopping').addEventListener('click', openShoppingModal);
    document.getElementById('closeShoppingModal').addEventListener('click', () => closeModal('shoppingModal'));
    document.getElementById('closeShoppingBtn').addEventListener('click', () => closeModal('shoppingModal'));
    document.getElementById('genShoppingBtn').addEventListener('click', generateShoppingList);

    document.getElementById('btnExport').addEventListener('click', handleExport);
    document.getElementById('btnImport').addEventListener('click', () => openModal('importModal'));
    document.getElementById('closeImportModal').addEventListener('click', () => closeModal('importModal'));
    document.getElementById('cancelImportBtn').addEventListener('click', () => closeModal('importModal'));
    document.getElementById('confirmImportBtn').addEventListener('click', handleConfirmImport);
    document.getElementById('importFile2').addEventListener('change', handleImportFileSelect);

    bindModalClickOutside('editModal');
    bindModalClickOutside('shoppingModal');
    bindModalClickOutside('importModal');
}

function bindFilterButtons(containerId, key) {
    document.querySelectorAll(containerId + ' .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll(containerId + ' .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            state[key] = this.dataset[key] || '';
            loadAll();
        });
    });
}

function bindModalClickOutside(id) {
    document.getElementById(id).addEventListener('click', function(e) {
        if (e.target === this) closeModal(id);
    });
}

function buildQueryString() {
    const params = [];
    if (state.keyword) params.push('q=' + encodeURIComponent(state.keyword));
    if (state.taste) params.push('taste=' + encodeURIComponent(state.taste));
    if (state.category) params.push('category=' + encodeURIComponent(state.category));
    if (state.time) params.push('time=' + state.time);
    if (state.ingredients) params.push('ingredients=' + encodeURIComponent(state.ingredients));
    if (state.sortBy) params.push('sortBy=' + state.sortBy);
    if (state.sortOrder) params.push('sortOrder=' + state.sortOrder);
    return params.length ? '?' + params.join('&') : '';
}

async function loadAll() {
    try {
        hideError();
        const [recipesRes, statsRes] = await Promise.all([
            fetch(API_BASE + '/recipes' + buildQueryString()),
            fetch(API_BASE + '/stats' + buildQueryString())
        ]);
        if (!recipesRes.ok || !statsRes.ok) throw new Error('请求失败');
        const recipes = await recipesRes.json();
        const stats = await statsRes.json();
        renderStats(stats);
        renderRecipes(recipes);
    } catch (e) {
        console.error(e);
        showError('加载失败，请检查服务器是否启动');
    }
}

function renderStats(stats) {
    document.getElementById('totalCount').textContent = stats.total;
    document.getElementById('under30Count').textContent = stats.under30;
    document.getElementById('filteredCount').textContent = stats.filtered;
}

function renderRecipes(recipes) {
    const grid = document.getElementById('recipesGrid');
    const empty = document.getElementById('emptyState');
    Object.keys(recipeDataMap).forEach(k => delete recipeDataMap[k]);

    if (!recipes.length) {
        grid.innerHTML = '';
        empty.style.display = 'block';
        return;
    }
    empty.style.display = 'none';

    grid.innerHTML = recipes.map(r => {
        recipeDataMap[r.id] = r;
        const tags = [];
        if (r.taste) tags.push(`<span class="tag tag-taste">${escapeHtml(r.taste)}</span>`);
        if (r.category) tags.push(`<span class="tag tag-category">${escapeHtml(r.category)}</span>`);
        tags.push(`<span class="tag tag-time">${r.estimatedTime}分钟</span>`);
        tags.push(`<span class="tag tag-difficulty">${'⭐'.repeat(r.difficulty || 1)}</span>`);

        const stars = r.rating ? '⭐'.repeat(r.rating) : '';

        const meta = [];
        if (r.servings) meta.push(`<span>👥 ${r.servings}人份</span>`);
        if (r.cost) meta.push(`<span>💰 约${r.cost}元</span>`);
        meta.push(`<span>📅 ${formatDate(r.createdAt)}</span>`);
        if (r.lastMadeAt) meta.push(`<span class="last-made">✅ 上次做: ${formatDate(r.lastMadeAt)}</span>`);

        const imageHtml = r.image
            ? `<div class="recipe-image"><img src="${escapeHtml(r.image)}" alt="" onerror="this.parentElement.innerHTML='🍽️'"></div>`
            : `<div class="recipe-image">🍽️</div>`;

        let stepsHtml = '';
        if (r.steps && r.steps.length) {
            stepsHtml = `<div class="recipe-section"><strong>📖 步骤：</strong><ol class="recipe-steps">${
                r.steps.map(s => `<li>${escapeHtml(s)}</li>`).join('')
            }</ol></div>`;
        }

        return `
        <div class="recipe-card" data-id="${r.id}">
            ${imageHtml}
            <div class="recipe-body">
                <div class="recipe-name-row">
                    <h3 class="recipe-name">${escapeHtml(r.name)}</h3>
                    <span class="recipe-rating">${stars}</span>
                </div>
                <div class="recipe-tags">${tags.join('')}</div>
                <div class="recipe-meta">${meta.join('')}</div>
                <div class="recipe-section">
                    <strong>🥬 食材：</strong>
                    <p>${escapeHtml(r.mainIngredients)}</p>
                </div>
                ${stepsHtml}
                ${r.notes ? `<div class="recipe-section"><strong>📝 备注：</strong><p>${escapeHtml(r.notes)}</p></div>` : ''}
            </div>
            <div class="recipe-actions">
                <button class="btn btn-edit">编辑</button>
                <button class="btn btn-copy">复制</button>
                <button class="btn btn-made">标记已做</button>
                <button class="btn btn-danger">删除</button>
            </div>
        </div>`;
    }).join('');
}

function handleCardClick(e) {
    const card = e.target.closest('.recipe-card');
    if (!card) return;
    const id = card.dataset.id;
    const recipe = recipeDataMap[id];
    if (!recipe) return;

    if (e.target.classList.contains('btn-edit')) {
        openEditModal(recipe);
    } else if (e.target.classList.contains('btn-copy')) {
        duplicateRecipe(id);
    } else if (e.target.classList.contains('btn-danger')) {
        deleteRecipe(id);
    } else if (e.target.classList.contains('btn-made')) {
        markAsMade(id);
    }
}

function openEditModal(r) {
    document.getElementById('editRecipeId').value = r.id;
    document.getElementById('editName').value = r.name || '';
    document.getElementById('editTaste').value = r.taste || '';
    document.getElementById('editCategory').value = r.category || '';
    document.getElementById('editEstimatedTime').value = r.estimatedTime || '';
    document.getElementById('editDifficulty').value = r.difficulty || 1;
    document.getElementById('editServings').value = r.servings || 2;
    document.getElementById('editCost').value = r.cost || '';
    document.getElementById('editRating').value = r.rating || 0;
    document.getElementById('editMainIngredients').value = r.mainIngredients || '';
    document.getElementById('editSteps').value = (r.steps || []).join('\n');
    document.getElementById('editNotes').value = r.notes || '';
    document.getElementById('editImage').value = r.image || '';
    document.getElementById('editError').textContent = '';
    openModal('editModal');
}

async function handleSaveEdit() {
    const id = document.getElementById('editRecipeId').value;
    const name = document.getElementById('editName').value.trim();
    const estimatedTime = parseInt(document.getElementById('editEstimatedTime').value);
    const mainIngredients = document.getElementById('editMainIngredients').value.trim();
    const errEl = document.getElementById('editError');

    if (!name) { errEl.textContent = '菜名不能为空'; return; }
    if (!estimatedTime || estimatedTime <= 0) { errEl.textContent = '预计用时必须大于0'; return; }
    if (!mainIngredients) { errEl.textContent = '主要食材不能为空'; return; }
    errEl.textContent = '';

    const stepsText = document.getElementById('editSteps').value;
    const steps = stepsText.split('\n').map(s => s.trim()).filter(s => s);

    const body = {
        name,
        taste: document.getElementById('editTaste').value,
        category: document.getElementById('editCategory').value,
        estimatedTime,
        difficulty: parseInt(document.getElementById('editDifficulty').value),
        servings: parseInt(document.getElementById('editServings').value) || 2,
        cost: parseFloat(document.getElementById('editCost').value) || 0,
        rating: parseInt(document.getElementById('editRating').value),
        mainIngredients,
        steps,
        notes: document.getElementById('editNotes').value,
        image: document.getElementById('editImage').value
    };

    try {
        const res = await fetch(API_BASE + '/recipes/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            if (data.errors) throw new Error(Object.values(data.errors).join('；'));
            throw new Error(data.error || '保存失败');
        }
        closeModal('editModal');
        showToast('修改成功！', 'success');
        loadAll();
    } catch (e) {
        errEl.textContent = e.message;
        showToast(e.message, 'error');
    }
}

async function handleAddRecipe(e) {
    e.preventDefault();
    clearErrors();

    const name = document.getElementById('name').value.trim();
    const estimatedTime = parseInt(document.getElementById('estimatedTime').value);
    const mainIngredients = document.getElementById('mainIngredients').value.trim();

    let hasError = false;
    if (!name) { setError('name', '菜名不能为空'); hasError = true; }
    if (!estimatedTime || estimatedTime <= 0) { setError('estimatedTime', '预计用时必须大于0分钟'); hasError = true; }
    if (!mainIngredients) { setError('mainIngredients', '主要食材不能为空'); hasError = true; }
    if (hasError) return;

    const stepsText = document.getElementById('steps').value;
    const steps = stepsText.split('\n').map(s => s.trim()).filter(s => s);

    const body = {
        name,
        taste: document.getElementById('taste').value,
        category: document.getElementById('category').value,
        estimatedTime,
        difficulty: parseInt(document.getElementById('difficulty').value),
        servings: parseInt(document.getElementById('servings').value) || 2,
        cost: parseFloat(document.getElementById('cost').value) || 0,
        rating: parseInt(document.getElementById('rating').value),
        mainIngredients,
        steps,
        notes: document.getElementById('notes').value,
        image: document.getElementById('image').value
    };

    try {
        const res = await fetch(API_BASE + '/recipes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            if (data.errors) {
                Object.entries(data.errors).forEach(([k, v]) => setError(k, v));
                throw new Error('请检查表单');
            }
            throw new Error(data.error || '添加失败');
        }
        document.getElementById('recipeForm').reset();
        document.getElementById('difficulty').value = '1';
        document.getElementById('servings').value = '2';
        document.getElementById('rating').value = '0';
        showToast('菜谱添加成功！', 'success');
        loadAll();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

async function deleteRecipe(id) {
    if (!confirm('确定删除这个菜谱吗？')) return;
    try {
        const res = await fetch(API_BASE + '/recipes/' + id, { method: 'DELETE' });
        if (!res.ok) throw new Error('删除失败');
        showToast('已删除', 'success');
        loadAll();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

async function duplicateRecipe(id) {
    try {
        const res = await fetch(API_BASE + '/recipes/' + id + '/duplicate', { method: 'GET' });
        if (!res.ok) throw new Error('复制失败');
        showToast('已复制一份菜谱', 'success');
        loadAll();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

async function markAsMade(id) {
    try {
        const res = await fetch(API_BASE + '/recipes/' + id + '/mark-made', { method: 'POST' });
        if (!res.ok) throw new Error('标记失败');
        showToast('已记录本次制作 👍', 'success');
        loadAll();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

function openShoppingModal() {
    const list = document.getElementById('shoppingRecipeList');
    list.innerHTML = Object.values(recipeDataMap).map(r =>
        `<label class="shopping-item">
            <input type="checkbox" value="${r.id}">
            <span>${escapeHtml(r.name)}</span>
        </label>`
    ).join('');
    document.getElementById('shoppingResult').innerHTML = '';
    openModal('shoppingModal');
}

async function generateShoppingList() {
    const ids = Array.from(document.querySelectorAll('#shoppingRecipeList input:checked')).map(i => i.value);
    if (!ids.length) {
        showToast('请先选择菜谱', 'warning');
        return;
    }
    try {
        const res = await fetch(API_BASE + '/shopping-list', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipeIds: ids })
        });
        const data = await res.json();
        const items = data.items || [];
        const result = document.getElementById('shoppingResult');
        if (!items.length) {
            result.innerHTML = '<p class="muted">没有需要购买的食材</p>';
            return;
        }
        result.innerHTML = `<h4>🛒 需要购买：</h4><ul>${items.map(i => `<li>${escapeHtml(i)}</li>`).join('')}</ul>`;
    } catch (e) {
        showToast(e.message, 'error');
    }
}

function handleExport() {
    window.location.href = API_BASE + '/export';
    showToast('开始导出...', 'info');
}

function handleImportFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;
    document.getElementById('importFileName').textContent = file.name;
    const reader = new FileReader();
    reader.onload = ev => { importFileContent = ev.target.result; };
    reader.readAsText(file);
}

async function handleConfirmImport() {
    if (!importFileContent) {
        showToast('请先选择 JSON 文件', 'warning');
        return;
    }
    const merge = document.getElementById('importMerge').checked;
    if (!merge && !confirm('覆盖模式会清空现有所有数据，确定继续吗？')) return;
    try {
        const res = await fetch(API_BASE + '/import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ data: importFileContent, merge })
        });
        if (!res.ok) throw new Error('导入失败');
        closeModal('importModal');
        showToast('导入成功！', 'success');
        document.getElementById('importFile2').value = '';
        document.getElementById('importFileName').textContent = '未选择文件';
        importFileContent = '';
        loadAll();
    } catch (e) {
        showToast(e.message, 'error');
    }
}

function openModal(id) { document.getElementById(id).style.display = 'flex'; }
function closeModal(id) { document.getElementById(id).style.display = 'none'; }

function setError(field, msg) {
    const el = document.getElementById('err-' + field);
    if (el) el.textContent = msg;
}
function clearErrors() {
    document.querySelectorAll('.error-text').forEach(e => e.textContent = '');
}

function showError(text) {
    document.getElementById('errorText').textContent = text;
    document.getElementById('errorState').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('recipesGrid').innerHTML = '';
}
function hideError() {
    document.getElementById('errorState').style.display = 'none';
}

function showToast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => {
        el.style.animation = 'fadeOut 0.3s ease forwards';
        setTimeout(() => el.remove(), 300);
    }, 2500);
}

function formatDate(ts) {
    if (!ts) return '';
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
