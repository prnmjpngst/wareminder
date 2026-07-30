<script>
  import { currentPage } from '../stores.js'

  const menu = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊' },
    { id: 'vehicles', label: 'Kendaraan', icon: '🚛' },
    { id: 'logs', label: 'Logs', icon: '📋' },
    { id: 'settings', label: 'Pengaturan', icon: '⚙️' },
  ]

  let active
  currentPage.subscribe(v => active = v)

  function navigate(id) {
    currentPage.set(id)
  }
</script>

<aside class="sidebar">
  <div class="logo">
    <span>WA Reminder Dishub</span>
  </div>
  <nav>
    {#each menu as item}
      <button class:active={active === item.id} on:click={() => navigate(item.id)}>
        <span>{item.icon}</span>
        <span>{item.label}</span>
      </button>
    {/each}
  </nav>
</aside>
