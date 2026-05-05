import Sidebar from './Sidebar'
import styles from './Layout.module.css'

function Layout({ children }) {
  return (
    <div className={styles.layout}>
      <Sidebar />
      <main className={styles.main}>
        <div className={styles.content}>
          {children}
        </div>
      </main>
    </div>
  )
}

export default Layout