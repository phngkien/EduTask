// src/services/api.js — EduTask API Service (kết nối Spring Boot backend)

const API_BASE_URL = "http://localhost:8080/api";

// ===== Token helpers =====
const getToken = () => localStorage.getItem("accessToken");
const getRefreshToken = () => localStorage.getItem("refreshToken");

const saveTokens = (accessToken, refreshToken) => {
  localStorage.setItem("accessToken", accessToken);
  if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
};

const getHeaders = () => ({
  "Content-Type": "application/json",
  ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
});

// ===== Core fetch wrapper với auto refresh =====
let isRefreshing = false;
let refreshQueue = [];

async function apiFetch(url, options = {}, retry = true) {
  const res = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers: { ...getHeaders(), ...(options.headers || {}) },
  });

  if (res.status === 401 && retry) {
    // Thử refresh token
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const rToken = getRefreshToken();
        if (!rToken) throw new Error("No refresh token");
        const refreshRes = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: rToken }),
        });
        if (!refreshRes.ok) throw new Error("Refresh failed");
        const data = await refreshRes.json();
        saveTokens(data.data.accessToken, data.data.refreshToken);
        isRefreshing = false;
        refreshQueue.forEach((cb) => cb());
        refreshQueue = [];
        // Retry original
        return apiFetch(url, options, false);
      } catch {
        isRefreshing = false;
        clearTokens();
        window.dispatchEvent(new CustomEvent("auth:logout"));
        throw new Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
      }
    } else {
      return new Promise((resolve, reject) => {
        refreshQueue.push(() =>
          apiFetch(url, options, false).then(resolve).catch(reject)
        );
      });
    }
  }

  const json = await res.json().catch(() => null);

  if (!res.ok) {
    const msg =
      json?.message || json?.error || `Lỗi ${res.status}: ${res.statusText}`;
    throw new Error(msg);
  }

  return json?.data !== undefined ? json.data : json;
}

// ===== API object =====
export const api = {
  // ── Auth ──────────────────────────────────────────────────────
  register: async (fullName, email, password) => {
    const res = await fetch(`${API_BASE_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fullName, email, password }),
    });
    const json = await res.json().catch(() => null);
    if (!res.ok) throw new Error(json?.message || "Đăng ký thất bại");
    const { accessToken, refreshToken, ...user } = json.data;
    saveTokens(accessToken, refreshToken);
    localStorage.setItem("user", JSON.stringify(user));
    return json.data;
  },

  login: async (email, password) => {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: email, password }),
    });
    const json = await res.json().catch(() => null);
    if (!res.ok) throw new Error(json?.message || "Đăng nhập thất bại");
    const { accessToken, refreshToken, ...user } = json.data;
    saveTokens(accessToken, refreshToken);
    localStorage.setItem("user", JSON.stringify(user));
    return json.data;
  },

  logout: async () => {
    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } catch (_) {
      // ignore
    } finally {
      clearTokens();
      window.dispatchEvent(new CustomEvent("auth:logout"));
    }
  },

  // ── Users ─────────────────────────────────────────────────────
  getCurrentUser: () => apiFetch("/users/me"),

  updateProfile: (data) =>
    apiFetch("/users/profile", {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  getAllUsers: () => apiFetch("/users"),

  // ── Groups ────────────────────────────────────────────────────
  getGroups: () => apiFetch("/groups/my"),

  getAllGroups: () => apiFetch("/groups"),

  getGroupById: (id) => apiFetch(`/groups/${id}`),

  createGroup: (data) =>
    apiFetch("/groups", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  getGroupMembers: (groupId) => apiFetch(`/groups/${groupId}/members`),

  addMemberToGroup: (groupId, userId, role = "MEMBER") =>
    apiFetch(`/groups/${groupId}/members?userId=${userId}&role=${role}`, {
      method: "POST",
    }),

  deleteGroup: (groupId) =>
    apiFetch(`/groups/${groupId}`, { method: "DELETE" }),

  // ── Tasks ─────────────────────────────────────────────────────
  getTasks: () => apiFetch("/tasks/my"),

  getAllTasks: () => apiFetch("/tasks"),

  getTasksByGroup: (groupId) => apiFetch(`/tasks/group/${groupId}`),

  createTask: (data) =>
    apiFetch("/tasks", {
      method: "POST",
      body: JSON.stringify({
        taskName: data.taskName,
        groupId: Number(data.groupId),
        assigneeId: data.assigneeId || null,
        dueDate: data.dueDate || null,
        status: data.status || "todo",
      }),
    }),

  updateTaskStatus: (taskId, status) =>
    apiFetch(`/tasks/${taskId}/status?status=${status}`, { method: "PUT" }),

  deleteTask: (taskId) =>
    apiFetch(`/tasks/${taskId}`, { method: "DELETE" }),

  // ── Notifications ─────────────────────────────────────────────
  getNotifications: (unreadOnly = false) =>
    apiFetch(`/notifications?unreadOnly=${unreadOnly}`),

  getUnreadCount: () => apiFetch("/notifications/unread-count"),

  markNotificationRead: (id) =>
    apiFetch(`/notifications/${id}/read`, { method: "PUT" }),

  markAllNotificationsRead: () =>
    apiFetch("/notifications/read-all", { method: "PUT" }),

  // ── Subscriptions ─────────────────────────────────────────────
  getPlans: () => apiFetch("/subscriptions/plans"),

  getActiveSubscription: () => apiFetch("/subscriptions/active"),

  subscribe: (planId, amount, paymentMethod = "ONLINE") =>
    apiFetch("/subscriptions/subscribe", {
      method: "POST",
      body: JSON.stringify({ planId, amount, paymentMethod }),
    }),

  getTransactions: () => apiFetch("/subscriptions/transactions"),
};
