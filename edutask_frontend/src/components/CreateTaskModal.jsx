import React, { useState } from 'react';
import { X } from 'lucide-react';

export default function CreateTaskModal({ isOpen, onClose, onCreateTask, groups, defaultGroupId }) {
  const [taskName, setTaskName] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [groupId, setGroupId] = useState(defaultGroupId || (groups[0]?.groupId || ''));

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!taskName.trim()) {
      alert("Vui lòng nhập tên công việc");
      return;
    }
    if (!groupId) {
      alert("Vui lòng chọn nhóm");
      return;
    }
    onCreateTask({
      taskName: taskName.trim(),
      dueDate: dueDate || new Date(Date.now() + 172800000).toISOString(), // default +2 days
      groupId: Number(groupId),
      status: 'todo'
    });
    setTaskName('');
    setDueDate('');
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Tạo công việc mới</h3>
          <button className="modal-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-group">
              <label className="form-label" htmlFor="taskName">Mô tả công việc</label>
              <input
                id="taskName"
                type="text"
                className="form-input"
                placeholder="Ví dụ: Đọc tài liệu nghiên cứu tham khảo"
                value={taskName}
                onChange={(e) => setTaskName(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="taskGroup">Chọn nhóm dự án</label>
              <select
                id="taskGroup"
                className="form-input"
                value={groupId}
                onChange={(e) => setGroupId(e.target.value)}
                required
              >
                <option value="" disabled>-- Chọn nhóm --</option>
                {groups.map((group) => (
                  <option key={group.groupId} value={group.groupId}>
                    {group.groupName}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="dueDate">Hạn hoàn thành</label>
              <input
                id="dueDate"
                type="datetime-local"
                className="form-input"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Hủy
            </button>
            <button type="submit" className="btn-primary" style={{ padding: '0.6rem 1.6rem', fontSize: '0.95rem' }}>
              Giao công việc
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

