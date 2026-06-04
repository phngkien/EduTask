import React from 'react';
import { Plus, Award, Calendar, Layers } from 'lucide-react';
import heroImg from '../assets/hero.png';

export default function HeroBanner({ user, onCreateGroupClick }) {
  return (
    <div className="hero-banner">
      <div className="hero-body">
        <div className="hero-text">
          <h1 className="hero-title">
            Quản lý & Bứt phá trong <span>Làm việc nhóm</span>
          </h1>
          <p className="hero-subtitle">
            Chào mừng quay trở lại, <strong>{user.fullName}</strong>! Theo dõi tiến độ, tích lũy điểm đóng góp và quản lý thời hạn bài tập cùng nhóm học tập một cách dễ dàng.
          </p>
          <button className="btn-primary" onClick={onCreateGroupClick}>
            <Plus size={20} />
            <span>Tạo nhóm mới</span>
          </button>
        </div>
        <div className="hero-illustrations">
          <img src={heroImg} className="hero-img" alt="Minh họa học tập cộng tác" />
        </div>
      </div>

      <div className="features-grid">
        <div className="feature-card">
          <div className="feature-icon-container">
            <Layers size={22} />
          </div>
          <div>
            <h3>Tiến độ trực quan</h3>
            <p>Theo dõi thanh tiến độ và các mốc quan trọng của dự án theo thời gian thực.</p>
          </div>
        </div>

        <div className="feature-card">
          <div className="feature-icon-container">
            <Award size={22} />
          </div>
          <div>
            <h3>Đóng góp công bằng</h3>
            <p>Tích lũy điểm số đóng góp tương ứng khi bạn hoàn thành tốt các nhiệm vụ được giao.</p>
          </div>
        </div>

        <div className="feature-card">
          <div className="feature-icon-container">
            <Calendar size={22} />
          </div>
          <div>
            <h3>Đồng bộ hạn chót</h3>
            <p>Tích hợp thời hạn công việc trực tiếp vào hệ thống nhắc nhở thông minh.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

