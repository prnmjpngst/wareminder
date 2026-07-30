<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'

  let status = {}
  let syncResult = null
  let syncing = false
  let settings = {
    spreadsheetId: '',
    windowStartDay: 3,
    windowEndDay: 0,
    scheduleStartHour: 8,
    scheduleEndHour: 20,
    maxPerRun: 5
  }
  let saveResult = null
  let saving = false

  onMount(() => load())

  async function load() {
    const [statusRes, settingsRes] = await Promise.all([
      api.getStatus(),
      api.getSettings()
    ])
    if (statusRes) status = statusRes
    if (settingsRes?.data) settings = { ...settings, ...settingsRes.data }
  }

  async function doSync() {
    syncing = true
    syncResult = null
    const res = await api.sync()
    syncResult = res
    syncing = false
    await load()
  }

  async function saveSettings() {
    saving = true
    saveResult = null
    const res = await api.updateSettings({
      spreadsheetId: settings.spreadsheetId,
      windowStartDay: Number(settings.windowStartDay),
      windowEndDay: Number(settings.windowEndDay),
      scheduleStartHour: Number(settings.scheduleStartHour),
      scheduleEndHour: Number(settings.scheduleEndHour),
      maxPerRun: Number(settings.maxPerRun)
    })
    saveResult = res
    saving = false
  }

  function fmtTime(ts) {
    if (!ts) return '-'
    return new Date(ts).toLocaleString('id-ID')
  }
</script>

<div class="page-header">
  <h1>Pengaturan</h1>
</div>

<div class="card" style="margin-bottom:1rem">
  <h3 style="margin-bottom:.75rem">Konfigurasi Google Sheet</h3>
  <div style="display:grid;gap:1rem;max-width:500px">
    <div>
      <label for="spreadsheetId" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Spreadsheet ID</label>
      <input id="spreadsheetId" type="text" bind:value={settings.spreadsheetId} placeholder="Masukkan Google Spreadsheet ID" />
    </div>
  </div>

  <h3 style="margin:.75rem 0">Jadwal Reminder</h3>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem;max-width:500px">
    <div>
      <label for="windowStart" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Window Mulai (H-)</label>
      <input id="windowStart" type="number" bind:value={settings.windowStartDay} min="0" max="30" />
    </div>
    <div>
      <label for="windowEnd" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Window Akhir (H-)</label>
      <input id="windowEnd" type="number" bind:value={settings.windowEndDay} min="0" max="30" />
    </div>
    <div>
      <label for="startHour" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Jam Mulai</label>
      <input id="startHour" type="number" bind:value={settings.scheduleStartHour} min="0" max="23" />
    </div>
    <div>
      <label for="endHour" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Jam Selesai</label>
      <input id="endHour" type="number" bind:value={settings.scheduleEndHour} min="0" max="23" />
    </div>
    <div>
      <label for="maxRun" style="font-size:.875rem;font-weight:600;display:block;margin-bottom:.25rem">Max Pesan per Run</label>
      <input id="maxRun" type="number" bind:value={settings.maxPerRun} min="1" max="20" />
    </div>
  </div>

  <div style="margin-top:1rem">
    <button class="btn btn-primary" on:click={saveSettings} disabled={saving}>
      {saving ? 'Menyimpan...' : 'Simpan Pengaturan'}
    </button>
    {#if saveResult}
      <span style="margin-left:.75rem;font-size:.875rem;color:{saveResult.success ? 'var(--success)' : 'var(--danger)'}">
        {saveResult.success ? 'Tersimpan' : 'Gagal: ' + saveResult.error}
      </span>
    {/if}
  </div>
</div>

<div class="card" style="margin-bottom:1rem">
  <h3 style="margin-bottom:.75rem">Informasi Server</h3>
  <table>
    <tr><td style="width:200px">Alamat Server</td><td><code>http://[ip-perangkat]:8080</code></td></tr>
    <tr><td>Layanan Berjalan</td><td><span class="badge" class:success={status.serviceRunning} class:danger={!status.serviceRunning}>{status.serviceRunning ? 'Ya' : 'Tidak'}</span></td></tr>
    <tr><td>Accessibility Service</td><td><span class="badge" class:success={status.accessibilityEnabled} class:danger={!status.accessibilityEnabled}>{status.accessibilityEnabled ? 'Aktif' : 'Nonaktif'}</span></td></tr>
    <tr><td>Check Terakhir</td><td>{fmtTime(status.lastCheck)}</td></tr>
    <tr><td>Sync Terakhir</td><td>{fmtTime(status.lastSync)}</td></tr>
  </table>
</div>

<div class="card" style="margin-bottom:1rem">
  <h3 style="margin-bottom:.75rem">Sinkronisasi Data</h3>
  <p style="font-size:.875rem;color:var(--text-secondary);margin-bottom:.75rem">
    Tarik data terbaru dari Google Sheet. Data akan digunakan untuk pengiriman reminder berikutnya.
  </p>
  <button class="btn btn-primary" on:click={doSync} disabled={syncing}>
    {syncing ? 'Menyinkronkan...' : 'Sync dari Google Sheet'}
  </button>
  {#if syncResult}
    <p style="margin-top:.75rem;font-size:.875rem">
      {#if syncResult.success}
        <span style="color:var(--success)">Berhasil sync {syncResult.data?.count ?? 0} kendaraan</span>
      {:else}
        <span style="color:var(--danger)">Gagal: {syncResult.error}</span>
      {/if}
    </p>
  {/if}
</div>

<div class="card">
  <h3 style="margin-bottom:.75rem">Cara Penggunaan</h3>
  <ol style="font-size:.875rem;line-height:1.8;padding-left:1.25rem">
    <li>Letakkan file <code>service_account.json</code> di <code>res/raw/</code> sebelum build</li>
    <li>Masukkan <strong>Spreadsheet ID</strong> di form di atas</li>
    <li>Nyalakan <strong>Accessibility Service</strong> di Pengaturan Android &rarr; Aksesbilitas &rarr; WA Reminder Dishub</li>
    <li>Nonaktifkan <strong>Optimasi Baterai</strong> untuk aplikasi ini</li>
    <li>Akses web control panel dari browser di jaringan lokal: <code>http://[ip-perangkat]:8080</code></li>
  </ol>
</div>
