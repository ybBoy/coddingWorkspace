const API_BASE = "/api/medicines";

let currentPurposeFilter = "";
let currentLocationFilter = "";
let currentKeyword = "";
let currentOnlyCheck = false;
let modalCallback = null;
let modalType = "confirm";
let searchTimeout = null;
let editingMedicineId = null;
let allMedicines = [];
let allStats = {};

const elements = {
    medicineList: document.getElementById("medicineList"),
    emptyState: document.getElementById("emptyState"),
    needsCheckCount: document.getElementById("needsCheckCount"),
    lowStockCount: document.getElementById("lowStockCount"),
    expiringCount: document.getElementById("expiringCount"),
    totalCount: document.getElementById("totalCount"),
    medicineForm: document.getElementById("medicineForm"),
    purposeFilter: document.getElementById("purposeFilter"),
    locationFilter: document.getElementById("locationFilter"),
    resetFilterBtn: document.getElementById("resetFilterBtn"),
    searchInput: document.getElementById("searchInput"),
    onlyCheckFilter: document.getElementById("onlyCheckFilter"),
    exportBtn: document.getElementById("exportBtn"),
    importInput: document.getElementById("importInput"),
    modalOverlay: document.getElementById("modalOverlay"),
    modalTitle: document.getElementById("modalTitle"),
    modalMessage: document.getElementById("modalMessage"),
    modalInputContainer: document.getElementById("modalInputContainer"),
    modalInput: document.getElementById("modalInput"),
    modalFormContainer: document.getElementById("modalFormContainer"),
    modalConfirm: document.getElementById("modalConfirm"),
    modalCancel: document.getElementById("modalCancel"),
    toast: document.getElementById("toast"),
    logsList: document.getElementById("logsList"),
};

function showToast(message, type = "success") {
    elements.toast.textContent = message;
    elements.toast.className = `toast ${type}`;
    elements.toast.style.display = "block";
    setTimeout(() => {
        elements.toast.style.display = "none";
    }, 3000);
}

function showModal(title, message, options = {}) {
    const {
        showInput = false,
        inputValue = 1,
        showForm = false,
        formHtml = "",
        modalClass = "",
        confirmText = "确定",
        cancelText = "取消",
        callback,
    } = options;

    modalType = showForm ? "form" : showInput ? "input" : "confirm";
    modalCallback = callback;

    elements.modalTitle.textContent = title;
    elements.modalMessage.textContent = message;
    elements.modalMessage.style.display = message ? "block" : "none";

    elements.modalInputContainer.style.display = showInput ? "block" : "none";
    if (showInput) {
        elements.modalInput.value = inputValue;
    }

    elements.modalFormContainer.style.display = showForm ? "block" : "none";
    if (showForm) {
        elements.modalFormContainer.innerHTML = formHtml;
    }

    elements.modalConfirm.textContent = confirmText;
    elements.modalCancel.textContent = cancelText;

    elements.modalOverlay.className = "modal-overlay" + (modalClass ? " " + modalClass : "");
    elements.modalOverlay.querySelector(".modal").className = "modal" + (modalClass ? " " + modalClass.replace("overlay-", "") : "");

    elements.modalOverlay.style.display = "flex";
    if (showInput) {
        setTimeout(() => elements.modalInput.focus(), 100);
    }
}

function hideModal() {
    elements.modalOverlay.style.display = "none";
    modalCallback = null;
    editingMedicineId = null;
}

elements.modalCancel.addEventListener("click", hideModal);
elements.modalConfirm.addEventListener("click", () => {
    if (modalType === "input") {
        const inputValue = parseInt(elements.modalInput.value) || 1;
        if (modalCallback) modalCallback(inputValue);
    } else if (modalType === "form") {
        if (modalCallback) modalCallback();
    } else {
        if (modalCallback) modalCallback();
    }
    hideModal();
});

elements.modalOverlay.addEventListener("click", (e) => {
    if (e.target === elements.modalOverlay) {
        hideModal();
    }
});

async function fetchMedicines() {
    const params = new URLSearchParams();
    if (currentPurposeFilter) params.append("purpose", currentPurposeFilter);
    if (currentLocationFilter) params.append("location", currentLocationFilter);
    if (currentKeyword) params.append("keyword", currentKeyword);

    const url = params.toString() ? `${API_BASE}?${params}` : API_BASE;

    try {
        const response = await fetch(url);
        const result = await response.json();
        return result;
    } catch (error) {
        console.error("获取药品列表失败:", error);
        showToast("获取药品列表失败", "error");
        return null;
    }
}

async function fetchLogs() {
    try {
        const response = await fetch(`${API_BASE}/logs?limit=30`);
        const result = await response.json();
        return result.data || [];
    } catch (error) {
        console.error("获取日志失败:", error);
        return [];
    }
}

