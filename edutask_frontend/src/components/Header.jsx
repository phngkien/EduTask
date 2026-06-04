import React, { useState } from 'react';
import { Search, Bell, LogOut, BookOpen, User, Check, CreditCard } from 'lucide-react';

export default function Header({ user, notifications, onMarkNotificationRead, onSearch, onProfileClick, onSubscriptionClick, onLogout }) {
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const [searchVal, setSearchVal] = useState('');

  const handleSearchChange = (e) => {
    setSearchVal(e.target.value);
    if (onSearch) onSearch(e.target.value);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (onSearch) onSearch(searchVal);
  };

  const unreadCount = notifications.length;

  return (
    <nav className="header-nav">
      <div className="header-left">
        <div className="logo-container">
          <div className="logo-icon">
            <BookOpen size={20} />
          </div>
          <span className="logo-text">EduTask</span>
        </div>
        <div className="nav-links">
          <span className="nav-item active">Bảng điều khiển</span>
          <span className="nav-item">Không gian làm việc</span>
          <span className="nav-item">Lịch biểu</span>
        </div>
      </div>

      <div className="header-center">
        <form onSubmit={handleSearchSubmit} className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Tìm kiếm nhóm hoặc công việc..."
            value={searchVal}
            onChange={handleSearchChange}
          />
          <button type="submit" className="search-button">
            <Search size={18} />
          </button>
        </form>
      </div>

      <div className="header-right">
        {/* Notifications Icon and Dropdown */}
        <div style={{ position: 'relative' }}>
          <button 
            className="icon-btn" 
            onClick={() => {
              setShowNotifications(!showNotifications);
              setShowProfileDropdown(false);
            }}
            title="Thông báo"
          >
            <Bell size={20} />
            {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
          </button>

          {showNotifications && (
            <div className="dropdown-panel notifications-dropdown">
              <div className="dropdown-header">
                <h3>Thông báo</h3>
                <span className="unread-lbl">{unreadCount} chưa đọc</span>
              </div>
              <div className="dropdown-body">
                {notifications.length === 0 ? (
                  <p className="no-notifications">Bạn không có thông báo chưa đọc nào.</p>
                ) : (
                  notifications.map((notif) => (
                    <div key={notif.notificationId} className="notification-item">
                      <p className="notif-content">{notif.content}</p>
                      <button 
                        className="btn-mark-read" 
                        onClick={() => onMarkNotificationRead(notif.notificationId)}
                        title="Đánh dấu đã đọc"
                      >
                        <Check size={14} />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User Profile and Dropdown */}
        <div style={{ position: 'relative' }}>
          <div 
            className="user-profile" 
            onClick={() => {
              setShowProfileDropdown(!showProfileDropdown);
              setShowNotifications(false);
            }}
          >
            <img 
              className="avatar" 
              src={user.avatarUrl || "https://api.dicebear.com/7.x/adventurer/svg?seed=default"} 
              alt={user.fullName} 
            />
            <span className="username">{user.fullName}</span>
          </div>

          {showProfileDropdown && (
            <div className="dropdown-panel profile-dropdown">
              <div className="profile-dropdown-info">
                <p className="prof-name">{user.fullName}</p>
                <p className="prof-email">{user.email}</p>
              </div>
              <div className="profile-dropdown-details">
                {user.skills && (
                  <p className="prof-skills"><strong>Kỹ năng:</strong> {user.skills}</p>
                )}
                {user.availability && (
                  <p className="prof-avail"><strong>Lịch trống:</strong> {user.availability}</p>
                )}
              </div>
              <button className="dropdown-item" onClick={() => { setShowProfileDropdown(false); onProfileClick(); }}>
                <User size={16} />
                <span>Thông tin cá nhân</span>
              </button>
              <button className="dropdown-item" onClick={() => { setShowProfileDropdown(false); onSubscriptionClick(); }}>
                <CreditCard size={16} />
                <span>Nâng cấp gói cước</span>
              </button>
              <div className="dropdown-divider"></div>
              <button className="dropdown-item logout-btn" onClick={() => { setShowProfileDropdown(false); onLogout(); }}>
                <LogOut size={16} />
                <span>Đăng xuất</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}

