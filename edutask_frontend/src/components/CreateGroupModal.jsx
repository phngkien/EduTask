import React, { useState } from 'react';
import { X } from 'lucide-react';

export default function CreateGroupModal({ isOpen, onClose, onCreateGroup }) {
  const [groupName, setGroupName] = useState('');
  const [deadline, setDeadline] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!groupName.trim()) {
      alert("Vui lòng nhập tên nhóm");
      return;
    }
    onCreateGroup({
      groupName: groupName.trim(),
      deadline: deadline || new Date(Date.now() + 864000000).toISOString() // default +10 days
    });
    setGroupName('');
    setDeadline('');
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">Tạo nhóm mới</h3>
          <button className="modal-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-group">
              <label className="form-label" htmlFor="groupName">Tên nhóm</label>
              <input
                id="groupName"
                type="text"
                className="form-input"
                placeholder="Ví dụ: Dự án Công nghệ Phần mềm"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="deadline">Hạn chót (Deadline)</label>
              <input
                id="deadline"
                type="datetime-local"
                className="form-input"
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Hủy
            </button>
            <button type="submit" className="btn-primary" style={{ padding: '0.6rem 1.6rem', fontSize: '0.95rem' }}>
              Tạo không gian làm việc
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

