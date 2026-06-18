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
 *     → 前端重新拉取列表和统计
 *     → 刷新页面显示
 */

// API 基础地址（同域部署，直接用相对路径）
const API_BASE = "/api";

// 页面状态
let currentScentFilter = "";
let filterDebounceTimer = null;

// ========== 页面初始化 ==========
document.addEventListener("DOMContentLoaded", () => {
  // 设置购买日期默认值为今天
  const today = new Date().toISOString().split("T")[0];
  document.getElementById("candle-date").value = today;

  // 绑定事件
  bindEvents();

  // 初次加载数据
  refreshAll();
});

/**
 * 绑定所有页面事件
 */
function bindEvents() {
  // 新增表单提交
  document.getElementById("add-form").addEventListener("submit", handleAddCandle);

  // 香型筛选输入（防抖）
  const filterInput = document.getElementById("scent-filter");
  filterInput.addEventListener("input", (e) => {
    clearTimeout(filterDebounceTimer);
    filterDebounceTimer = setTimeout(() => {
      currentScentFilter = e.target.value.trim();
      refreshAll();
    }, 300);
  });
}

// ========== 数据刷新 ==========
/**
 * 刷新所有数据：蜡烛列表 + 统计数据
 * 流程：同时请求列表和统计接口 → 更新 DOM
 */
async function refreshAll() {
  try {
    const [candles, stats] = await Promise.all([
      fetchCandles(currentScentFilter),
      fetchStats(currentScentFilter),
    ]);
    renderCandles(candles);
    renderStats(stats);
  } catch (err) {
    console.error("刷新数据失败:", err);
  }
}

/**
 * 获取蜡烛列表
 * @param {string} scent - 香型筛选关键词
 * @returns {Promise<Array>} 蜡烛数组
 */
async function fetchCandles(scent = "") {
  const url = scent
    ? `${API_BASE}/candles?scent=${encodeURIComponent(scent)}`
    : `${API_BASE}/candles`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("获取蜡烛列表失败");
  return res.json();
}

/**
 * 获取统计数据
 * @param {string} scent - 香型筛选关键词
 * @returns {Promise<Object>} 统计对象
 */
async function fetchStats(scent = "") {
  const url = scent
    ? `${API_BASE}/stats?scent=${encodeURIComponent(scent)}`
    : `${API_BASE}/stats`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("获取统计数据失败");
  return res.json();
}

// ========== 渲染函数 ==========
/**
 * 渲染统计数据
 * @param {Object} stats - 统计对象
 */
function renderStats(stats) {
  document.getElementById("stat-total").textContent = stats.total_count;
  document.getElementById("stat-low").textContent = stats.low_count;
  document.getElementById("stat-filtered").textContent = stats.filtered_count;
}

/**
 * 渲染蜡烛卡片列表
 * @param {Array} candles - 蜡烛数组
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

  // 为每张卡片绑定事件
  listEl.querySelectorAll(".candle-card").forEach((card) => {
    const id = card.dataset.id;

    // 点燃按钮
    card.querySelector(".btn-light").addEventListener("click", () => handleLight(id));

    // 删除按钮
    card.querySelector(".btn-delete").addEventListener("click", () => handleDelete(id));

    // 剩余比例滑块
    const rangeInput = card.querySelector(".remaining-range");
    const numberInput = card.querySelector(".remaining-number");

    // 滑块拖动时同步数字
    rangeInput.addEventListener("input", () => {
      numberInput.value = rangeInput.value;
    });

    // 数字修改时同步滑块
    numberInput.addEventListener("input", () => {
      let val = parseInt(numberInput.value) || 0;
      val = Math.max(0, Math.min(100, val));
      rangeInput.value = val;
    });

    // 滑块释放时提交更新
    rangeInput.addEventListener("change", () => {
      handleUpdateRemaining(id, parseInt(rangeInput.value));
    });

    // 数字输入失焦时提交更新
    numberInput.addEventListener("change", () => {
      handleUpdateRemaining(id, parseInt(numberInput.value));
    });
  });
}

/**
 * 创建单支蜡烛的卡片 HTML
 * @param {Object} candle - 蜡烛对象
 * @returns {string} HTML 字符串
 */
function createCandleCard(candle) {
  const isLow = candle.remaining_ratio < 15;
  const lowBadge = isLow ? '<span class="low-badge">快用完了</span>' : "";
  const cardClass = isLow ? "candle-card is-low" : "candle-card";
  const noteHtml = candle.note
    ? `<div class="candle-note">${escapeHtml(candle.note)}</div>`
    : "";

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

      <div class="card-actions">
        <button class="btn btn-primary btn-small btn-light">🔥 点燃</button>
        <button class="btn btn-danger btn-small btn-delete">删除</button>
      </div>
    </div>
  `;
}

// ========== 业务操作 ==========
/**
 * 处理新增蜡烛
 * 流程：表单数据 → POST /api/candles → 重置表单 → 刷新列表
 */
async function handleAddCandle(e) {
  e.preventDefault();

  const name = document.getElementById("candle-name").value.trim();
  const scent = document.getElementById("candle-scent").value.trim();
  const capacity = parseFloat(document.getElementById("candle-capacity").value);
  const purchaseDate = document.getElementById("candle-date").value;
  const remainingRatio = parseInt(document.getElementById("candle-remaining").value) || 100;
  const note = document.getElementById("candle-note").value.trim();

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
      }),
    });

    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "添加失败");
    }

    // 重置表单（保留日期默认值）
    document.getElementById("add-form").reset();
    document.getElementById("candle-date").value = new Date().toISOString().split("T")[0];
    document.getElementById("candle-remaining").value = 100;

    // 刷新列表
    refreshAll();
  } catch (err) {
    alert(err.message);
    console.error("添加蜡烛失败:", err);
  }
}

/**
 * 处理点燃操作（使用次数 +1）
 * 流程：点击按钮 → PUT /api/candles/:id/light → 刷新列表
 */
async function handleLight(id) {
  try {
    const res = await fetch(`${API_BASE}/candles/${id}/light`, { method: "PUT" });
    if (!res.ok) throw new Error("点燃失败");
    refreshAll();
  } catch (err) {
    alert(err.message);
    console.error("点燃蜡烛失败:", err);
  }
}

/**
 * 处理修改剩余比例
 * 流程：滑块/数字变化 → PUT /api/candles/:id/remaining → 刷新列表
 */
async function handleUpdateRemaining(id, value) {
  try {
    const res = await fetch(`${API_BASE}/candles/${id}/remaining`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ remaining_ratio: value }),
    });
    if (!res.ok) throw new Error("更新失败");
    refreshAll();
  } catch (err) {
    console.error("更新剩余比例失败:", err);
  }
}

/**
 * 处理删除蜡烛
 * 流程：确认 → DELETE /api/candles/:id → 刷新列表
 */
async function handleDelete(id) {
  if (!confirm("确定要删除这支蜡烛吗？删除后无法恢复哦～")) return;

  try {
    const res = await fetch(`${API_BASE}/candles/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("删除失败");
    refreshAll();
  } catch (err) {
    alert(err.message);
    console.error("删除蜡烛失败:", err);
  }
}

// ========== 工具函数 ==========
/**
 * HTML 转义，防止 XSS
 */
function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}
