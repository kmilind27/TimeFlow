import { useState, useEffect } from 'react'
import { CheckCircle, AlertCircle, X } from 'lucide-react'
import styles from './AutoDismissMessage.module.css'

const AutoDismissMessage = ({ message, type = 'success', onDismiss }) => {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    if (!message) return
    setVisible(true)
    const timer = setTimeout(() => {
      setVisible(false)
      if (onDismiss) onDismiss()
    }, 4000)
    return () => clearTimeout(timer)
  }, [message])

  if (!message || !visible) return null

  return (
    <div className={`${styles.message} ${styles[type]}`}>
      <span>
        {type === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />} {message}
      </span>
      <button
        className={styles.closeBtn}
        onClick={() => {
          setVisible(false)
          if (onDismiss) onDismiss()
        }}
      >
        <X size={16} />
      </button>
    </div>
  )
}

export default AutoDismissMessage