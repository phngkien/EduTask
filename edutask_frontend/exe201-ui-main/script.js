const pages = {
  dashboard: "Dashboard",
  tasks: "Công việc",
  groups: "Nhóm học",
  members: "Thành viên",
  notifications: "Thông báo",
  plans: "Gói đăng ký",
};

const breadcrumbs = {
  dashboard: "Xin chào 👋",
  tasks: "Quản lý công việc từ backend",
  groups: "Các nhóm học của bạn",
  members: "Chọn một nhóm để xem thành viên",
  profile: "Cập nhật thông tin cá nhân",
  notifications: "Thông báo từ hệ thống",
  plans: "Quản lý gói đăng ký",
};

const API_BASE = "http://localhost:8080/api";
let currentUser = null;
let currentGroups = [];
let currentTasks = [];
let currentUsers = [];
let currentTaskFilter = "all";
let activeGroupId = null;
let activeGroupName = "";
let currentNotifications = [];
let currentPlans = [];
let currentTransactions = [];
let currentActiveSubscription = null;

const loginButton = document.getElementById("login-button");
const registerButton = document.getElementById("register-button");

async function apiRequest(url, options = {}, retry = true) {
  const token = localStorage.getItem("accessToken");

  const res = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  const data = await res.json().catch(() => null);

  if (res.status === 401 && retry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) return apiRequest(url, options, false);
    forceLogout();
    throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  }

  if (!res.ok || data?.success === false) {
    throw new Error(data?.message || "Có lỗi xảy ra");
  }

  return data;
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem("refreshToken");
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    const data = await res.json().catch(() => null);
    if (!res.ok || data?.success === false) return false;

    const auth = getData(data);
    if (!auth?.accessToken) return false;

    localStorage.setItem("accessToken", auth.accessToken);
    localStorage.setItem("refreshToken", auth.refreshToken || refreshToken);
    localStorage.setItem("user", JSON.stringify(auth));
    return true;
  } catch (error) {
    return false;
  }
}

function forceLogout() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
}

function getData(res) {
  return res?.data ?? res;
}

function showPage(name) {
  document
    .querySelectorAll(".page")
    .forEach((p) => p.classList.remove("active"));
  document
    .querySelectorAll(".nav-item")
    .forEach((n) => n.classList.remove("active"));

  const page = document.getElementById("page-" + name);
  if (page) page.classList.add("active");

  const pageTitle = document.getElementById("page-title");
  if (pageTitle) pageTitle.textContent = pages[name] || "EduTask";

  const breadcrumb = document.getElementById("page-breadcrumb");
  if (breadcrumb) breadcrumb.textContent = breadcrumbs[name] || "";

  document.querySelectorAll(".nav-item").forEach((btn) => {
    if (btn.getAttribute("onclick")?.includes(name))
      btn.classList.add("active");
  });
  function getPlanVisual(plan, index) {
    const visuals = [
      {
        theme: "plan-theme-basic",
        badges: ["Mới", "Tiết kiệm"],
        discount: 15,
        subtitle: "Phù hợp để bắt đầu trải nghiệm",
        highlight: false,
        bestValue: "",
      },
      {
        theme: "plan-theme-popular",
        badges: ["Hot", "Phổ biến", "Bán chạy"],
        discount: 30,
        subtitle: "Lựa chọn được nhiều người dùng nhất",
        highlight: true,
        bestValue: "BEST VALUE",
      },
      {
        theme: "plan-theme-premium",
        badges: ["Pro", "Nổi bật", "Ưu đãi lớn"],
        discount: 40,
        subtitle: "Tối ưu hiệu quả học nhóm và quản lý task",
        highlight: false,
        bestValue: "",
      },
      {
        theme: "plan-theme-elite",
        badges: ["VIP", "Cao cấp"],
        discount: 50,
        subtitle: "Trải nghiệm cao cấp với ưu đãi mạnh nhất",
        highlight: false,
        bestValue: "",
      },
    ];

    return visuals[index % visuals.length];
  }

  function buildPlanFeatures(plan) {
    const raw = (plan.features || "").trim();
    if (!raw) {
      return [
        "Quản lý công việc nhóm dễ dàng",
        "Theo dõi deadline nhanh chóng",
        "Nhận thông báo và cập nhật liên tục",
      ];
    }

    return raw
      .split(/[,;\n]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .slice(0, 4);
  }

  function calculateOldPrice(price, discount) {
    const p = Number(price || 0);
    if (!p) return 0;
    return Math.round(p / (1 - discount / 100));
  }

  if (name === "dashboard") renderDashboard();
  if (name === "tasks") renderTasks(currentTaskFilter);
  if (name === "groups") renderGroups();
  if (name === "profile") renderProfile();
  if (name === "notifications") renderNotifications();
  if (name === "plans") renderPlans();
}

function filterTasks(el, type) {
  document
    .querySelectorAll(".filter-pill")
    .forEach((p) => p.classList.remove("active"));
  el.classList.add("active");
  currentTaskFilter = type;
  renderTasks(type);
}

function openLoginModal() {
  document.getElementById("login-modal")?.classList.add("open");
}

function closeLoginModal() {
  document.getElementById("login-modal")?.classList.remove("open");
}

function openRegisterModal() {
  document.getElementById("register-modal")?.classList.add("open");
}

function closeRegisterModal() {
  document.getElementById("register-modal")?.classList.remove("open");
}

function openModal() {
  document.getElementById("modal-create")?.classList.add("open");
  fillGroupSelects();
  const firstGroupId = document.getElementById("task-group-select")?.value;
  if (firstGroupId) loadMembersForTaskSelect(firstGroupId);
}

function openGroupModal() {
  document.getElementById("modal-create-group")?.classList.add("open");
}

function closeGroupModal() {
  document.getElementById("modal-create-group")?.classList.remove("open");
}

function openInviteMemberModal() {
  if (!activeGroupId) {
    alert("Vui lòng chọn một nhóm trước");
    return;
  }
  fillUserSelect();
  document.getElementById("modal-invite-member")?.classList.add("open");
}

function closeInviteMemberModal() {
  document.getElementById("modal-invite-member")?.classList.remove("open");
}

function closeModal() {
  document.getElementById("modal-create")?.classList.remove("open");
}

function openAssignModal() {
  document.getElementById("modal-assign")?.classList.add("open");
  fillGroupSelects();
}

function closeAssignModal() {
  document.getElementById("modal-assign")?.classList.remove("open");
}

if (loginButton) {
  loginButton.addEventListener("click", async () => {
    const username = document.getElementById("login-email")?.value.trim();
    const password = document.getElementById("login-password")?.value.trim();

    if (!username || !password) {
      alert("Vui lòng nhập email và mật khẩu");
      return;
    }

    try {
      const res = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });

      const auth = getData(res);
      localStorage.setItem("accessToken", auth.accessToken);
      localStorage.setItem("refreshToken", auth.refreshToken || "");
      localStorage.setItem("user", JSON.stringify(auth));
      window.location.href = "dashboard.html";
    } catch (error) {
      alert(error.message || "Đăng nhập thất bại");
    }
  });
}

