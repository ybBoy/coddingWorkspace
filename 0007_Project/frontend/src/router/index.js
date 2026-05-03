import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import PartsList from '../views/PartsList.vue'
import PartsSearch from '../views/PartsSearch.vue'
import StockOperation from '../views/StockOperation.vue'
import RestockList from '../views/RestockList.vue'
import RecordsList from '../views/RecordsList.vue'

import UserPartsSearch from '../views/user/UserPartsSearch.vue'
import UserRecordsList from '../views/user/UserRecordsList.vue'

import AdminHome from '../views/admin/AdminHome.vue'
import AdminPartsList from '../views/admin/AdminPartsList.vue'
import AdminPartsSearch from '../views/admin/AdminPartsSearch.vue'
import AdminRecordsList from '../views/admin/AdminRecordsList.vue'
import AdminRestockList from '../views/admin/AdminRestockList.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home',
    redirect: '/user/search'
  },
  {
    path: '/user',
    redirect: '/user/search'
  },
  {
    path: '/user/search',
    name: 'UserPartsSearch',
    component: UserPartsSearch,
    meta: { role: 'user' }
  },
  {
    path: '/user/records',
    name: 'UserRecordsList',
    component: UserRecordsList,
    meta: { role: 'user' }
  },
  {
    path: '/admin',
    name: 'AdminHome',
    component: AdminHome,
    meta: { role: 'admin' }
  },
  {
    path: '/admin/parts',
    name: 'AdminPartsList',
    component: AdminPartsList,
    meta: { role: 'admin' }
  },
  {
    path: '/admin/search',
    name: 'AdminPartsSearch',
    component: AdminPartsSearch,
    meta: { role: 'admin' }
  },
  {
    path: '/admin/stock',
    name: 'AdminStockOperation',
    component: StockOperation,
    meta: { role: 'admin' }
  },
  {
    path: '/admin/records',
    name: 'AdminRecordsList',
    component: AdminRecordsList,
    meta: { role: 'admin' }
  },
  {
    path: '/admin/restock',
    name: 'AdminRestockList',
    component: AdminRestockList,
    meta: { role: 'admin' }
  },
  {
    path: '/parts',
    name: 'PartsList',
    component: PartsList
  },
  {
    path: '/search',
    name: 'PartsSearch',
    component: PartsSearch
  },
  {
    path: '/stock',
    name: 'StockOperation',
    component: StockOperation
  },
  {
    path: '/restock',
    name: 'RestockList',
    component: RestockList
  },
  {
    path: '/records',
    name: 'RecordsList',
    component: RecordsList
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
