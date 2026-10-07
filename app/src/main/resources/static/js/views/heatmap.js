import { escapeHtml } from "../app.js";

const WINDOWS = [
  { v: "week",    label: "Tuần này" },
  { v: "2weeks",  label: "2 tuần tới" },
  { v: "month",   label: "Tháng tới" },
];

/**
 * MANAGER/ADMIN heatmap — one cell per (member × horizon).
 * Reads every member via /api/roster then /api/workload/{id}?window= for each,
 * which is fine at 10–50 members (02 API §5.2).
 * When the dedicated /api/heatmap endpoint (M7+) lands, swap the loop for a single call.
 */
export async function renderHeatmap(root, { api }) {
  const members = await api.get("/api/roster");
  if (!members.length) {
    root.innerHTML = `<div class="card"><h1>Heatmap</h1><p class="empty">Chưa có member nào trong roster.</p></div>`;
    return;
  }

  root.innerHTML = `
    <div class="card">
      <h1>Heatmap — Khối lượng theo thành viên × khoảng</h1>
      <div class="heatmap" id="hm"
           style="grid-template-columns: 180px repeat(${WINDOWS.length}, 1fr);">
        <div class="cell"></div>
        ${WINDOWS.map(w => `<div class="cell"><strong>${w.label}</strong></div>`).join("")}
      </div>
    </div>
  `;

  const hm = root.querySelector("#hm");
  for (const m of members) {
    hm.insertAdjacentHTML("beforeend",
      `<div class="cell"><div class="name">${escapeHtml(m.displayName)}</div>
        <div class="metric">${escapeHtml(m.role)}</div></div>`);
    for (const w of WINDOWS) {
      try {
        const wl = await api.get(`/api/workload/${m.id}?window=${w.v}`);
        hm.insertAdjacentHTML("beforeend",
          `<div class="cell band-${wl.band}">
             <div class="name">${Number(wl.allocationRate).toFixed(0)}%</div>
             <div class="metric">${Number(wl.committedMd).toFixed(1)} / ${Number(wl.standardMd).toFixed(1)} MD</div>
           </div>`);
      } catch (_) {
        hm.insertAdjacentHTML("beforeend", `<div class="cell"><div class="metric">—</div></div>`);
      }
    }
  }
}
