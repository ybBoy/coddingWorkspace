// weekend.js — 前端交互逻辑
// 负责发送 fetch 请求到 Python 后端，获取/新增/修改/删除地点，渲染卡片和统计信息
// 数据流：用户新增/修改/删除/筛选地点 → weekend.js 用 fetch 调 Python 后端接口
//        → place_manager 更新内存数据 → place_store 保存 JSON 文件
//        → 前端重新拉取地点列表并刷新统计和卡片

const API_BASE = "/api/places";

const $ = (sel) => document.querySelector(sel);

const filterType = $("#filterType");
const filterTag = $("#filterTag");
const sortByCost = $("#sortByCost");
const sortByDate = $("#sortByDate");
const searchInput = $("#searchInput");
const maxCostInput = $("#maxCost");
const recommendBtn = $("#recommendBtn");
const cardList = $("#cardList");
const addForm = $("#addForm");
const exportBtn = $("#exportBtn");
const importBtn = $("#importBtn");
const importFile = $("#importFile");

const editModal = $("#editModal");
const editIdInput = $("#editId");
const editSaveBtn = $("#editSave");
const editCancelBtn = $("#editCancel");

let isRecommendMode = false;
let searchDebounceTimer = null;

function wantStars(level) {
    return "★".repeat(level) + "☆".repeat(5 - level);
}

function parseTags(str) {
    if (!str) return [];
    return str.split(/[,，]/).map(s => s.trim()).filter(Boolean);
}

function isThisWeekend(dateStr) {
    if (!dateStr) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const target = new Date(dateStr);
    target.setHours(0, 0, 0, 0);
    const dayOfWeek = today.getDay();
    const daysToSat = (6 - dayOfWeek + 7) % 7;
    const daysToSun = daysToSat + 1;
    const sat = new Date(today);
    sat.setDate(today.getDate() + daysToSat);
    const sun = new Date(today);
    sun.setDate(today.getDate() + daysToSun);
    return target.getTime() === sat.getTime() || target.getTime() === sun.getTime();
}

function isFuture(dateStr) {
    if (!dateStr) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const target = new Date(dateStr);
    target.setHours(0, 0, 0, 0);
    return target.getTime() > today.getTime() && !isThisWeekend(dateStr);
}

function groupByDate(places) {
    const groups = {
        weekend: { title: "🗓️ 本周末", list: [] },
        future: { title: "📆 未来安排", list: [] },
        none: { title: "📝 未安排日期", list: [] },
    };
    for (const p of places) {
        if (!p.plan_date) groups.none.list.push(p);
        else if (isThisWeekend(p.plan_date)) groups.weekend.list.push(p);
        else if (isFuture(p.plan_date)) groups.future.list.push(p);
        else groups.none.list.push(p);
    }
    return groups;
}

function renderStats(stats) {
    $("#avgCost").textContent = "¥" + stats.avg_cost;
    $("#top3Count").textContent = stats.top3_count + " 个";
    $("#filteredCount").textContent = stats.filtered_count + " 个";
}

function renderDashboard(dash) {
    $("#dashTotal").textContent = dash.total_count;
    $("#dashVisited").textContent = dash.visited_count;
    $("#dashUnvisited").textContent = dash.unvisited_count;
    $("#dashBudget").textContent = "¥" + dash.total_budget;

    const typeDash = $("#typeDash");
    const items = Object.entries(dash.type_counts)
        .map(([name, count]) => `
            <div class="type-dash-item">
                <span class="type-dash-name">${escHtml(name)}</span>
                <span class="type-dash-count">${count}</span>
            </div>
        `)
        .join("");
    typeDash.innerHTML = items || '<span style="color:#aaa;font-size:13px;">暂无数据</span>';

    const curVal = filterTag.value;
    filterTag.innerHTML =
        '<option value="">全部</option>' +
        dash.all_tags.map(t => `<option value="${escHtml(t)}" ${t === curVal ? "selected" : ""}>${escHtml(t)}</option>`).join("");
}

function renderCards(places) {
    if (places.length === 0) {
        const tip = isRecommendMode
            ? "还没有可推荐的地点，快去添加一些吧~ 🌟"
            : "还没有添加任何地点，从上方表单开始吧 ✨";
        cardList.innerHTML = `<div class="empty-tip" id="emptyTip">${tip}</div>`;
        return;
    }

    const groups = groupByDate(places);
    const groupOrder = ["weekend", "future", "none"];

    let html = "";
    for (const key of groupOrder) {
        const g = groups[key];
        if (g.list.length === 0) continue;
        html += `<div class="date-group">
            <div class="date-group-title">${g.title}（${g.list.length}）</div>
            <div class="date-group-cards">`;
        for (const p of g.list) {
            html += buildCard(p);
        }
        html += "</div></div>";
    }
    cardList.innerHTML = html;
}

