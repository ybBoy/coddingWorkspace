const API_BASE = "/api/baking";

let meta = {
    success_levels: [],
    dessert_types: [],
    sort_fields: [],
    sort_field_labels: {},
    sort_orders: [],
    sort_order_labels: {},
};

let state = {
    newImageData: null,
    editImageData: null,
};

function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str == null ? "" : String(str);
    return div.innerHTML;
}

function formatDate(isoString) {
    try {
        const d = new Date(isoString);
        return d.toLocaleString("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    } catch {
        return isoString || "";
    }
}

function numOrNull(v) {
    if (v === "" || v === null || v === undefined) return null;
    const n = Number(v);
    return isNaN(n) ? null : n;
}

function fileToDataUrl(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function setImagePreview(previewEl, data, placeholder = "暂无图片预览", clearBtn = null) {
    if (data) {
        previewEl.innerHTML = `<img src="${data}" alt="预览">`;
        if (clearBtn) clearBtn.style.display = "";
    } else {
        previewEl.innerHTML = `<span class="image-preview-placeholder">${placeholder}</span>`;
        if (clearBtn) clearBtn.style.display = "none";
    }
}

function showTips(el, warnings) {
    if (!warnings || warnings.length === 0) {
        el.className = "validate-tips hidden";
        el.innerHTML = "";
        return;
    }
    const hasWarn = warnings.some(w => w.level === "warn");
    const hasInfo = warnings.some(w => w.level === "info");
    const hasError = warnings.some(w => w.level === "error");
    const level = hasError ? "error" : hasWarn ? "warn" : "info";
    el.className = `validate-tips ${level}`;
    el.innerHTML = `<ul>${warnings.map(w => `<li>${escapeHtml(w.message)}</li>`).join("")}</ul>`;
}

async function fetchMeta() {
    const res = await fetch(`${API_BASE}/meta`);
    meta = await res.json();
    populateMetaSelects();
}

function populateMetaSelects() {
    const typeFilter = document.getElementById("dessertTypeFilter");
    meta.dessert_types.forEach(t => {
        if (!typeFilter.querySelector(`option[value="${t}"]`)) {
            const opt = document.createElement("option");
            opt.value = t; opt.textContent = t;
            typeFilter.appendChild(opt);
        }
    });

    const successFilter = document.getElementById("successLevelFilter");
    successFilter.innerHTML = `<option value="全部">全部</option>` +
        meta.success_levels.map(s => `<option value="${s}">${s}</option>`).join("");

    const sortBy = document.getElementById("sortBy");
    sortBy.innerHTML = meta.sort_fields.map(f =>
        `<option value="${f}">${meta.sort_field_labels[f] || f}</option>`
    ).join("");
    sortBy.value = "created_at";

    const sortOrder = document.getElementById("sortOrder");
    sortOrder.innerHTML = meta.sort_orders.map(o =>
        `<option value="${o}">${meta.sort_order_labels[o] || o}</option>`
    ).join("");
    sortOrder.value = "desc";

    ["dessertType", "editDessertType"].forEach(id => {
        const el = document.getElementById(id);
        el.innerHTML = meta.dessert_types.map(t =>
            `<option value="${t}">${t}</option>`
        ).join("");
    });

    ["successLevel", "editSuccessLevel"].forEach(id => {
        const el = document.getElementById(id);
        el.innerHTML = meta.success_levels.map(s =>
            `<option value="${s}">${s}</option>`
        ).join("");
        el.value = "一般";
    });
}

async function fetchTrials() {
    const params = new URLSearchParams();
    const dt = document.getElementById("dessertTypeFilter").value;
    const sl = document.getElementById("successLevelFilter").value;
    const q = document.getElementById("searchInput").value.trim();
    const sb = document.getElementById("sortBy").value;
    const so = document.getElementById("sortOrder").value;
    if (dt) params.set("dessert_type", dt);
    if (sl) params.set("success_level", sl);
    if (q) params.set("search", q);
    params.set("sort_by", sb);
    params.set("sort_order", so);

    const url = `${API_BASE}/trials${params.toString() ? "?" + params.toString() : ""}`;
    const res = await fetch(url);
    const data = await res.json();
    updateStats(data.statistics);
    renderTrials(data.trials);
}

function updateStats(stats) {
    document.getElementById("totalCount").textContent = stats.total;
    document.getElementById("successRate").textContent = stats.success_rate + "%";
    document.getElementById("filteredCount").textContent = stats.filtered_count;
}

function renderScores(t) {
    const tags = [];
    if (t.taste_score != null) tags.push(`<span class="score-tag">👅 口感 <span class="score-val">${t.taste_score}</span></span>`);
    if (t.look_score != null) tags.push(`<span class="score-tag">👀 外观 <span class="score-val">${t.look_score}</span></span>`);
    if (t.texture_score != null) tags.push(`<span class="score-tag">🍞 组织 <span class="score-val">${t.texture_score}</span></span>`);
    if (!tags.length) return "";
    return `<div class="card-scores">${tags.join("")}</div>`;
}

function renderTrials(trials) {
    const listEl = document.getElementById("trialsList");
    const emptyEl = document.getElementById("emptyState");
    listEl.innerHTML = "";
    if (!trials.length) {
        emptyEl.classList.remove("hidden");
        return;
    }
    emptyEl.classList.add("hidden");

    trials.forEach(t => {
        const card = document.createElement("div");
        card.className = "trial-card";
        card.dataset.id = t.id;

        const thumb = t.image_data
            ? `<div class="card-thumbnail"><img src="${t.image_data}" alt="成品"></div>`
            : "";

        card.innerHTML = `
            ${thumb}
            <div class="card-header">
                <div>
                    <div class="card-title">${escapeHtml(t.dessert_name)}</div>
                    <span class="card-type">${escapeHtml(t.dessert_type)} · ${escapeHtml(t.recipe_version || "v1")}</span>
                </div>
                <span class="success-badge success-${escapeHtml(t.success_level)}">${escapeHtml(t.success_level)}</span>
            </div>
            <div class="card-meta">
                <div class="meta-item">
                    <span class="meta-label">🌡️ 温度</span>
                    <span class="meta-value">${t.temperature}°C</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">⏱️ 时间</span>
                    <span class="meta-value">${t.duration_minutes} 分钟</span>
                </div>
            </div>
            ${renderScores(t)}
            <div class="card-success">
                <label for="success-${t.id}">成功程度：</label>
                <select id="success-${t.id}" class="success-select" data-id="${t.id}">
                    ${meta.success_levels.map(s =>
                        `<option value="${s}" ${s === t.success_level ? "selected" : ""}>${s}</option>`
                    ).join("")}
                </select>
            </div>
            ${t.notes ? `<div class="card-notes">📝 ${escapeHtml(t.notes)}</div>` : ""}
            <div class="card-date">记录时间：${formatDate(t.created_at)}</div>
            <div class="card-actions">
                <button class="btn btn-edit edit-btn" data-id="${t.id}">✏️ 编辑</button>
                <button class="btn btn-danger delete-btn" data-id="${t.id}">🗑️ 删除</button>
            </div>
        `;
        listEl.appendChild(card);
    });

    attachCardEvents();
}

function attachCardEvents() {
    document.querySelectorAll(".success-select").forEach(sel => {
        sel.addEventListener("change", async e => {
            const id = parseInt(e.target.dataset.id);
            try {
                const res = await fetch(`${API_BASE}/trials/${id}/success`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ success_level: e.target.value }),
                });
                if (res.ok) {
                    refreshAll();
                }
            } catch (err) { console.error(err); }
        });
    });

    document.querySelectorAll(".edit-btn").forEach(btn => {
        btn.addEventListener("click", e => {
            const id = parseInt(e.currentTarget.dataset.id);
            openEditModal(id);
        });
    });

    document.querySelectorAll(".delete-btn").forEach(btn => {
        btn.addEventListener("click", async e => {
            const id = parseInt(e.currentTarget.dataset.id);
            if (!confirm("确定要删除这条试验记录吗？")) return;
            try {
                const res = await fetch(`${API_BASE}/trials/${id}`, { method: "DELETE" });
                if (res.ok) refreshAll();
                else alert("删除失败");
            } catch (err) { console.error(err); }
        });
    });
}

