import { escapeHtml } from "../app.js";

const BAND_LABEL = {
  dark_green:  "Dark Green (<60%)",
  light_green: "Light Green (60–85%)",
  yellow:      "Yellow (85–100%)",
  red:         "Red (>100%)",
};
const WINDOW_LABEL = { week: "Tuần này", "2weeks": "2 tuần tới", month: "Tháng tới" };

export async function renderWorkload(root, ctx) {
  const { me, params, api } = ctx;
  const win = params.get("window") || "week";
  const memberId = params.get("memberId") || me.memberId;

  const wl = await api.get(`/api/workload/${memberId}?window=${encodeURIComponent(win)}`);
  const bandClass = `band-${wl.band}`;

  root.innerHTML = `
    <div class="filters">
      <label>Khoảng<select id="window">${["week","2weeks","month"]
        .map(w => `<option value="${w}" ${w===win?"selected":""}>${WINDOW_LABEL[w]}</option>`).join("")}</select></label>
      ${(me.role === "ADMIN" || me.role === "MANAGER")
        ? `<label>Member ID<input type="text" id="memberId" value="${escapeHtml(memberId)}"></label>` : ""}
      <button id="go">Xem</button>
    </div>

    <div class="card">
      <h1>Khối lượng công việc — ${escapeHtml(wl.memberName || "")}</h1>
      <p class="pill">${WINDOW_LABEL[wl.horizon] || wl.horizon}</p>
      <p>Cửa sổ: <code>${wl.windowStart}</code> → <code>${wl.windowEnd}</code></p>
      <table>
        <tr><th>Standard MD</th><td class="num">${fmt(wl.standardMd)}</td></tr>
        <tr><th>Committed MD</th><td class="num">${fmt(wl.committedMd)}</td></tr>
        <tr><th>Available MD</th><td class="num">${fmt(wl.availableMd)}</td></tr>
        <tr><th>Allocation Rate</th>
            <td class="num">${fmt(wl.allocationRate)}% <span class="badge ${bandClass}">${BAND_LABEL[wl.band] || wl.band}</span></td></tr>
      </table>
    </div>

    ${wl.overload && wl.overload.isOverloaded ? `
    <div class="card">
      <h2>⚠️ Overload</h2>
      <ul>${wl.overload.reasons.map(r => `
        <li><strong>${escapeHtml(r.code)}</strong>: ${escapeHtml(r.detail || "")}
            ${r.hardDeadlineCriteria && r.hardDeadlineCriteria.length ?
              `<br><small>Hard-deadline: ${r.hardDeadlineCriteria.map(escapeHtml).join(", ")}</small>` : ""}
        </li>`).join("")}</ul>
    </div>` : ""}

    <div class="card">
      <h2>Phân bổ theo dự án</h2>
      ${wl.projectBreakdown && wl.projectBreakdown.length ? `
        <table>
          <thead><tr><th>Project</th><th class="num">MD cam kết</th></tr></thead>
          <tbody>${wl.projectBreakdown.map(p => `
            <tr><td><code>${escapeHtml(p.projectKey)}</code></td><td class="num">${fmt(p.committedMd)}</td></tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Chưa có issue hoạt động nào trong cửa sổ này.</p>`}
    </div>

    <div class="card">
      <h2>Issue không có estimate (B-RULE-02)</h2>
      <p>${wl.unestimated ? `<strong>${wl.unestimated.count}</strong> issue · ${fmt(wl.unestimated.md)} MD (0.5 MD / issue)` : "0"}</p>
    </div>
  `;

  root.querySelector("#go").addEventListener("click", () => {
    const newWin = root.querySelector("#window").value;
    const newMid = root.querySelector("#memberId") ? root.querySelector("#memberId").value : memberId;
    window.location.hash = `#/workload?window=${encodeURIComponent(newWin)}&memberId=${encodeURIComponent(newMid)}`;
  });
}

function fmt(n) {
  if (n == null) return "—";
  const num = Number(n);
  return isFinite(num) ? num.toFixed(2) : String(n);
}
