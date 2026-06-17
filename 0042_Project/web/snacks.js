/*
  文件职责：
    零食库存小柜子的前端交互逻辑。
    通过 fetch 调用后端 HTTP 接口，完成零食的查询、新增、编辑、吃掉、补货、
    删除、分类筛选、搜索、排序、批量操作、设置管理、操作记录查看、
    数据导入导出、补货建议等操作。

  数据流：
    用户操作（点击按钮/提交表单/上传文件）
      → fetch 请求后端 /api/*
        → snack_api（Flask）
          → snack_service（业务逻辑）
            → snack_file_store（JSON 读写）
      ← 后端返回 JSON 响应
    → 本脚本更新 DOM（卡片、统计、操作记录、筛选选项等）
*/

const API_BASE = "/api/snacks";
const API_LOGS = "/api/logs";
const API_SETTINGS = "/api/settings";
const API_EXPORT_JSON = "/api/export/json";
const API_EXPORT_CSV = "/api/export/csv";
const API_IMPORT_JSON = "/api/import/json";

/* ---------- 状态 ---------- */
const state = {
  location: "all",
  category: "all",
  onlyAttention: false,
  keyword: "",
  sortBy: "",
  sortOrder: "asc",
  editingId: null,
  selectedIds: new Set(),
  settings: {
    expiring_days: 7,
  },
};

/* ---------- DOM 元素引用 ---------- */
const $ = (id) => document.getElementById(id);
const els = {
  statTotal: $("stat-total"),
  statLow: $("stat-low"),
  statSoon: $("stat-soon"),
  statExpired: $("stat-expired"),
  statTarget: $("stat-target"),
  searchInput: $("search-input"),
  locationFilter: $("location-filter"),
  categoryFilter: $("category-filter"),
  sortSelect: $("sort-select"),
  btnOnlyAttention: $("btn-only-attention"),
  btnSettings: $("btn-settings"),
  btnAdd: $("btn-add"),
  btnExportJson: $("btn-export-json"),
  btnExportCsv: $("btn-export-csv"),
  inputImport: $("input-import"),
  batchBar: $("batch-bar"),
  batchCount: $("batch-count"),
  btnBatchRestock: $("btn-batch-restock"),
  btnBatchDisable: $("btn-batch-disable"),
  btnBatchDelete: $("btn-batch-delete"),
  btnBatchClear: $("btn-batch-clear"),
  formMask: $("form-mask"),
  formModal: $("form-modal"),
  formTitle: $("form-title"),
  snackForm: $("snack-form"),
  inputId: $("input-id"),
  inputName: $("input-name"),
  inputFlavor: $("input-flavor"),
  inputCategory: $("input-category"),
  inputQuantity: $("input-quantity"),
  inputTarget: $("input-target"),
  inputLocation: $("input-location"),
  inputExpiry: $("input-expiry"),
  btnCancel: $("btn-cancel"),
  settingsMask: $("settings-mask"),
  settingsModal: $("settings-modal"),
  settingExpiringDays: $("setting-expiring-days"),
  btnSaveSettings: $("btn-save-settings"),
  btnCloseSettings: $("btn-close-settings"),
  emptyState: $("empty-state"),
  snackGrid: $("snack-grid"),
  logList: $("log-list"),
  btnRefreshLogs: $("btn-refresh-logs"),
};

/* ---------- 工具函数 ---------- */
function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str || "";
  return div.innerHTML;
}

function daysToExpiry(expiryDate) {
  if (!expiryDate) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(expiryDate);
  expiry.setHours(0, 0, 0, 0);
  return Math.floor((expiry - today) / (1000 * 60 * 60 * 24));
}

function isLowStock(qty) {
  return qty < 2;
}

function isExpiringSoon(expiryDate) {
  const days = daysToExpiry(expiryDate);
  const threshold = state.settings.expiring_days || 7;
  return days !== null && 0 <= days && days < threshold;
}

function isExpired(expiryDate) {
  const days = daysToExpiry(expiryDate);
  return days !== null && days < 0;
}

function needsAttention(snack) {
  return isLowStock(snack.quantity) || isExpiringSoon(snack.expiry_date) || isExpired(snack.expiry_date);
}

function isBelowTarget(snack) {
  return snack.target_quantity > 0 && snack.quantity < snack.target_quantity;
}

