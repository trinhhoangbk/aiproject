import { escapeHtml } from "../app.js";

/** Placeholder screen — PipelineController ships in M7 (PLAN-024). Entity + repo already live. */
export async function renderPipeline(root) {
  root.innerHTML = `
    <div class="card">
      <h1>Pipeline dự án</h1>
      <p>Chức năng CRUD cho pipeline (dự án đã có nhu cầu, chưa phân phối) sẽ hoàn thiện ở <strong>M7 PLAN-024</strong>.</p>
      <p class="empty">Hiện tại entity + repo đã sẵn sàng, chờ CP-1 được ký và M7 landing.</p>
    </div>
  `;
}
