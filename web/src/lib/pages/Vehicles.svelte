<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'

  let vehicles = []
  let page = 1
  let total = 0
  let search = ''
  let loading = false

  onMount(() => load())

  async function load() {
    loading = true
    const res = await api.getVehicles(page, search)
    if (res) {
      vehicles = res.data || []
      total = res.total || 0
    }
    loading = false
  }

  function doSearch() {
    page = 1
    load()
  }

  function prevPage() {
    if (page > 1) { page--; load() }
  }

  function nextPage() {
    if (page * 50 < total) { page++; load() }
  }

  async function sendOne(row) {
    await api.sendOne(row)
  }

  function fmtTime(ts) {
    if (!ts) return '-'
    return new Date(ts).toLocaleString('id-ID')
  }
</script>

<div class="page-header">
  <h1>Data Kendaraan</h1>
</div>

<div class="search-bar">
  <input type="text" placeholder="Cari No. Kendaraan / Nama Pemilik / No. HP" bind:value={search} on:input={doSearch} />
  <button class="btn btn-outline" on:click={api.sync}>Sync</button>
</div>

<div class="card">
  {#if loading}
    <p style="text-align:center;color:var(--text-secondary)">Memuat...</p>
  {:else if vehicles.length === 0}
    <p style="text-align:center;color:var(--text-secondary)">Tidak ada data. Sync dari Google Sheet terlebih dahulu.</p>
  {:else}
    <div style="overflow-x:auto">
      <table>
        <thead>
          <tr>
            <th>No. Kendaraan</th>
            <th>Pemilik</th>
            <th>No. HP</th>
            <th>Masa Berlaku</th>
            <th>Status</th>
            <th>Aksi</th>
          </tr>
        </thead>
        <tbody>
          {#each vehicles as v}
            <tr>
              <td><strong>{v.nomorKendaraan}</strong></td>
              <td>{v.namaPemilik}</td>
              <td>{v.nomorHP}</td>
              <td>{v.masaBerlaku}</td>
              <td>
                {#if v.done === 'done'}
                  <span class="badge success">Terkirim</span>
                {:else}
                  <span class="badge warning">Pending</span>
                {/if}
              </td>
              <td>
                <button class="btn btn-primary btn-sm" on:click={() => sendOne(v.rowIndex)} disabled={v.done === 'done'}>
                  Kirim WA
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <button class="btn btn-outline" on:click={prevPage} disabled={page <= 1}>Sebelumnya</button>
      <span style="padding:.5rem;font-size:.875rem">Halaman {page}</span>
      <button class="btn btn-outline" on:click={nextPage} disabled={page * 50 >= total}>Selanjutnya</button>
    </div>
  {/if}
</div>