async function fetchFilterOptions() {
    try {
        const response = await fetch(`${API_BASE}/filter-options`);
        const result = await response.json();
        return result.data;
    } catch (error) {
        console.error("获取筛选选项失败:", error);
        return null;
    }
}

function updateFilterOptions(options) {
    if (!options) return;

    const currentPurpose = elements.purposeFilter.value;
    const currentLocation = elements.locationFilter.value;

    elements.purposeFilter.innerHTML = '<option value="">全部用途</option>';
    options.purposes.forEach(p => {
        const option = document.createElement("option");
        option.value = p;
        option.textContent = p;
        elements.purposeFilter.appendChild(option);
    });

    elements.locationFilter.innerHTML = '<option value="">全部位置</option>';
    options.locations.forEach(l => {
        const option = document.createElement("option");
        option.value = l;
        option.textContent = l;
        elements.locationFilter.appendChild(option);
    });

    elements.purposeFilter.value = currentPurpose;
    elements.locationFilter.value = currentLocation;
}

function getDaysUntilExpiry(expiryDate) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiry = new Date(expiryDate);
    expiry.setHours(0, 0, 0, 0);
    return Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));
}

function renderMedicineCard(medicine) {
    const daysLeft = getDaysUntilExpiry(medicine.expiry_date);
    const isQuantityLow = medicine.quantity < 3;
    const isExpirySoon = daysLeft < 30;

    let expiryText = medicine.expiry_date;
    let expiryClass = "";
    if (daysLeft < 0) {
        expiryText = `${medicine.expiry_date} (已过期)`;
        expiryClass = "expiry-soon";
    } else if (daysLeft < 30) {
        expiryText = `${medicine.expiry_date} (还剩${daysLeft}天)`;
        expiryClass = "expiry-soon";
    }

    const quantityClass = isQuantityLow ? "quantity-low" : "";

    let alertTags = "";
    if (medicine.needs_check) {
        const reasons = medicine.check_reason.split("、");
        alertTags = reasons.map(r => `<span class="alert-tag">⚠️ ${r}</span>`).join("");
    }

    const cardClass = medicine.needs_check ? "medicine-card needs-check" : "medicine-card";

    return `
        <div class="${cardClass}" data-id="${medicine.id}">
            <div class="card-header">
                <div class="card-title">${medicine.name}</div>
                <div>${alertTags}</div>
            </div>
            <div class="card-info">
                <div class="info-row">
                    <span class="info-label">用途：</span>
                    <span class="info-value">${medicine.purpose}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">数量：</span>
                    <span class="info-value ${quantityClass}">${medicine.quantity} ${medicine.unit}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">有效期：</span>
                    <span class="info-value ${expiryClass}">${expiryText}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">位置：</span>
                    <span class="info-value">${medicine.location}</span>
                </div>
                ${medicine.remark ? `
                <div class="info-row">
                    <span class="info-label">备注：</span>
                    <span class="info-value">${medicine.remark}</span>
                </div>
                ` : ""}
            </div>
            <div class="card-actions">
                <button class="btn btn-primary btn-small" onclick="handleUse('${medicine.id}', '${medicine.name.replace(/'/g, "\\'")}', ${medicine.quantity})">
                    使用
                </button>
                <button class="btn btn-secondary btn-small" onclick="handleReplenish('${medicine.id}', '${medicine.name.replace(/'/g, "\\'")}')">
                    补充
                </button>
                <button class="btn btn-edit btn-small" onclick="handleEdit('${medicine.id}')">
                    编辑
                </button>
                <button class="btn btn-danger btn-small" onclick="handleDelete('${medicine.id}', '${medicine.name.replace(/'/g, "\\'")}')">
                    删除
                </button>
            </div>
        </div>
    `;
}

function renderMedicines() {
    let displayData = allMedicines;

    if (currentOnlyCheck) {
        displayData = allMedicines.filter(m => m.needs_check);
    }

    elements.needsCheckCount.textContent = allStats.needs_check || 0;
    elements.lowStockCount.textContent = allStats.low_stock_count || 0;
    elements.expiringCount.textContent = allStats.expiring_count || 0;
    elements.totalCount.textContent = allStats.total || 0;

    if (displayData.length === 0) {
        elements.medicineList.innerHTML = "";
        elements.emptyState.style.display = "block";
        if (currentOnlyCheck && allMedicines.length > 0) {
            elements.emptyState.querySelector("p").textContent = "当前没有需要检查的药品 🎉";
        } else {
            elements.emptyState.querySelector("p").textContent = "还没有添加药品，快来添加吧！";
        }
        return;
    }

    elements.emptyState.style.display = "none";
    elements.medicineList.innerHTML = displayData.map(renderMedicineCard).join("");
}

