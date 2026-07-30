<script>
  import { onMount } from 'svelte'
  import { currentPage, stats, loading } from './lib/stores.js'
  import { api } from './lib/api.js'
  import Sidebar from './lib/components/Sidebar.svelte'
  import Dashboard from './lib/pages/Dashboard.svelte'
  import Vehicles from './lib/pages/Vehicles.svelte'
  import Logs from './lib/pages/Logs.svelte'
  import Settings from './lib/pages/Settings.svelte'

  let page

  currentPage.subscribe(v => page = v)

  onMount(() => {
    loadStats()
  })

  async function loadStats() {
    loading.set(true)
    try {
      const res = await api.getStats()
      if (res && res.totalVehicles !== undefined) stats.set(res)
    } catch (e) {
      console.error('Stats load failed:', e)
    }
    loading.set(false)
  }
</script>

<div class="layout">
  <Sidebar />
  <main class="main">
    {#if page === 'dashboard'}
      <Dashboard onRefresh={loadStats} />
    {:else if page === 'vehicles'}
      <Vehicles />
    {:else if page === 'logs'}
      <Logs />
    {:else if page === 'settings'}
      <Settings />
    {/if}
  </main>
</div>
