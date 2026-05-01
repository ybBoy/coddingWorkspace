import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/guidance',
    name: 'Guidance',
    component: () => import('../views/Guidance.vue')
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('../views/Statistics.vue')
  },
  {
    path: '/display',
    name: 'Display',
    component: () => import('../views/Display.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router