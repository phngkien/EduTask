import React, { useState, useEffect } from 'react';
import { api, clearTokens } from './services/api';
import Header from './components/Header';
import HeroBanner from './components/HeroBanner';
import GroupsList from './components/GroupsList';
import TasksList from './components/TasksList';
import GroupDetailsView from './components/GroupDetailsView';
import CreateGroupModal from './components/CreateGroupModal';
import CreateTaskModal from './components/CreateTaskModal';
import AuthView from './components/AuthView';
import SubscriptionView from './components/SubscriptionView';
import ProfileView from './components/ProfileView';
import './App.css';

function App() {
  const [user, setUser] = useState(null);
  const [groups, setGroups] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [notifications, setNotifications] = useState([]);
  
  // Workspace views & detail panel states
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [activeGroupMembers, setActiveGroupMembers] = useState([]);
  const [activeGroupTasks, setActiveGroupTasks] = useState([]);

  // Modals state
  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);
  const [defaultTaskGroupId, setDefaultTaskGroupId] = useState(null);

  // Search filter query
  const [searchQuery, setSearchQuery] = useState('');

  // Router views: 'dashboard' | 'profile' | 'subscription'
  const [currentView, setCurrentView] = useState('dashboard');

  const loadDashboardData = async () => {
    try {
      const loadedGroups = await api.getGroups();
      setGroups([...loadedGroups]);

      const loadedTasks = await api.getTasks();
      setTasks([...loadedTasks]);

      const loadedNotifs = await api.getNotifications();
      setNotifications([...loadedNotifs]);
    } catch (error) {
      console.error("Failed to load dashboard data:", error);
    }
  };

  // Initial Load
  useEffect(() => {
    const initData = async () => {
      const token = localStorage.getItem("accessToken");
      if (token) {
        try {
          const currentUser = await api.getCurrentUser();
          setUser(currentUser);
          await loadDashboardData();
        } catch (error) {
          console.error("Initialization failed:", error);
          clearTokens();
        }
      }
    };
    initData();

    // Lắng nghe sự kiện đăng xuất khi token hết hạn
    const handleAuthLogout = () => {
      setUser(null);
      setGroups([]);
      setTasks([]);
      setNotifications([]);
      setCurrentView('dashboard');
    };
    window.addEventListener("auth:logout", handleAuthLogout);
    return () => window.removeEventListener("auth:logout", handleAuthLogout);
  }, []);

  // Update selected group detailed view when groups or tasks are updated
  useEffect(() => {
    if (selectedGroup) {
      const refreshGroupDetails = async () => {
        try {
          const members = await api.getGroupMembers(selectedGroup.groupId);
          setActiveGroupMembers([...members]);

          const gTasks = await api.getTasksByGroup(selectedGroup.groupId);
          setActiveGroupTasks([...gTasks]);

          // Refresh the group item itself to keep progress synced
          const allGroups = await api.getGroups();
          const refreshedGroup = allGroups.find(g => g.groupId === selectedGroup.groupId);
          if (refreshedGroup) {
            setSelectedGroup(refreshedGroup);
          }
        } catch (error) {
          console.error("Failed to load group details:", error);
        }
      };
      refreshGroupDetails();
    }
  }, [groups, tasks]);

  // Handler: Select a group workspace
  const handleGroupSelect = async (group) => {
    try {
      const members = await api.getGroupMembers(group.groupId);
      setActiveGroupMembers(members);

      const groupTasks = await api.getTasksByGroup(group.groupId);
      setActiveGroupTasks(groupTasks);

      setSelectedGroup(group);
    } catch (error) {
      console.error("Error navigating to group workspace:", error);
    }
  };

  // Handler: Create Group
  const handleCreateGroup = async (groupData) => {
    try {
      await api.createGroup(groupData);
      const updatedGroups = await api.getGroups();
      setGroups([...updatedGroups]);
    } catch (error) {
      console.error("Failed to create group:", error);
    }
  };

  // Handler: Create Task
  const handleCreateTask = async (taskData) => {
    try {
      await api.createTask(taskData);
      const updatedTasks = await api.getTasks();
      setTasks([...updatedTasks]);

      // Refresh groups list since new tasks affect progress ratios
      const updatedGroups = await api.getGroups();
      setGroups([...updatedGroups]);
    } catch (error) {
      console.error("Failed to create task:", error);
    }
  };

  // Handler: Update Task Status (rotate / checkoff)
  const handleUpdateTaskStatus = async (taskId, nextStatus) => {
    try {
      await api.updateTaskStatus(taskId, nextStatus);
      
      // Sync list states
      const updatedTasks = await api.getTasks();
      setTasks([...updatedTasks]);

      const updatedGroups = await api.getGroups();
      setGroups([...updatedGroups]);
    } catch (error) {
      console.error("Failed to update task status:", error);
    }
  };

  // Handler: Invite/Add Member
  const handleAddMember = async (groupId, userId, role) => {
    try {
      await api.addMemberToGroup(groupId, userId, role);
      
      // Sync group metrics
      const updatedGroups = await api.getGroups();
      setGroups([...updatedGroups]);

      // Trigger active detail reload
      const members = await api.getGroupMembers(groupId);
      setActiveGroupMembers([...members]);
    } catch (error) {
      console.error("Failed to add group member:", error);
    }
  };

  // Handler: Mark Notification Read
  const handleMarkNotificationRead = async (notificationId) => {
    try {
      await api.markNotificationRead(notificationId);
      const remainingNotifs = await api.getNotifications();
      setNotifications([...remainingNotifs]);
    } catch (error) {
      console.error("Failed to clear notification:", error);
    }
  };

  // Filter lists based on top search bar queries
  const filteredGroups = groups.filter(g => 
    g.groupName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredTasks = tasks.filter(t => 
    t.taskName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleAuthSuccess = async (authResponse) => {
    try {
      const currentUser = await api.getCurrentUser();
      setUser(currentUser);
      await loadDashboardData();
      setCurrentView('dashboard');
    } catch (err) {
      console.error(err);
    }
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch (_) {}
    setUser(null);
    setGroups([]);
    setTasks([]);
    setNotifications([]);
    setCurrentView('dashboard');
  };

  if (!user) {
    return <AuthView onAuthSuccess={handleAuthSuccess} />;
  }

  return (
    <div className="app-container">
      {/* SVGs decoration for premium glassmorphism background */}
      <div className="decorator-left">
        <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
          <path fill="#2563EB" d="M47.7,-64.8C61.4,-57.4,71.8,-42.7,77.4,-26.6C83,-10.4,83.9,7.1,78.2,22.7C72.6,38.2,60.5,51.8,46.1,61C31.7,70.2,15.9,75,-0.9,76.3C-17.7,77.5,-35.4,75.2,-49.5,65.9C-63.5,56.7,-74,40.4,-78.7,22.8C-83.3,5.1,-82.1,-13.9,-74.6,-29.7C-67.1,-45.6,-53.4,-58.3,-38.3,-65.2C-23.2,-72,-6.6,-73.1,10.6,-74.8C27.8,-76.5,41.4,-72.1,47.7,-64.8Z" transform="translate(100 100)" />
        </svg>
      </div>
      <div className="decorator-right">
        <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
          <path fill="#10B981" d="M37.9,-51.2C49.9,-46.3,61,-37.2,68.9,-25.1C76.8,-12.9,81.5,2.3,79.5,17C77.4,31.7,68.7,45.8,56.6,55.5C44.4,65.2,28.8,70.5,13,73C-2.7,75.4,-18.6,75.1,-32.7,69.5C-46.8,63.9,-59.1,53,-67,39.4C-74.8,25.8,-78.3,9.5,-76.3,-5.7C-74.4,-20.9,-67,-35,-56.3,-41.8C45.6,-48.7,31.6,-48.3,37.9,-51.2Z" transform="translate(100 100)" />
        </svg>
      </div>

      <Header 
        user={user} 
        notifications={notifications} 
        onMarkNotificationRead={handleMarkNotificationRead}
        onSearch={setSearchQuery}
        onProfileClick={() => setCurrentView('profile')}
        onSubscriptionClick={() => setCurrentView('subscription')}
        onLogout={handleLogout}
      />

      <main className="main-content">
        {currentView === 'profile' ? (
          <ProfileView 
            user={user} 
            onProfileUpdate={setUser} 
            onBack={() => setCurrentView('dashboard')} 
          />
        ) : currentView === 'subscription' ? (
          <SubscriptionView 
            user={user} 
            onBack={() => setCurrentView('dashboard')} 
          />
        ) : selectedGroup ? (
          <GroupDetailsView
            group={selectedGroup}
            members={activeGroupMembers}
            tasks={activeGroupTasks}
            onBack={() => setSelectedGroup(null)}
            onAddMember={handleAddMember}
            onAddTaskClick={(gId) => {
              setDefaultTaskGroupId(gId);
              setIsCreateTaskOpen(true);
            }}
            onUpdateTaskStatus={handleUpdateTaskStatus}
          />
        ) : (
          <>
            <HeroBanner 
              user={user} 
              onCreateGroupClick={() => setIsCreateGroupOpen(true)} 
            />

            <div className="dashboard-grid">
              {/* Left Column: Groups Workspace */}
              <div className="dashboard-column">
                <div className="column-header">
                  <h3 className="column-title">Không gian làm việc nhóm</h3>
                </div>
                
                <GroupsList 
                  groups={filteredGroups} 
                  onGroupSelect={handleGroupSelect}
                  selectedGroupId={null}
                />
              </div>

              {/* Right Column: Personal Checklist */}
              <div className="dashboard-column">
                <div className="column-header">
                  <h3 className="column-title">Danh sách nhiệm vụ cá nhân</h3>
                </div>

                <TasksList 
                  tasks={filteredTasks}
                  onUpdateTaskStatus={handleUpdateTaskStatus}
                  onCreateTaskClick={() => {
                    setDefaultTaskGroupId(null);
                    setIsCreateTaskOpen(true);
                  }}
                />
              </div>
            </div>
          </>
        )}
      </main>

      {/* Modals */}
      <CreateGroupModal
        isOpen={isCreateGroupOpen}
        onClose={() => setIsCreateGroupOpen(false)}
        onCreateGroup={handleCreateGroup}
      />

      <CreateTaskModal
        isOpen={isCreateTaskOpen}
        onClose={() => setIsCreateTaskOpen(false)}
        onCreateTask={handleCreateTask}
        groups={groups}
        defaultGroupId={defaultTaskGroupId}
      />
    </div>
  );
}

export default App;
