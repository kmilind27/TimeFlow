# TimeFlow

**Timesheet and Leave Management System**

A comprehensive employee timesheet and leave management system built with React and Vite.

## Features

- 🕐 **Timesheet Management** - Log work hours, track weekly timesheets
- 📅 **Leave Management** - Apply for leave, view history and balance
- 👥 **Role-Based Access** - Employee, Manager, and Admin roles
- ✅ **Approval Workflow** - Manager review and approval system
- 📊 **Admin Dashboard** - System overview and user management
- 🔐 **Secure Authentication** - JWT-based auth with role protection

## Tech Stack

- React 19
- React Router v7
- Axios
- Vite
- CSS Modules

## Getting Started

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build
```

## Project Structure

```
src/
├── api/          # API service layer
├── components/   # Reusable components
├── context/      # React context (Auth)
├── pages/        # Page components
│   ├── admin/
│   ├── auth/
│   ├── leave/
│   ├── manager/
│   └── timesheet/
└── utils/        # Helper functions
```

---

## React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)