function collectFormData(prefix = "") {
    const $ = id => document.getElementById(prefix + id);
    return {
        dessert_name: $("dessertName") ? $("dessertName").value.trim() : "",
        dessert_type: $("dessertType") ? $("dessertType").value : "",
        recipe_version: $("recipeVersion") ? $("recipeVersion").value.trim() || "v1" : "v1",
        temperature: numOrNull($("temperature") ? $("temperature").value : null),
        duration_minutes: numOrNull($("durationMinutes") ? $("durationMinutes").value : null),
        success_level: $("successLevel") ? $("successLevel").value : "一般",
        taste_score: numOrNull($("tasteScore") ? $("tasteScore").value : null),
        look_score: numOrNull($("lookScore") ? $("lookScore").value : null),
        texture_score: numOrNull($("textureScore") ? $("textureScore").value : null),
        notes: $("notes") ? $("notes").value.trim() : "",
    };
}

async function handleNewSubmit(e) {
    e.preventDefault();
    const payload = collectFormData("");
    payload.image_data = state.newImageData || null;

    if (!payload.dessert_name || !payload.dessert_type ||
        payload.temperature == null || payload.duration_minutes == null) {
        alert("请填写所有必填字段");
        return;
    }

    const dupWarn = await (await fetch(`${API_BASE}/trials/validate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })).json();

    if (dupWarn.warnings && dupWarn.warnings.length) {
        const hasDup = dupWarn.warnings.some(w => w.code === "duplicate");
        if (hasDup) {
            const ok = confirm("⚠️  " + dupWarn.warnings[0].message + "\n\n是否仍然继续添加？");
            if (!ok) return;
        }
    }

    try {
        const res = await fetch(`${API_BASE}/trials`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (res.ok) {
            resetNewForm();
            refreshAll();
        } else {
            const err = await res.json().catch(() => ({}));
            alert(err.error || "添加失败");
        }
    } catch (err) {
        console.error(err);
    }
}

function resetNewForm() {
    document.getElementById("trialForm").reset();
    document.getElementById("successLevel").value = "一般";
    document.getElementById("recipeVersion").value = "v1";
    document.getElementById("temperature").value = "180";
    document.getElementById("durationMinutes").value = "30";
    state.newImageData = null;
    setImagePreview(
        document.getElementById("imagePreview"),
        null,
        "暂无图片预览",
        document.getElementById("clearImageBtn")
    );
    showTips(document.getElementById("validateTips"), []);
}

async function runValidate(prefix, tipsEl, excludeId = null) {
    const payload = collectFormData(prefix);
    const url = excludeId != null
        ? `${API_BASE}/trials/${excludeId}/validate`
        : `${API_BASE}/trials/validate`;
    try {
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        const data = await res.json();
        showTips(tipsEl, data.warnings || []);
    } catch {
        showTips(tipsEl, []);
    }
}

async function openEditModal(id) {
    try {
        const res = await fetch(`${API_BASE}/trials/${id}`);
        if (!res.ok) return;
        const t = await res.json();

        document.getElementById("editTrialId").value = t.id;
        document.getElementById("editDessertName").value = t.dessert_name;
        document.getElementById("editDessertType").value = t.dessert_type;
        document.getElementById("editRecipeVersion").value = t.recipe_version || "v1";
        document.getElementById("editTemperature").value = t.temperature;
        document.getElementById("editDurationMinutes").value = t.duration_minutes;
        document.getElementById("editSuccessLevel").value = t.success_level;
        document.getElementById("editTasteScore").value = t.taste_score ?? "";
        document.getElementById("editLookScore").value = t.look_score ?? "";
        document.getElementById("editTextureScore").value = t.texture_score ?? "";
        document.getElementById("editNotes").value = t.notes || "";

        state.editImageData = t.image_data || null;
        setImagePreview(
            document.getElementById("editImagePreview"),
            state.editImageData,
            "暂无图片预览",
            document.getElementById("editClearImageBtn")
        );
        showTips(document.getElementById("editValidateTips"), []);
        document.getElementById("editModal").classList.remove("hidden");
    } catch (err) {
        console.error(err);
    }
}

function closeEditModal() {
    document.getElementById("editModal").classList.add("hidden");
    state.editImageData = null;
}

async function saveEdit() {
    const id = parseInt(document.getElementById("editTrialId").value);
    const payload = collectFormData("edit");
    payload.image_data = state.editImageData;

    if (!payload.dessert_name || !payload.dessert_type ||
        payload.temperature == null || payload.duration_minutes == null) {
        alert("请填写所有必填字段");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/trials/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (res.ok) {
            closeEditModal();
            refreshAll();
        } else {
            const err = await res.json().catch(() => ({}));
            alert(err.error || "保存失败");
        }
    } catch (err) { console.error(err); }
}

async function fetchComparison() {
    try {
        const res = await fetch(`${API_BASE}/versions`);
        const data = await res.json();
        renderComparison(data.comparisons || []);
    } catch (err) { console.error(err); }
}

function renderComparison(list) {
    const container = document.getElementById("compareList");
    const empty = document.getElementById("compareEmpty");
    container.innerHTML = "";
    if (!list.length) {
        empty.classList.remove("hidden");
        return;
    }
    empty.classList.add("hidden");

    list.forEach(item => {
        const versions = item.versions || [];
        let bestIdx = -1;
        let bestRank = -1;
        versions.forEach((v, i) => {
            const rank = (v.avg_score ? v.avg_score * 10 : 0) + v.success_rank * 20;
            if (rank > bestRank) { bestRank = rank; bestIdx = i; }
        });

        const card = document.createElement("div");
        card.className = "compare-card";
        card.innerHTML = `
            <div class="compare-header">
                <div class="compare-title">${escapeHtml(item.dessert_name)} <span class="card-type" style="margin-left:8px;">${escapeHtml(item.dessert_type)}</span></div>
                <div class="compare-count">共 ${item.version_count} 个版本</div>
            </div>
            <div class="compare-versions">
                ${versions.map((v, i) => `
                    <div class="compare-version ${i === bestIdx ? "best" : ""}">
                        <div class="cv-version">${escapeHtml(v.version)}</div>
                        <div class="cv-row"><span class="cv-label">温度</span><span class="cv-value">${v.temperature}°C</span></div>
                        <div class="cv-row"><span class="cv-label">时间</span><span class="cv-value">${v.duration_minutes}分</span></div>
                        <div class="cv-row"><span class="cv-label">评价</span><span class="cv-value">${escapeHtml(v.success_level)}</span></div>
                        ${v.taste_score != null ? `<div class="cv-row"><span class="cv-label">口感</span><span class="cv-value">${v.taste_score}</span></div>` : ""}
                        ${v.look_score != null ? `<div class="cv-row"><span class="cv-label">外观</span><span class="cv-value">${v.look_score}</span></div>` : ""}
                        ${v.texture_score != null ? `<div class="cv-row"><span class="cv-label">组织</span><span class="cv-value">${v.texture_score}</span></div>` : ""}
                        ${v.avg_score != null ? `<div class="cv-row"><span class="cv-label">均分</span><span class="cv-value">${v.avg_score}</span></div>` : ""}
                    </div>
                `).join("")}
            </div>
        `;
        container.appendChild(card);
    });
}

function triggerDownload(href, filename) {
    const a = document.createElement("a");
    a.href = href;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}

function exportJson() {
    triggerDownload(`${API_BASE}/export/json`, "baking_trials.json");
}

function exportCsv() {
    triggerDownload(`${API_BASE}/export/csv`, "baking_trials.csv");
}

function openImportModal() {
    document.getElementById("importModalFile").value = "";
    document.getElementById("importMode").value = "append";
    showTips(document.getElementById("importStatus"), []);
    document.getElementById("importModal").classList.remove("hidden");
}

function closeImportModal() {
    document.getElementById("importModal").classList.add("hidden");
}

async function confirmImport() {
    const fileEl = document.getElementById("importModalFile");
    const file = fileEl.files && fileEl.files[0];
    const statusEl = document.getElementById("importStatus");
    if (!file) {
        showTips(statusEl, [{ level: "warn", message: "请先选择要导入的 JSON 文件" }]);
        return;
    }
    try {
        const text = await file.text();
        let items = JSON.parse(text);
        if (!Array.isArray(items)) {
            if (items && Array.isArray(items.trials)) items = items.trials;
            else throw new Error("JSON 不是数组格式");
        }
        const mode = document.getElementById("importMode").value;
        if (mode === "replace") {
            const ok = confirm("⚠️ 替换模式会清空所有现有记录，确定继续吗？");
            if (!ok) return;
        }

        const res = await fetch(`${API_BASE}/import`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ items, mode }),
        });
        const data = await res.json();
        const warnings = [];
        warnings.push({ level: "success", message: `成功导入 ${data.imported} 条记录` });
        if (data.errors && data.errors.length) {
            data.errors.slice(0, 10).forEach(e => {
                warnings.push({ level: "error", message: `第 ${e.row} 行: ${e.error}` });
            });
            if (data.errors.length > 10) {
                warnings.push({ level: "warn", message: `...还有 ${data.errors.length - 10} 条错误未显示` });
            }
        }
        showTips(statusEl, warnings);
        if (data.imported > 0) {
            setTimeout(() => {
                closeImportModal();
                refreshAll();
            }, 1200);
        }
    } catch (err) {
        showTips(statusEl, [{ level: "error", message: "导入失败：" + err.message }]);
    }
}

async function refreshAll() {
    await fetchTrials();
    await fetchComparison();
}

const debouncedValidateNew = debounce(() => {
    runValidate("", document.getElementById("validateTips"), null);
}, 350);

const debouncedValidateEdit = debounce(() => {
    const idEl = document.getElementById("editTrialId");
    const id = idEl ? parseInt(idEl.value) : null;
    if (!id) return;
    runValidate("edit", document.getElementById("editValidateTips"), id);
}, 350);

const debouncedFetch = debounce(fetchTrials, 300);

document.addEventListener("DOMContentLoaded", async () => {
    await fetchMeta();
    await refreshAll();

    document.getElementById("trialForm").addEventListener("submit", handleNewSubmit);
    document.getElementById("resetFormBtn").addEventListener("click", resetNewForm);
    document.getElementById("dessertTypeFilter").addEventListener("change", fetchTrials);
    document.getElementById("successLevelFilter").addEventListener("change", fetchTrials);
    document.getElementById("sortBy").addEventListener("change", fetchTrials);
    document.getElementById("sortOrder").addEventListener("change", fetchTrials);
    document.getElementById("searchInput").addEventListener("input", debouncedFetch);

    ["dessertName", "recipeVersion", "temperature", "durationMinutes", "tasteScore", "lookScore", "textureScore"]
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener("input", debouncedValidateNew);
        });

    document.getElementById("imageInput").addEventListener("change", async e => {
        const file = e.target.files && e.target.files[0];
        if (!file) return;
        try {
            state.newImageData = await fileToDataUrl(file);
            setImagePreview(
                document.getElementById("imagePreview"),
                state.newImageData,
                "暂无图片预览",
                document.getElementById("clearImageBtn")
            );
        } catch { alert("图片读取失败"); }
    });

    document.getElementById("clearImageBtn").addEventListener("click", () => {
        state.newImageData = null;
        document.getElementById("imageInput").value = "";
        setImagePreview(
            document.getElementById("imagePreview"),
            null,
            "暂无图片预览",
            document.getElementById("clearImageBtn")
        );
    });

    document.getElementById("closeEditBtn").addEventListener("click", closeEditModal);
    document.getElementById("cancelEditBtn").addEventListener("click", closeEditModal);
    document.getElementById("saveEditBtn").addEventListener("click", saveEdit);
    document.getElementById("editModal").addEventListener("click", e => {
        if (e.target.id === "editModal") closeEditModal();
    });

    ["editDessertName", "editRecipeVersion", "editTemperature", "editDurationMinutes", "editTasteScore", "editLookScore", "editTextureScore"]
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener("input", debouncedValidateEdit);
        });

    document.getElementById("editImageInput").addEventListener("change", async e => {
        const file = e.target.files && e.target.files[0];
        if (!file) return;
        try {
            state.editImageData = await fileToDataUrl(file);
            setImagePreview(
                document.getElementById("editImagePreview"),
                state.editImageData,
                "暂无图片预览",
                document.getElementById("editClearImageBtn")
            );
        } catch { alert("图片读取失败"); }
    });

    document.getElementById("editClearImageBtn").addEventListener("click", () => {
        state.editImageData = null;
        document.getElementById("editImageInput").value = "";
        setImagePreview(
            document.getElementById("editImagePreview"),
            null,
            "暂无图片预览",
            document.getElementById("editClearImageBtn")
        );
    });

    document.getElementById("exportJsonBtn").addEventListener("click", exportJson);
    document.getElementById("exportCsvBtn").addEventListener("click", exportCsv);
    document.getElementById("importBtn").addEventListener("click", openImportModal);
    document.getElementById("closeImportBtn").addEventListener("click", closeImportModal);
    document.getElementById("cancelImportBtn").addEventListener("click", closeImportModal);
    document.getElementById("confirmImportBtn").addEventListener("click", confirmImport);
    document.getElementById("importModal").addEventListener("click", e => {
        if (e.target.id === "importModal") closeImportModal();
    });
});
