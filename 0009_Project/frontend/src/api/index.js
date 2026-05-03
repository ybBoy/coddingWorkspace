import request from './request'

export function login(data) {
  return request.post('/login', data)
}

export function checkAdmin(employeeId) {
  return request.get(`/check-admin/${employeeId}`)
}

export function getDepartments() {
  return request.get('/departments')
}

export function getDepartment(id) {
  return request.get(`/departments/${id}`)
}

export function createDepartment(data) {
  return request.post('/departments', data)
}

export function updateDepartment(id, data) {
  return request.put(`/departments/${id}`, data)
}

export function deleteDepartment(id) {
  return request.delete(`/departments/${id}`)
}

export function getRoles() {
  return request.get('/roles')
}

export function getRole(id) {
  return request.get(`/roles/${id}`)
}

export function createRole(data) {
  return request.post('/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/roles/${id}`)
}

export function getEmployees() {
  return request.get('/employees')
}

export function getEmployee(id) {
  return request.get(`/employees/${id}`)
}

export function getEmployeesByDepartment(departmentId) {
  return request.get(`/employees/department/${departmentId}`)
}

export function createEmployee(data) {
  return request.post('/employees', data)
}

export function updateEmployee(id, data) {
  return request.put(`/employees/${id}`, data)
}

export function deleteEmployee(id) {
  return request.delete(`/employees/${id}`)
}

export function getLeaves() {
  return request.get('/leaves')
}

export function getLeave(id) {
  return request.get(`/leaves/${id}`)
}

export function getMyLeaves(employeeId) {
  return request.get(`/leaves/employee/${employeeId}`)
}

export function getPendingLeaves() {
  return request.get('/leaves/pending')
}

export function getApprovedLeaves() {
  return request.get('/leaves/approved')
}

export function getRejectedLeaves() {
  return request.get('/leaves/rejected')
}

export function applyLeave(data) {
  return request.post('/leaves/apply', data)
}

export function approveLeave(data) {
  return request.post('/leaves/approve', data)
}