if (registerButton) {
  registerButton.addEventListener("click", async () => {
    const fullName = document.getElementById("register-fullname")?.value.trim();
    const email = document.getElementById("register-email")?.value.trim();
    const password = document.getElementById("register-password")?.value.trim();
    const confirmPassword = document
      .getElementById("register-confirm-password")
      ?.value.trim();

    if (!fullName || !email || !password || !confirmPassword) {
      alert("Vui lòng nhập đầy đủ thông tin");
      return;
    }

    if (password.length < 6) {
      alert("Mật khẩu phải có ít nhất 6 ký tự");
      return;
    }

    if (password !== confirmPassword) {
      alert("Mật khẩu nhập lại không khớp");
      return;
    }

    try {
      const res = await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({ fullName, email, password }),
      });

      const auth = getData(res);
      localStorage.setItem("accessToken", auth.accessToken);
      localStorage.setItem("refreshToken", auth.refreshToken || "");
      localStorage.setItem("user", JSON.stringify(auth));
      window.location.href = "dashboard.html";
    } catch (error) {
      alert(error.message || "Đăng ký thất bại");
    }
  });
}

async function logout() {
  try {
    await apiRequest("/auth/logout", { method: "POST" });
  } catch (error) {
    console.warn("Logout backend failed", error.message);
  }
  forceLogout();
  window.location.href = "index.html";
}

async function initDashboardPage() {
  const token = localStorage.getItem("accessToken");
  if (!token) {
    window.location.href = "index.html";
    return;
  }

  try {
    await loadCurrentUser();
    await Promise.all([
      loadGroups(),
      loadTasks(),
      loadUsers(),
      loadNotifications(),
      loadSubscriptions(),
    ]);
    fillGroupSelects();
    fillTaskGroupFilter();
    renderDashboard();
    renderGroups();
    renderTasks(currentTaskFilter);
    renderProfile();
    renderNotifications();
    renderPlans();
  } catch (error) {
    console.error(error);
    alert(error.message || "Không tải được dữ liệu từ backend");
  }
}

async function refreshAllData() {
  try {
    await Promise.all([
      loadCurrentUser(),
      loadGroups(),
      loadTasks(),
      loadUsers(),
      loadNotifications(),
      loadSubscriptions(),
    ]);
    fillGroupSelects();
    fillTaskGroupFilter();
    renderDashboard();
    renderGroups();
    renderTasks(currentTaskFilter);
    renderProfile();
    renderNotifications();
    renderPlans();
    showToast("Đã làm mới dữ liệu");
  } catch (error) {
    alert(error.message || "Không làm mới được dữ liệu");
  }
}

async function loadCurrentUser() {
  const res = await apiRequest("/users/me");
  currentUser = getData(res);

  const name = currentUser.fullName || currentUser.email || "Người dùng";
  const avatar = currentUser.avatarUrl ? null : getInitials(name);

  const breadcrumb = document.getElementById("page-breadcrumb");
  if (breadcrumb) breadcrumb.textContent = `Xin chào, ${name} 👋`;

  const userName = document.getElementById("current-user-name");
  if (userName) userName.textContent = name;

  const userRole = document.getElementById("current-user-role");
  if (userRole) userRole.textContent = currentUser.role || "Người dùng";

  const userAvatar = document.getElementById("current-user-avatar");
  if (userAvatar) userAvatar.textContent = avatar || "?";
}

async function loadGroups() {
  try {
    const res = await apiRequest("/groups/my");
    currentGroups = Array.isArray(getData(res)) ? getData(res) : [];
  } catch (error) {
    console.warn("Không tải được /groups/my, thử /groups", error.message);
    const res = await apiRequest("/groups");
    currentGroups = Array.isArray(getData(res)) ? getData(res) : [];
  }
}

async function loadTasks() {
  try {
    const res = await apiRequest("/tasks/my");
    currentTasks = Array.isArray(getData(res)) ? getData(res) : [];
  } catch (error) {
    console.warn("Không tải được /tasks/my, thử /tasks", error.message);
    const res = await apiRequest("/tasks");
    currentTasks = Array.isArray(getData(res)) ? getData(res) : [];
  }
}

