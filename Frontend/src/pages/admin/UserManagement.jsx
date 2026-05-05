import React, { useState, useEffect } from 'react'
import { Search, X, MoreVertical, RefreshCw, UserX, UserCheck, User, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getAllUsers, changeRole, changeStatus, updateManager } from '../../api/authApi'
import { useAuth } from '../../context/AuthContext'
import styles from './Admin.module.css'

const UserManagement = () => {
  const { user } = useAuth()

  const [users, setUsers] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [search, setSearch] = useState('')

  //Track which user's action panel is open
  const [activeUser, setActiveUser] = useState(null)
  const [actionType, setActionType] = useState('')
  const [actionValue, setActionValue] = useState('')
  const [actioning, setActioning] = useState(false)
  const [actionError, setActionError] = useState('')
  const [openMenuUserId, setOpenMenuUserId] = useState(null)

  useEffect(() => {
    fetchUsers()
  }, [])

  async function fetchUsers() {
    try {
      setError('')
      const response = await getAllUsers()
      setUsers(response.data)
    } catch (err) {
      setError('Failed to load users')
    }
  }

  function handleOpenAction(userId, type) {
    setActiveUser(userId)
    setActionType(type)
    setActionValue('')
    setActionError('')
    setOpenMenuUserId(null)
  }

  function handleCloseAction() {
    setActiveUser(null)
    setActionType('')
    setActionValue('')
    setActionError('')
  }

  function toggleMenu(userId) {
    setOpenMenuUserId(openMenuUserId === userId ? null : userId)
  }

  async function handleRoleChange(userId) {
    if (!actionValue) {
      setActionError('Please select a role')
      return
    }
    if (userId === user?.userId) {
      setActionError('You cannot change your own role')
      return
    }

    setActioning(true)
    setActionError('')

    try {
      await changeRole(userId, { role: actionValue })
      setSuccess(`Role updated to ${actionValue} successfully!`)
      handleCloseAction()
      fetchUsers()
    } catch (err) {
      setActionError(
        err.response?.data?.message ||
        'Failed to change role'
      )
    } finally {
      setActioning(false)
    }
  }

  async function handleStatusChange(userId, newStatus) {
    setActioning(true)
    setActionError('')

    try {
      await changeStatus(userId, { status: newStatus })
      setSuccess(`Status updated to ${newStatus} successfully!`)
      handleCloseAction()
      fetchUsers()
    } catch (err) {
      setActionError(
        err.response?.data?.message ||
        'Failed to change status'
      )
    } finally {
      setActioning(false)
    }
  }

  async function handleManagerChange(userId) {
    if (!actionValue) {
      setActionError('Please select a manager')
      return
    }

    setActioning(true)
    setActionError('')

    try {
      await updateManager(userId, { managerId: parseInt(actionValue) })
      setSuccess('Manager assigned successfully!')
      handleCloseAction()
      fetchUsers()
    } catch (err) {
      setActionError(
        err.response?.data?.message ||
        'Failed to assign manager'
      )
    } finally {
      setActioning(false)
    }
  }

  function getRoleBadgeClass(role) {
    if (role === 'ADMIN') return styles.roleAdmin
    if (role === 'MANAGER') return styles.roleManager
    return styles.roleEmployee
  }

  function getStatusBadgeClass(status) {
    return status === 'ACTIVE'
      ? styles.statusActive
      : styles.statusInactive
  }

  // ✅ Filter users by search
  const filteredUsers = users.filter(u =>
    u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.employeeCode?.toLowerCase().includes(search.toLowerCase())
  )

  if (error) {
    return (
      <Layout>
        <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
      </Layout>
    )
  }

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>User Management</h2>
          <p className={styles.pageSubtitle}>
            Manage roles and status of all employees
          </p>
        </div>
        <div className={styles.userCount}>
          {users.length} total users
        </div>
      </div>

      <AutoDismissMessage
        message={error}
        type="error"
        onDismiss={() => setError('')}
      />
      <AutoDismissMessage
        message={success}
        type="success"
        onDismiss={() => setSuccess('')}
      />

      {/* ── Search Bar ── */}
      <div className={styles.searchBar}>
        <span className={styles.searchIcon}><Search size={18} /></span>
        <input
          type="text"
          placeholder="Search by name, email or employee code..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className={styles.searchInput}
        />
        {search && (
          <button
            className={styles.clearSearch}
            onClick={() => setSearch('')}
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* ── Users List ── */}
      <div className={styles.userList}>
        {filteredUsers.length === 0 ? (
          <div className={styles.emptyState}>
            <span className={styles.emptyIcon}><User size={48} /></span>
            <h3>No users found</h3>
            <p>Try a different search term</p>
          </div>
        ) : (
          filteredUsers.map((u) => (
            <div key={u.id} className={styles.userCard}>

              {/* ── User Info ── */}
              <div className={styles.userCardHeader}>
                <div className={styles.userInfo}>
                  <div className={styles.userAvatar}>
                    {u.fullName?.charAt(0).toUpperCase()}
                  </div>
                  <div className={styles.userDetails}>
                    <div className={styles.userNameRow}>
                      <span className={styles.userName}>
                        {u.fullName}
                      </span>
                      {u.id === user?.userId && (
                        <span className={styles.youBadge}>You</span>
                      )}
                    </div>
                    <span className={styles.userEmail}>
                      {u.email}
                    </span>
                    <span className={styles.userEmpCode}>
                      {u.employeeCode}
                    </span>
                  </div>
                </div>

                {/* Badges */}
                <div className={styles.userBadges}>
                  <span className={`${styles.roleBadge} ${getRoleBadgeClass(u.role)}`}>
                    {u.role}
                  </span>
                  <span className={`${styles.statusBadge} ${getStatusBadgeClass(u.status)}`}>
                    {u.status}
                  </span>
                </div>

                {/* Kebab Menu */}
                {u.id !== user?.userId && (
                  <div className={styles.kebabMenuContainer}>
                    <button
                      className={styles.kebabMenuBtn}
                      onClick={() => toggleMenu(u.id)}
                    >
                      <MoreVertical size={18} />
                    </button>
                    {openMenuUserId === u.id && (
                      <div className={styles.kebabMenu}>
                        <button
                          className={styles.menuItem}
                          onClick={() => handleOpenAction(u.id, 'role')}
                        >
                          <RefreshCw size={14} /> Change Role
                        </button>
                        <button
                          className={styles.menuItem}
                          onClick={() => {
                            const newStatus = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
                            handleOpenAction(u.id, 'status')
                            setActionValue(newStatus)
                          }}
                        >
                          {u.status === 'ACTIVE' ? <><UserX size={14} /> Deactivate</> : <><UserCheck size={14} /> Activate</>}
                        </button>
                        {(u.role === 'EMPLOYEE' || u.role === 'MANAGER') && (
                          <button
                            className={styles.menuItem}
                            onClick={() => handleOpenAction(u.id, 'manager')}
                          >
                            <User size={14} /> Assign Manager
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* ── Action Panel ── */}
              {activeUser === u.id && (
                <div className={styles.actionPanel}>

                  <AutoDismissMessage
                    message={actionError}
                    type="error"
                    onDismiss={() => setActionError('')}
                  />

                  {actionType === 'role' ? (
                    <>
                      <p className={styles.actionLabel}>
                        Select new role for <strong>{u.fullName}</strong>:
                      </p>
                      <div className={styles.roleOptions}>
                        {['EMPLOYEE', 'MANAGER', 'ADMIN'].map((role) => (
                          <button
                            key={role}
                            className={`${styles.roleOption} ${actionValue === role
                                ? styles.roleOptionActive
                                : ''
                              }`}
                            onClick={() => setActionValue(role)}
                          >
                            {role}
                          </button>
                        ))}
                      </div>
                    </>
                  ) : actionType === 'status' ? (
                    <>
                      <p className={styles.actionLabel}>
                        Are you sure you want to {actionValue === 'ACTIVE' ? 'activate' : 'deactivate'} <strong>{u.fullName}</strong>?
                      </p>
                    </>
                  ) : (
                    <>
                      <p className={styles.actionLabel}>
                        Assign manager for <strong>{u.fullName}</strong>:
                      </p>
                      <select
                        className={styles.managerSelect}
                        value={actionValue}
                        onChange={(e) => setActionValue(e.target.value)}
                      >
                        <option value="">Select a manager</option>
                        {users
                          .filter(user =>
                            (user.role === 'MANAGER' || user.role === 'ADMIN') &&
                            user.id !== u.id
                          )
                          .map(manager => (
                            <option key={manager.id} value={manager.id}>
                              {manager.fullName} ({manager.role})
                            </option>
                          ))}
                      </select>
                    </>
                  )}

                  <div className={styles.actionPanelButtons}>
                    <button
                      className={styles.cancelActionBtn}
                      onClick={handleCloseAction}
                    >
                      Cancel
                    </button>
                    <button
                      className={styles.confirmActionBtn}
                      disabled={actioning}
                      onClick={() => {
                        if (actionType === 'role') handleRoleChange(u.id)
                        else if (actionType === 'status') handleStatusChange(u.id, actionValue)
                        else handleManagerChange(u.id)
                      }}
                    >
                      {actioning ? 'Saving...' : 'Confirm'}
                    </button>
                  </div>

                </div>
              )}

            </div>
          ))
        )}
      </div>

    </Layout>
  )
}

export default UserManagement