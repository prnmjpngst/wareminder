const BASE = ''

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options
  })
  return res.json()
}

export const api = {
  getVehicles: (page = 1, search = '') =>
    request(`/api/vehicles?page=${page}&search=${encodeURIComponent(search)}`),

  getVehicle: (row) => request(`/api/vehicles/${row}`),

  getStats: () => request('/api/stats'),

  getStatus: () => request('/api/status'),

  getLogs: (page = 1) => request(`/api/logs?page=${page}`),

  getRecentLogs: () => request('/api/logs/recent'),

  sendAll: () => request('/api/send/all', { method: 'POST' }),

  sendOne: (row) => request(`/api/send/${row}`, { method: 'POST' }),

  sync: () => request('/api/send/sync', { method: 'POST' }),

  getSettings: () => request('/api/settings'),

  updateSettings: (data) => request('/api/settings', {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}
