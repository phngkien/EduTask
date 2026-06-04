import React, { useState } from 'react';
import { api } from '../services/api';
import { BookOpen, User, Mail, Lock, LogIn, UserPlus } from 'lucide-react';

export default function AuthView({ onAuthSuccess }) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!email || !password || (!isLogin && !fullName)) {
      setError('Vui lòng điền đầy đủ thông tin');
      setLoading(false);
      return;
    }

    try {
      let data;
      if (isLogin) {
        data = await api.login(email, password);
      } else {
        data = await api.register(fullName, email, password);
      }
      onAuthSuccess(data);
    } catch (err) {
      setError(err.message || 'Đã xảy ra lỗi, vui lòng thử lại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-view-container">
      {/* Decorative SVG backgrounds */}
      <div className="auth-decorator-1">
        <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
          <path fill="#2563EB" d="M47.7,-64.8C61.4,-57.4,71.8,-42.7,77.4,-26.6C83,-10.4,83.9,7.1,78.2,22.7C72.6,38.2,60.5,51.8,46.1,61C31.7,70.2,15.9,75,-0.9,76.3C-17.7,77.5,-35.4,75.2,-49.5,65.9C-63.5,56.7,-74,40.4,-78.7,22.8C-83.3,5.1,-82.1,-13.9,-74.6,-29.7C-67.1,-45.6,-53.4,-58.3,-38.3,-65.2C-23.2,-72,-6.6,-73.1,10.6,-74.8Z" transform="translate(100 100)" />
        </svg>
      </div>
      
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">
            <BookOpen size={24} />
          </div>
          <h1 className="auth-title">EduTask</h1>
          <p className="auth-subtitle">
            {isLogin 
              ? 'Đăng nhập để vào Không gian học tập nhóm' 
              : 'Tạo tài khoản để quản lý bài tập và kết nối nhóm'}
          </p>
        </div>

        {error && <div className="auth-error-banner">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          {!isLogin && (
            <div className="auth-input-group">
              <label className="auth-label">Họ và Tên</label>
              <div className="auth-input-wrapper">
                <User className="auth-input-icon" size={18} />
                <input
                  type="text"
                  className="auth-input"
                  placeholder="Nguyễn Văn A"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  disabled={loading}
                />
              </div>
            </div>
          )}

          <div className="auth-input-group">
            <label className="auth-label">Email</label>
            <div className="auth-input-wrapper">
              <Mail className="auth-input-icon" size={18} />
              <input
                type="email"
                className="auth-input"
                placeholder="example@edutask.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading}
              />
            </div>
          </div>

          <div className="auth-input-group">
            <label className="auth-label">Mật khẩu</label>
            <div className="auth-input-wrapper">
              <Lock className="auth-input-icon" size={18} />
              <input
                type="password"
                className="auth-input"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
              />
            </div>
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading}>
            {loading ? (
              <span className="auth-spinner"></span>
            ) : isLogin ? (
              <>
                <LogIn size={18} />
                <span>Đăng Nhập</span>
              </>
            ) : (
              <>
                <UserPlus size={18} />
                <span>Đăng Ký Tài Khoản</span>
              </>
            )}
          </button>
        </form>

        <div className="auth-toggle">
          {isLogin ? (
            <p>
              Chưa có tài khoản?{' '}
              <button 
                type="button" 
                onClick={() => { setIsLogin(false); setError(''); }}
                className="auth-toggle-link"
                disabled={loading}
              >
                Đăng ký ngay
              </button>
            </p>
          ) : (
            <p>
              Đã có tài khoản?{' '}
              <button 
                type="button" 
                onClick={() => { setIsLogin(true); setError(''); }}
                className="auth-toggle-link"
                disabled={loading}
              >
                Đăng nhập
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