function buildCard(p) {
    const tagsHtml = (p.tags && p.tags.length > 0)
        ? `<div class="card-tags">${p.tags.map(t => `<span class="card-tag">#${escHtml(t)}</span>`).join("")}</div>`
        : "";
    return `
        <div class="place-card ${p.visited ? "visited" : ""}" data-id="${p.id}">
            <div class="card-header">
                <span class="card-name">${escHtml(p.name)}</span>
                <span class="card-type">${escHtml(p.place_type || "未分类")}</span>
            </div>
            ${p.plan_date ? `<div class="card-date">📅 ${escHtml(p.plan_date)}</div>` : ""}
            ${tagsHtml}
            <div class="card-info">
                <span><span class="card-info-label">预计花费</span> ¥${p.estimated_cost}</span>
                <span><span class="card-info-label">交通方式</span> ${escHtml(p.transport || "未设定")}</span>
                <span><span class="card-info-label">想去程度</span> <span class="want-stars">${wantStars(p.want_level)}</span></span>
            </div>
            <div class="card-notes">${escHtml(p.notes || "")}</div>
            <div class="card-actions">
                <button class="btn btn-visited" onclick="toggleVisited('${p.id}')">
                    ${p.visited ? "✓ 已去过" : "未去过"}
                </button>
                <button class="btn btn-edit" onclick="openFullEdit('${p.id}')">编辑</button>
                <button class="btn btn-danger" onclick="deletePlace('${p.id}')">删除</button>
            </div>
        </div>
    `;
}

function escHtml(str) {
    const d = document.createElement("div");
    d.textContent = str;
    return d.innerHTML;
}

async function fetchPlaces() {
    if (isRecommendMode) return;
    const params = new URLSearchParams();
    const type = filterType.value;
    if (type) params.set("type", type);
    const tag = filterTag.value;
    if (tag) params.set("tag", tag);
    if (sortByCost.checked) params.set("sort_by_cost", "1");
    if (sortByDate.checked) params.set("sort_by_date", "1");
    const keyword = searchInput.value.trim();
    if (keyword) params.set("keyword", keyword);
    const maxCost = maxCostInput.value.trim();
    if (maxCost !== "") params.set("max_cost", maxCost);

    const res = await fetch(API_BASE + "?" + params.toString());
    const data = await res.json();
    renderStats(data.stats);
    renderDashboard(data.dashboard);
    renderCards(data.places);
}

async function fetchRecommend() {
    isRecommendMode = true;
    const res = await fetch(API_BASE + "/recommend");
    const data = await res.json();
    renderStats(data.stats);
    renderDashboard(data.dashboard);
    renderCards(data.places);
    $("#filteredCount").textContent = data.places.length + " 个（推荐）";
}

function exitRecommendMode() {
    if (!isRecommendMode) return;
    isRecommendMode = false;
}

addForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const tags = parseTags($("#inputTags").value);
    const body = {
        name: $("#inputName").value.trim(),
        place_type: $("#inputType").value,
        estimated_cost: parseFloat($("#inputCost").value) || 0,
        transport: $("#inputTransport").value,
        want_level: parseInt($("#inputWant").value),
        notes: $("#inputNotes").value.trim(),
        plan_date: $("#inputDate").value || "",
        tags,
    };
    if (!body.name) return;

    const res = await fetch(API_BASE, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });

    if (res.ok) {
        addForm.reset();
        $("#inputWant").value = "3";
        exitRecommendMode();
        fetchPlaces();
    } else {
        const err = await res.json();
        alert(err.error || "添加失败");
    }
});

function openFullEdit(id) {
    const params = new URLSearchParams();
    if (isRecommendMode) {
        fetch(API_BASE + "/recommend")
            .then(r => r.json())
            .then(data => {
                const p = data.places.find(x => x.id === id);
                fillEditForm(p);
            });
    } else {
        const type = filterType.value;
        if (type) params.set("type", type);
        const tag = filterTag.value;
        if (tag) params.set("tag", tag);
        if (sortByCost.checked) params.set("sort_by_cost", "1");
        if (sortByDate.checked) params.set("sort_by_date", "1");
        const keyword = searchInput.value.trim();
        if (keyword) params.set("keyword", keyword);
        const maxCost = maxCostInput.value.trim();
        if (maxCost !== "") params.set("max_cost", maxCost);
        fetch(API_BASE + "?" + params.toString())
            .then(r => r.json())
            .then(data => {
                const p = data.places.find(x => x.id === id);
                fillEditForm(p);
            });
    }
}

