import { escapeHtml } from "../app.js";

export async function renderDaily(root, { me, params, api }) {
  const date = params.get("date") || new Date().toISOString().slice(0, 10);
  const memberId = params.get("memberId") || me.memberId;
  const excludeDiscarded = params.get("excludeDiscarded") !== "false";

  const url = `/api/reporting/daily?date=${encodeURIComponent(date)}`
    + `&memberId=${encodeURIComponent(memberId)}`
    + `&excludeDiscarded=${excludeDiscarded}`;
  const rpt = await api.get(url);

  root.innerHTML = `
    <div class="filters">
      <label>Ngày<input type="date" id="date" value="${escapeHtml(date)}"></label>
      ${(me.role === "ADMIN" || me.role === "MANAGER")
        ? `<label>Member ID<input id="memberId" value="${escapeHtml(memberId)}"></label>` : ""}
      <label>Loại trừ Discarded
        <select id="excludeDiscarded">
          <option value="true"  ${excludeDiscarded ? "selected" : ""}>Có</option>
          <option value="false" ${!excludeDiscarded ? "selected" : ""}>Không</option>
        </select>
      </label>
      <button id="go">Xem</button>
    </div>

    <div class="card">
      <h1>Báo cáo ngày — ${escapeHtml(rpt.date)}</h1>

      <h2>Issue đóng (${rpt.done.length})</h2>
      ${rpt.done.length ? `
        <table>
          <thead><tr><th>Issue</th><th>Project</th><th>Resolution</th><th>Discarded</th></tr></thead>
          <tbody>${rpt.done.map(d => `
            <tr>
              <td><code>${escapeHtml(d.issueKey)}</code></td>
              <td><code>${escapeHtml(d.projectKey)}</code></td>
              <td>${escapeHtml(d.resolution || "—")}</td>
              <td>${d.discarded ? `<span class="badge band-yellow">Discarded</span>` : "—"}</td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Không có issue đóng trong ngày này.</p>`}

      <h2 style="margin-top:16px">Tổng worklog theo project</h2>
      ${rpt.worklogTotals.length ? `
        <table>
          <thead><tr><th>Project</th><th class="num">Giờ</th></tr></thead>
          <tbody>
            ${rpt.worklogTotals.map(w => `
              <tr><td><code>${escapeHtml(w.projectKey)}</code></td><td class="num">${fmt(w.hours)}</td></tr>`).join("")}
            <tr><th>Tổng</th><td class="num"><strong>${fmt(rpt.totalWorklogHours)}</strong></td></tr>
          </tbody>
        </table>` : `<p class="empty">Không có worklog nào trong ngày này.</p>`}
    </div>
  `;

  root.querySelector("#go").addEventListener("click", () => {
    const d  = root.querySelector("#date").value;
    const mid = root.querySelector("#memberId") ? root.querySelector("#memberId").value : memberId;
    const ex = root.querySelector("#excludeDiscarded").value;
    window.location.hash = `#/daily?date=${encodeURIComponent(d)}&memberId=${encodeURIComponent(mid)}&excludeDiscarded=${ex}`;
  });
}

function fmt(n) { return n == null ? "—" : Number(n).toFixed(2); }
