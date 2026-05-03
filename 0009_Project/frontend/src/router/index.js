import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'departments',
        name: 'Departments',
        component: () => import('../views/DepartmentManagement.vue'),
        meta: { title: '部门管理', requireAdmin: true }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('../views/RoleManagement.vue'),
        meta: { title: '角色管理', requireAdmin: true }
      },
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('../views/EmployeeManagement.vue'),
        meta: { title: '用户管理', requireAdmin: true }
      },
      {
        path: 'leave-apply',
        name: 'LeaveApply',
        component: () => import('../views/LeaveApply.vue'),
        meta: { title: '请假申请' }
      },
      {
        path: 'my-leaves',
        name: 'MyLeaves',
        component: () => import('../views/MyLeaves.vue'),
        meta: { title: '我的请假记录' }
      },
      {
        path: 'leave-approval',
        name: 'LeaveApproval',
        component: () => import('../views/LeaveApproval.vue'),
        meta: { title: '请假审批', requireAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  
  if (to.path === '/login') {
    if (user) {
      next('/')
    } else {
      next()
    }
  } else {
    if (!user) {
      next('/login')
    } else {
      if (to.meta.requireAdmin && user.roleId !== 1) {
        next('/dashboard')
      } else {
        next()
      }
    }
  }
})

export default router