import { escapeHtml } from "../app.js";

/**
 * Member search box for ADMIN / MANAGER screens: type part of a name or email,
 * pick from the suggestions. The UUID the API needs stays in a hidden #memberId
 * field, so views keep reading root.querySelector("#memberId").value.
 *
 * Returns "" for MEMBER (they only ever see themselves). With allowTeam, an empty
 * box (or "Cả team") means the whole team — the API's memberId-omitted roll-up.
 *
 * Call bindMemberPicker(root) once the view's HTML is in the DOM.
 */
export const TEAM_LABEL = "Cả team";

export async function memberPicker(api, me, selectedId, { allowTeam = false } = {}) {
  if (me.role !== "ADMIN" && me.role !== "MANAGER") return "";
  const roster = await api.get("/api/roster").catch(_ => []);
  const current = roster.find(m => m.id === selectedId);
  const shown = current ? current.displayName : (allowTeam && !selectedId ? TEAM_LABEL : "");
  const options = roster.map(m =>
    `<option value="${escapeHtml(m.displayName)}" data-id="${escapeHtml(m.id)}" data-email="${escapeHtml(m.email)}">${escapeHtml(m.email)}</option>`);
  if (allowTeam) options.unshift(`<option value="${TEAM_LABEL}" data-id="">Tổng hợp tất cả thành viên</option>`);
  return `
    <label>Thành viên
      <input id="memberSearch" list="memberList" autocomplete="off"
             placeholder="Gõ tên hoặc email…" value="${escapeHtml(shown)}"
             data-allow-team="${allowTeam}">
      <datalist id="memberList">${options.join("")}</datalist>
      <input type="hidden" id="memberId" value="${escapeHtml(selectedId || "")}">
      <small id="memberHint" class="error" hidden>Không tìm thấy thành viên</small>
    </label>`;
}

/** Resolve the typed text to a member id: exact name/email first, then a unique partial match. */
export function bindMemberPicker(root) {
  const box = root.querySelector("#memberSearch");
  if (!box) return;
  const hidden = root.querySelector("#memberId");
  const hint = root.querySelector("#memberHint");
  const allowTeam = box.dataset.allowTeam === "true";
  const opts = [...root.querySelectorAll("#memberList option")].map(o => ({
    name: o.value, id: o.dataset.id, email: (o.dataset.email || "").toLowerCase(),
  }));

  const resolve = () => {
    const t = box.value.trim().toLowerCase();
    if (!t) {
      if (allowTeam) { hidden.value = ""; hint.hidden = true; }
      return;
    }
    const exact = opts.find(o => o.name.toLowerCase() === t || o.email === t);
    const partial = opts.filter(o => o.name.toLowerCase().includes(t) || o.email.includes(t));
    const hit = exact || (partial.length === 1 ? partial[0] : null);
    if (hit) { hidden.value = hit.id; hint.hidden = true; }
    else { hint.hidden = false; hint.textContent = partial.length > 1
      ? `Có ${partial.length} người khớp — gõ cụ thể hơn` : "Không tìm thấy thành viên"; }
  };
  box.addEventListener("input", resolve);
  box.addEventListener("change", resolve);
  box.addEventListener("keydown", e => {
    if (e.key === "Enter") { resolve(); const go = root.querySelector("#go"); if (go && hint.hidden) go.click(); }
  });
}
