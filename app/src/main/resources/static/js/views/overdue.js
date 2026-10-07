import { escapeHtml } from "../app.js";
import { memberPicker } from "./_member.js";

const OB_LABEL = { "1_to_3_days": "1–3 ngày", "4_to_7_days": "4–7 ngày", "more_than_a_week": ">1 tuần" };
const TPR_LABEL = { yellow: "Cảnh báo (yellow)", red: "Nguy cấp (red)", none: "An toàn" };

export async function renderOverdue(root, { me, params, api }) {
  const memberId = params.get("memberId") || me.memberId;
  const [overdue, warnings, picker] = await Promise.all([
    api.get(`/api/overdue?memberId=${encodeURIComponent(memberId)}`),
    api.get(`/api/overdue/warnings?memberId=${encodeURIComponent(memberId)}`),
    memberPicker(api, me, memberId),
  ]);

  root.innerHTML = `
    <div class="filters">
      ${picker ? picker + `<button id="go">Xem</button>` : ""}
    </div>

    <div class="card">
      <h1>Issue quá hạn (${overdue.length})</h1>
      ${overdue.length ? `
        <table>
          <thead><tr><th>Issue</th><th>Project</th><th>Due date</th><th class="num">Trễ (ngày)</th><th>Band</th></tr></thead>
          <tbody>${overdue.map(o => `
            <tr>
              <td><code>${escapeHtml(o.issueKey)}</code></td>
              <td><code>${escapeHtml(o.projectKey)}</code></td>
              <td>${escapeHtml(o.dueDate)}</td>
              <td class="num">${o.daysOverdue}</td>
              <td><span class="badge band-${o.overdueBand}">${OB_LABEL[o.overdueBand] || o.overdueBand}</span></td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Không có issue nào quá hạn. 🎉</p>`}
    </div>

    <div class="card">
      <h2>Cảnh báo sớm (${warnings.length}) — TPR theo DEC-009</h2>
      ${warnings.length ? `
        <table>
          <thead><tr><th>Issue</th><th>Project</th><th>Due date</th><th class="num">TPR</th><th>Band</th></tr></thead>
          <tbody>${warnings.map(w => `
            <tr>
              <td><code>${escapeHtml(w.issueKey)}</code></td>
              <td><code>${escapeHtml(w.projectKey)}</code></td>
              <td>${escapeHtml(w.dueDate)}</td>
              <td class="num">${fmt(w.tpr)}</td>
              <td><span class="badge band-${w.tprBand}">${TPR_LABEL[w.tprBand] || w.tprBand}</span></td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Không có issue nào trong vùng cảnh báo.</p>`}
    </div>
  `;

  const go = root.querySelector("#go");
  if (go) go.addEventListener("click", () => {
    const mid = root.querySelector("#memberId").value;
    window.location.hash = `#/overdue?memberId=${encodeURIComponent(mid)}`;
  });
}

function fmt(n) { return n == null ? "—" : Number(n).toFixed(3); }
