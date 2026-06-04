import React, { useState } from 'react';
import { ArrowLeft, UserPlus, Plus, Award, ShieldAlert, Calendar, CheckSquare } from 'lucide-react';

export default function GroupDetailsView({ 
  group, 
  members, 
  tasks, 
  onBack, 
  onAddMember, 
  onAddTaskClick, 
  onUpdateTaskStatus 
}) {
  const [showAddMemberForm, setShowAddMemberForm] = useState(false);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [newMemberRole, setNewMemberRole] = useState('MEMBER');

  const handleAddMemberSubmit = (e) => {
    e.preventDefault();
    if (!newMemberEmail.trim()) return;
    
    // Generate a mock ID or translate email to mock ID
    const fakeId = Math.floor(Math.random() * 1000) + 10;
    onAddMember(group.groupId, fakeId, newMemberRole);
    setNewMemberEmail('');
    setShowAddMemberForm(false);
    alert(`Đã gửi lời mời tham gia nhóm đến thành viên (${newMemberEmail}) thành công!`);
  };

  const getProgressColor = (progress) => {
    if (progress >= 80) return 'linear-gradient(90deg, #10b981 0%, #059669 100%)';
    if (progress >= 40) return 'linear-gradient(90deg, #3b82f6 0%, #2563eb 100%)';
    return 'linear-gradient(90deg, #f59e0b 0%, #ea580c 100%)';
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'todo': return 'Cần làm';
      case 'in_progress': return 'Đang làm';
      case 'done': return 'Hoàn thành';
      default: return status;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    if (dateStr === "Tomorrow" || dateStr === "Ngày mai") return "Ngày mai";
    if (dateStr.includes('T')) {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    return dateStr;
  };

  const rotateStatus = (task) => {
    const statusCycle = ['todo', 'in_progress', 'done'];
    const currentIndex = statusCycle.indexOf(task.status);
    const nextIndex = (currentIndex + 1) % statusCycle.length;
    onUpdateTaskStatus(task.taskId, statusCycle[nextIndex]);
  };

  return (
    <div className="group-details-panel">
      <div className="detail-header-row">
        <button className="btn-back" onClick={onBack}>
          <ArrowLeft size={16} />
          <span>Quay lại Bảng điều khiển</span>
        </button>

        <div className="action-row">
          <button className="btn-add-inline" onClick={() => setShowAddMemberForm(!showAddMemberForm)}>
            <UserPlus size={16} />
            <span>Mời thành viên</span>
          </button>
          <button className="btn-open" onClick={() => onAddTaskClick(group.groupId)}>
            <Plus size={16} />
            <span>Thêm việc</span>
          </button>
        </div>
      </div>

      {showAddMemberForm && (
        <form onSubmit={handleAddMemberSubmit} className="add-member-inline-form">
          <h4>Mời thành viên nhóm mới</h4>
          <div className="form-row">
            <input
              type="email"
              placeholder="Nhập email thành viên (Ví dụ: hung@edutask.com)"
              value={newMemberEmail}
              onChange={(e) => setNewMemberEmail(e.target.value)}
              required
              className="form-input"
            />
            <select
              value={newMemberRole}
              onChange={(e) => setNewMemberRole(e.target.value)}
              className="form-input"
            >
              <option value="MEMBER">Thành viên</option>
              <option value="ADMIN">Quản trị</option>
            </select>
            <button type="submit" className="btn-primary" style={{ padding: '0.5rem 1.2rem' }}>
              Thêm thành viên
            </button>
            <button type="button" className="btn-secondary" onClick={() => setShowAddMemberForm(false)}>
              Hủy
            </button>
          </div>
        </form>
      )}

      <div className="detail-title-block">
        <h2 className="detail-title">{group.groupName}</h2>
        <div className="detail-subtitle-row">
          <p className="group-owner">
            Quản trị nhóm: <strong>{group.creator?.fullName || 'Phó Ngọc Mai'}</strong>
          </p>
          <span className="dot-divider">•</span>
          <p className="group-deadline">
            Hạn chót dự án: <strong>{formatDate(group.deadline)}</strong>
          </p>
        </div>

        {/* Progress Tracker */}
        <div className="detail-progress-wrapper" style={{ marginTop: '1rem', maxWidth: '30rem' }}>
          <p className="form-label" style={{ marginBottom: '0.4rem', display: 'flex', justifyContent: 'space-between' }}>
            <span>Tiến độ dự án</span>
            <span style={{ fontWeight: 800 }}>{group.progress || 0}%</span>
          </p>
          <div className="progress-track" style={{ height: '12px' }}>
            <div 
              className="progress-bar" 
              style={{ 
                width: `${group.progress || 0}%`,
                background: getProgressColor(group.progress || 0)
              }}
            />
          </div>
        </div>
      </div>

      {/* Grid: Members & Tasks */}
      <div className="dashboard-grid" style={{ gridTemplateColumns: '1.2fr 1fr', gap: '2rem' }}>
        
        {/* Members Column */}
        <div className="dashboard-column">
          <h3 className="members-section-title">Danh sách nhóm & Điểm đóng góp</h3>
          <div className="members-table-container">
            <table className="members-table">
              <thead>
                <tr>
                  <th>Họ và tên</th>
                  <th>Vai trò</th>
                  <th>Điểm đóng góp</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member, index) => (
                  <tr key={index}>
                    <td>
                      <div className="member-row-avatar">
                        <img 
                          className="avatar" 
                          src={member.user.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${member.user.fullName}`} 
                          alt={member.user.fullName} 
                        />
                        <span>{member.user.fullName}</span>
                      </div>
                    </td>
                    <td>
                      <span className={`role-badge ${member.role.toLowerCase()}`}>
                        {member.role === 'ADMIN' ? 'Quản trị' : 'Thành viên'}
                      </span>
                    </td>
                    <td>
                      <span className="score-badge">
                        {member.contributionScore} điểm
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Workspace Tasks Column */}
        <div className="dashboard-column">
          <h3 className="members-section-title">Nhiệm vụ của nhóm</h3>
          <div className="tasks-list" style={{ border: '1px solid var(--border-color)', borderRadius: '16px', padding: '1rem', backgroundColor: '#fff' }}>
            {tasks.length === 0 ? (
              <p className="no-notifications" style={{ textAlign: 'center', padding: '2rem 0' }}>Không có nhiệm vụ nào được giao trong nhóm này.</p>
            ) : (
              tasks.map((task) => (
                <div key={task.taskId} className={`task-item ${task.status === 'done' ? 'done' : ''}`} style={{ padding: '0.8rem 0.2rem' }}>
                  <div className="task-left">
                    <div className="task-checkbox-container" onClick={() => rotateStatus(task)}>
                      <div className="task-checkbox">
                        {task.status === 'done' && <CheckSquare size={12} />}
                      </div>
                    </div>
                    <div className="task-details">
                      <span className="task-title" style={{ fontSize: '0.95rem' }}>{task.taskName}</span>
                      <div className="task-meta">
                        <span>Giao cho: <strong>{task.assignee?.fullName || 'Minh Đức'}</strong></span>
                        <span>•</span>
                        <span>Hạn: {formatDate(task.dueDate)}</span>
                      </div>
                    </div>
                  </div>
                  <div>
                    <button 
                      className={`status-badge ${task.status}`}
                      onClick={() => rotateStatus(task)}
                    >
                      {getStatusText(task.status)}
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

      </div>
    </div>
  );
}