async function loadUsers() {
  try {
    const res = await apiRequest("/users");
    currentUsers = Array.isArray(getData(res)) ? getData(res) : [];
  } catch (error) {
    console.warn("Không tải được danh sách users", error.message);
    currentUsers = currentUser ? [currentUser] : [];
  }
}

function renderDashboard() {
  const total = currentTasks.length;
  const completed = currentTasks.filter(isDone).length;
  const doing = currentTasks.filter(isDoing).length;

  setText("stat-total-tasks", total);
  setText("stat-completed-tasks", completed);
  setText("stat-doing-tasks", doing);
  setText("stat-total-groups", currentGroups.length);

  setText(
    "stat-total-tasks-desc",
    total ? "Tải từ backend" : "Chưa có công việc",
  );
  setText(
    "stat-completed-tasks-desc",
    completed
      ? `${completed}/${total} công việc`
      : "Chưa hoàn thành công việc nào",
  );
  setText(
    "stat-doing-tasks-desc",
    doing ? `${doing} công việc đang làm` : "Chưa có công việc đang làm",
  );
  setText(
    "stat-total-groups-desc",
    currentGroups.length ? "Nhóm của tài khoản hiện tại" : "Chưa có nhóm",
  );
  setText("nav-task-count", total);

  const list = document.getElementById("dashboard-task-list");
  if (!list) return;
  list.innerHTML = "";

  const recentTasks = currentTasks.slice(0, 5);
  if (!recentTasks.length) {
    list.innerHTML = `<div class="empty-text">Chưa có công việc nào</div>`;
    return;
  }

  recentTasks.forEach((task) =>
    list.appendChild(createDashboardTaskItem(task)),
  );
}

function createDashboardTaskItem(task) {
  const item = document.createElement("div");
  item.className = "task-item";
  item.innerHTML = `
    <div class="task-check ${isDone(task) ? "done" : ""}" onclick="updateTaskStatus(${task.taskId}, '${isDone(task) ? "TODO" : "DONE"}')"></div>
    <div class="task-content">
      <div class="task-name ${isDone(task) ? "done" : ""}">${escapeHtml(task.taskName || "Không có tên")}</div>
      <div class="task-meta">
        <span class="tag tag-blue">${escapeHtml(task.groupName || "Chưa có nhóm")}</span>
        <span>${formatDate(task.dueDate)}</span>
      </div>
    </div>
  `;
  return item;
}

function renderTasks(filter = "all") {
  const lists = {
    todo: document.getElementById("todo-tasks"),
    doing: document.getElementById("doing-tasks"),
    done: document.getElementById("done-tasks"),
  };

  Object.values(lists).forEach((list) => {
    if (list) list.innerHTML = "";
  });

  const filteredTasks = getFilteredTasks();
  const todoTasks = filteredTasks.filter((t) => !isDone(t) && !isDoing(t));
  const doingTasks = filteredTasks.filter(isDoing);
  const doneTasks = filteredTasks.filter(isDone);

  setText("todo-tasks-count", todoTasks.length);
  setText("doing-tasks-count", doingTasks.length);
  setText("done-tasks-count", doneTasks.length);

  renderTaskColumn(
    lists.todo,
    filter === "all" || filter === "todo" ? todoTasks : [],
  );
  renderTaskColumn(
    lists.doing,
    filter === "all" || filter === "doing" ? doingTasks : [],
  );
  renderTaskColumn(
    lists.done,
    filter === "all" || filter === "done" ? doneTasks : [],
  );
}

function getFilteredTasks() {
  const search =
    document.getElementById("task-search-input")?.value.trim().toLowerCase() ||
    "";
  const groupId = document.getElementById("task-group-filter")?.value || "";

  return currentTasks.filter((task) => {
    const matchesSearch =
      !search ||
      [task.taskName, task.groupName, task.assigneeName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(search));
    const matchesGroup = !groupId || String(task.groupId) === String(groupId);
    return matchesSearch && matchesGroup;
  });
}

function clearTaskFilters() {
  const search = document.getElementById("task-search-input");
  const groupFilter = document.getElementById("task-group-filter");
  if (search) search.value = "";
  if (groupFilter) groupFilter.value = "";
  renderTasks(currentTaskFilter);
}

