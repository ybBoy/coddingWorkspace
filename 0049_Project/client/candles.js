/**
 * candles.js - 香氛蜡烛管理页面交互逻辑
 * 文件职责：处理前端所有用户交互，通过 fetch 与后端 API 通信。
 * 
 * 整体调用流程：
 *   用户操作（新增/点燃/修改剩余/删除/筛选）
 *     → candles.js 构造 fetch 请求
 *     → Python Flask 接口接收
 *     → candle_service 更新内存数据
 *     → candle_json_store 保存到 JSON 文件
 *     → 返回响应
 *     → 前端重新拉取列表、统计、香型、低库存
 *     → 刷新页面显示
 *
 * 新增功能（v2）：
 *   - 点燃弹窗：输入备注和燃烧时长 → 调用 light 接口
 *   - 香型标签筛选：点击标签 → 设定筛选条件 → 刷新列表
 *   - 低库存提醒区：渲染剩余 < 15% 的蜡烛
 *   - 导入导出：导出下载 JSON、导入上传 JSON
 *   - 使用历史：卡片显示最近一次使用记录
 *   - 自动估算：新增时设置总燃烧时长，点燃时输入本次时长自动扣减
 */

const API_BASE = "/api";

let currentScentFilter = "";
let filterDebounceTimer = null;
let pendingLightId = null;

// ========== 页面初始化 ==========
document.addEventListener("DOMContentLoaded", () => {
  const today = new Date().toISOString().split("T")[0];
  document.getElementById("candle-date").value = today;

  bindEvents();
  refreshAll();
});

/**
 * 绑定所有页面事件
 */
function bindEvents() {
  document.getElementById("add-form").addEventListener("submit", handleAddCandle);

  const filterInput = document.getElementById("scent-filter");
  filterInput.addEventListener("input", (e) => {
    clearTimeout(filterDebounceTimer);
    filterDebounceTimer = setTimeout(() => {
      currentScentFilter = e.target.value.trim();
      updateScentTagActive();
      refreshAll();
    }, 300);
  });

  document.getElementById("light-confirm").addEventListener("click", handleLightConfirm);
  document.getElementById("light-cancel").addEventListener("click", closeLightModal);

  document.getElementById("btn-export").addEventListener("click", handleExport);

  document.getElementById("import-file").addEventListener("change", handleImport);
}

// ========== 数据刷新 ==========
/**
 * 刷新所有数据：蜡烛列表 + 统计 + 香型标签 + 低库存
 */
async function refreshAll() {
  try {
    const [candles, stats, scents, lowCandles] = await Promise.all([
      fetchCandles(currentScentFilter),
      fetchStats(currentScentFilter),
      fetchScents(),
      fetchLowCandles(),
    ]);
    renderCandles(candles);
    renderStats(stats);
    renderScentTags(scents);
    renderLowSection(lowCandles);
  } catch (err) {
    console.error("刷新数据失败:", err);
  }
}

async function fetchCandles(scent = "") {
  const url = scent
    ? `${API_BASE}/candles?scent=${encodeURIComponent(scent)}`
    : `${API_BASE}/candles`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("获取蜡烛列表失败");
  return res.json();
}

async function fetchStats(scent = "") {
  const url = scent
    ? `${API_BASE}/stats?scent=${encodeURIComponent(scent)}`
    : `${API_BASE}/stats`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("获取统计数据失败");
  return res.json();
}

async function fetchScents() {
  const res = await fetch(`${API_BASE}/scents`);
  if (!res.ok) throw new Error("获取香型列表失败");
  return res.json();
}

async function fetchLowCandles() {
  const res = await fetch(`${API_BASE}/low-candles`);
  if (!res.ok) throw new Error("获取低库存列表失败");
  return res.json();
}

// ========== 渲染函数 ==========
function renderStats(stats) {
  document.getElementById("stat-total").textContent = stats.total_count;
  document.getElementById("stat-low").textContent = stats.low_count;
  document.getElementById("stat-filtered").textContent = stats.filtered_count;
}

/**
 * 渲染香型标签
 */
function renderScentTags(scents) {
  const container = document.getElementById("scent-tags");
  container.innerHTML = scents.map((s) => {
    const activeClass = currentScentFilter && s === currentScentFilter ? " active" : "";
    return `<span class="scent-tag${activeClass}" data-scent="${escapeHtml(s)}">${escapeHtml(s)}</span>`;
  }).join("");

  container.querySelectorAll(".scent-tag").forEach((tag) => {
    tag.addEventListener("click", () => {
      const scent = tag.dataset.scent;
      if (currentScentFilter === scent) {
        currentScentFilter = "";
        document.getElementById("scent-filter").value = "";
      } else {
        currentScentFilter = scent;
        document.getElementById("scent-filter").value = scent;
      }
      updateScentTagActive();
      refreshAll();
    });
  });
}

