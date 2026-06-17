const API_BASE = "/api/gears";

const elTotalWeight = document.getElementById("total-weight");
const elPackedCount = document.getElementById("packed-count");
const elUnpackedEssential = document.getElementById("unpacked-essential");
const elCategoryFilter = document.getElementById("category-filter");
const elGearForm = document.getElementById("gear-form");
const elGearList = document.getElementById("gear-list");
const elCategoryList = document.getElementById("category-list");

async function api(endpoint, options = {}) {
    const resp = await fetch(API_BASE + endpoint, {
        "Content-Type": "application/json",
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers,
        },
    });
    if (!resp.ok) {
        const err = await resp.json().catch(() => ({ error: "请求失败" }));
        throw new Error(err.error || "请求失败");
    }
    return resp.json();
}

async function loadStats() {
    const stats = await api("/stats");
    elTotalWeight.textContent = stats.total_weight;
    elPackedCount.textContent = stats.packed_count;
    elUnpackedEssential.textContent = stats.unpacked_essential_count;
}

async function loadCategories() {
    const categories = await api("/categories");
    const current = elCategoryFilter.value;
    elCategoryFilter.innerHTML = '<option value="全部">全部</option>';
    categories.forEach((cat) => {
        const opt = document.createElement("option");
        opt.value = cat;
        opt.textContent = cat;
        elCategoryFilter.appendChild(opt);
    });
    elCategoryFilter.value = current;

    elCategoryList.innerHTML = "";
    categories.forEach((cat) => {
        const opt = document.createElement("option");
        opt.value = cat;
        elCategoryList.appendChild(opt);
    });
}

async function loadGears() {
    const category = elCategoryFilter.value;
    const params = category !== "全部" ? `?category=${encodeURIComponent(category)}` : "";
    const gears = await api(params);
    renderGears(gears);
}

function renderGears(gears) {
    if (gears.length === 0) {
        elGearList.innerHTML = '<div class="empty-state">🏕️ 暂无装备，添加一些吧</div>';
        return;
    }

    elGearList.innerHTML = gears
        .map((gear) => {
            const packedClass = gear.packed ? "packed" : "";
            const unpackedEssentialClass = gear.essential && !gear.packed ? "unpacked-essential" : "";
            const essentialBadgeClass = gear.essential
                ? gear.packed
                    ? "essential"
                    : "unpacked-essential"
                : "";
            const essentialLabel = gear.essential
                ? gear.packed
                    ? "必带"
                    : "必带⚠"
                : "";
            const essentialBtnClass = gear.essential ? "btn-essential is-essential" : "btn-essential";

            return `
                <div class="gear-card ${packedClass} ${unpackedEssentialClass}" data-id="${gear.id}">
                    <div class="gear-packed-toggle">
                        <input type="checkbox" ${gear.packed ? "checked" : ""} 
                               onchange="togglePacked('${gear.id}')"
                               title="标记已打包">
                    </div>
                    <div class="gear-body">
                        <div class="gear-header">
                            <span class="gear-name">${escapeHtml(gear.name)}</span>
                            ${gear.category ? `<span class="gear-category-tag">${escapeHtml(gear.category)}</span>` : ""}
                            ${essentialLabel ? `<span class="gear-essential-badge ${essentialBadgeClass}">${essentialLabel}</span>` : ""}
                        </div>
                        <div class="gear-meta">
                            <span>${gear.weight} kg</span>
                        </div>
                        ${gear.notes ? `<div class="gear-notes">${escapeHtml(gear.notes)}</div>` : ""}
                    </div>
                    <div class="gear-actions">
                        <button class="btn-icon ${essentialBtnClass}" onclick="toggleEssential('${gear.id}')" title="标记必带">★</button>
                        <button class="btn-icon btn-delete" onclick="deleteGear('${gear.id}')" title="删除">✕</button>
                    </div>
                </div>
            `;
        })
        .join("");
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}

async function togglePacked(gearId) {
    await api(`/${gearId}/toggle-packed`, { method: "PATCH" });
    await refresh();
}

async function toggleEssential(gearId) {
    await api(`/${gearId}/toggle-essential`, { method: "PATCH" });
    await refresh();
}

async function deleteGear(gearId) {
    if (!confirm("确定要删除这件装备吗？")) return;
    await api(`/${gearId}`, { method: "DELETE" });
    await refresh();
}

elGearForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = document.getElementById("gear-name").value.trim();
    const category = document.getElementById("gear-category").value.trim();
    const weight = parseFloat(document.getElementById("gear-weight").value) || 0;
    const essential = document.getElementById("gear-essential").checked;
    const notes = document.getElementById("gear-notes").value.trim();

    if (!name) return;

    try {
        await api("", {
            method: "POST",
            body: JSON.stringify({ name, category, weight, essential, notes }),
        });
        elGearForm.reset();
        document.getElementById("gear-weight").value = "0";
        await refresh();
    } catch (err) {
        alert(err.message);
    }
});

elCategoryFilter.addEventListener("change", loadGears);

async function refresh() {
    await Promise.all([loadStats(), loadCategories(), loadGears()]);
}

refresh();