function renderTaskColumn(container, tasks) {
  if (!container) return;
  container.innerHTML = "";

  if (!tasks.length) {
    container.innerHTML = `<div class="empty-text">Chưa có công việc nào</div>`;
    return;
  }

  tasks.forEach((task) => {
    const card = document.createElement("div");
    card.className = "kanban-task";
    card.innerHTML = `
      <div class="kanban-task-name">${escapeHtml(task.taskName || "Không có tên")}</div>
      <div class="task-meta" style="margin-bottom: 10px">
        <span class="tag tag-blue">${escapeHtml(task.groupName || "Chưa có nhóm")}</span>
        <span class="tag ${isDone(task) ? "tag-green" : isDoing(task) ? "tag-amber" : "tag-gray"}">${statusText(task.status)}</span>
      </div>
      <div class="kanban-task-footer">
        <div class="kanban-task-due">${formatDate(task.dueDate)}</div>
        <div class="mini-avatar" style="background: #eef2fd; color: #1a3ab0">${getInitials(task.assigneeName || currentUser?.fullName || "?")}</div>
      </div>
      <div class="task-actions">
        ${isDoing(task) || isDone(task) ? `<button class="mini-btn" onclick="updateTaskStatus(${task.taskId}, 'TODO')">Chưa làm</button>` : `<button class="mini-btn" onclick="updateTaskStatus(${task.taskId}, 'DOING')">Bắt đầu</button>`}
        ${isDone(task) ? "" : `<button class="mini-btn success" onclick="updateTaskStatus(${task.taskId}, 'DONE')">Xong</button>`}
        <button class="mini-btn danger" onclick="deleteTask(${task.taskId})">Xóa</button>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderGroups() {
  const sidebar = document.getElementById("sidebar-groups-list");
  const groupsList = document.getElementById("groups-list");

  if (sidebar) {
    sidebar.innerHTML = "";
    if (!currentGroups.length) {
      sidebar.innerHTML = `<div class="empty-text">Chưa có nhóm nào</div>`;
    } else {
      currentGroups.forEach((group) => {
        const item = document.createElement("div");
        item.className = "group-item";
        item.onclick = () => openMembers(group.groupId, group.groupName);
        item.innerHTML = `<span class="group-dot"></span><span class="group-name">${escapeHtml(group.groupName || "Nhóm không tên")}</span>`;
        sidebar.appendChild(item);
      });
    }
  }

  if (!groupsList) return;
  groupsList.innerHTML = "";

  if (!currentGroups.length) {
    groupsList.innerHTML = `<div class="empty-text">Chưa có nhóm học nào</div>`;
    return;
  }

  currentGroups.forEach((group) => {
    if (group.status === "ACTIVE") {
      const progress = Number(group.progress || 0);
      const card = document.createElement("div");
      card.className = "group-card";
      card.onclick = () => openMembers(group.groupId, group.groupName);
      card.innerHTML = `
        <div class="group-card-header">
          <div class="group-card-color" style="background: var(--accent-bg)">
            <svg fill="none" viewBox="0 0 24 24" stroke="var(--accent)" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div class="group-card-name">${escapeHtml(group.groupName || "Nhóm không tên")}</div>
          <div class="group-card-subject">${escapeHtml(group.status || "Đang hoạt động")}</div>
        </div>
        <div class="group-card-body">
          <div class="group-card-meta">
            <div class="group-meta-item">${group.membersCount || 0} thành viên</div>
            <div class="group-meta-item">${group.totalTasks || 0} nhiệm vụ</div>
          </div>
          <div class="group-progress">
            <div class="group-progress-fill" style="width: ${progress}%; background: var(--accent)"></div>
          </div>
          <div style="display:flex; justify-content:space-between; margin-top:6px;">
            <span style="font-size:11.5px; color:var(--text3)">${progress}% hoàn thành</span>
            <span style="font-size:11.5px; color:var(--text3)">Hạn: ${formatDate(group.deadline)}</span>
          </div>
          <div class="card-actions-row">
            <button class="mini-btn" onclick="event.stopPropagation(); openMembers(${group.groupId}, '${escapeJs(group.groupName || "Nhóm")}')">Xem thành viên</button>
            <button class="mini-btn danger" onclick="event.stopPropagation(); deleteGroup(${group.groupId})">Xóa nhóm</button>
          </div>
        </div>
      `;
      groupsList.appendChild(card);
    }
  });
}

async function openMembers(groupId, groupName = "") {
  activeGroupId = groupId;
  activeGroupName = groupName;
  showPage("members");
  setText("members-title", `Thành viên – ${groupName || "Nhóm"}`);

  const tbody = document.getElementById("members-list");
  if (tbody)
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:24px;">Đang tải thành viên...</td></tr>`;

  try {
    const res = await apiRequest(`/groups/${groupId}/members`);
    const members = Array.isArray(getData(res)) ? getData(res) : [];
    renderMembers(members);
  } catch (error) {
    if (tbody)
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:24px;">${escapeHtml(error.message)}</td></tr>`;
  }
}

function renderMembers(members) {
  const tbody = document.getElementById("members-list");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!members.length) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:24px;">Nhóm này chưa có thành viên</td></tr>`;
    return;
  }

  members.forEach((member) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>
        <div class="member-cell">
          <div class="member-avatar" style="background:#eef2fd; color:#1a3ab0">${getInitials(member.fullName || member.email || "?")}</div>
          <div>
            <div class="member-full-name">${escapeHtml(member.fullName || "Không có tên")}</div>
            <div class="member-email">${escapeHtml(member.email || "")}</div>
          </div>
        </div>
      </td>
      <td><span class="role-badge tag tag-blue">${escapeHtml(member.role || "Thành viên")}</span></td>
      <td><span style="font-family:'DM Mono', monospace; font-size:13px;">-</span></td>
      <td>
        <div class="score-bar-wrap">
          <div class="score-bar"><div class="score-fill" style="width:${member.contributionScore || 0}%; background:var(--accent)"></div></div>
          <span class="score-num">${member.contributionScore || 0}</span>
        </div>
      </td>
      <td><span class="tag tag-green">Hoạt động</span></td>
    `;
    tbody.appendChild(tr);
  });
}

function fillGroupSelects() {
  const selects = [
    document.getElementById("task-group-select"),
    document.getElementById("assign-group-select"),
  ].filter(Boolean);

  selects.forEach((select) => {
    select.innerHTML = `<option value="">Chọn nhóm</option>`;
    currentGroups.forEach((group) => {
      const option = document.createElement("option");
      option.value = group.groupId;
      option.textContent = group.groupName;
      select.appendChild(option);
    });
  });

  const taskGroupSelect = document.getElementById("task-group-select");
  if (taskGroupSelect?.value) loadMembersForTaskSelect(taskGroupSelect.value);
}

function fillTaskGroupFilter() {
  const select = document.getElementById("task-group-filter");
  if (!select) return;
  const currentValue = select.value;
  select.innerHTML = `<option value="">Tất cả nhóm</option>`;
  currentGroups.forEach((group) => {
    const option = document.createElement("option");
    option.value = group.groupId;
    option.textContent = group.groupName;
    select.appendChild(option);
  });
  if (currentValue) select.value = currentValue;
}

function fillUserSelect() {
  const select = document.getElementById("invite-user-select");
  if (!select) return;
  select.innerHTML = `<option value="">Chọn thành viên</option>`;
  currentUsers
    .filter((user) => user.userId !== currentUser?.userId)
    .forEach((user) => {
      const option = document.createElement("option");
      option.value = user.userId;
      option.textContent = `${user.fullName || user.email} (${user.email})`;
      select.appendChild(option);
    });
}

async function loadMembersForTaskSelect(groupId) {
  const select = document.getElementById("task-assignee-select");
  if (!select) return;
  select.innerHTML = `<option value="">Giao cho tôi</option>`;

  if (!groupId) return;

  try {
    const res = await apiRequest(`/groups/${groupId}/members`);
    const members = Array.isArray(getData(res)) ? getData(res) : [];
    members.forEach((member) => {
      const option = document.createElement("option");
      option.value = member.userId;
      option.textContent = member.fullName || member.email;
      select.appendChild(option);
    });
  } catch (error) {
    console.warn("Không tải được thành viên nhóm", error.message);
  }
}

async function createGroup() {
  const groupName = document.getElementById("group-name")?.value.trim();
  const deadline = document.getElementById("group-deadline")?.value;

  if (!groupName) {
    alert("Vui lòng nhập tên nhóm");
    return;
  }

  try {
    await apiRequest("/groups", {
      method: "POST",
      body: JSON.stringify({
        groupName,
        deadline: deadline ? `${deadline}T23:59:00` : null,
      }),
    });

    document.getElementById("group-name").value = "";
    document.getElementById("group-deadline").value = "";
    closeGroupModal();
    await loadGroups();
    fillGroupSelects();
    fillTaskGroupFilter();
    renderDashboard();
    renderGroups();
    alert("Tạo nhóm thành công");
  } catch (error) {
    alert(error.message || "Không tạo được nhóm");
  }
}

async function addMemberToCurrentGroup() {
  const userId = document.getElementById("invite-user-select")?.value;
  const role = document.getElementById("invite-role-select")?.value || "MEMBER";

  if (!activeGroupId) {
    alert("Vui lòng chọn nhóm trước");
    return;
  }

  if (!userId) {
    alert("Vui lòng chọn thành viên");
    return;
  }

  try {
    await apiRequest(
      `/groups/${activeGroupId}/members?userId=${encodeURIComponent(userId)}&role=${encodeURIComponent(role)}`,
      {
        method: "POST",
      },
    );

    closeInviteMemberModal();
    await loadGroups();
    await openMembers(activeGroupId, activeGroupName);
    renderGroups();
    alert("Thêm thành viên thành công");
  } catch (error) {
    alert(error.message || "Không thêm được thành viên");
  }
}

async function createTask() {
  const taskName = document.getElementById("task-title")?.value.trim();
  const groupId = document.getElementById("task-group-select")?.value;
  const selectedAssigneeId =
    document.getElementById("task-assignee-select")?.value || null;
  const assigneeId = selectedAssigneeId || currentUser?.userId || null;
  const date = document.getElementById("task-deadline")?.value;

  if (!taskName || !groupId) {
    alert("Vui lòng nhập tên công việc và chọn nhóm");
    return;
  }

  const body = {
    taskName,
    groupId: Number(groupId),
    assigneeId: assigneeId ? Number(assigneeId) : null,
    dueDate: date ? `${date}T23:59:00` : null,
    status: "TODO",
  };

  try {
    await apiRequest("/tasks", {
      method: "POST",
      body: JSON.stringify(body),
    });

    closeModal();
    document.getElementById("task-title").value = "";
    document.getElementById("task-description").value = "";
    document.getElementById("task-deadline").value = "";
    await Promise.all([loadTasks(), loadGroups()]);
    fillTaskGroupFilter();
    renderDashboard();
    renderTasks(currentTaskFilter);
    renderGroups();
    alert("Tạo công việc thành công");
  } catch (error) {
    alert(error.message || "Không tạo được công việc");
  }
}

async function updateTaskStatus(taskId, status) {
  try {
    await apiRequest(
      `/tasks/${taskId}/status?status=${encodeURIComponent(status)}`,
      { method: "PUT" },
    );
    await loadTasks();
    renderDashboard();
    renderTasks(currentTaskFilter);
  } catch (error) {
    alert(error.message || "Không cập nhật được trạng thái");
  }
}

async function deleteTask(taskId) {
  if (!confirm("Xóa công việc này?")) return;
  try {
    await apiRequest(`/tasks/${taskId}`, { method: "DELETE" });
    await Promise.all([loadTasks(), loadGroups()]);
    renderDashboard();
    renderTasks(currentTaskFilter);
    renderGroups();
    showToast("Đã xóa công việc");
  } catch (error) {
    alert(error.message || "Không xóa được công việc");
  }
}

async function deleteGroup(groupId) {
  if (
    !confirm(
      "Xóa nhóm này? Các công việc trong nhóm có thể không còn hiển thị.",
    )
  )
    return;
  try {
    await apiRequest(`/groups/${groupId}`, { method: "DELETE" });
    if (String(activeGroupId) === String(groupId)) {
      activeGroupId = null;
      activeGroupName = "";
      setText("members-title", "Thành viên");
      const tbody = document.getElementById("members-list");
      if (tbody)
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:24px;">Chọn một nhóm để xem thành viên</td></tr>`;
    }
    await Promise.all([loadGroups(), loadTasks()]);
    fillGroupSelects();
    fillTaskGroupFilter();
    renderDashboard();
    renderGroups();
    renderTasks(currentTaskFilter);
    showToast("Đã xóa nhóm");
  } catch (error) {
    alert(error.message || "Không xóa được nhóm");
  }
}

