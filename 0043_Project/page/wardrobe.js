const API_BASE = "/api";

let currentClothes = [];
let selectedOutfitIds = new Set();

const $ = (id) => document.getElementById(id);

function showToast(message, type = "info") {
    const toast = $("toast");
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    setTimeout(() => {
        toast.className = `toast ${type}`;
    }, 2500);
}

async function apiFetch(path, options = {}) {
    const defaultHeaders = { "Content-Type": "application/json" };
    const config = {
        ...options,
        headers: { ...defaultHeaders, ...options.headers },
    };
    const resp = await fetch(`${API_BASE}${path}`, config);
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok) {
        const msg = data.error || `请求失败 (${resp.status})`;
        showToast(msg, "error");
        throw new Error(msg);
    }
    return data;
}

async function loadStats() {
    try {
        const stats = await apiFetch("/stats");
        $("totalCount").textContent = stats.total_count || 0;
        renderTopWorn(stats.top_worn || []);
    } catch (e) {
        console.error("加载统计失败", e);
    }
}

function renderTopWorn(topWorn) {
    const container = $("topWornList");
    if (!topWorn || topWorn.length === 0) {
        container.innerHTML = '<div class="top-worn-empty">暂无记录</div>';
        return;
    }
    container.innerHTML = topWorn.map((item, idx) => `
        <div class="top-worn-item">
            <span class="top-worn-name">
                <span class="top-worn-rank rank-${idx + 1}">${idx + 1}</span>
                ${escapeHtml(item.name)}
            </span>
            <span class="top-worn-count">${item.wear_count} 次</span>
        </div>
    `).join("");
}

async function loadFilters() {
    try {
        const filters = await apiFetch("/filters");
        populateFilter("filterType", filters.types || []);
        populateFilter("filterColor", filters.colors || []);
        populateFilter("filterSeason", filters.seasons || []);
    } catch (e) {
        console.error("加载筛选选项失败", e);
    }
}

function populateFilter(selectId, options) {
    const select = $(selectId);
    const currentValue = select.value;
    select.innerHTML = '<option value="all">全部</option>' +
        options.map(opt => `<option value="${escapeHtml(opt)}">${escapeHtml(opt)}</option>`).join("");
    if (options.includes(currentValue)) {
        select.value = currentValue;
    }
}

async function loadClothes() {
    const type = $("filterType").value;
    const color = $("filterColor").value;
    const season = $("filterSeason").value;

    const params = new URLSearchParams();
    if (type && type !== "all") params.append("type", type);
    if (color && color !== "all") params.append("color", color);
    if (season && season !== "all") params.append("season", season);

    const queryString = params.toString() ? `?${params.toString()}` : "";

    try {
        currentClothes = await apiFetch(`/clothes${queryString}`);
        $("filteredCount").textContent = currentClothes.length;
        renderClothes(currentClothes);
    } catch (e) {
        console.error("加载衣物失败", e);
    }
}

function escapeHtml(str) {
    if (!str) return "";
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
}

function formatDate(isoStr) {
    if (!isoStr) return "从未穿过";
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit" });
    } catch {
        return "未知";
    }
}