function renderLogs(logs) {
    if (!logs || logs.length === 0) {
        elements.logsList.innerHTML = '<div class="log-empty">暂无操作记录</div>';
        return;
    }

    const html = logs.map(log => {
        const isUse = log.operation_type === "使用";
        const icon = isUse ? "📤" : "📥";
        const titleClass = isUse ? "use" : "replenish";
        const date = new Date(log.created_at);
        const dateStr = date.toLocaleString("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
        const qtyText = `${log.quantity}${log.unit || ""}`;
        return `
            <div class="log-item">
                <div class="log-icon">${icon}</div>
                <div class="log-content">
                    <div class="log-title ${titleClass}">
                        ${log.operation_type}「${log.medicine_name}」 ${isUse ? "-" : "+"}${qtyText}
                    </div>
                    <div class="log-meta">${dateStr}</div>
                </div>
            </div>
        `;
    }).join("");

    elements.logsList.innerHTML = html;
}

async function loadMedicines() {
    elements.medicineList.innerHTML = '<div class="loading">加载中...</div>';
    const result = await fetchMedicines();
    if (result) {
        allMedicines = result.data || [];
        allStats = result.stats || {};
        renderMedicines();
    }

    const options = await fetchFilterOptions();
    updateFilterOptions(options);

    const logs = await fetchLogs();
    renderLogs(logs);
}

async function handleAddMedicine(e) {
    e.preventDefault();

    const formData = {
        name: document.getElementById("name").value.trim(),
        purpose: document.getElementById("purpose").value.trim(),
        quantity: parseInt(document.getElementById("quantity").value) || 0,
        unit: document.getElementById("unit").value.trim(),
        expiry_date: document.getElementById("expiry_date").value,
        location: document.getElementById("location").value.trim(),
        remark: document.getElementById("remark").value.trim(),
    };

    try {
        const response = await fetch(API_BASE, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(formData),
        });

        const result = await response.json();

        if (response.ok) {
            showToast("药品添加成功！");
            elements.medicineForm.reset();
            loadMedicines();
        } else {
            showToast(result.message || "添加失败", "error");
        }
    } catch (error) {
        console.error("添加药品失败:", error);
        showToast("添加药品失败", "error");
    }
}

async function handleUse(id, name, currentQuantity) {
    showModal(
        "记录使用",
        `请输入「${name}」的使用数量：`,
        {
            showInput: true,
            inputValue: 1,
            callback: async (amount) => {
                if (amount < 1) {
                    showToast("使用数量必须大于0", "error");
                    return;
                }
                if (amount > currentQuantity) {
                    showToast(`库存不足，当前仅有 ${currentQuantity}`, "error");
                    return;
                }
                try {
                    const response = await fetch(`${API_BASE}/${id}/use`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ amount }),
                    });
                    const result = await response.json();
                    if (response.ok) {
                        showToast(`已记录使用 ${amount} ${result.data.unit}`);
                        loadMedicines();
                    } else {
                        showToast(result.message || "操作失败", "error");
                    }
                } catch (error) {
                    console.error("记录使用失败:", error);
                    showToast("操作失败", "error");
                }
            }
        }
    );
}

async function handleReplenish(id, name) {
    showModal(
        "补充库存",
        `请输入「${name}」的补充数量：`,
        {
            showInput: true,
            inputValue: 10,
            callback: async (amount) => {
                if (amount < 1) {
                    showToast("补充数量必须大于0", "error");
                    return;
                }
                try {
                    const response = await fetch(`${API_BASE}/${id}/replenish`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ amount }),
                    });
                    const result = await response.json();
                    if (response.ok) {
                        showToast(`已补充 ${amount} ${result.data.unit}`);
                        loadMedicines();
                    } else {
                        showToast(result.message || "操作失败", "error");
                    }
                } catch (error) {
                    console.error("补充库存失败:", error);
                    showToast("操作失败", "error");
                }
            }
        }
    );
}

