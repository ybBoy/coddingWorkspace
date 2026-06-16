/*
  文件职责：
    零食库存小柜子的前端交互逻辑。
    通过 fetch 调用后端 HTTP 接口，完成零食的查询、新增、吃掉、补货、删除等操作，
    并将数据渲染为货架卡片，更新顶部统计信息。

  数据流：
    用户操作（点击按钮/提交表单）
      → fetch 请求后端 /api/snacks/*
        → snack_api（Flask）
          → snack_service（业务逻辑）
            → snack_file_store（JSON 读写）
      ← 后端返回 JSON 响应
    → 本脚本调用 renderSnacks() 更新 DOM
*/

const API_BASE = "/api/snacks";
let currentLocation = "all";

/* ---------- DOM 元素引用 ---------- */
const els = {
  statTotal: document.getElementById("stat-total"),
  statAttention: document.getElementById("stat-attention"),
  locationFilter: document.getElementById("location-filter"),
  btnAdd: document.getElementById("btn-add"),
  addForm: document.getElementById("add-form"),
  btnCancelAdd: document.getElementById("btn-cancel-add"),
  inputName: document.getElementById("input-name"),
  inputFlavor: document.getElementById("input-flavor"),
  inputQuantity: document.getElementById("input-quantity"),
  inputLocation: document.getElementById("input-location"),
  inputExpiry: document.getElementById("input-expiry"),
  emptyState: document.getElementById("empty-state"),
  snackGrid: document.getElementById("snack-grid"),
};

/* ---------- 工具函数 ---------- */
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
  return days !== null && days < 7;
}

function needsAttention(snack) {
  return isLowStock(snack.quantity) || isExpiringSoon(snack.expiry_date) || snack.disabled;
}

/* ---------- API 请求 ---------- */
async function fetchSnacks() {
  const url = currentLocation === "all" ? API_BASE : `${API_BASE}?location=${encodeURIComponent(currentLocation)}`;
  const res = await fetch(url);
  return res.json();
}

async function fetchLocations() {
  const res = await fetch(`${API_BASE}/locations`);
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

/* ---------- 渲染逻辑 ---------- */
function renderStats(total, attentionCount) {
  els.statTotal.textContent = `共 ${total} 种`;
  els.statAttention.textContent = `⚠ 需处理 ${attentionCount}`;
}

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
    currentLocation = "all";
  }
}

function buildBadges(snack) {
  const badges = [];
  if (snack.disabled) {
    badges.push('<span class="badge badge-disabled">不再购买</span>');
  }
  if (isLowStock(snack.quantity)) {
    badges.push('<span class="badge badge-warn">库存不足</span>');
  }
  if (isExpiringSoon(snack.expiry_date)) {
    const days = daysToExpiry(snack.expiry_date);
    const text = days < 0 ? `已过期${-days}天` : `还剩${days}天`;
    badges.push(`<span class="badge badge-warn">保质期${text}</span>`);
  }
  return badges.join("");
}

function buildQuantityHtml(qty) {
  let cls = "snack-quantity";
  if (qty === 0) cls += " zero";
  else if (qty < 2) cls += " low";
  return `<span class="${cls}">${qty}</span> 份`;
}

function buildExpiryHtml(expiryDate) {
  if (!expiryDate) return '<span class="snack-info"><span class="label">保质期:</span>未设置</span>';
  const days = daysToExpiry(expiryDate);
  let cls = "snack-expiry";
  if (days !== null && days < 7) cls += " warn";
  let dayText = "";
  if (days !== null) {
    if (days < 0) dayText = ` (已过期${-days}天)`;
    else dayText = ` (还剩${days}天)`;
  }
  return `<span class="snack-info"><span class="label">保质期:</span><span class="${cls}">${expiryDate}${dayText}</span></span>`;
}