async function loadNotifications() {
  try {
    const res = await apiRequest("/notifications");
    currentNotifications = Array.isArray(getData(res)) ? getData(res) : [];
    await loadUnreadNotificationCount();
  } catch (error) {
    console.warn("Không tải được thông báo", error.message);
    currentNotifications = [];
    setText("nav-notification-count", 0);
  }
}

async function loadUnreadNotificationCount() {
  try {
    const res = await apiRequest("/notifications/unread-count");
    const data = getData(res) || {};
    setText("nav-notification-count", data.count || 0);
  } catch (error) {
    const count = currentNotifications.filter(
      (n) => !n.read && !n.isRead,
    ).length;
    setText("nav-notification-count", count);
  }
}

async function loadSubscriptions() {
  await Promise.all([
    loadPlans(),
    loadActiveSubscription(),
    loadTransactions(),
  ]);
}

async function loadPlans() {
  try {
    const res = await apiRequest("/subscriptions/plans");
    currentPlans = Array.isArray(getData(res)) ? getData(res) : [];
  } catch (error) {
    console.warn("Không tải được danh sách gói", error.message);
    currentPlans = [];
  }
}

async function loadActiveSubscription() {
  try {
    const res = await apiRequest("/subscriptions/active");
    currentActiveSubscription = getData(res) || null;
  } catch (error) {
    console.warn("Không tải được gói hiện tại", error.message);
    currentActiveSubscription = null;
  }
}

