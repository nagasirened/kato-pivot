import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/sensitive-word',
    name: 'SensitiveWord',
    redirect: '/sensitive-word/list',
    children: [
      {
        path: 'list',
        name: 'SensitiveWordList',
        component: () => import('@/views/SensitiveWord/List.vue')
      }
    ]
  },
  {
    path: '/sensitive-test',
    name: 'SensitiveTest',
    component: () => import('@/views/SensitiveTest/Index.vue')
  },
  {
    path: '/import-export',
    name: 'ImportExport',
    component: () => import('@/views/SensitiveWord/Import.vue')
  },
  {
    path: '/operation-log',
    name: 'OperationLog',
    component: () => import('@/views/OperationLog/List.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
