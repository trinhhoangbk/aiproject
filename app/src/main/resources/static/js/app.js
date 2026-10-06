// PLAN-032 SPA — vanilla JS module. No bundler, no Node, no framework (PLAN-COND-02).
// Loads /api/auth/me, mounts a hash-router, renders screens. All UI copy is Vietnamese (DEC-016).

import { renderLogin }     from "./views/login.js";
import { renderWorkload }  from "./views/workload.js";
import { renderDaily }     from "./views/daily.js";
import { renderOverdue }   from "./views/overdue.js";
import { renderHeatmap }   from "./views/heatmap.js";
import { renderEta }       from "./views/eta.js";
import { renderPipeline }  from "./views/pipeline.js";
import { renderBalancing } from "./views/balancing.js";
import { renderAdmin }     from "./views/admin.js";

const VIEW = document.getElementById("view");
const NAV  = document.getElementById("nav");
const ME   = document.getElementById("me");
const LOGOUT = document.getElementById("logout");

export const api = {
  async get(path)  { return call("GET", path); },
  async post(path, body) { return call("POST", path, body); },
  async put(path, body)  { return call("PUT",  path, body); },
  async del(path)  { return call("DELETE", path); },
};

async function call(method, path, body) {
  const res = await fetch(path, {
    method,
    headers: body ? { "Content-Type": "application/json" } : {},
    credentials: "same-origin",
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) { window.location.hash = "#/login"; throw new Error("unauthorized"); }
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const msg = (data && (data.detail || data.title)) || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

let currentMe = null;

async function loadMe() {
  try {
    const me = await api.get("/api/auth/me");
    if (me && me.authenticated !== false && me.memberId) {
      currentMe = me;
      ME.textContent = `${me.email} · ${me.role}`;
      NAV.hidden = false;
      LOGOUT.hidden = false;
      filterNavByRole(me.role);
      return me;
    }
  } catch (_) { /* fall through to login */ }
  currentMe = null;
  NAV.hidden = true;
  LOGOUT.hidden = true;
  ME.textContent = "";
  return null;
}

function filterNavByRole(role) {
  for (const a of NAV.querySelectorAll("a[data-role]")) {
    const allowed = a.dataset.role.split(",").map(s => s.trim());
    a.hidden = !allowed.includes(role);
  }
}

const ROUTES = {
  "/login":     renderLogin,
  "/workload":  renderWorkload,
  "/daily":     renderDaily,
  "/overdue":   renderOverdue,
  "/heatmap":   renderHeatmap,
  "/eta":       renderEta,
  "/pipeline":  renderPipeline,
  "/balancing": renderBalancing,
  "/admin":     renderAdmin,
};

async function route() {
  const hash = (window.location.hash || "#/workload").slice(1).split("?")[0];
  const query = (window.location.hash.split("?")[1] || "");
  const params = new URLSearchParams(query);

  if (!currentMe && hash !== "/login") {
    const me = await loadMe();
    if (!me) { window.location.hash = "#/login"; return; }
  }

  const render = ROUTES[hash] || renderWorkload;

  // Highlight active nav.
  for (const a of NAV.querySelectorAll("a[data-nav]")) {
    a.classList.toggle("active", "#" + hash === a.getAttribute("href"));
  }

  VIEW.innerHTML = `<p class="loading">Đang tải…</p>`;
  try {
    await render(VIEW, { me: currentMe, params, api, reload: route });
  } catch (e) {
    VIEW.innerHTML = `<div class="card"><h1>Lỗi</h1><p>${escapeHtml(e.message || String(e))}</p></div>`;
  }
}

export function escapeHtml(s) {
  return String(s == null ? "" : s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

LOGOUT.addEventListener("click", async (ev) => {
  ev.preventDefault();
  try { await api.post("/api/auth/logout"); } catch (_) {}
  currentMe = null;
  window.location.hash = "#/login";
});

window.addEventListener("hashchange", route);
document.getElementById("buildstamp").textContent = new Date().toISOString().slice(0,10);
await loadMe();
await route();