async function loadTransactions() {
  try {
    const res = await apiRequest("/subscriptions/transactions");
    currentTransactions = Array.isArray(getData(res)) ? getData(res) : [];
  } catch (error) {
    console.warn("Không tải được giao dịch", error.message);
    currentTransactions = [];
  }
}

function renderProfile() {
  if (!currentUser) return;
  const fields = {
    "profile-fullname": currentUser.fullName || "",
    "profile-email": currentUser.email || "",
    "profile-avatar": currentUser.avatarUrl || "",
    "profile-skills": currentUser.skills || "",
    "profile-availability": currentUser.availability || "",
  };
  Object.entries(fields).forEach(([id, value]) => {
    const el = document.getElementById(id);
    if (el && document.activeElement !== el) el.value = value;
  });
}

async function updateProfile() {
  const fullName = document.getElementById("profile-fullname")?.value.trim();
  const avatarUrl = document.getElementById("profile-avatar")?.value.trim();
  const skills = document.getElementById("profile-skills")?.value.trim();
  const availability = document
    .getElementById("profile-availability")
    ?.value.trim();

  if (!fullName) {
    alert("Vui lòng nhập họ tên");
    return;
  }

  try {
    const res = await apiRequest("/users/profile", {
      method: "PUT",
      body: JSON.stringify({ fullName, avatarUrl, skills, availability }),
    });
    currentUser = getData(res);
    await loadCurrentUser();
    renderProfile();
    alert("Cập nhật hồ sơ thành công");
  } catch (error) {
    alert(error.message || "Không cập nhật được hồ sơ");
  }
}

async function deleteMyAccount() {
  if (!confirm("Bạn chắc chắn muốn vô hiệu hóa tài khoản này?")) return;
  try {
    await apiRequest("/users/me", { method: "DELETE" });
    forceLogout();
    alert("Tài khoản đã được vô hiệu hóa");
    window.location.href = "index.html";
  } catch (error) {
    alert(error.message || "Không xóa được tài khoản");
  }
}

function renderNotifications() {
  const list = document.getElementById("notifications-list");
  if (!list) return;
  list.innerHTML = "";

  if (!currentNotifications.length) {
    list.innerHTML = `<div class="empty-text">Chưa có thông báo nào</div>`;
    return;
  }

  currentNotifications.forEach((notification) => {
    const isRead = notification.read ?? notification.isRead;
    const item = document.createElement("div");
    item.className = "activity-item";
    item.innerHTML = `
      <div class="activity-dot" style="background:${isRead ? "#f3f3f3" : "#eef2fd"}; color:${isRead ? "#777" : "#1a3ab0"}; font-size:11px">${isRead ? "✓" : "!"}</div>
      <div class="activity-text">${escapeHtml(notification.content || "Thông báo")}</div>
      <div class="activity-time">${formatDateTime(notification.createdAt)}</div>
      ${isRead ? "" : `<button class="btn btn-ghost" style="padding:4px 8px;font-size:11px" onclick="markNotificationRead(${notification.notificationId})">Đã đọc</button>`}
    `;
    list.appendChild(item);
  });
}

async function markNotificationRead(id) {
  try {
    await apiRequest(`/notifications/${id}/read`, { method: "PUT" });
    await loadNotifications();
    renderNotifications();
  } catch (error) {
    alert(error.message || "Không cập nhật được thông báo");
  }
}

async function markAllNotificationsRead() {
  try {
    await apiRequest("/notifications/read-all", { method: "PUT" });
    await loadNotifications();
    renderNotifications();
  } catch (error) {
    alert(error.message || "Không cập nhật được thông báo");
  }
}

function renderPlans() {
  renderActiveSubscription();
  renderPlanCards();
  renderTransactions();
}