function renderClothes(clothes) {
    const grid = $("clothesGrid");

    if (!clothes || clothes.length === 0) {
        grid.innerHTML = `
            <div class="empty-state">
                <p>衣橱空空如也～</p>
                <p class="empty-sub">添加你的第一件衣物吧！</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = clothes.map(c => `
        <div class="clothing-card ${c.is_long_time_no_wear ? "long-time-no-wear" : ""}" data-id="${c.id}">
            <div class="card-header">
                <div>
                    <div class="card-name">${escapeHtml(c.name)}</div>
                </div>
            </div>
            <div style="margin-bottom: 12px;">
                <span class="card-tag type">${escapeHtml(c.type)}</span>
                <span class="card-tag color">${escapeHtml(c.color)}</span>
                <span class="card-tag season">${escapeHtml(c.season)}</span>
            </div>
            <div class="card-info">
                <div class="card-info-row">
                    <span>穿着次数</span>
                    <span><span class="wear-count-badge">👕 ${c.wear_count}</span></span>
                </div>
                <div class="card-info-row">
                    <span>${c.has_been_worn ? "上次穿着" : "购入时间"}</span>
                    <span>${c.has_been_worn ? formatDate(c.last_worn_at) : formatDate(c.created_at)}</span>
                </div>
                <div class="card-info-row">
                    <span>${c.has_been_worn ? "距上次" : "已购入"}</span>
                    <span>${c.days_since_last_worn} 天</span>
                </div>
            </div>
            ${c.is_long_time_no_wear ? `
            <div class="warning-badge">⚠️ ${c.has_been_worn ? "很久没穿了" : "从未穿过"}</div>
            ` : ""}
            ${c.remark ? `<div class="card-remark">${escapeHtml(c.remark)}</div>` : ""}
            <div class="card-actions">
                <button class="btn btn-sm btn-secondary" onclick="openEditModal('${c.id}')">编辑</button>
                <button class="btn btn-sm btn-danger" onclick="deleteClothing('${c.id}')">删除</button>
            </div>
        </div>
    `).join("");
}

async function addClothing(e) {
    e.preventDefault();
    const body = {
        name: $("name").value,
        type: $("type").value,
        color: $("color").value,
        season: $("season").value,
        remark: $("remark").value,
    };

    try {
        await apiFetch("/clothes", {
            method: "POST",
            body: JSON.stringify(body),
        });
        showToast("添加成功！", "success");
        e.target.reset();
        await refreshAll();
    } catch (e) {
        console.error("添加失败", e);
    }
}

async function deleteClothing(id) {
    if (!confirm("确定要删除这件衣物吗？")) return;
    try {
        await apiFetch(`/clothes/${id}`, { method: "DELETE" });
        showToast("删除成功！", "success");
        await refreshAll();
    } catch (e) {
        console.error("删除失败", e);
    }
}

function openEditModal(id) {
    const clothing = currentClothes.find(c => c.id === id);
    if (!clothing) return;

    $("editId").value = clothing.id;
    $("editName").value = clothing.name;
    $("editType").value = clothing.type;
    $("editColor").value = clothing.color;
    $("editSeason").value = clothing.season;
    $("editRemark").value = clothing.remark || "";

    $("editModal").style.display = "flex";
}

function closeEditModal() {
    $("editModal").style.display = "none";
    $("editClothingForm").reset();
}

async function saveEdit() {
    const id = $("editId").value;
    if (!id) return;

    const body = {
        name: $("editName").value,
        type: $("editType").value,
        color: $("editColor").value,
        season: $("editSeason").value,
        remark: $("editRemark").value,
    };

    try {
        await apiFetch(`/clothes/${id}`, {
            method: "PUT",
            body: JSON.stringify(body),
        });
        showToast("保存成功！", "success");
        closeEditModal();
        await refreshAll();
    } catch (e) {
        console.error("保存失败", e);
    }
}

function openOutfitModal() {
    renderOutfitSelectList();
    selectedOutfitIds.clear();
    $("outfitNote").value = "";
    $("outfitModal").style.display = "flex";
}

function closeOutfitModal() {
    $("outfitModal").style.display = "none";
    selectedOutfitIds.clear();
}

async function renderOutfitSelectList() {
    const container = $("outfitSelectList");
    let allClothes = [];
    try {
        allClothes = await apiFetch("/clothes");
    } catch (e) {
        allClothes = currentClothes;
    }

    if (!allClothes || allClothes.length === 0) {
        container.innerHTML = '<div class="empty-state small">暂无衣物可选择</div>';
        return;
    }

    container.innerHTML = allClothes.map(c => `
        <label class="outfit-select-item" data-id="${c.id}">
            <input type="checkbox" value="${c.id}" onchange="toggleOutfitItem('${c.id}', this.checked)">
            <div class="outfit-select-info">
                <div class="outfit-select-name">${escapeHtml(c.name)}</div>
                <div class="outfit-select-meta">${escapeHtml(c.type)} · ${escapeHtml(c.color)} · ${escapeHtml(c.season)}</div>
            </div>
        </label>
    `).join("");
}

function toggleOutfitItem(id, checked) {
    if (checked) {
        selectedOutfitIds.add(id);
    } else {
        selectedOutfitIds.delete(id);
    }
    document.querySelector(`.outfit-select-item[data-id="${id}"]`)?.classList.toggle("selected", checked);
}

async function confirmOutfit() {
    if (selectedOutfitIds.size === 0) {
        showToast("请至少选择一件衣物", "warning");
        return;
    }

    const body = {
        clothing_ids: Array.from(selectedOutfitIds),
        note: $("outfitNote").value,
    };

    try {
        const result = await apiFetch("/outfit", {
            method: "POST",
            body: JSON.stringify(body),
        });
        const count = result.updated_clothes ? result.updated_clothes.length : 0;
        showToast(`记录成功！${count} 件衣物穿着次数 +1`, "success");
        closeOutfitModal();
        await refreshAll();
    } catch (e) {
        console.error("记录穿搭失败", e);
    }
}

function resetFilters() {
    $("filterType").value = "all";
    $("filterColor").value = "all";
    $("filterSeason").value = "all";
    loadClothes();
}

async function refreshAll() {
    await Promise.all([loadStats(), loadFilters(), loadClothes()]);
}

function initEventListeners() {
    $("addClothingForm").addEventListener("submit", addClothing);
    $("filterType").addEventListener("change", loadClothes);
    $("filterColor").addEventListener("change", loadClothes);
    $("filterSeason").addEventListener("change", loadClothes);
    $("resetFilterBtn").addEventListener("click", resetFilters);
    $("openOutfitModal").addEventListener("click", openOutfitModal);
    $("closeOutfitModal").addEventListener("click", closeOutfitModal);
    $("cancelOutfitBtn").addEventListener("click", closeOutfitModal);
    $("confirmOutfitBtn").addEventListener("click", confirmOutfit);
    $("closeEditModal").addEventListener("click", closeEditModal);
    $("cancelEditBtn").addEventListener("click", closeEditModal);
    $("saveEditBtn").addEventListener("click", saveEdit);

    document.querySelectorAll(".modal-overlay").forEach(overlay => {
        overlay.addEventListener("click", (e) => {
            if (e.target === overlay) {
                overlay.style.display = "none";
            }
        });
    });
}

document.addEventListener("DOMContentLoaded", () => {
    initEventListeners();
    refreshAll();
});
