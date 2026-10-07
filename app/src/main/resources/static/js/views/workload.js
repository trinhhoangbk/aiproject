import { escapeHtml } from "../app.js";
import { memberPicker, bindMemberPicker } from "./_member.js";

// Field names follow WorkloadView (02 API §5.2) exactly:
// window, anchor, windowStart, windowEnd, standardMd, committedMd, availableMd,
// allocationRate, band, overload{flag, reasons[{level,date,committedH,capacityH,hardDeadlineCriteria}]},
// projects[{projectKey, issueCount, remainingH, ratioByHours, ratioByCount}], unestimated{count, defaultEachMd}

const BAND_LABEL = {
  dark_green:  "Dark Green (<60%)",
  light_green: "Light Green (60–85%)",
  yellow:      "Yellow (85–100%)",
  red:         "Red (>100%)",
};
const WINDOW_LABEL = { week: "Tuần này", "2weeks": "2 tuần tới", month: "Tháng tới" };
const CRITERIA_LABEL = {
  fix_version_release: "Fix version có ngày release",
  priority_blocker:    "Priority Blocker/Critical hoặc nhãn Hard-Deadline",
  hub_lock_flag:       "Đã khoá deadline trên Hub",
};

export async function renderWorkload(root, ctx) {
  const { me, params, api } = ctx;
  const win = params.get("window") || "week";
  const memberId = params.get("memberId") || me.memberId;

  const [wl, picker] = await Promise.all([
    api.get(`/api/workload/${memberId}?window=${encodeURIComponent(win)}`),
    memberPicker(api, me, memberId),
  ]);
  const overloaded = wl.overload && wl.overload.flag === "red";
  const projects = wl.projects || [];
  const un = wl.unestimated || { count: 0, defaultEachMd: 0.5 };

  root.innerHTML = `
    <div class="filters">
      <label>Khoảng<select id="window">${["week","2weeks","month"]
        .map(w => `<option value="${w}" ${w===win?"selected":""}>${WINDOW_LABEL[w]}</option>`).join("")}</select></label>
      ${picker}
      <button id="go">Xem</button>
    </div>

    <div class="card">
      <h1>Khối lượng công việc — ${escapeHtml(wl.memberName || "")}</h1>
      <p><span class="pill">${WINDOW_LABEL[wl.window] || escapeHtml(wl.window)}</span>
         Cửa sổ: <code>${escapeHtml(wl.windowStart)}</code> → <code>${escapeHtml(wl.windowEnd)}</code></p>
      <table>
        <tr><th>Standard MD</th><td class="num">${fmt(wl.standardMd)}</td></tr>
        <tr><th>Committed MD</th><td class="num">${fmt(wl.committedMd)}</td></tr>
        <tr><th>Available MD</th><td class="num">${fmt(wl.availableMd)}</td></tr>
        <tr><th>Allocation Rate</th>
            <td class="num">${fmt(wl.allocationRate)}% <span class="badge band-${escapeHtml(wl.band)}">${BAND_LABEL[wl.band] || escapeHtml(wl.band)}</span></td></tr>
      </table>
    </div>

    <div class="card">
      <h2>${overloaded ? "⚠️ Quá tải" : "Quá tải: không"}</h2>
      ${overloaded ? `<ul>${wl.overload.reasons.map(r => r.level === "week"
          ? `<li><strong>Theo tuần</strong>: ${fmt(r.committedH)} h &gt; ${fmt(r.capacityH)} h (B-RULE-03)</li>`
          : `<li><strong>Ngày ${escapeHtml(r.date)}</strong>: ${fmt(r.committedH)} h &gt; ${fmt(r.capacityH)} h
               — không dời được vì: ${(r.hardDeadlineCriteria || []).map(c => escapeHtml(CRITERIA_LABEL[c] || c)).join(", ")}</li>`
        ).join("")}</ul>`
      : `<p class="empty">Không vượt 40 h/tuần và không có ngày nào quá tải với deadline cứng.</p>`}
    </div>

    <div class="card">
      <h2>Phân bổ theo dự án (các issue đang mở)</h2>
      ${projects.length ? `
        <table>
          <thead><tr><th>Project</th><th class="num">Số issue</th><th class="num">Giờ còn lại</th>
                     <th class="num">% theo giờ</th></tr></thead>
          <tbody>${projects.map(p => `
            <tr><td><code>${escapeHtml(p.projectKey)}</code></td>
                <td class="num">${p.issueCount}</td>
                <td class="num">${fmt(p.remainingH)}</td>
                <td class="num">${pct(p.ratioByHours)}</td></tr>`).join("")}
          </tbody>
        </table>` : `<p class="empty">Không có issue nào đang mở được giao cho thành viên này.</p>`}
    </div>

    <div class="card">
      <h2>Issue chưa ước lượng (B-RULE-02)</h2>
      <p><strong>${un.count}</strong> issue · tạm tính ${fmt(un.defaultEachMd)} MD / issue
         (= ${fmt(un.count * Number(un.defaultEachMd))} MD)</p>
    </div>
  `;
  bindMemberPicker(root);

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

function pct(r) {
  if (r == null) return "—";
  return (Number(r) * 100).toFixed(0) + "%";
}
