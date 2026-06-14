const API_BASE = 'http://localhost:8080/api';

let currentTasteFilter = '';
let currentTimeFilter = '';

document.addEventListener('DOMContentLoaded', function() {
    loadRecipes();
    loadStats();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('recipeForm').addEventListener('submit', handleAddRecipe);

    document.querySelectorAll('#tasteFilters .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('#tasteFilters .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentTasteFilter = this.dataset.taste;
            loadRecipes();
            loadStats();
        });
    });

    document.querySelectorAll('#timeFilters .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('#timeFilters .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentTimeFilter = this.dataset.time;
            loadRecipes();
            loadStats();
        });
    });

    document.getElementById('closeModal').addEventListener('click', closeModal);
    document.getElementById('cancelEdit').addEventListener('click', closeModal);
    document.getElementById('saveEdit').addEventListener('click', handleSaveEdit);

    document.getElementById('editModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeModal();
        }
    });
}

function buildQueryString() {
    const params = [];
    if (currentTasteFilter) {
        params.push('taste=' + encodeURIComponent(currentTasteFilter));
    }
    if (currentTimeFilter) {
        params.push('time=' + currentTimeFilter);
    }
    return params.length > 0 ? '?' + params.join('&') : '';
}

async function loadRecipes() {
    try {
        const response = await fetch(API_BASE + '/recipes' + buildQueryString());
        const recipes = await response.json();
        renderRecipes(recipes);
    } catch (error) {
        console.error('加载菜谱失败:', error);
    }
}

async function loadStats() {
    try {
        const response = await fetch(API_BASE + '/stats' + buildQueryString());
        const stats = await response.json();
        document.getElementById('totalCount').textContent = stats.total;
        document.getElementById('under30Count').textContent = stats.under30;
        document.getElementById('filteredCount').textContent = stats.filtered;
    } catch (error) {
        console.error('加载统计失败:', error);
    }
}

function renderRecipes(recipes) {
    const grid = document.getElementById('recipesGrid');
    const emptyState = document.getElementById('emptyState');

    if (recipes.length === 0) {
        grid.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    grid.innerHTML = recipes.map(recipe => `
        <div class="recipe-card" data-id="${recipe.id}">
            <div class="recipe-header">
                <h3 class="recipe-name">${escapeHtml(recipe.name)}</h3>
                <div class="recipe-tags">
                    <span class="tag tag-taste">${escapeHtml(recipe.taste)}</span>
                    <span class="tag tag-time">${recipe.estimatedTime}分钟</span>
                </div>
            </div>
            <div class="recipe-body">
                <div class="recipe-ingredients">
                    <strong>🥬 主要食材：</strong>
                    <p>${escapeHtml(recipe.mainIngredients)}</p>
                </div>
                <div class="recipe-notes">
                    <strong>📝 做法备注：</strong>
                    <p>${escapeHtml(recipe.notes) || '暂无备注'}</p>
                </div>
            </div>
            <div class="recipe-actions">
                <button class="btn btn-edit" onclick="openEditModal('${recipe.id}', '${escapeHtml(recipe.notes).replace(/'/g, "\\'")}')">编辑备注</button>
                <button class="btn btn-danger" onclick="deleteRecipe('${recipe.id}')">删除</button>
            </div>
        </div>
    `).join('');
}

async function handleAddRecipe(e) {
    e.preventDefault();

    const recipe = {
        name: document.getElementById('name').value.trim(),
        taste: document.getElementById('taste').value,
        estimatedTime: parseInt(document.getElementById('estimatedTime').value),
        mainIngredients: document.getElementById('mainIngredients').value.trim(),
        notes: document.getElementById('notes').value.trim()
    };

    try {
        const response = await fetch(API_BASE + '/recipes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(recipe)
        });

        if (response.ok) {
            document.getElementById('recipeForm').reset();
            loadRecipes();
            loadStats();
        } else {
            alert('添加失败，请重试');
        }
    } catch (error) {
        console.error('添加菜谱失败:', error);
        alert('添加失败，请检查服务器是否启动');
    }
}

function openEditModal(id, notes) {
    document.getElementById('editRecipeId').value = id;
    document.getElementById('editNotes').value = notes;
    document.getElementById('editModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('editModal').style.display = 'none';
    document.getElementById('editRecipeId').value = '';
    document.getElementById('editNotes').value = '';
}

async function handleSaveEdit() {
    const id = document.getElementById('editRecipeId').value;
    const notes = document.getElementById('editNotes').value.trim();

    try {
        const response = await fetch(API_BASE + '/recipes/' + id, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ notes: notes })
        });

        if (response.ok) {
            closeModal();
            loadRecipes();
        } else {
            alert('保存失败，请重试');
        }
    } catch (error) {
        console.error('保存备注失败:', error);
        alert('保存失败，请检查服务器是否启动');
    }
}

async function deleteRecipe(id) {
    if (!confirm('确定要删除这道菜谱吗？')) {
        return;
    }

    try {
        const response = await fetch(API_BASE + '/recipes/' + id, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadRecipes();
            loadStats();
        } else {
            alert('删除失败，请重试');
        }
    } catch (error) {
        console.error('删除菜谱失败:', error);
        alert('删除失败，请检查服务器是否启动');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
