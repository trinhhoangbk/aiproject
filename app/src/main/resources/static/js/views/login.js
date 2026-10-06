import { escapeHtml } from "../app.js";

export async function renderLogin(root) {
  root.innerHTML = `
    <div class="login-wrap">
      <form class="card login-card" id="loginForm">
        <h1>Đăng nhập</h1>
        <label>Email</label>
        <input type="email" name="email" required autocomplete="username">
        <label>Mật khẩu</label>
        <input type="password" name="password" required autocomplete="current-password">
        <button type="submit">Đăng nhập</button>
        <div class="error" id="loginError" hidden></div>
      </form>
    </div>
  `;

  root.querySelector("#loginForm").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const form = ev.target;
    const data = new URLSearchParams(new FormData(form));
    const err = root.querySelector("#loginError");
    err.hidden = true; err.textContent = "";
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: data,
        credentials: "same-origin",
      });
      if (!res.ok) {
        err.hidden = false;
        err.textContent = res.status === 401 ? "Email hoặc mật khẩu không đúng." : `Lỗi: HTTP ${res.status}`;
        return;
      }
      window.location.hash = "#/workload";
      window.location.reload();
    } catch (e) {
      err.hidden = false;
      err.textContent = escapeHtml(e.message || "Không kết nối được tới máy chủ.");
    }
  });
}
