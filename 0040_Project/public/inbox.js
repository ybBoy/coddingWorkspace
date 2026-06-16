/*
  前端交互脚本：处理页面所有用户交互
  数据流说明：
    用户操作（新增/收藏/删除/筛选） → 调用 fetch 请求后端 API
    → 后端 routes 接收请求 → idea_service 更新内存数据
    → json_store 写入本地 JSON 文件
    → 前端收到响应后重新请求列表 → 刷新卡片和统计数据
*/

const API_BASE = "/api/ideas";

// 当前筛选状态
const filterState = {
  keyword: "",
  tag: "",
  onlyFavorite: false,
};

// 防抖计时器
let searchTimer = null;

// ========== 页面初始化 ==========
document.addEventListener("DOMContentLoaded", () => {
  initForm();
  initSearch();
  initTagFilter();
  initFavoriteFilter();
  loadTags();
  refreshAll();
});

// ========== 初始化事件 ==========

function initForm() {
  const form = document.getElementById("idea-form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    await handleCreateIdea();
  });
}

function initSearch() {
  const searchInput = document.getElementById("search");
  searchInput.addEventListener("input", (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      filterState.keyword = e.target.value.trim();
      loadIdeas();
    }, 300);
  });
}

function initTagFilter() {
  const tagList = document.getElementById("tag-filter-list");
  tagList.addEventListener("click", (e) => {
    const tagEl = e.target.closest(".tag");
    if (!tagEl) return;
    const tag = tagEl.dataset.tag;
    filterState.tag = tag;
    updateTagActiveState();
    loadIdeas();
  });
}

function initFavoriteFilter() {
  const checkbox = document.getElementById("only-favorite");
  checkbox.addEventListener("change", (e) => {
    filterState.onlyFavorite = e.target.checked;
    loadIdeas();
  });
}

// ========== 数据加载 ==========

async function refreshAll() {
  await Promise.all([loadIdeas(), loadStats(), loadTags()]);
}

async function loadIdeas() {
  const params = new URLSearchParams();
  if (filterState.keyword) params.set("keyword", filterState.keyword);
  if (filterState.tag) params.set("tag", filterState.tag);
  if (filterState.onlyFavorite) params.set("only_favorite", "true");

  try {
    const res = await fetch(`${API_BASE}?${params}`);
    const ideas = await res.json();
    renderIdeas(ideas);
    updateFilteredCount(ideas.length);
  } catch (err) {
    console.error("加载灵感失败:", err);
  }
}

async function loadStats() {
  try {
    const res = await fetch(`${API_BASE}/stats`);
    const stats = await res.json();
    document.getElementById("stat-total").textContent = stats.total;
    document.getElementById("stat-favorites").textContent = stats.favorites;
  } catch (err) {
    console.error("加载统计失败:", err);
  }
}

async function loadTags() {
  try {
    const res = await fetch(`${API_BASE}/tags`);
    const tags = await res.json();
    renderTagFilter(tags);
  } catch (err) {
    console.error("加载标签失败:", err);
  }
}

// ========== 渲染 ==========

function renderIdeas(ideas) {
  const wall = document.getElementById("idea-wall");
  const emptyState = document.getElementById("empty-state");

  if (ideas.length === 0) {
    wall.innerHTML = "";
    emptyState.style.display = "block";
    return;
  }

  emptyState.style.display = "none";
  wall.innerHTML = ideas.map((idea, index) => createCardHTML(idea, index)).join("");

  wall.querySelectorAll(".favorite-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const id = e.currentTarget.dataset.id;
      await toggleFavorite(id);
    });
  });

  wall.querySelectorAll(".delete-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      const id = e.currentTarget.dataset.id;
      if (confirm("确定要删除这条灵感吗？")) {
        await deleteIdea(id);
      }
    });
  });
}