function updateScentTagActive() {
  document.querySelectorAll(".scent-tag").forEach((tag) => {
    if (currentScentFilter && tag.dataset.scent === currentScentFilter) {
      tag.classList.add("active");
    } else {
      tag.classList.remove("active");
    }
  });
}

/**
 * 渲染低库存提醒区
 */
function renderLowSection(lowCandles) {
  const section = document.getElementById("low-section");
  const listEl = document.getElementById("low-list");

  if (lowCandles.length === 0) {
    section.style.display = "none";
    return;
  }

  section.style.display = "block";
  listEl.innerHTML = lowCandles.map((c) => `
    <div class="low-chip">
      <span class="low-chip-name">${escapeHtml(c.name)}</span>
      <span class="low-chip-ratio">${c.remaining_ratio}%</span>
    </div>
  `).join("");
}

/**
 * 渲染蜡烛卡片列表
 */
function renderCandles(candles) {
  const listEl = document.getElementById("candles-list");
  const emptyEl = document.getElementById("empty-state");

  if (candles.length === 0) {
    listEl.innerHTML = "";
    emptyEl.style.display = "block";
    return;
  }

  emptyEl.style.display = "none";
  listEl.innerHTML = candles.map((c) => createCandleCard(c)).join("");

  listEl.querySelectorAll(".candle-card").forEach((card) => {
    const id = card.dataset.id;

    card.querySelector(".btn-light").addEventListener("click", () => openLightModal(id));

    card.querySelector(".btn-delete").addEventListener("click", () => handleDelete(id));

    const rangeInput = card.querySelector(".remaining-range");
    const numberInput = card.querySelector(".remaining-number");

    rangeInput.addEventListener("input", () => {
      numberInput.value = rangeInput.value;
    });

    numberInput.addEventListener("input", () => {
      if (numberInput.value === "") return;
      let val = parseInt(numberInput.value);
      if (isNaN(val)) return;
      val = Math.max(0, Math.min(100, val));
      rangeInput.value = val;
    });

    rangeInput.addEventListener("change", () => {
      handleUpdateRemaining(id, parseInt(rangeInput.value));
    });

    numberInput.addEventListener("change", () => {
      const raw = numberInput.value.trim();
      if (raw === "" || isNaN(parseInt(raw))) {
        showToast("剩余比例不能为空，已恢复原值");
        refreshAll();
        return;
      }
      const val = Math.max(0, Math.min(100, parseInt(raw)));
      numberInput.value = val;
      rangeInput.value = val;
      handleUpdateRemaining(id, val);
    });
  });
}

/**
 * 创建单支蜡烛的卡片 HTML
 */
function createCandleCard(candle) {
  const isLow = candle.remaining_ratio < 15;
  const lowBadge = isLow ? '<span class="low-badge">快用完了</span>' : "";
  const cardClass = isLow ? "candle-card is-low" : "candle-card";
  const noteHtml = candle.note
    ? `<div class="candle-note">${escapeHtml(candle.note)}</div>`
    : "";

  const burnInfoHtml = candle.total_burn_hours > 0
    ? `<div class="info-row">
         <span class="info-label">已燃烧</span>
         <span class="info-value">${candle.burned_hours} / ${candle.total_burn_hours} h</span>
       </div>`
    : "";

  let lastUsageHtml = "";
  if (candle.usage_logs && candle.usage_logs.length > 0) {
    const last = candle.usage_logs[candle.usage_logs.length - 1];
    const detailParts = [];
    if (last.burn_hours > 0) detailParts.push(`${last.burn_hours}h`);
    if (last.note) detailParts.push(escapeHtml(last.note));
    const detailStr = detailParts.length > 0 ? ` · ${detailParts.join(" · ")}` : "";
    lastUsageHtml = `<div class="usage-last">
      <span class="usage-last-time">上次使用: ${last.time}</span>
      <span class="usage-last-detail">${detailStr}</span>
    </div>`;
  }

  return `
    <div class="${cardClass}" data-id="${candle.id}">
      ${lowBadge}
      <h3 class="candle-name">${escapeHtml(candle.name)}</h3>
      <p class="candle-scent">· ${escapeHtml(candle.scent)} ·</p>
      
      <div class="candle-info">
        <div class="info-row">
          <span class="info-label">容量</span>
          <span class="info-value">${candle.capacity} g</span>
        </div>
        <div class="info-row">
          <span class="info-label">购买日期</span>
          <span class="info-value">${candle.purchase_date || "-"}</span>
        </div>
        <div class="info-row">
          <span class="info-label">使用次数</span>
          <span class="info-value">${candle.use_count} 次</span>
        </div>
        ${burnInfoHtml}
      </div>

      <div class="remaining-bar-wrapper">
        <div class="remaining-bar-bg">
          <div class="remaining-bar-fill" style="width: ${candle.remaining_ratio}%"></div>
        </div>
        <div class="remaining-text">剩余 ${candle.remaining_ratio}%</div>
      </div>

      <div class="remaining-edit">
        <input type="range" class="remaining-range" min="0" max="100" value="${candle.remaining_ratio}">
        <input type="number" class="remaining-number" min="0" max="100" value="${candle.remaining_ratio}">
      </div>

      ${noteHtml}
      ${lastUsageHtml}

      <div class="card-actions">
        <button class="btn btn-primary btn-small btn-light">🔥 点燃</button>
        <button class="btn btn-danger btn-small btn-delete">删除</button>
      </div>
    </div>
  `;
}

