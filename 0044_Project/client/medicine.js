const API_BASE = "/api/medicines";

let currentPurposeFilter = "";
let currentLocationFilter = "";
let modalCallback = null;

const elements = {
    medicineList: document.getElementById("medicineList"),
    emptyState: document.getElementById("emptyState"),
    needsCheckCount: document.getElementById("needsCheckCount"),
    totalCount: document.getElementById("totalCount"),
    medicineForm: document.getElementById("medicineForm"),
    purposeFilter: document.getElementById("purposeFilter"),
    locationFilter: document.getElementById("locationFilter"),
    resetFilterBtn: document.getElementById("resetFilterBtn"),
    modalOverlay: document.getElementById("modalOverlay"),
    modalTitle: document.getElementById("modalTitle"),
    modalMessage: document.getElementById("modalMessage"),
    modalInputContainer: document.getElementById("modalInputContainer"),
    modalInput: document.getElementById("modalInput"),
    modalConfirm: document.getElementById("modalConfirm"),
    modalCancel: document.getElementById("modalCancel"),
    toast: document.getElementById("toast"),
};

function showToast(message, type = "success") {
    elements.toast.textContent = message;
    elements.toast.className = `toast ${type}`;
    elements.toast.style.display = "block";
    setTimeout(() => {
        elements.toast.style.display = "none";
    }, 3000);
}

function showModal(title, message, showInput = false, inputValue = 1, callback) {
    elements.modalTitle.textContent = title;
    elements.modalMessage.textContent = message;
    elements.modalInputContainer.style.display = showInput ? "block" : "none";
    elements.modalInput.value = inputValue;
    modalCallback = callback;
    elements.modalOverlay.style.display = "flex";
    if (showInput) {
        setTimeout(() => elements.modalInput.focus(), 100);
    }
}

function hideModal() {
    elements.modalOverlay.style.display = "none";
    modalCallback = null;
}

elements.modalCancel.addEventListener("click", hideModal);
elements.modalConfirm.addEventListener("click", () => {
    if (modalCallback) {
        const inputValue = elements.modalInputContainer.style.display !== "none"
            ? parseInt(elements.modalInput.value) || 1
            : null;
        modalCallback(inputValue);
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
                <button class="btn btn-primary btn-small" onclick="handleUse('${medicine.id}', '${medicine.name}', ${medicine.quantity})">
                    使用
                </button>
                <button class="btn btn-secondary btn-small" onclick="handleReplenish('${medicine.id}', '${medicine.name}')">
                    补充
                </button>
                <button class="btn btn-danger btn-small" onclick="handleDelete('${medicine.id}', '${medicine.name}')">
                    删除
                </button>
            </div>
        </div>
    `;
}

function renderMedicines(result) {
    if (!result) return;

    const { data, stats } = result;

    elements.needsCheckCount.textContent = stats.needs_check;
    elements.totalCount.textContent = stats.total;

    if (data.length === 0) {
        elements.medicineList.innerHTML = "";
        elements.emptyState.style.display = "block";
        return;
    }

    elements.emptyState.style.display = "none";
    elements.medicineList.innerHTML = data.map(renderMedicineCard).join("");
}

async function loadMedicines() {
    elements.medicineList.innerHTML = '<div class="loading">加载中...</div>';
    const result = await fetchMedicines();
    renderMedicines(result);

    const options = await fetchFilterOptions();
    updateFilterOptions(options);
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
    const maxAmount = Math.max(1, currentQuantity);
    showModal(
        "记录使用",
        `请输入「${name}」的使用数量：`,
        true,
        1,
        async (amount) => {
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
    );
}

async function handleReplenish(id, name) {
    showModal(
        "补充库存",
        `请输入「${name}」的补充数量：`,
        true,
        10,
        async (amount) => {
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
    );
}

async function handleDelete(id, name) {
    showModal(
        "确认删除",
        `确定要删除「${name}」吗？此操作不可恢复。`,
        false,
        null,
        async () => {
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
    elements.purposeFilter.value = "";
    elements.locationFilter.value = "";
    loadMedicines();
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        hideModal();
    }
    if (e.key === "Enter" && elements.modalOverlay.style.display === "flex") {
        if (document.activeElement !== elements.modalInput) {
            elements.modalConfirm.click();
        }
    }
});

loadMedicines();
