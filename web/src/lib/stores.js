import { writable } from 'svelte/store'

export const currentPage = writable('dashboard')
export const stats = writable(null)
export const logs = writable([])
export const vehicles = writable([])
export const loading = writable(false)
export const error = writable(null)
