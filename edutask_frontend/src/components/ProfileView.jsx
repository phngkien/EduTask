import React, { useState } from 'react';
import { api } from '../services/api';
import { ArrowLeft, User, Briefcase, Calendar, Save, Sparkles } from 'lucide-react';

export default function ProfileView({ user, onProfileUpdate, onBack }) {
  const [fullName, setFullName] = useState(user.fullName || '');
  const [skills, setSkills] = useState(user.skills || '');
  const [availability, setAvailability] = useState(user.availability || '');
  const [avatarUrl, setAvatarUrl] = useState(user.avatarUrl || '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    if (!fullName.trim()) {
      setError('Họ và tên không được để trống');
      return;
    }

    setSaving(true);
    try {
      const data = await api.updateProfile({
        fullName,
        skills,
        availability,
        avatarUrl
      });
      // Gọi callback để cập nhật state User ở App.jsx
      onProfileUpdate(data);
      alert('Cập nhật hồ sơ cá nhân thành công!');
      onBack();
    } catch (err) {
      setError(err.message || 'Cập nhật thất bại, vui lòng thử lại');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="profile-view-panel">
      <div className="profile-header-row">
        <button className="btn-back" onClick={onBack}>
          <ArrowLeft size={18} />
          <span>Quay lại</span>
        </button>
        <h2 className="profile-view-title">Hồ sơ cá nhân</h2>
      </div>

      <div className="profile-view-content">
        <div className="profile-left-column">
          <div className="profile-avatar-card">
            <img 
              className="profile-big-avatar" 
              src={avatarUrl || "https://api.dicebear.com/7.x/adventurer/svg?seed=default"} 
              alt={fullName} 
            />
            <h3>{fullName || 'Chưa đặt tên'}</h3>
            <p>{user.email}</p>
            <span className="profile-role-badge">{user.role || 'USER'}</span>

            <div className="avatar-presets">
              <h4>Thay đổi hình đại diện (Preset)</h4>
              <div className="presets-row">
                {['Duc', 'Nam', 'Trang', 'Viet', 'Ly'].map((seed) => {
                  const url = `https://api.dicebear.com/7.x/adventurer/svg?seed=${seed}`;
                  return (
                    <img 
                      key={seed} 
                      className={`preset-thumb ${avatarUrl === url ? 'selected' : ''}`}
                      src={url} 
                      alt={seed}
                      onClick={() => setAvatarUrl(url)}
                    />
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        <div className="profile-right-column">
          <div className="profile-edit-card">
            <div className="card-header">
              <Sparkles size={20} className="primary-color-icon" />
              <h3>Cập nhật thông tin chi tiết</h3>
            </div>

            {error && <div className="profile-error-banner">{error}</div>}

            <form onSubmit={handleSubmit} className="profile-form">
              <div className="form-group">
                <label className="form-label">Họ và tên</label>
                <div className="input-with-icon">
                  <User size={18} className="input-field-icon" />
                  <input
                    type="text"
                    className="form-input"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Kỹ năng chuyên môn</label>
                <div className="input-with-icon">
                  <Briefcase size={18} className="input-field-icon" />
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Ví dụ: Java, Spring Boot, React, Figma"
                    value={skills}
                    onChange={(e) => setSkills(e.target.value)}
                    disabled={saving}
                  />
                </div>
                <p className="form-help-text">Nhập các kỹ năng của bạn, phân tách nhau bằng dấu phẩy.</p>
              </div>

              <div className="form-group">
                <label className="form-label">Lịch trình rảnh rỗi (Availability)</label>
                <div className="input-with-icon">
                  <Calendar size={18} className="input-field-icon" />
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Ví dụ: Full-time, Sáng thứ 2, Chiều thứ 7"
                    value={availability}
                    onChange={(e) => setAvailability(e.target.value)}
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="profile-form-footer">
                <button type="button" className="btn-secondary" onClick={onBack} disabled={saving}>
                  Hủy bỏ
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? (
                    <span className="spinner" style={{ width: '1.2rem', height: '1.2rem' }}></span>
                  ) : (
                    <>
                      <Save size={18} />
                      <span>Lưu thay đổi</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