function renderActiveSubscription() {
  const box = document.getElementById("active-subscription-box");
  if (!box) return;

  if (!currentActiveSubscription) {
    box.innerHTML = `<div class="empty-text">Chưa có gói đăng ký</div>`;
    return;
  }

  box.innerHTML = `
    <div class="active-plan-box">
      <div class="active-plan-top">
        <div>
          <div class="active-plan-status">✨ ĐANG SỬ DỤNG</div>
          <div class="active-plan-title">
            ${escapeHtml(currentActiveSubscription.planName || "Gói hiện tại")}
          </div>
          <div class="active-plan-price">
            ${formatMoney(currentActiveSubscription.price, currentActiveSubscription.currency)}
            <span>/ gói</span>
          </div>
        </div>
        <div class="active-plan-chip">
          Hết hạn: ${formatDate(currentActiveSubscription.endDate)}
        </div>
      </div>

      <div class="active-plan-meta">
        <div class="active-plan-chip">Trạng thái: ${escapeHtml(currentActiveSubscription.status || "ACTIVE")}</div>
        <div class="active-plan-chip">Thanh toán thành công</div>
        <div class="active-plan-chip">Ưu đãi đang áp dụng</div>
      </div>

      <div class="active-plan-desc">
        ${escapeHtml(currentActiveSubscription.features || "Bạn đang sử dụng gói dịch vụ để quản lý học nhóm hiệu quả hơn.")}
      </div>
    </div>
  `;
}

function renderPlanCards() {
  const list = document.getElementById("plans-list");
  if (!list) return;

  list.innerHTML = "";

  if (!currentPlans || !currentPlans.length) {
    list.innerHTML = `<div class="empty-text">Backend chưa có gói nào trong database</div>`;
    return;
  }

  currentPlans.forEach((plan, index) => {
    const price = Number(plan.price || 0);
    const durationDays = plan.durationDays || 0;
    const currency = plan.currency || "VND";

    const visuals = [
      {
        theme: "plan-theme-basic",
        badges: ["Mới", "Tiết kiệm"],
        discount: 15,
        subtitle: "Phù hợp để bắt đầu trải nghiệm",
        bestValue: "",
      },
      {
        theme: "plan-theme-popular",
        badges: ["Hot", "Phổ biến", "Bán chạy"],
        discount: 30,
        subtitle: "Lựa chọn được nhiều người dùng nhất",
        bestValue: "BEST VALUE",
      },
      {
        theme: "plan-theme-premium",
        badges: ["Pro", "Ưu đãi lớn"],
        discount: 40,
        subtitle: "Tối ưu học nhóm và quản lý task",
        bestValue: "",
      },
      {
        theme: "plan-theme-elite",
        badges: ["VIP", "Cao cấp"],
        discount: 50,
        subtitle: "Trải nghiệm cao cấp nhất",
        bestValue: "",
      },
    ];

    const visual = visuals[index % visuals.length];
    const oldPrice =
      price > 0 ? Math.round(price / (1 - visual.discount / 100)) : 0;

    const rawFeatures = plan.features || "";
    const features = rawFeatures
      ? rawFeatures
          .split(/[,;\n]+/)
          .map((item) => item.trim())
          .filter(Boolean)
      : [
          "Quản lý công việc nhóm dễ dàng",
          "Theo dõi deadline nhanh chóng",
          "Nhận thông báo và cập nhật liên tục",
        ];

    const card = document.createElement("div");
    card.className = `plan-card ${visual.theme} ${index === 1 ? "plan-highlight-ring" : ""}`;

    card.innerHTML = `
      ${visual.bestValue ? `<div class="plan-best-value">${visual.bestValue}</div>` : ""}

      <div class="plan-badges">
        ${visual.badges.map((badge) => `<span class="plan-badge">${badge}</span>`).join("")}
      </div>

      <div class="plan-name">${escapeHtml(plan.planName || "Gói đăng ký")}</div>
      <div class="plan-sub">${escapeHtml(visual.subtitle)}</div>

      <div class="plan-discount-wrap">
        <div>
          <div class="plan-discount-big">-${visual.discount}%</div>
          <div class="plan-discount-label">Ưu đãi nổi bật</div>
        </div>

        <div class="plan-price-box">
          <div class="plan-old-price">
            ${oldPrice ? formatMoney(oldPrice, currency) : ""}
          </div>
          <div class="plan-price">
            ${formatMoney(price, currency)}
          </div>
          <div class="plan-price-unit">/ ${durationDays} ngày</div>
        </div>
      </div>

      <div class="plan-meta-row">
        <span class="plan-meta-chip">${durationDays} ngày sử dụng</span>
        <span class="plan-meta-chip">${escapeHtml(currency)}</span>
        <span class="plan-meta-chip">Kích hoạt nhanh</span>
      </div>

      <div class="plan-feature-list">
        ${features
          .slice(0, 4)
          .map(
            (feature) => `
          <div class="plan-feature">
            <span class="plan-feature-icon">✓</span>
            <span>${escapeHtml(feature)}</span>
          </div>
        `,
          )
          .join("")}
      </div>

      <button class="plan-cta" onclick="subscribePlan(${plan.planId}, ${price})">
        Đăng ký ngay
      </button>
    `;

    list.appendChild(card);
  });
}
async function subscribePlan(planId, amount) {
  if (!confirm("Xác nhận đăng ký gói này?")) return;
  try {
    await apiRequest("/subscriptions/subscribe", {
      method: "POST",
      body: JSON.stringify({ planId, amount, paymentMethod: "BANK_TRANSFER" }),
    });
    await loadSubscriptions();
    renderPlans();
    alert("Đăng ký gói thành công");
  } catch (error) {
    alert(error.message || "Không đăng ký được gói");
  }
}

