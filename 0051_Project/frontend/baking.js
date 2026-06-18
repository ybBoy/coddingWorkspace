const API_BASE = "/api/baking";

let successLevels = [];
let dessertTypes = [];

async function fetchMeta() {
    try {
        const res = await fetch(`${API_BASE}/meta`);
        const data = await res.json();
        successLevels = data.success_levels;
        dessertTypes = data.dessert_types;
        populateSelectOptions();
    } catch (err) {
        console.error("加载元数据失败:", err);
    }
}

function populateSelectOptions() {
    const typeFilter = document.getElementById("dessertTypeFilter");
    dessertTypes.forEach(t => {
        const opt = document.createElement("option");
        opt.value = t;
        opt.textContent = t;
        typeFilter.appendChild(opt);
    });

    const typeSelect = document.getElementById("dessertType");
    dessertTypes.forEach(t => {
        const opt = document.createElement("option");
        opt.value = t;
        opt.textContent = t;
        typeSelect.appendChild(opt);
    });

    const successSelect = document.getElementById("successLevel");
    successLevels.forEach(s => {
        const opt = document.createElement("option");
        opt.value = s;
        opt.textContent = s;
        successSelect.appendChild(opt);
    });
    successSelect.value = "一般";
}

async function fetchTrials() {
    const typeFilter = document.getElementById("dessertTypeFilter").value;
    const search = document.getElementById("searchInput").value.trim();

    const params = new URLSearchParams();
    if (typeFilter) params.set("dessert_type", typeFilter);
    if (search) params.set("search", search);

    const url = `${API_BASE}/trials${params.toString() ? "?" + params.toString() : ""}`;

    try {
        const res = await fetch(url);
        const data = await res.json();
        updateStats(data.statistics);
        renderTrials(data.trials);
    } catch (err) {
        console.error("加载试验记录失败:", err);
    }
}

function updateStats(stats) {
    document.getElementById("totalCount").textContent = stats.total;
    document.getElementById("successRate").textContent = stats.success_rate + "%";
    document.getElementById("filteredCount").textContent = stats.filtered_count;
}

function formatDate(isoString) {
    try {
        const d = new Date(isoString);
        return d.toLocaleString("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    } catch {
        return isoString;
    }
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str || "";
    return div.innerHTML;
}

function renderTrials(trials) {
    const listEl = document.getElementById("trialsList");
    const emptyEl = document.getElementById("emptyState");

    listEl.innerHTML = "";

    if (trials.length === 0) {
        emptyEl.classList.remove("hidden");
        return;
    }

    emptyEl.classList.add("hidden");

    trials.forEach(trial => {
        const card = document.createElement("div");
        card.className = "trial-card";
        card.dataset.id = trial.id;

        card.innerHTML = `
            <div class="card-header">
                <div>
                    <div class="card-title">${escapeHtml(trial.dessert_name)}</div>
                    <span class="card-type">${escapeHtml(trial.dessert_type)} · ${escapeHtml(trial.recipe_version || "v1")}</span>
                </div>
                <span class="success-badge success-${escapeHtml(trial.success_level)}">${escapeHtml(trial.success_level)}</span>
            </div>
            <div class="card-meta">
                <div class="meta-item">
                    <span class="meta-label">🌡️ 温度</span>
                    <span class="meta-value">${trial.temperature}°C</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">⏱️ 时间</span>
                    <span class="meta-value">${trial.duration_minutes} 分钟</span>
                </div>
            </div>
            <div class="card-success">
                <label for="success-${trial.id}">修改成功程度：</label>
                <select id="success-${trial.id}" class="success-select">
                    ${successLevels.map(s => `<option value="${s}" ${s === trial.success_level ? "selected" : ""}>${s}</option>`).join("")}
                </select>
            </div>
            ${trial.notes ? `<div class="card-notes">📝 ${escapeHtml(trial.notes)}</div>` : ""}
            <div class="card-date">记录时间：${formatDate(trial.created_at)}</div>
            <div class="card-actions">
                <button class="btn btn-danger delete-btn" data-id="${trial.id}">🗑️ 删除</button>
            </div>
        `;

        listEl.appendChild(card);
    });

    attachCardEvents();
}

function attachCardEvents() {
    document.querySelectorAll(".success-select").forEach(select => {
        select.addEventListener("change", async (e) => {
            const card = e.target.closest(".trial-card");
            const trialId = parseInt(card.dataset.id);
            const newLevel = e.target.value;

            try {
                const res = await fetch(`${API_BASE}/trials/${trialId}/success`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ success_level: newLevel })
                });

                if (res.ok) {
                    const updated = await res.json();
                    const badge = card.querySelector(".success-badge");
                    badge.textContent = updated.success_level;
                    badge.className = `success-badge success-${updated.success_level}`;
                    fetchTrials();
                }
            } catch (err) {
                console.error("更新成功程度失败:", err);
            }
        });
    });

    document.querySelectorAll(".delete-btn").forEach(btn => {
        btn.addEventListener("click", async (e) => {
            const trialId = parseInt(e.target.dataset.id);
            if (!confirm("确定要删除这条试验记录吗？")) return;

            try {
                const res = await fetch(`${API_BASE}/trials/${trialId}`, {
                    method: "DELETE"
                });

                if (res.ok) {
                    fetchTrials();
                } else {
                    alert("删除失败");
                }
            } catch (err) {
                console.error("删除失败:", err);
            }
        });
    });
}

async function handleSubmit(e) {
    e.preventDefault();

    const payload = {
        dessert_name: document.getElementById("dessertName").value.trim(),
        dessert_type: document.getElementById("dessertType").value,
        recipe_version: document.getElementById("recipeVersion").value.trim() || "v1",
        temperature: parseInt(document.getElementById("temperature").value),
        duration_minutes: parseInt(document.getElementById("durationMinutes").value),
        success_level: document.getElementById("successLevel").value,
        notes: document.getElementById("notes").value.trim()
    };

    if (!payload.dessert_name || !payload.dessert_type || !payload.temperature || !payload.duration_minutes) {
        alert("请填写所有必填字段");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/trials`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            document.getElementById("trialForm").reset();
            document.getElementById("successLevel").value = "一般";
            document.getElementById("recipeVersion").value = "v1";
            document.getElementById("temperature").value = "180";
            document.getElementById("durationMinutes").value = "30";
            fetchTrials();
        } else {
            const err = await res.json();
            alert(err.error || "添加失败");
        }
    } catch (err) {
        console.error("添加记录失败:", err);
    }
}

function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

document.addEventListener("DOMContentLoaded", async () => {
    await fetchMeta();
    await fetchTrials();

    document.getElementById("trialForm").addEventListener("submit", handleSubmit);
    document.getElementById("dessertTypeFilter").addEventListener("change", fetchTrials);
    document.getElementById("searchInput").addEventListener("input", debounce(fetchTrials, 300));
});