function fillEditForm(p) {
    if (!p) { alert("未找到地点"); return; }
    editIdInput.value = p.id;
    $("#editName").value = p.name || "";
    $("#editType").value = p.place_type || "";
    $("#editCost").value = p.estimated_cost || 0;
    $("#editDate").value = p.plan_date || "";
    $("#editTransport").value = p.transport || "";
    $("#editWant").value = String(p.want_level || 3);
    $("#editTags").value = (p.tags || []).join(", ");
    $("#editNotes").value = p.notes || "";
    editModal.style.display = "flex";
}

editCancelBtn.addEventListener("click", () => {
    editModal.style.display = "none";
});

editModal.addEventListener("click", (e) => {
    if (e.target === editModal) editModal.style.display = "none";
});

editSaveBtn.addEventListener("click", async () => {
    const id = editIdInput.value;
    const tags = parseTags($("#editTags").value);
    const body = {
        name: $("#editName").value.trim(),
        place_type: $("#editType").value,
        estimated_cost: parseFloat($("#editCost").value) || 0,
        plan_date: $("#editDate").value || "",
        transport: $("#editTransport").value,
        want_level: parseInt($("#editWant").value),
        notes: $("#editNotes").value.trim(),
        tags,
    };
    if (!body.name) { alert("名称不能为空"); return; }
    const res = await fetch(API_BASE + "/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    editModal.style.display = "none";
    if (res.ok) {
        if (isRecommendMode) fetchRecommend();
        else fetchPlaces();
    } else {
        const err = await res.json();
        alert(err.error || "修改失败");
    }
});

async function toggleVisited(id) {
    const res = await fetch(API_BASE + "/" + id + "/toggle-visited", {
        method: "POST",
    });
    if (res.ok) {
        if (isRecommendMode) fetchRecommend();
        else fetchPlaces();
    } else {
        const err = await res.json();
        alert(err.error || "切换失败");
    }
}

async function deletePlace(id) {
    if (!confirm("确定要删除这个地点吗？")) return;
    const res = await fetch(API_BASE + "/" + id, { method: "DELETE" });
    if (res.ok) {
        if (isRecommendMode) fetchRecommend();
        else fetchPlaces();
    } else {
        const err = await res.json();
        alert(err.error || "删除失败");
    }
}

exportBtn.addEventListener("click", async () => {
    const res = await fetch(API_BASE + "/export");
    const data = await res.json();
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    const d = new Date();
    const stamp = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
    a.download = `weekend_places_${stamp}.json`;
    a.click();
    URL.revokeObjectURL(url);
});

importBtn.addEventListener("click", () => {
    importFile.click();
});

importFile.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!confirm("导入会覆盖现有全部数据，确认继续？")) {
        importFile.value = "";
        return;
    }
    try {
        const text = await file.text();
        const data = JSON.parse(text);
        const res = await fetch(API_BASE + "/import", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        const result = await res.json();
        if (res.ok) {
            alert(result.message);
            exitRecommendMode();
            fetchPlaces();
        } else {
            alert(result.error || "导入失败");
        }
    } catch (err) {
        alert("文件解析失败：" + err.message);
    } finally {
        importFile.value = "";
    }
});

filterType.addEventListener("change", () => {
    exitRecommendMode();
    fetchPlaces();
});

filterTag.addEventListener("change", () => {
    exitRecommendMode();
    fetchPlaces();
});

sortByCost.addEventListener("change", () => {
    exitRecommendMode();
    fetchPlaces();
});

sortByDate.addEventListener("change", () => {
    exitRecommendMode();
    fetchPlaces();
});

searchInput.addEventListener("input", () => {
    exitRecommendMode();
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(fetchPlaces, 300);
});

maxCostInput.addEventListener("input", () => {
    exitRecommendMode();
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(fetchPlaces, 300);
});

recommendBtn.addEventListener("click", () => {
    if (isRecommendMode) {
        exitRecommendMode();
        fetchPlaces();
    } else {
        fetchRecommend();
    }
});

fetchPlaces();