function renderTransactions() {
  const list = document.getElementById("transactions-list");
  if (!list) return;
  list.innerHTML = "";

  if (!currentTransactions.length) {
    list.innerHTML = `<div class="empty-text">Chưa có giao dịch nào</div>`;
    return;
  }

  currentTransactions.forEach((transaction) => {
    const item = document.createElement("div");
    item.className = "activity-item";
    item.innerHTML = `
      <div class="activity-dot" style="background:#e8f5ee; color:#1a7a4a; font-size:11px">$</div>
      <div class="activity-text"><strong>${escapeHtml(transaction.planName || "Gói")}</strong> – ${formatMoney(transaction.amount, "VND")} – ${escapeHtml(transaction.paymentMethod || "")}</div>
      <div class="activity-time">${formatDateTime(transaction.createdAt)}</div>
      <span class="tag tag-green">${escapeHtml(transaction.status || "SUCCESS")}</span>
    `;
    list.appendChild(item);
  });
}

async function applyAssign() {
  const groupId = document.getElementById("assign-group-select")?.value;
  const resultList = document.getElementById("assign-result-list");

  if (!groupId) {
    alert("Vui lòng chọn nhóm trước khi phân công tự động");
    return;
  }

  if (resultList) {
    resultList.innerHTML = `<div class="empty-text">Đang phân công tự động...</div>`;
  }

  try {
    if (!currentTasks || currentTasks.length === 0) {
      await loadTasks();
    }

    const tasksInGroup = currentTasks.filter((task) => {
      const sameGroup = String(task.groupId) === String(groupId);
      const notDone = !isDone(task);
      return sameGroup && notDone;
    });

    if (!tasksInGroup.length) {
      if (resultList) {
        resultList.innerHTML = `<div class="empty-text">Nhóm này chưa có công việc cần phân công</div>`;
      }
      return;
    }

    const assignedResults = [];

    for (const task of tasksInGroup) {
      const assignedTask = await apiRequest(
        `/assignments/tasks/${task.taskId}/auto-assign`,
        {
          method: "POST",
        },
      );

      assignedResults.push(getData(assignedTask));
    }

    if (resultList) {
      resultList.innerHTML = assignedResults
        .map((task) => {
          const assigneeName =
            task.assignee?.fullName ||
            task.assigneeName ||
            `User ID ${task.assigneeId || task.assignee?.userId || "?"}`;

          return `
          <div style="
            background: #fff;
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 10px;
          ">
            <div style="font-weight: 700; color: var(--text); margin-bottom: 4px;">
              ${escapeHtml(task.taskName || "Công việc")}
            </div>
            <div style="font-size: 12px; color: var(--text2);">
              Đã giao cho: <strong>${escapeHtml(assigneeName)}</strong>
            </div>
            <div style="font-size: 12px; color: var(--accent-text); margin-top: 4px;">
              Điểm phù hợp: ${task.assignmentScore || "?"}/100
            </div>
            <div style="font-size: 12px; color: var(--text2); margin-top: 4px;">
              ${escapeHtml(task.assignmentReason || "Đã phân công tự động thành công.")}
            </div>
          </div>
        `;
        })
        .join("");
    }

    await loadTasks();
    renderDashboard();
    renderTasks(currentTaskFilter);

    alert("Phân công tự động thành công!");
  } catch (error) {
    console.error(error);
    if (resultList) {
      resultList.innerHTML = `
        <div class="empty-text" style="color: red;">
          Lỗi phân công: ${escapeHtml(error.message || "Không thể phân công tự động")}
        </div>
      `;
    }
    alert(error.message || "Không thể phân công tự động");
  }
}

function isDone(task) {
  return ["DONE", "COMPLETED", "HOAN_THANH"].includes(
    String(task.status || "").toUpperCase(),
  );
}

function isDoing(task) {
  return ["DOING", "IN_PROGRESS", "PROCESSING", "DANG_LAM"].includes(
    String(task.status || "").toUpperCase(),
  );
}

function statusText(status) {
  const s = String(status || "TODO").toUpperCase();
  if (["DONE", "COMPLETED", "HOAN_THANH"].includes(s)) return "Hoàn thành";
  if (["DOING", "IN_PROGRESS", "PROCESSING", "DANG_LAM"].includes(s))
    return "Đang làm";
  return "Chưa làm";
}

function formatDate(value) {
  if (!value) return "Chưa có hạn";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Chưa có hạn";
  return date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
}

function formatDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatMoney(value, currency = "VND") {
  const amount = Number(value || 0);
  try {
    return amount.toLocaleString("vi-VN") + " " + (currency || "VND");
  } catch (e) {
    return `${amount} ${currency || "VND"}`;
  }
}

function getInitials(name = "") {
  return (
    String(name)
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .map((word) => word[0])
      .join("")
      .substring(0, 2)
      .toUpperCase() || "?"
  );
}

function escapeJs(value = "") {
  return String(value)
    .replaceAll("\\", "\\\\")
    .replaceAll("'", "\\'")
    .replaceAll('"', '\\"');
}

function showToast(message) {
  const notif = document.createElement("div");
  notif.style.cssText =
    "position:fixed;bottom:24px;right:24px;background:#1C1A17;color:#fff;padding:12px 18px;border-radius:10px;font-size:13.5px;z-index:999;box-shadow:0 4px 20px rgba(0,0,0,0.2);";
  notif.textContent = message;
  document.body.appendChild(notif);
  setTimeout(() => notif.remove(), 2500);
}

function setText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function escapeHtml(value = "") {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

document.querySelectorAll(".modal-overlay").forEach((overlay) => {
  overlay.addEventListener("click", function (e) {
    if (e.target === this) this.classList.remove("open");
  });
});

document.addEventListener("DOMContentLoaded", () => {
  const isDashboardPage =
    window.location.pathname.includes("dashboard.html") ||
    document.getElementById("page-dashboard");
  if (isDashboardPage) initDashboardPage();
});
