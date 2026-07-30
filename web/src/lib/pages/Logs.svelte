<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'

  let logs = []
  let page = 1
  let total = 0
  let loading = false

  onMount(() => load())

  async function load() {
    loading = true
    try {
      const res = await api.getLogs(page)
      if (res) {
        logs = res.data || []
        total = res.total || 0
      }
    } catch (e) {
      console.error('Logs load failed:', e)
    }
    loading = false
  }

  function prevPage() {
    if (page > 1) { page--; load() }
  }

  function nextPage() {
    if (page * 50 < total) { page++; load() }
  }

  function fmtTime(ts) {
    if (!ts) return '-'
    return new Date(ts).toLocaleString('id-ID')
  }
</script>

<div class="page-header">
  <h1>Log Pengiriman</h1>
</div>

<div class="card">
  {#if loading}
    <p style="text-align:center;color:var(--text-secondary)">Memuat...</p>
  {:else if logs.length === 0}
    <p style="text-align:center;color:var(--text-secondary)">Belum ada log pengiriman</p>
  {:else}
    <div class="timeline">
      {#each logs as log}
        <div class="timeline-item">
          <div class="timeline-icon {log.status === 'SENT' ? 'success' : 'failed'}">
            {log.status === 'SENT' ? '✓' : '✗'}
          </div>
          <div class="timeline-body">
            <div class="title">{log.nomorKendaraan}</div>
            <div class="sub">
              {log.namaPemilik} — {log.nomorHP}
            </div>
            <div class="time">
              {fmtTime(log.timestamp)}
              &ensp;
              <span class="badge" class:success={log.status === 'SENT'} class:danger={log.status === 'FAILED'}>
                {log.status}
              </span>
              {#if log.error}
                &ensp;<span style="color:var(--danger)">{log.error}</span>
              {/if}
            </div>
          </div>
        </div>
      {/each}
    </div>

    <div class="pagination">
      <button class="btn btn-outline" on:click={prevPage} disabled={page <= 1}>Sebelumnya</button>
      <span style="padding:.5rem;font-size:.875rem">Halaman {page} ({total} total)</span>
      <button class="btn btn-outline" on:click={nextPage} disabled={page * 50 >= total}>Selanjutnya</button>
    </div>
  {/if}
</div>