// ========== 弹窗 ==========
function openLightModal(id) {
  pendingLightId = id;
  document.getElementById("light-note").value = "";
  document.getElementById("light-hours").value = 0;
  document.getElementById("light-modal").style.display = "flex";
}

function closeLightModal() {
  pendingLightId = null;
  document.getElementById("light-modal").style.display = "none";
}

async function handleLightConfirm() {
  if (!pendingLightId) return;
  const note = document.getElementById("light-note").value.trim();
  const burnHours = parseFloat(document.getElementById("light-hours").value) || 0;

  try {
    const res = await fetch(`${API_BASE}/candles/${pendingLightId}/light`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ note, burn_hours: burnHours }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "点燃失败");
    }
    closeLightModal();
    showToast("已记录点燃~", "info");
    refreshAll();
  } catch (err) {
    showToast(err.message);
  }
}

// ========== 业务操作 ==========
async function handleAddCandle(e) {
  e.preventDefault();

  const name = document.getElementById("candle-name").value.trim();
  const scent = document.getElementById("candle-scent").value.trim();
  const capacity = parseFloat(document.getElementById("candle-capacity").value);
  const purchaseDate = document.getElementById("candle-date").value;
  const remainingRatio = parseInt(document.getElementById("candle-remaining").value) || 100;
  const note = document.getElementById("candle-note").value.trim();
  const totalBurnHours = parseFloat(document.getElementById("candle-burn-hours").value) || 0;

  if (!name || !scent || !capacity) {
    alert("请填写名称、香型和容量~");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/candles`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name,
        scent,
        capacity,
        purchase_date: purchaseDate,
        remaining_ratio: remainingRatio,
        note,
        total_burn_hours: totalBurnHours,
      }),
    });

    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "添加失败");
    }

    document.getElementById("add-form").reset();
    document.getElementById("candle-date").value = new Date().toISOString().split("T")[0];
    document.getElementById("candle-remaining").value = 100;

    refreshAll();
  } catch (err) {
    alert(err.message);
  }
}

async function handleUpdateRemaining(id, value) {
  try {
    const res = await fetch(`${API_BASE}/candles/${id}/remaining`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ remaining_ratio: value }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "更新失败");
    }
    refreshAll();
  } catch (err) {
    showToast(err.message);
    refreshAll();
  }
}

async function handleDelete(id) {
  if (!confirm("确定要删除这支蜡烛吗？删除后无法恢复哦～")) return;

  try {
    const res = await fetch(`${API_BASE}/candles/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("删除失败");
    refreshAll();
  } catch (err) {
    showToast(err.message);
  }
}

// ========== 导入导出 ==========
async function handleExport() {
  try {
    const res = await fetch(`${API_BASE}/export`);
    if (!res.ok) throw new Error("导出失败");
    const data = await res.json();
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `candles_backup_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast("导出成功！", "info");
  } catch (err) {
    showToast(err.message);
  }
}

async function handleImport(e) {
  const file = e.target.files[0];
  if (!file) return;

  try {
    const text = await file.text();
    const data = JSON.parse(text);

    if (!Array.isArray(data)) {
      throw new Error("JSON 文件格式不正确，应为数组");
    }

    const res = await fetch(`${API_BASE}/import`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ data, merge: true }),
    });

    if (!res.ok) {
      const result = await res.json().catch(() => ({}));
      throw new Error(result.error || "导入失败");
    }

    const result = await res.json();
    const msg = result.mode === "merge"
      ? `导入成功：新增 ${result.added} 条，跳过 ${result.skipped} 条重复`
      : `导入成功：覆盖为 ${result.count} 条`;
    showToast(msg, "info");
    refreshAll();
  } catch (err) {
    showToast(err.message);
  }

  e.target.value = "";
}

// ========== 工具函数 ==========
function showToast(message, type = "error") {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    document.body.appendChild(container);
  }
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("toast-fade-out");
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}
