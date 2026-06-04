import React from 'react';
import { Users, Calendar, ArrowRight, FolderKanban } from 'lucide-react';

export default function GroupsList({ groups, onGroupSelect, selectedGroupId }) {
  const getProgressColor = (progress) => {
    if (progress >= 80) return 'linear-gradient(90deg, #10b981 0%, #059669 100%)'; // success
    if (progress >= 40) return 'linear-gradient(90deg, #3b82f6 0%, #2563eb 100%)'; // primary/info
    return 'linear-gradient(90deg, #f59e0b 0%, #ea580c 100%)'; // warning
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

  return (
    <div className="group-card-stack">
      {groups.length === 0 ? (
        <div className="empty-state-card">
          <FolderKanban size={48} className="empty-icon" />
          <p>Không tìm thấy nhóm hoạt động nào. Hãy tạo nhóm mới để bắt đầu!</p>
        </div>
      ) : (
        groups.map((group) => {
          const isSelected = selectedGroupId === group.groupId;
          return (
            <div 
              key={group.groupId} 
              className={`group-card ${isSelected ? 'selected-group-card' : ''}`}
            >
              <div className="group-info-left">
                <div className="group-avatar-box">
                  <Users size={24} />
                </div>
                <div className="group-details">
                  <div className="group-title-row">
                    <span 
                      className="group-name" 
                      onClick={() => onGroupSelect(group)}
                    >
                      {group.groupName}
                    </span>
                    <span className="group-deadline">
                      Hạn chót: {formatDate(group.deadline)}
                    </span>
                  </div>
                  
                  <div className="group-owner">
                    <span>Người tạo: <strong>{group.creator?.fullName || 'Không rõ'}</strong></span>
                  </div>

                  <div className="group-metrics">
                    <span className="metrics-txt">
                      Nhiệm vụ: {group.completedTasks}/{group.totalTasks}
                    </span>
                    <span className="metrics-txt">•</span>
                    <span className="metrics-txt">
                      Thành viên: {group.membersCount}
                    </span>
                  </div>

                  {/* Progress tracker */}
                  <div className="progress-container">
                    <div className="progress-track">
                      <div 
                        className="progress-bar" 
                        style={{ 
                          width: `${group.progress || 0}%`,
                          background: getProgressColor(group.progress || 0)
                        }}
                      />
                    </div>
                    <span className="progress-percent">{group.progress || 0}%</span>
                  </div>
                </div>
              </div>

              <div className="action-row">
                <button className="btn-open" onClick={() => onGroupSelect(group)}>
                  <span>Vào nhóm</span>
                  <ArrowRight size={14} style={{ marginLeft: '4px' }} />
                </button>
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}