function handleEdit(id) {
    const medicine = allMedicines.find(m => m.id === id);
    if (!medicine) {
        showToast("药品不存在", "error");
        return;
    }

    editingMedicineId = id;

    const formHtml = `
        <div class="edit-form">
            <div class="form-row">
                <div class="form-group">
                    <label>药名 *</label>
                    <input type="text" id="edit_name" value="${medicine.name}" required>
                </div>
                <div class="form-group">
                    <label>用途 *</label>
                    <input type="text" id="edit_purpose" value="${medicine.purpose}" required>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>数量 *</label>
                    <input type="number" id="edit_quantity" min="0" value="${medicine.quantity}" required>
                </div>
                <div class="form-group">
                    <label>单位 *</label>
                    <input type="text" id="edit_unit" value="${medicine.unit}" required>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>有效期 *</label>
                    <input type="date" id="edit_expiry_date" value="${medicine.expiry_date}" required>
                </div>
                <div class="form-group">
                    <label>存放位置 *</label>
                    <input type="text" id="edit_location" value="${medicine.location}" required>
                </div>
            </div>
            <div class="form-group">
                <label>备注</label>
                <textarea id="edit_remark" rows="2">${medicine.remark || ""}</textarea>
            </div>
        </div>
    `;

    showModal(
        "编辑药品信息",
        "",
        {
            showForm: true,
            formHtml: formHtml,
            modalClass: "edit-modal overlay-edit-modal",
            confirmText: "保存",
            callback: async () => {
                const formData = {
                    name: document.getElementById("edit_name").value.trim(),
                    purpose: document.getElementById("edit_purpose").value.trim(),
                    quantity: parseInt(document.getElementById("edit_quantity").value) || 0,
                    unit: document.getElementById("edit_unit").value.trim(),
                    expiry_date: document.getElementById("edit_expiry_date").value,
                    location: document.getElementById("edit_location").value.trim(),
                    remark: document.getElementById("edit_remark").value.trim(),
                };

                if (!formData.name || !formData.purpose || !formData.unit || !formData.expiry_date || !formData.location) {
                    showToast("请填写所有必填项", "error");
                    return;
                }
                if (formData.quantity < 0) {
                    showToast("数量不能为负数", "error");
                    return;
                }

                try {
                    const response = await fetch(`${API_BASE}/${editingMedicineId}`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(formData),
                    });
                    const result = await response.json();
                    if (response.ok) {
                        showToast("药品信息已更新");
                        loadMedicines();
                    } else {
                        showToast(result.message || "更新失败", "error");
                    }
                } catch (error) {
                    console.error("更新药品失败:", error);
                    showToast("更新失败", "error");
                }
            }
        }
    );
}

async function handleDelete(id, name) {
    showModal(
        "确认删除",
        `确定要删除「${name}」吗？此操作不可恢复。`,
        {
            callback: async () => {
                try {
                    const response = await fetch(`${API_BASE}/${id}`, {
                        method: "DELETE",
                    });
                    if (response.ok) {
                        showToast("药品已删除");
                        loadMedicines();
                    } else {
                        const result = await response.json();
                        showToast(result.message || "删除失败", "error");
                    }
                } catch (error) {
                    console.error("删除药品失败:", error);
                    showToast("删除失败", "error");
                }
            }
        }
    );
}

function handleExport() {
    window.location.href = `${API_BASE}/export`;
    showToast("数据导出中...");
}

function handleImport(e) {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.name.endsWith(".json")) {
        showToast("请选择 JSON 文件", "error");
        e.target.value = "";
        return;
    }

    showModal(
        "确认导入",
        `将导入文件「${file.name}」中的药品数据，是否继续？`,
        {
            callback: async () => {
                try {
                    const formData = new FormData();
                    formData.append("file", file);

                    const response = await fetch(`${API_BASE}/import`, {
                        method: "POST",
                        body: formData,
                    });
                    const result = await response.json();
                    if (response.ok) {
                        showToast(result.message || "导入成功");
                        loadMedicines();
                    } else {
                        showToast(result.message || "导入失败", "error");
                    }
                } catch (error) {
                    console.error("导入失败:", error);
                    showToast("导入失败", "error");
                }
                e.target.value = "";
            }
        }
    );
}

elements.medicineForm.addEventListener("submit", handleAddMedicine);

elements.purposeFilter.addEventListener("change", (e) => {
    currentPurposeFilter = e.target.value;
    loadMedicines();
});

elements.locationFilter.addEventListener("change", (e) => {
    currentLocationFilter = e.target.value;
    loadMedicines();
});

elements.resetFilterBtn.addEventListener("click", () => {
    currentPurposeFilter = "";
    currentLocationFilter = "";
    currentKeyword = "";
    currentOnlyCheck = false;
    elements.purposeFilter.value = "";
    elements.locationFilter.value = "";
    elements.searchInput.value = "";
    elements.onlyCheckFilter.checked = false;
    loadMedicines();
});

elements.searchInput.addEventListener("input", (e) => {
    if (searchTimeout) clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        currentKeyword = e.target.value.trim();
        loadMedicines();
    }, 300);
});

elements.onlyCheckFilter.addEventListener("change", (e) => {
    currentOnlyCheck = e.target.checked;
    renderMedicines();
});

elements.exportBtn.addEventListener("click", handleExport);
elements.importInput.addEventListener("change", handleImport);

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        hideModal();
    }
    if (e.key === "Enter" && elements.modalOverlay.style.display === "flex") {
        if (modalType === "input") {
            if (document.activeElement === elements.modalInput) {
                elements.modalConfirm.click();
            }
        } else if (modalType !== "form") {
            elements.modalConfirm.click();
        }
    }
});

loadMedicines();
