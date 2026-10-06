import { escapeHtml } from "../app.js";

const STATE_LABEL = {
  DRAFT: "Nháp",
  READY_TO_ALLOCATE: "Sẵn sàng phân phối",
  ALLOCATED: "Đã phân phối",
  CHANGES_REQUIRED: "Cần chỉnh sửa",
  REJECTED: "Từ chối",
  CLOSED: "Đóng",
  REASSIGNED: "Chuyển lại",
};

export async function renderPipeline(root, { params, api }) {
  const filterState = params.get("state") || "";
  const url = filterState ? `/api/pipeline?state=${filterState}` : "/api/pipeline";
  const list = await api.get(url);

  root.innerHTML = `
    <div class="filters">
      <label>Trạng thái
        <select id="state">
          <option value="">— Tất cả —</option>
          ${Object.entries(STATE_LABEL).map(([k, v]) =>
            `<option value="${k}" ${filterState===k?"selected":""}>${v}</option>`).join("")}
        </select>
      </label>
      <button id="go">Lọc</button>
      <button id="new" style="margin-left:auto">+ Thêm pipeline</button>
    </div>

    <div class="card">
      <h1>Pipeline dự án (${list.length})</h1>
      ${list.length ? `
        <table>
          <thead><tr>
            <th>Tên</th><th class="num">MD</th><th>Deadline</th><th>Trạng thái</th>
            <th>Kỹ năng yêu cầu</th><th></th>
          </tr></thead>
          <tbody>${list.map(p => `
            <tr>
              <td><strong>${escapeHtml(p.name)}</strong><br><small>${escapeHtml(p.objective)}</small></td>
              <td class="num">${Number(p.totalEstimatedMd).toFixed(1)}</td>
              <td>${p.targetDeadline}</td>
              <td><span class="badge band-${stateBand(p.state)}">${STATE_LABEL[p.state] || p.state}</span></td>
              <td>${p.requiredSkills.map(s => `<span class="pill">${escapeHtml(s.name)}</span>`).join("")}</td>
              <td><a href="#/balancing?pipelineId=${p.id}">Gợi ý phân phối →</a></td>
            </tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Chưa có pipeline nào.</p>`}
    </div>

    <div class="card" id="newForm" hidden>
      <h2>Thêm pipeline mới</h2>
      <label>Tên<input id="f_name" type="text"></label>
      <label>Mục tiêu<input id="f_objective" type="text"></label>
      <label>Deadline<input id="f_deadline" type="date"></label>
      <label>Tổng MD ước tính<input id="f_md" type="number" step="0.1" min="0.1"></label>
      <label>Kỹ năng (L1, phẩy ngăn cách)
        <input id="f_skills" type="text" placeholder="BACKEND,FRONTEND"></label>
      <div style="margin-top:12px"><button id="f_save">Tạo</button>
      <button id="f_cancel" type="button">Huỷ</button></div>
      <div class="error" id="f_err" hidden></div>
    </div>
  `;

  root.querySelector("#go").addEventListener("click", () => {
    const v = root.querySelector("#state").value;
    window.location.hash = v ? `#/pipeline?state=${v}` : "#/pipeline";
  });

  root.querySelector("#new").addEventListener("click", () => {
    root.querySelector("#newForm").hidden = false;
  });
  root.querySelector("#f_cancel").addEventListener("click", () => {
    root.querySelector("#newForm").hidden = true;
  });
  root.querySelector("#f_save").addEventListener("click", async () => {
    const err = root.querySelector("#f_err");
    err.hidden = true; err.textContent = "";
    const req = {
      name: root.querySelector("#f_name").value.trim(),
      objective: root.querySelector("#f_objective").value.trim(),
      targetDeadline: root.querySelector("#f_deadline").value,
      totalEstimatedMd: Number(root.querySelector("#f_md").value || 0),
      requiredSkills: root.querySelector("#f_skills").value
        .split(",").map(s => s.trim()).filter(Boolean)
        .map(l1 => ({ l1, l2: "" })),
    };
    try {
      await api.post("/api/pipeline", req);
      window.location.reload();
    } catch (e) {
      err.hidden = false;
      err.textContent = e.message || "Không tạo được pipeline.";
    }
  });
}

function stateBand(s) {
  switch (s) {
    case "DRAFT": return "none";
    case "READY_TO_ALLOCATE": return "yellow";
    case "ALLOCATED": return "light_green";
    case "CHANGES_REQUIRED": return "yellow";
    case "REJECTED": return "red";
    case "CLOSED": return "dark_green";
    case "REASSIGNED": return "none";
    default: return "none";
  }
}