function createCardHTML(idea, index) {
  const colorClass = `color-${index % 6}`;
  const starClass = idea.is_favorite ? "starred" : "not-starred";
  const starIcon = idea.is_favorite ? "★" : "☆";

  const titleHTML = idea.title
    ? `<div class="card-title">${escapeHTML(idea.title)}</div>`
    : "";

  const sourceHTML = idea.source
    ? `<div class="card-source">— ${escapeHTML(idea.source)}</div>`
    : "";

  const tagsHTML = idea.tags && idea.tags.length
    ? `<div class="card-tags">${idea.tags
        .map((t) => `<span class="card-tag">${escapeHTML(t)}</span>`)
        .join("")}</div>`
    : "";

  const date = formatDate(idea.created_at);

  return `
    <div class="idea-card ${colorClass}">
      <span class="favorite-btn ${starClass}" data-id="${idea.id}" title="${idea.is_favorite ? "取消收藏" : "收藏"}">${starIcon}</span>
      ${titleHTML}
      <div class="card-content">${escapeHTML(idea.content)}</div>
      ${sourceHTML}
      ${tagsHTML}
      <span class="card-date">${date}</span>
      <span class="delete-btn" data-id="${idea.id}" title="删除">🗑 删除</span>
    </div>
  `;
}

function renderTagFilter(tags) {
  const list = document.getElementById("tag-filter-list");
  const allTag = `<span class="tag tag-all ${filterState.tag === "" ? "active" : ""}" data-tag="">全部</span>`;
  const tagHTML = tags
    .map(
      (tag) =>
        `<span class="tag ${filterState.tag === tag ? "active" : ""}" data-tag="${escapeHTML(tag)}">${escapeHTML(tag)}</span>`
    )
    .join("");
  list.innerHTML = allTag + tagHTML;
}

function updateTagActiveState() {
  document.querySelectorAll("#tag-filter-list .tag").forEach((tagEl) => {
    if (tagEl.dataset.tag === filterState.tag) {
      tagEl.classList.add("active");
    } else {
      tagEl.classList.remove("active");
    }
  });
}

function updateFilteredCount(count) {
  document.getElementById("stat-filtered").textContent = count;
}

// ========== 操作 ==========

async function handleCreateIdea() {
  const title = document.getElementById("title").value.trim();
  const content = document.getElementById("content").value.trim();
  const tagsInput = document.getElementById("tags").value.trim();
  const source = document.getElementById("source").value.trim();

  if (!content) {
    alert("内容不能为空哦～");
    return;
  }

  const tags = tagsInput
    ? tagsInput.split(/[,，]/).map((t) => t.trim()).filter((t) => t)
    : [];

  try {
    const res = await fetch(API_BASE, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title, content, tags, source }),
    });

    if (!res.ok) {
      const err = await res.json();
      alert(err.error || "创建失败");
      return;
    }

    document.getElementById("idea-form").reset();
    filterState.tag = "";
    filterState.keyword = "";
    filterState.onlyFavorite = false;
    document.getElementById("search").value = "";
    document.getElementById("only-favorite").checked = false;
    updateTagActiveState();

    await refreshAll();
  } catch (err) {
    console.error("创建灵感失败:", err);
    alert("网络错误，创建失败");
  }
}

async function toggleFavorite(id) {
  try {
    const res = await fetch(`${API_BASE}/${id}/favorite`, {
      method: "POST",
    });
    if (!res.ok) return;
    await Promise.all([loadIdeas(), loadStats()]);
  } catch (err) {
    console.error("切换收藏失败:", err);
  }
}

async function deleteIdea(id) {
  try {
    const res = await fetch(`${API_BASE}/${id}`, {
      method: "DELETE",
    });
    if (!res.ok) return;
    await refreshAll();
  } catch (err) {
    console.error("删除灵感失败:", err);
    alert("删除失败");
  }
}

// ========== 工具函数 ==========

function escapeHTML(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function formatDate(isoStr) {
  const d = new Date(isoStr);
  const month = d.getMonth() + 1;
  const day = d.getDate();
  return `${month}月${day}日`;
}