function renderSnackCard(snack) {
  const cardCls = ["snack-card"];
  if (needsAttention(snack)) cardCls.push("attention");
  if (snack.disabled) cardCls.push("disabled");

  return `
    <div class="${cardCls.join(" ")}" data-id="${snack.id}">
      <div class="snack-header">
        <div>
          <div class="snack-name">${escapeHtml(snack.name)}</div>
          ${snack.flavor ? `<div class="snack-flavor">${escapeHtml(snack.flavor)}</div>` : ""}
        </div>
        <div class="snack-badges">${buildBadges(snack)}</div>
      </div>
      <div class="snack-info">
        <div><span class="label">数量:</span>${buildQuantityHtml(snack.quantity)}</div>
        <div><span class="label">位置:</span>${escapeHtml(snack.location || "未指定")}</div>
        <div>${buildExpiryHtml(snack.expiry_date)}</div>
      </div>
      <div class="snack-actions">
        <button class="btn btn-warn" data-action="consume">吃掉一份</button>
        <button class="btn btn-primary" data-action="restock">补货 +1</button>
        <button class="btn btn-default" data-action="toggle">${snack.disabled ? "恢复购买" : "不再购买"}</button>
        <button class="btn btn-danger" data-action="delete">删除</button>
      </div>
    </div>
  `;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str || "";
  return div.innerHTML;
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

/* ---------- 事件绑定 ---------- */
function bindEvents() {
  els.btnAdd.addEventListener("click", () => {
    els.addForm.classList.toggle("hidden");
  });

  els.btnCancelAdd.addEventListener("click", () => {
    els.addForm.classList.add("hidden");
    resetForm();
  });

  els.addForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = els.inputName.value.trim();
    if (!name) {
      alert("请填写零食名称");
      return;
    }
    const payload = {
      name,
      flavor: els.inputFlavor.value.trim(),
      quantity: parseInt(els.inputQuantity.value) || 0,
      location: els.inputLocation.value.trim(),
      expiry_date: els.inputExpiry.value,
    };
    const result = await addSnack(payload);
    if (result.success) {
      resetForm();
      els.addForm.classList.add("hidden");
      await loadAll();
    } else {
      alert(result.message || "添加失败");
    }
  });

  els.locationFilter.addEventListener("change", async (e) => {
    currentLocation = e.target.value;
    await loadSnacks();
  });

  els.snackGrid.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const card = btn.closest(".snack-card");
    if (!card) return;
    const id = card.dataset.id;
    const action = btn.dataset.action;

    if (action === "consume") {
      await consumeSnack(id);
      await loadAll();
    } else if (action === "restock") {
      const amount = prompt("补货数量：", "1");
      if (amount === null) return;
      const n = parseInt(amount);
      if (isNaN(n) || n <= 0) {
        alert("请输入大于 0 的数量");
        return;
      }
      await restockSnack(id, n);
      await loadAll();
    } else if (action === "toggle") {
      await toggleDisabled(id);
      await loadAll();
    } else if (action === "delete") {
      if (!confirm("确定要删除这个零食吗？")) return;
      await deleteSnack(id);
      await loadAll();
    }
  });
}

function resetForm() {
  els.inputName.value = "";
  els.inputFlavor.value = "";
  els.inputQuantity.value = "1";
  els.inputLocation.value = "";
  els.inputExpiry.value = "";
}

/* ---------- 数据加载 ---------- */
async function loadSnacks() {
  const result = await fetchSnacks();
  if (result.success) {
    renderSnacks(result.data);
    renderStats(result.data.length, result.attention_count || 0);
  }
}

async function loadLocations() {
  const result = await fetchLocations();
  if (result.success) {
    renderLocationOptions(result.data);
  }
}

async function loadAll() {
  await Promise.all([loadSnacks(), loadLocations()]);
}

/* ---------- 启动 ---------- */
document.addEventListener("DOMContentLoaded", async () => {
  bindEvents();
  await loadAll();
});
