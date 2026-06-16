// weekend.js — 前端交互逻辑
// 负责发送 fetch 请求到 Python 后端，获取/新增/修改/删除地点，渲染卡片和统计信息
// 数据流：用户新增/修改/删除/筛选地点 → weekend.js 用 fetch 调 Python 后端接口
//        → place_manager 更新内存数据 → place_store 保存 JSON 文件
//        → 前端重新拉取地点列表并刷新统计和卡片

const API_BASE = "/api/places";

const $ = (sel) => document.querySelector(sel);

const filterType = $("#filterType");
const sortByCost = $("#sortByCost");
const searchInput = $("#searchInput");
const maxCostInput = $("#maxCost");
const recommendBtn = $("#recommendBtn");
const cardList = $("#cardList");
const addForm = $("#addForm");

const editModal = $("#editModal");
const editIdInput = $("#editId");
const editWantSelect = $("#editWant");
const editSaveBtn = $("#editSave");
const editCancelBtn = $("#editCancel");

let isRecommendMode = false;
let searchDebounceTimer = null;

function wantStars(level) {
    return "★".repeat(level) + "☆".repeat(5 - level);
}

function renderStats(stats) {
    $("#avgCost").textContent = "¥" + stats.avg_cost;
    $("#top3Count").textContent = stats.top3_count + " 个";
    $("#filteredCount").textContent = stats.filtered_count + " 个";
}

function renderCards(places) {
    if (places.length === 0) {
        const tip = isRecommendMode
            ? "还没有可推荐的地点，快去添加一些吧~ 🌟"
            : "还没有添加任何地点，从上方表单开始吧 ✨";
        cardList.innerHTML = `<div class="empty-tip" id="emptyTip">${tip}</div>`;
        return;
    }

    cardList.innerHTML = places
        .map(
            (p) => `
        <div class="place-card ${p.visited ? "visited" : ""}" data-id="${p.id}">
            <div class="card-header">
                <span class="card-name">${escHtml(p.name)}</span>
                <span class="card-type">${escHtml(p.place_type || "未分类")}</span>
            </div>
            ${p.plan_date ? `<div class="card-date" style="align-self:flex-start;">📅 ${escHtml(p.plan_date)}</div>` : ""}
            <div class="card-info">
                <span><span class="card-info-label">预计花费</span> ¥${p.estimated_cost}</span>
                <span><span class="card-info-label">交通方式</span> ${escHtml(p.transport || "未设定")}</span>
                <span><span class="card-info-label">想去程度</span> <span class="want-stars">${wantStars(p.want_level)}</span></span>
            </div>
            <div class="card-notes">${escHtml(p.notes || "")}</div>
            <div class="card-actions">
                <button class="btn btn-visited" onclick="toggleVisited('${p.id}')">
                    ${p.visited ? "未去过" : "已去过"}
                </button>
                <button class="btn btn-primary btn-sm" onclick="openEdit('${p.id}', ${p.want_level})">修改想去程度</button>
                <button class="btn btn-danger" onclick="deletePlace('${p.id}')">删除</button>
            </div>
        </div>
    `
        )
        .join("");
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
    if (sortByCost.checked) params.set("sort_by_cost", "1");
    const keyword = searchInput.value.trim();
    if (keyword) params.set("keyword", keyword);
    const maxCost = maxCostInput.value.trim();
    if (maxCost !== "") params.set("max_cost", maxCost);

    const res = await fetch(API_BASE + "?" + params.toString());
    const data = await res.json();
    renderStats(data.stats);
    renderCards(data.places);
}

async function fetchRecommend() {
    isRecommendMode = true;
    const res = await fetch(API_BASE + "/recommend");
    const data = await res.json();
    renderCards(data.places);
    $("#filteredCount").textContent = data.places.length + " 个（推荐）";
}

function exitRecommendMode() {
    if (!isRecommendMode) return;
    isRecommendMode = false;
    fetchPlaces();
}

addForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
        name: $("#inputName").value.trim(),
        place_type: $("#inputType").value,
        estimated_cost: parseFloat($("#inputCost").value) || 0,
        transport: $("#inputTransport").value,
        want_level: parseInt($("#inputWant").value),
        notes: $("#inputNotes").value.trim(),
        plan_date: $("#inputDate").value || "",
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

function openEdit(id, currentWant) {
    editIdInput.value = id;
    editWantSelect.value = String(currentWant);
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
    const want_level = parseInt(editWantSelect.value);
    const res = await fetch(API_BASE + "/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ want_level }),
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

filterType.addEventListener("change", () => {
    exitRecommendMode();
    fetchPlaces();
});

sortByCost.addEventListener("change", () => {
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
    } else {
        fetchRecommend();
    }
});

fetchPlaces();
