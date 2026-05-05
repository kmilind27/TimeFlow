import React, { useState, useEffect } from 'react'
import Layout from '../../components/Layout'
import { getAllUsers } from '../../api/authApi'
import { useAuth } from '../../context/AuthContext'
import { AlertCircle, Users } from 'lucide-react'
import styles from './Manager.module.css'

const Team = () => {
  const { user } = useAuth()

  const [teamMembers, setTeamMembers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchTeam()
  }, [])

  async function fetchTeam() {
    try {
      
      setError('')
      const response = await getAllUsers()
      // Filter users where managerId matches current user's ID
      const myTeam = response.data.filter(u => u.managerId === user?.userId)
      setTeamMembers(myTeam)
    } catch (err) {
      setError('Failed to load team members')
    } finally {
      
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

  

  return (
    <Layout>

      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>My Team</h2>
          <p className={styles.pageSubtitle}>
            Employees reporting to you
          </p>
        </div>
        <div className={styles.countBadge}>
          {teamMembers.length} member{teamMembers.length !== 1 ? 's' : ''}
        </div>
      </div>

      {error && (
        <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
      )}

      {teamMembers.length === 0 ? (
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}><Users size={48} /></span>
          <h3>No team members yet</h3>
          <p>Employees will appear here once assigned to you by admin</p>
        </div>
      ) : (
        <div className={styles.userList}>
          {teamMembers.map((member) => (
            <div key={member.id} className={styles.userCard}>
              <div className={styles.userCardHeader}>
                <div className={styles.userInfo}>
                  <div className={styles.userAvatar}>
                    {member.fullName?.charAt(0).toUpperCase()}
                  </div>
                  <div className={styles.userDetails}>
                    <span className={styles.userName}>
                      {member.fullName}
                    </span>
                    <span className={styles.userEmail}>
                      {member.email}
                    </span>
                    <span className={styles.userEmpCode}>
                      {member.employeeCode}
                    </span>
                  </div>
                </div>

                <div className={styles.userBadges}>
                  <span className={`${styles.roleBadge} ${getRoleBadgeClass(member.role)}`}>
                    {member.role}
                  </span>
                  <span className={`${styles.statusBadge} ${getStatusBadgeClass(member.status)}`}>
                    {member.status}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

    </Layout>
  )
}

export default Team
