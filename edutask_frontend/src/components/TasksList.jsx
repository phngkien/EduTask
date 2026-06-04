import React, { useState } from 'react';
import { Check, Calendar, Plus, ClipboardList } from 'lucide-react';

export default function TasksList({ tasks, onUpdateTaskStatus, onCreateTaskClick }) {
  const [activeFilter, setActiveFilter] = useState('all');

  const filteredTasks = tasks.filter(task => {
    if (activeFilter === 'all') return true;
    return task.status === activeFilter;
  });

  const getStatusLabelClass = (status) => {
    switch (status) {
      case 'todo': return 'status-badge todo';
      case 'in_progress': return 'status-badge in_progress';
      case 'done': return 'status-badge done';
      default: return 'status-badge';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'todo': return 'Cần làm';
      case 'in_progress': return 'Đang làm';
      case 'done': return 'Hoàn thành';
      default: return status;
    }
  };

  const handleCheckboxClick = (task) => {
    const nextStatus = task.status === 'done' ? 'todo' : 'done';
    onUpdateTaskStatus(task.taskId, nextStatus);
  };

  const rotateStatus = (task) => {
    const statusCycle = ['todo', 'in_progress', 'done'];
    const currentIndex = statusCycle.indexOf(task.status);
    const nextIndex = (currentIndex + 1) % statusCycle.length;
    onUpdateTaskStatus(task.taskId, statusCycle[nextIndex]);
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
    <div className="tasks-container">
      <div className="tasks-header-row">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <ClipboardList size={20} className="primary-color-icon" />
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800 }}>Nhiệm vụ được giao</h3>
        </div>
        <button className="btn-add-inline" onClick={onCreateTaskClick} title="Tạo công việc mới">
          <Plus size={16} />
          <span>Việc mới</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="filters-tab-row">
        <button 
          className={`filter-tab ${activeFilter === 'all' ? 'active' : ''}`}
          onClick={() => setActiveFilter('all')}
        >
          Tất cả
        </button>
        <button 
          className={`filter-tab ${activeFilter === 'todo' ? 'active' : ''}`}
          onClick={() => setActiveFilter('todo')}
        >
          Cần làm
        </button>
        <button 
          className={`filter-tab ${activeFilter === 'in_progress' ? 'active' : ''}`}
          onClick={() => setActiveFilter('in_progress')}
        >
          Đang làm
        </button>
        <button 
          className={`filter-tab ${activeFilter === 'done' ? 'active' : ''}`}
          onClick={() => setActiveFilter('done')}
        >
          Hoàn thành
        </button>
      </div>

      <div className="tasks-list">
        {filteredTasks.length === 0 ? (
          <div className="empty-tasks-state">
            <p>Không có công việc nào khớp với bộ lọc này.</p>
          </div>
        ) : (
          filteredTasks.map((task) => (
            <div 
              key={task.taskId} 
              className={`task-item ${task.status === 'done' ? 'done' : ''}`}
            >
              <div className="task-left">
                <div 
                  className="task-checkbox-container" 
                  onClick={() => handleCheckboxClick(task)}
                >
                  <div className="task-checkbox">
                    {task.status === 'done' && <Check size={12} />}
                  </div>
                </div>
                
                <div className="task-details">
                  <span className="task-title">{task.taskName}</span>
                  <div className="task-meta">
                    <span className="task-group-tag">{task.group?.groupName}</span>
                    <span>•</span>
                    <span className="task-due">
                      <Calendar size={11} style={{ marginRight: '3px', verticalAlign: 'middle' }} />
                      {formatDate(task.dueDate)}
                    </span>
                  </div>
                </div>
              </div>

              <div>
                <button 
                  className={getStatusLabelClass(task.status)}
                  onClick={() => rotateStatus(task)}
                  title="Nhấn để đổi trạng thái (Cần làm -> Đang làm -> Hoàn thành)"
                >
                  {getStatusText(task.status)}
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