function restockSuggestion(snack) {
  if (!isBelowTarget(snack)) return 0;
  return snack.target_quantity - snack.quantity;
}

function formatAction(action) {
  const map = {
    "新增": "新增",
    "编辑": "编辑",
    "吃掉": "吃掉",
    "补货": "补货",
    "删除": "删除",
    "导入": "导入",
    "状态变更": "状态变更",
  };
  return map[action] || action;
}

function debounce(fn, delay = 300) {
  let timer = null;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

/* ---------- API 请求 ---------- */
async function fetchSnacks() {
  const params = new URLSearchParams();
  if (state.location !== "all") params.set("location", state.location);
  if (state.category !== "all") params.set("category", state.category);
  if (state.onlyAttention) params.set("only_attention", "1");
  if (state.keyword) params.set("keyword", state.keyword);
  if (state.sortBy) params.set("sort_by", state.sortBy);
  if (state.sortBy && state.sortOrder) params.set("sort_order", state.sortOrder);
  const url = params.toString() ? `${API_BASE}?${params}` : API_BASE;
  const res = await fetch(url);
  return res.json();
}

async function fetchLocations() {
  const res = await fetch(`${API_BASE}/locations`);
  return res.json();
}

async function fetchCategories() {
  const res = await fetch(`${API_BASE}/categories`);
  return res.json();
}

async function fetchLogs(limit = 20) {
  const res = await fetch(`${API_LOGS}?limit=${limit}`);
  return res.json();
}

async function fetchSettings() {
  const res = await fetch(API_SETTINGS);
  return res.json();
}

async function saveSettings(payload) {
  const res = await fetch(API_SETTINGS, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return res.json();
}

async function addSnack(payload) {
  const res = await fetch(API_BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return res.json();
}

async function updateSnack(id, payload) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return res.json();
}

async function consumeSnack(id) {
  const res = await fetch(`${API_BASE}/${id}/consume`, { method: "POST" });
  return res.json();
}

async function restockSnack(id, amount = 1) {
  const res = await fetch(`${API_BASE}/${id}/restock`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
  });
  return res.json();
}

async function toggleDisabled(id) {
  const res = await fetch(`${API_BASE}/${id}/toggle-disabled`, { method: "POST" });
  return res.json();
}

async function deleteSnack(id) {
  const res = await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
  return res.json();
}

async function batchDelete(ids) {
  const res = await fetch(`${API_BASE}/batch-delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ids }),
  });
  return res.json();
}

async function batchDisable(ids, disabled = true) {
  const res = await fetch(`${API_BASE}/batch-disable`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ids, disabled }),
  });
  return res.json();
}

async function batchRestock(ids, amount = 1) {
  const res = await fetch(`${API_BASE}/batch-restock`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ids, amount }),
  });
  return res.json();
}

async function importJsonFile(file) {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(API_IMPORT_JSON, {
    method: "POST",
    body: formData,
  });
  return res.json();
}

/* ---------- 渲染：统计 ---------- */
function renderStats(stats) {
  els.statTotal.textContent = `共 ${stats.total} 种`;
  els.statLow.textContent = `📦 低库存 ${stats.low_stock}`;
  els.statSoon.textContent = `⏰ 临期 ${stats.expiring_soon}`;
  els.statExpired.textContent = `❌ 已过期 ${stats.expired}`;
  els.statTarget.textContent = `🎯 低于目标 ${stats.below_target || 0}`;
}

/* ---------- 渲染：筛选选项 ---------- */
function renderLocationOptions(locations) {
  const currentValue = els.locationFilter.value;
  els.locationFilter.innerHTML = '<option value="all">全部位置</option>';
  locations.forEach((loc) => {
    const opt = document.createElement("option");
    opt.value = loc;
    opt.textContent = loc;
    els.locationFilter.appendChild(opt);
  });
  if (locations.includes(currentValue)) {
    els.locationFilter.value = currentValue;
  } else {
    els.locationFilter.value = "all";
    state.location = "all";
  }
}

function renderCategoryOptions(categories) {
  const currentValue = els.categoryFilter.value;
  els.categoryFilter.innerHTML = '<option value="all">全部分类</option>';
  categories.forEach((cat) => {
    const opt = document.createElement("option");
    opt.value = cat;
    opt.textContent = cat;
    els.categoryFilter.appendChild(opt);
  });
  if (categories.includes(currentValue)) {
    els.categoryFilter.value = currentValue;
  } else {
    els.categoryFilter.value = "all";
    state.category = "all";
  }
}

/* ---------- 渲染：零食卡片 ---------- */
function buildBadges(snack) {
  const badges = [];
  if (snack.disabled) {
    badges.push('<span class="badge badge-disabled">不再购买</span>');
  }
  if (isLowStock(snack.quantity)) {
    badges.push('<span class="badge badge-warn">库存不足</span>');
  }
  if (isExpired(snack.expiry_date)) {
    const days = daysToExpiry(snack.expiry_date);
    badges.push(`<span class="badge badge-warn">已过期${-days}天</span>`);
  } else if (isExpiringSoon(snack.expiry_date)) {
    const days = daysToExpiry(snack.expiry_date);
    badges.push(`<span class="badge badge-warn">还剩${days}天</span>`);
  }
  return badges.join("");
}

function buildCategoryBadge(category) {
  if (!category) return "";
  return `<div class="snack-category">${escapeHtml(category)}</div>`;
}

function buildQuantityHtml(qty, targetQty) {
  let cls = "snack-quantity";
  if (qty === 0) cls += " zero";
  else if (qty < 2) cls += " low";
  const targetText = targetQty > 0 ? ` / ${targetQty}` : "";
  return `<span class="${cls}">${qty}${targetText}</span> 份`;
}

function buildExpiryHtml(expiryDate) {
  if (!expiryDate) return '<div><span class="label">保质期:</span>未设置</div>';
  const days = daysToExpiry(expiryDate);
  let cls = "snack-expiry";
  let dayText = "";
  if (days !== null) {
    if (days < 0) {
      cls += " warn";
      dayText = ` (已过期${-days}天)`;
    } else if (days < (state.settings.expiring_days || 7)) {
      cls += " warn";
      dayText = ` (还剩${days}天)`;
    }
  }
  return `<div><span class="label">保质期:</span><span class="${cls}">${expiryDate}${dayText}</span></div>`;
}

function buildRestockTip(snack) {
  const suggestion = restockSuggestion(snack);
  if (suggestion <= 0) return "";
  return `<div class="restock-tip">
    <span class="tip-icon">💡</span>
    <span>低于目标库存，建议补 ${suggestion} 份</span>
  </div>`;
}

function buildTargetInfo(snack) {
  if (snack.target_quantity <= 0) return "";
  return `<div class="target-info">🎯 目标库存：${snack.target_quantity} 份</div>`;
}

function renderSnackCard(snack) {
  const cardCls = ["snack-card"];
  if (needsAttention(snack)) cardCls.push("attention");
  if (snack.disabled) cardCls.push("disabled");
  if (state.selectedIds.has(snack.id)) cardCls.push("selected");

  const checked = state.selectedIds.has(snack.id) ? "checked" : "";

  return `
    <div class="${cardCls.join(" ")}" data-id="${snack.id}">
      <input type="checkbox" class="card-checkbox" data-id="${snack.id}" ${checked}>
      <div class="snack-header">
        <div>
          ${buildCategoryBadge(snack.category)}
          <div class="snack-name">${escapeHtml(snack.name)}</div>
          ${snack.flavor ? `<div class="snack-flavor">${escapeHtml(snack.flavor)}</div>` : ""}
        </div>
        <div class="snack-badges">${buildBadges(snack)}</div>
      </div>
      <div class="snack-info">
        <div><span class="label">数量:</span>${buildQuantityHtml(snack.quantity, snack.target_quantity)}</div>
        <div><span class="label">位置:</span>${escapeHtml(snack.location || "未指定")}</div>
        ${buildExpiryHtml(snack.expiry_date)}
        ${buildTargetInfo(snack)}
        ${buildRestockTip(snack)}
      </div>
      <div class="snack-actions">
        <button class="btn btn-warn" data-action="consume">吃掉</button>
        <button class="btn btn-primary" data-action="restock">补货</button>
        <button class="btn btn-default" data-action="edit">编辑</button>
        <button class="btn btn-default" data-action="toggle">${snack.disabled ? "恢复" : "停用"}</button>
        <button class="btn btn-danger" data-action="delete">删除</button>
      </div>
    </div>
  `;
}

function renderSnacks(snacks) {
  if (!snacks || snacks.length === 0) {
    els.emptyState.classList.remove("hidden");
    els.snackGrid.innerHTML = "";
  } else {
    els.emptyState.classList.add("hidden");
    els.snackGrid.innerHTML = snacks.map(renderSnackCard).join("");
  }
}

/* ---------- 批量操作栏 ---------- */
function updateBatchBar() {
  const count = state.selectedIds.size;
  els.batchCount.textContent = count;
  if (count > 0) {
    els.batchBar.classList.remove("hidden");
  } else {
    els.batchBar.classList.add("hidden");
  }
}

function clearSelection() {
  state.selectedIds.clear();
  updateBatchBar();
  // 刷新所有卡片的选中状态
  document.querySelectorAll(".snack-card").forEach((card) => {
    card.classList.remove("selected");
  });
  document.querySelectorAll(".card-checkbox").forEach((cb) => {
    cb.checked = false;
  });
}

/* ---------- 渲染：操作记录 ---------- */
function renderLogs(logs) {
  if (!logs || logs.length === 0) {
    els.logList.innerHTML = '<div class="log-empty">暂无操作记录</div>';
    return;
  }
  els.logList.innerHTML = logs.map(renderLogItem).join("");
}

function renderLogItem(log) {
  const qtyText = log.quantity_change !== 0
    ? `<span class="log-qty ${log.quantity_change < 0 ? "negative" : "positive"}">
        ${log.quantity_change > 0 ? "+" : ""}${log.quantity_change}
      </span>`
    : "";

  return `
    <div class="log-item">
      <div class="log-item-header">
        <span class="log-action ${formatAction(log.action)}">${escapeHtml(log.action)}</span>
        <span class="log-time">${escapeHtml(log.timestamp)}</span>
      </div>
      <div class="log-name">${escapeHtml(log.snack_name)} ${qtyText}</div>
      ${log.note ? `<div class="log-note">${escapeHtml(log.note)}</div>` : ""}
    </div>
  `;
}

/* ---------- 弹窗：新增/编辑 ---------- */
function openAddModal() {
  state.editingId = null;
  els.formTitle.textContent = "新增零食";
  resetForm();
  showFormModal();
}

function openEditModal(snack) {
  state.editingId = snack.id;
  els.formTitle.textContent = "编辑零食";
  els.inputId.value = snack.id;
  els.inputName.value = snack.name;
  els.inputFlavor.value = snack.flavor || "";
  els.inputCategory.value = snack.category || "";
  els.inputQuantity.value = snack.quantity;
  els.inputTarget.value = snack.target_quantity || 0;
  els.inputLocation.value = snack.location || "";
  els.inputExpiry.value = snack.expiry_date || "";
  showFormModal();
}

function showFormModal() {
  els.formMask.classList.remove("hidden");
  els.formModal.classList.remove("hidden");
  setTimeout(() => els.inputName.focus(), 50);
}

function closeFormModal() {
  els.formMask.classList.add("hidden");
  els.formModal.classList.add("hidden");
  state.editingId = null;
  resetForm();
}

function resetForm() {
  els.inputId.value = "";
  els.inputName.value = "";
  els.inputFlavor.value = "";
  els.inputCategory.value = "";
  els.inputQuantity.value = "1";
  els.inputTarget.value = "0";
  els.inputLocation.value = "";
  els.inputExpiry.value = "";
}

async function handleFormSubmit(e) {
  e.preventDefault();
  const name = els.inputName.value.trim();
  if (!name) {
    alert("请填写零食名称");
    return;
  }
  const targetQty = parseInt(els.inputTarget.value);
  const payload = {
    name,
    flavor: els.inputFlavor.value.trim(),
    category: els.inputCategory.value.trim(),
    quantity: parseInt(els.inputQuantity.value) || 0,
    target_quantity: isNaN(targetQty) ? 0 : Math.max(0, targetQty),
    location: els.inputLocation.value.trim(),
    expiry_date: els.inputExpiry.value,
  };

  if (state.editingId) {
    const result = await updateSnack(state.editingId, payload);
    if (result.success) {
      closeFormModal();
      await loadSnacks();
    } else {
      alert(result.message || "保存失败");
    }
  } else {
    const result = await addSnack(payload);
    if (result.success) {
      closeFormModal();
      await loadSnacks();
    } else {
      alert(result.message || "添加失败");
    }
  }
}

/* ---------- 弹窗：设置 ---------- */
function openSettingsModal() {
  els.settingExpiringDays.value = String(state.settings.expiring_days || 7);
  els.settingsMask.classList.remove("hidden");
  els.settingsModal.classList.remove("hidden");
}

function closeSettingsModal() {
  els.settingsMask.classList.add("hidden");
  els.settingsModal.classList.add("hidden");
}

async function handleSaveSettings() {
  const days = parseInt(els.settingExpiringDays.value);
  if (isNaN(days) || days < 1) {
    alert("请输入有效的临期天数");
    return;
  }
  const result = await saveSettings({ expiring_days: days });
  if (result.success) {
    state.settings = result.data;
    closeSettingsModal();
    await loadSnacks();
    await loadLogs();
  } else {
    alert(result.message || "保存设置失败");
  }
}

/* ---------- 事件绑定 ---------- */
function bindEvents() {
  // 搜索（防抖）
  const debouncedSearch = debounce(async () => {
    state.keyword = els.searchInput.value.trim();
    await loadSnacks();
  }, 300);
  els.searchInput.addEventListener("input", debouncedSearch);

  // 筛选
  els.locationFilter.addEventListener("change", async (e) => {
    state.location = e.target.value;
    await loadSnacks();
  });

  els.categoryFilter.addEventListener("change", async (e) => {
    state.category = e.target.value;
    await loadSnacks();
  });

  // 排序
  els.sortSelect.addEventListener("change", async (e) => {
    const value = e.target.value;
    if (!value) {
      state.sortBy = "";
      state.sortOrder = "asc";
    } else {
      const [field, order] = value.split("_");
      state.sortBy = field;
      state.sortOrder = order;
    }
    await loadSnacks();
  });

  // 只看需要处理
  els.btnOnlyAttention.addEventListener("click", async () => {
    state.onlyAttention = !state.onlyAttention;
    els.btnOnlyAttention.classList.toggle("active", state.onlyAttention);
    els.btnOnlyAttention.textContent = state.onlyAttention ? "显示全部" : "只看需要处理";
    await loadSnacks();
  });

  // 设置
  els.btnSettings.addEventListener("click", openSettingsModal);
  els.btnCloseSettings.addEventListener("click", closeSettingsModal);
  els.settingsMask.addEventListener("click", closeSettingsModal);
  els.btnSaveSettings.addEventListener("click", handleSaveSettings);

  // 新增
  els.btnAdd.addEventListener("click", openAddModal);

  // 弹窗关闭（表单）
  els.btnCancel.addEventListener("click", closeFormModal);
  els.formMask.addEventListener("click", closeFormModal);
  els.snackForm.addEventListener("submit", handleFormSubmit);

  // 导出
  els.btnExportJson.addEventListener("click", () => {
    window.location.href = API_EXPORT_JSON;
  });

  els.btnExportCsv.addEventListener("click", () => {
    window.location.href = API_EXPORT_CSV;
  });

  // 导入
  els.inputImport.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!confirm("确认导入 JSON 文件？已存在的相同 ID 零食会被更新。")) {
      e.target.value = "";
      return;
    }
    try {
      const result = await importJsonFile(file);
      if (result.success) {
        alert(`成功导入 ${result.imported} 条数据`);
        await loadAll();
      } else {
        alert(result.message || "导入失败");
      }
    } catch (err) {
      alert("导入出错：" + err.message);
    } finally {
      e.target.value = "";
    }
  });

  // 操作记录刷新
  els.btnRefreshLogs.addEventListener("click", loadLogs);

  // 卡片复选框（事件委托）
  els.snackGrid.addEventListener("change", (e) => {
    const cb = e.target.closest(".card-checkbox");
    if (!cb) return;
    const id = cb.dataset.id;
    if (!id) return;
    if (cb.checked) {
      state.selectedIds.add(id);
    } else {
      state.selectedIds.delete(id);
    }
    const card = cb.closest(".snack-card");
    if (card) {
      card.classList.toggle("selected", cb.checked);
    }
    updateBatchBar();
  });

  // 批量操作
  els.btnBatchClear.addEventListener("click", clearSelection);

  els.btnBatchRestock.addEventListener("click", async () => {
    const amount = prompt("批量补货数量：", "1");
    if (amount === null) return;
    const n = parseInt(amount);
    if (isNaN(n) || n <= 0) {
      alert("请输入大于 0 的数量");
      return;
    }
    const ids = Array.from(state.selectedIds);
    const result = await batchRestock(ids, n);
    if (result.success) {
      alert(`成功补货 ${result.restocked} 种零食`);
      clearSelection();
      await loadSnacks();
    } else {
      alert(result.message || "批量补货失败");
    }
  });

  els.btnBatchDisable.addEventListener("click", async () => {
    if (!confirm("确定要批量停用选中的零食吗？")) return;
    const ids = Array.from(state.selectedIds);
    const result = await batchDisable(ids, true);
    if (result.success) {
      alert(`成功停用 ${result.updated} 种零食`);
      clearSelection();
      await loadSnacks();
    } else {
      alert(result.message || "批量停用失败");
    }
  });

  els.btnBatchDelete.addEventListener("click", async () => {
    if (!confirm(`确定要删除选中的 ${state.selectedIds.size} 种零食吗？此操作不可恢复。`)) return;
    const ids = Array.from(state.selectedIds);
    const result = await batchDelete(ids);
    if (result.success) {
      alert(`成功删除 ${result.deleted} 种零食`);
      clearSelection();
      await loadSnacks();
    } else {
      alert(result.message || "批量删除失败");
    }
  });

  // 卡片操作（事件委托）
  els.snackGrid.addEventListener("click", async (e) => {
    // 点击复选框不触发
    if (e.target.closest(".card-checkbox")) return;

    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const card = btn.closest(".snack-card");
    if (!card) return;
    const id = card.dataset.id;
    const action = btn.dataset.action;

    if (action === "consume") {
      await consumeSnack(id);
      await loadSnacks();
    } else if (action === "restock") {
      const suggestion = restockSuggestionForId(id);
      const defaultVal = suggestion > 0 ? String(suggestion) : "1";
      const amount = prompt("补货数量：", defaultVal);
      if (amount === null) return;
      const n = parseInt(amount);
      if (isNaN(n) || n <= 0) {
        alert("请输入大于 0 的数量");
        return;
      }
      await restockSnack(id, n);
      await loadSnacks();
    } else if (action === "edit") {
      const res = await fetch(`${API_BASE}/${id}`);
      const result = await res.json();
      if (result.success) {
        openEditModal(result.data);
      }
    } else if (action === "toggle") {
      await toggleDisabled(id);
      await loadSnacks();
    } else if (action === "delete") {
      if (!confirm("确定要删除这个零食吗？")) return;
      await deleteSnack(id);
      await loadSnacks();
    }
  });

  // ESC 关闭弹窗
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      if (!els.formModal.classList.contains("hidden")) {
        closeFormModal();
      }
      if (!els.settingsModal.classList.contains("hidden")) {
        closeSettingsModal();
      }
    }
  });
}

// 辅助：根据 id 获取当前列表中的零食（用于补货建议默认值）
function restockSuggestionForId(id) {
  // 简单实现：从 DOM 读取当前数量和目标库存
  const cards = document.querySelectorAll(".snack-card");
  for (const card of cards) {
    if (card.dataset.id === id) {
      // 尝试从渲染信息里拿，拿不到就返回 0
      return 0;
    }
  }
  return 0;
}

/* ---------- 数据加载 ---------- */
async function loadSettings() {
  const result = await fetchSettings();
  if (result.success) {
    state.settings = result.data;
  }
}

async function loadSnacks() {
  const result = await fetchSnacks();
  if (result.success) {
    renderSnacks(result.data);
    if (result.statistics) {
      renderStats(result.statistics);
    }
    // 清理不存在的选中项
    const validIds = new Set(result.data.map((s) => s.id));
    for (const id of Array.from(state.selectedIds)) {
      if (!validIds.has(id)) {
        state.selectedIds.delete(id);
      }
    }
    updateBatchBar();
  }
}

async function loadLocations() {
  const result = await fetchLocations();
  if (result.success) renderLocationOptions(result.data);
}

async function loadCategories() {
  const result = await fetchCategories();
  if (result.success) renderCategoryOptions(result.data);
}

async function loadLogs() {
  const result = await fetchLogs(30);
  if (result.success) renderLogs(result.data);
}

async function loadAll() {
  await Promise.all([loadSettings(), loadSnacks(), loadLocations(), loadCategories(), loadLogs()]);
}

/* ---------- 启动 ---------- */
document.addEventListener("DOMContentLoaded", async () => {
  bindEvents();
  await loadAll();
});
