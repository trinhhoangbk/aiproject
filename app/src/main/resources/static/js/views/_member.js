import { escapeHtml } from "../app.js";

/**
 * Member picker for ADMIN / MANAGER screens: a roster dropdown instead of a raw UUID box.
 * Returns "" for MEMBER (they only ever see themselves). With allowTeam, an empty value
 * means "whole team" (the API's memberId-omitted roll-up).
 */
export async function memberPicker(api, me, selectedId, { allowTeam = false } = {}) {
  if (me.role !== "ADMIN" && me.role !== "MANAGER") return "";
  const roster = await api.get("/api/roster").catch(_ => []);
  const opts = roster.map(m =>
    `<option value="${escapeHtml(m.id)}" ${m.id === selectedId ? "selected" : ""}>${escapeHtml(m.displayName)}</option>`);
  if (allowTeam) opts.unshift(`<option value="" ${!selectedId ? "selected" : ""}>— Cả team —</option>`);
  return `<label>Thành viên<select id="memberId">${opts.join("")}</select></label>`;
}
