import { escapeHtml } from "../app.js";

/**
 * Admin screen — danh sách member + xem lịch sử audit gần nhất.
 * Mutation tạo/sửa member hoàn thiện sau; ưu tiên landing M9 (audit) + M8 security hiển thị.
 */
export async function renderAdmin(root, { api }) {
  const [members, audits] = await Promise.all([
    api.get("/api/roster"),
    api.get("/api/audit?limit=25").catch(_ => []),
  ]);

  root.innerHTML = `
    <div class="card">
      <h1>Quản trị · Danh sách thành viên (${members.length})</h1>
      <table>
        <thead><tr><th>Tên</th><th>Email</th><th>Role</th><th>Jira Account</th><th>Active</th></tr></thead>
        <tbody>${members.map(m => `
          <tr>
            <td>${escapeHtml(m.displayName)}</td>
            <td><code>${escapeHtml(m.email)}</code></td>
            <td><span class="badge band-${roleBand(m.role)}">${escapeHtml(m.role)}</span></td>
            <td><code>${escapeHtml(m.jiraAccountId)}</code></td>
            <td>${m.active ? "✓" : "—"}</td>
          </tr>`).join("")}
        </tbody>
      </table>
    </div>

    <div class="card">
      <h2>Audit gần nhất</h2>
      ${audits.length ? `
        <table>
          <thead><tr><th>Thời gian</th><th>Hành động</th><th>Target</th><th>Kết quả</th></tr></thead>
          <tbody>${audits.map(a => `
            <tr>
              <td><small>${escapeHtml(a.occurredAt)}</small></td>
              <td><code>${escapeHtml(a.action)}</code></td>
              <td><code>${escapeHtml(a.targetType)}:${escapeHtml(a.targetId)}</code></td>
              <td>${a.result === "OK"
                 ? `<span class="badge band-light_green">OK</span>`
                 : `<span class="badge band-red">${escapeHtml(a.result)}</span>`}</td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Chưa có audit event nào.</p>`}
    </div>
  `;
}

function roleBand(r) {
  return r === "ADMIN" ? "red" : r === "MANAGER" ? "yellow" : "light_green";
}
