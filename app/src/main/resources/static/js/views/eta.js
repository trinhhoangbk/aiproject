import { escapeHtml } from "../app.js";
import { memberPicker, bindMemberPicker } from "./_member.js";

export async function renderEta(root, { me, params, api }) {
  const memberId = params.get("memberId") || me.memberId;
  const [eta, picker] = await Promise.all([
    api.get(`/api/eta/${encodeURIComponent(memberId)}`),
    memberPicker(api, me, memberId),
  ]);

  root.innerHTML = `
    <div class="filters">
      ${picker ? picker + `<button id="go">Xem</button>` : ""}
    </div>
    <div class="card">
      <h1>ETA cá nhân — mỏ-neo ${escapeHtml(eta.anchor)}</h1>
      <p>α đo trên <strong>${eta.observationWindowWorkingDays}</strong> working-day gần nhất (F-03).</p>
      ${eta.projects.length ? `
        <table>
          <thead><tr>
            <th>Project</th>
            <th class="num">Remaining (h)</th>
            <th class="num">Remaining (MD)</th>
            <th class="num">α</th>
            <th class="num">RWD</th>
            <th>ETA</th>
            <th>Ghi chú</th>
          </tr></thead>
          <tbody>${eta.projects.map(p => `
            <tr>
              <td><code>${escapeHtml(p.projectKey)}</code></td>
              <td class="num">${fmt(p.remainingHours)}</td>
              <td class="num">${fmt(p.remainingMd)}</td>
              <td class="num">${fmt3(p.allocationShare)}</td>
              <td class="num">${p.rwd ?? "—"}</td>
              <td>${p.etaDate || "—"}</td>
              <td>${p.note ? `<small>${escapeHtml(p.note)}</small>` : ""}</td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Chưa có issue hoạt động nào gán cho thành viên này.</p>`}
    </div>
  `;
  bindMemberPicker(root);

  const go = root.querySelector("#go");
  if (go) go.addEventListener("click", () => {
    window.location.hash = `#/eta?memberId=${encodeURIComponent(root.querySelector("#memberId").value)}`;
  });
}

function fmt(n)  { return n == null ? "—" : Number(n).toFixed(2); }
function fmt3(n) { return n == null ? "—" : Number(n).toFixed(3); }
