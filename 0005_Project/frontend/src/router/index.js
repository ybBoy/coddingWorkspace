import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import PartsList from '../views/PartsList.vue'
import PartsSearch from '../views/PartsSearch.vue'
import StockOperation from '../views/StockOperation.vue'
import RestockList from '../views/RestockList.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
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
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
