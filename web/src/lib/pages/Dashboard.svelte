<script>
  import { onMount } from 'svelte'
  import { stats, loading } from '../stores.js'
  import { api } from '../api.js'

  let s
  let status
  let recentLogs = []
  let sending = false
  let sendResult = null

  stats.subscribe(v => s = v)

  onMount(() => {
    load()
  })

  async function load() {
    const [statusRes, logRes] = await Promise.all([
      api.getStatus(),
      api.getRecentLogs()
    ])
    status = statusRes
    recentLogs = logRes?.data || []
  }

  async function sendNow() {
    sending = true
    sendResult = null
    const res = await api.sendAll()
    sendResult = res
    sending = false
    await load()
  }

  function fmtTime(ts) {
    if (!ts) return '-'
    return new Date(ts).toLocaleString('id-ID')
  }
</script>

<div class="page-header">
  <h1>Dashboard</h1>
</div>

<div class="stats-grid">
  <div class="stat-card">
    <div class="value">{s?.activeVehicles ?? '-'}</div>
    <div class="label">Kendaraan Aktif</div>
  </div>
  <div class="stat-card">
    <div class="value">{s?.expiringSoon ?? '-'}</div>
    <div class="label">Akan Berakhir</div>
  </div>
  <div class="stat-card">
    <div class="value">{s?.expiredVehicles ?? '-'}</div>
    <div class="label">Sudah Dikirim</div>
  </div>
  <div class="stat-card">
    <div class="value">{s?.totalVehicles ?? '-'}</div>
    <div class="label">Total Kendaraan</div>
  </div>
</div>

<div class="card" style="margin-bottom:1rem">
  <h3 style="margin-bottom:.75rem">Status Layanan</h3>
  <p style="font-size:.9rem;color:var(--text-secondary)">
    Service: <span class="badge" class:success={status?.serviceRunning} class:danger={!status?.serviceRunning}>
      {status?.serviceRunning ? 'Berjalan' : 'Berhenti'}
    </span>
    &ensp;Accessibility: <span class="badge" class:success={status?.accessibilityEnabled} class:danger={!status?.accessibilityEnabled}>
      {status?.accessibilityEnabled ? 'Aktif' : 'Nonaktif'}
    </span>
  </p>
  <p style="font-size:.8rem;color:var(--text-secondary);margin-top:.25rem">
    Sync terakhir: {fmtTime(status?.lastSync)} &ensp; Check terakhir: {fmtTime(status?.lastCheck)}
  </p>
</div>

<div style="display:flex;gap:.75rem;margin-bottom:1.5rem">
  <button class="btn btn-primary" on:click={sendNow} disabled={sending}>
    {sending ? 'Mengirim...' : 'Kirim Reminder Sekarang'}
  </button>
</div>

{#if sendResult}
  <div class="card" style="margin-bottom:1rem">
    <p>
      Terkirim: <strong>{sendResult.data?.sent ?? 0}</strong>
      &ensp;Gagal: <strong>{sendResult.data?.failed ?? 0}</strong>
      &ensp;Total: <strong>{sendResult.data?.total ?? 0}</strong>
    </p>
  </div>
{/if}

<div class="card">
  <h3 style="margin-bottom:.75rem">Log Terbaru</h3>
  {#if recentLogs.length === 0}
    <p style="color:var(--text-secondary);font-size:.9rem">Belum ada pengiriman</p>
  {:else}
    <div class="timeline">
      {#each recentLogs as log}
        <div class="timeline-item">
          <div class="timeline-icon {log.status === 'SENT' ? 'success' : 'failed'}">
            {log.status === 'SENT' ? '✓' : '✗'}
          </div>
          <div class="timeline-body">
            <div class="title">{log.nomorKendaraan} — {log.namaPemilik}</div>
            <div class="sub">{log.nomorHP}</div>
            <div class="time">{fmtTime(log.timestamp)}{#if log.error} — {log.error}{/if}</div>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>
