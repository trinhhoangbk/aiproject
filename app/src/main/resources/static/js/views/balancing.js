import { escapeHtml } from "../app.js";

export async function renderBalancing(root, { params, api }) {
  const pipelineId = params.get("pipelineId") || "";
  if (!pipelineId) {
    root.innerHTML = `
      <div class="card">
        <h1>Cân đối nguồn lực</h1>
        <p>Chọn 1 pipeline từ <a href="#/pipeline">danh sách pipeline</a> để xem gợi ý phân phối.</p>
      </div>`;
    return;
  }

  const sugg = await api.get(`/api/balancing/${encodeURIComponent(pipelineId)}`);

  root.innerHTML = `
    <div class="card">
      <h1>Gợi ý phân phối — pipeline <code>${escapeHtml(pipelineId)}</code></h1>
      <p>Cửa sổ: <code>${sugg.windowStart}</code> → <code>${sugg.windowEnd}</code> ·
         Yêu cầu: <strong>${Number(sugg.requiredMd).toFixed(2)} MD</strong></p>
      <p>Kỹ năng yêu cầu: ${sugg.requiredSkills.map(s =>
        `<span class="pill">${escapeHtml(s.l1)}${s.l2 ? "/" + escapeHtml(s.l2) : ""}</span>`).join(" ")}</p>
      <p><small>Đã xét ${sugg.totalCandidatesConsidered} thành viên ·
        loại vì kỹ năng: ${sugg.rejectedBySkill} · loại vì năng lực: ${sugg.rejectedByCapacity}</small></p>

      <h2>Ứng viên (${sugg.candidates.length})</h2>
      ${sugg.candidates.length ? `
        <table>
          <thead><tr>
            <th>Thành viên</th><th class="num">Available MD</th><th class="num">Daily h</th>
            <th>Kỹ năng khớp</th><th></th>
          </tr></thead>
          <tbody>${sugg.candidates.map(c => `
            <tr>
              <td>${escapeHtml(c.displayName)}</td>
              <td class="num"><strong>${Number(c.availableMd).toFixed(2)}</strong></td>
              <td class="num">${Number(c.dailyHours).toFixed(1)}</td>
              <td>${c.matchedSkills.map(s =>
                `<span class="pill">${escapeHtml(s.l1)}${s.l2 ? "/" + escapeHtml(s.l2) : ""}</span>`).join(" ")}</td>
              <td><button data-mid="${c.memberId}" data-name="${escapeHtml(c.displayName)}" class="assign">Phân phối…</button></td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Không có thành viên nào phù hợp — nới lỏng yêu cầu kỹ năng hoặc rời deadline.</p>`}
    </div>

    <div class="card" id="assignForm" hidden>
      <h2>Phân phối issue</h2>
      <label>Issue key (vd. ABC-123)<input id="a_issue" type="text"></label>
      <label>Lý do (bắt buộc)<input id="a_reason" type="text" placeholder="Lí do phân phối — sẽ comment vào Jira"></label>
      <input id="a_mid" type="hidden">
      <div style="margin-top:12px">
        <button id="a_submit">Gửi phân phối</button>
        <button id="a_cancel" type="button">Huỷ</button>
      </div>
      <div class="error" id="a_err" hidden></div>
      <div class="pill" id="a_ok" hidden></div>
    </div>
  `;

  root.querySelectorAll(".assign").forEach(btn => {
    btn.addEventListener("click", (ev) => {
      const mid = ev.target.dataset.mid;
      root.querySelector("#a_mid").value = mid;
      root.querySelector("#a_reason").value = `Phân phối theo pipeline ${pipelineId} — ${ev.target.dataset.name}`;
      root.querySelector("#assignForm").hidden = false;
    });
  });
  root.querySelector("#a_cancel").addEventListener("click", () => {
    root.querySelector("#assignForm").hidden = true;
  });
  root.querySelector("#a_submit").addEventListener("click", async () => {
    const err = root.querySelector("#a_err");
    const ok  = root.querySelector("#a_ok");
    err.hidden = true; ok.hidden = true;
    const req = {
      issueKey: root.querySelector("#a_issue").value.trim(),
      targetMemberId: root.querySelector("#a_mid").value,
      reason: root.querySelector("#a_reason").value.trim(),
      pipelineId: pipelineId,
    };
    try {
      const res = await api.post("/api/assignments", req);
      ok.hidden = false;
      ok.textContent = res.dryRun
        ? `DRY_RUN — đã audit intent, không gọi Jira (JIRA_WRITE_DRY_RUN=true).`
        : `Đã gửi assignee + comment tới Jira cho ${escapeHtml(req.issueKey)}.`;
    } catch (e) {
      err.hidden = false;
      err.textContent = e.message || "Không gửi được phân phối.";
    }
  });
}
