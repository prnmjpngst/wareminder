// Navigation
const navButtons = document.querySelectorAll('.nav-btn');
const pages = document.querySelectorAll('.page');

navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        const targetPage = btn.dataset.page;
        
        navButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        
        pages.forEach(page => page.classList.remove('active'));
        document.getElementById(targetPage).classList.add('active');
        
        // Load data for the page
        if (targetPage === 'dashboard') loadDashboard();
        else if (targetPage === 'vehicles') loadVehicles();
        else if (targetPage === 'logs') loadLogs();
        else if (targetPage === 'settings') loadSettings();
    });
});

// API Helper
async function api(endpoint, options = {}) {
    try {
        const response = await fetch(`/api${endpoint}`, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || 'API error');
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Dashboard
async function loadDashboard() {
    try {
        const [stats, status] = await Promise.all([
            api('/stats'),
            api('/status')
        ]);

        document.getElementById('stat-total').textContent = stats.totalVehicles || 0;
        document.getElementById('stat-expiring').textContent = stats.expiringSoon || 0;
        document.getElementById('stat-sent').textContent = stats.sentToday || 0;

        const serviceStatus = document.getElementById('service-status');
        serviceStatus.textContent = status.serviceRunning ? 'Berjalan' : 'Berhenti';
        serviceStatus.className = 'badge ' + (status.serviceRunning ? 'success' : 'danger');

        const accessibilityStatus = document.getElementById('accessibility-status');
        accessibilityStatus.textContent = status.accessibilityEnabled ? 'Aktif' : 'Nonaktif';
        accessibilityStatus.className = 'badge ' + (status.accessibilityEnabled ? 'success' : 'danger');

        document.getElementById('last-sync').textContent = status.lastSync 
            ? new Date(status.lastSync).toLocaleString('id-ID') 
            : 'Belum pernah';
    } catch (error) {
        console.error('Failed to load dashboard:', error);
    }
}

// Vehicles
async function loadVehicles(search = '') {
    try {
        const data = await api(`/vehicles?search=${encodeURIComponent(search)}`);
        const container = document.getElementById('vehicles-list');
        
        if (!data.data || data.data.length === 0) {
            container.innerHTML = '<p style="text-align:center;padding:20px;color:#666">Tidak ada data kendaraan</p>';
            return;
        }

        const table = `
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
                    ${data.data.map(v => `
                        <tr>
                            <td><strong>${v.nomorKendaraan}</strong></td>
                            <td>${v.namaPemilik}</td>
                            <td>${v.nomorHP}</td>
                            <td>${v.masaBerlaku}</td>
                            <td><span class="badge ${v.done === 'done' ? 'success' : 'warning'}">${v.done === 'done' ? 'Terkirim' : 'Pending'}</span></td>
                            <td>
                                <button class="btn btn-primary" onclick="sendReminder(${v.rowIndex})" ${v.done === 'done' ? 'disabled' : ''}>
                                    Kirim WA
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        container.innerHTML = table;
    } catch (error) {
        console.error('Failed to load vehicles:', error);
    }
}

async function sendReminder(rowIndex) {
    if (!confirm('Kirim reminder untuk kendaraan ini?')) return;
    
    try {
        const btn = event.target;
        btn.disabled = true;
        btn.textContent = 'Mengirim...';
        
        await api(`/send/${rowIndex}`, { method: 'POST' });
        
        alert('Reminder berhasil dikirim!');
        loadVehicles();
    } catch (error) {
        alert('Gagal mengirim reminder: ' + error.message);
    }
}

// Logs
async function loadLogs() {
    try {
        const data = await api('/logs');
        const container = document.getElementById('logs-list');
        
        if (!data.data || data.data.length === 0) {
            container.innerHTML = '<p style="text-align:center;padding:20px;color:#666">Belum ada log pengiriman</p>';
            return;
        }

        const logs = data.data.map(log => `
            <div class="log-item ${log.status === 'SENT' ? 'success' : 'failed'}">
                <div class="log-header">
                    <span>${log.nomorKendaraan}</span>
                    <span class="badge ${log.status === 'SENT' ? 'success' : 'danger'}">${log.status}</span>
                </div>
                <div class="log-details">
                    <div>Pemilik: ${log.namaPemilik}</div>
                    <div>No. HP: ${log.nomorHP}</div>
                    <div>Waktu: ${new Date(log.timestamp).toLocaleString('id-ID')}</div>
                    ${log.error ? `<div style="color:#dc3545">Error: ${log.error}</div>` : ''}
                </div>
            </div>
        `).join('');
        
        container.innerHTML = logs;
    } catch (error) {
        console.error('Failed to load logs:', error);
    }
}

// Settings
async function loadSettings() {
    try {
        const settings = await api('/settings');
        
        document.getElementById('sheet-id-input').value = settings.spreadsheetId || '';
        document.getElementById('window-start').value = settings.windowStartDay || 3;
        document.getElementById('window-end').value = settings.windowEndDay || 0;
        document.getElementById('schedule-start').value = settings.scheduleStartHour || 8;
        document.getElementById('schedule-end').value = settings.scheduleEndHour || 20;
        document.getElementById('max-per-run').value = settings.maxPerRun || 5;
        
        // Check service account status
        const status = await api('/status');
        const uploadStatus = document.getElementById('upload-status');
        if (status.sheetsAvailable) {
            uploadStatus.textContent = '✓ Service account sudah terkonfigurasi';
            uploadStatus.style.color = '#28a745';
        } else {
            uploadStatus.textContent = '✗ Service account belum dikonfigurasi';
            uploadStatus.style.color = '#dc3545';
        }
    } catch (error) {
        console.error('Failed to load settings:', error);
    }
}

// Service Account Upload
document.getElementById('btn-upload').addEventListener('click', () => {
    document.getElementById('file-input').click();
});

document.getElementById('file-input').addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    if (!file.name.endsWith('.json')) {
        alert('File harus berformat JSON');
        return;
    }
    
    const uploadStatus = document.getElementById('upload-status');
    uploadStatus.textContent = 'Mengupload...';
    uploadStatus.style.color = '#666';
    
    try {
        const text = await file.text();
        
        const response = await fetch('/api/upload-service-account', {
            method: 'POST',
            body: text,
            headers: { 'Content-Type': 'text/plain' }
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            uploadStatus.textContent = '✓ Service account berhasil diupload!';
            uploadStatus.style.color = '#28a745';
            alert('Service account berhasil diupload!');
        } else {
            throw new Error(data.error || 'Upload failed');
        }
    } catch (error) {
        uploadStatus.textContent = '✗ Gagal upload: ' + error.message;
        uploadStatus.style.color = '#dc3545';
        alert('Gagal upload service account: ' + error.message);
    }
});

// Save Sheet ID
document.getElementById('btn-save-sheet-id').addEventListener('click', async () => {
    const sheetId = document.getElementById('sheet-id-input').value.trim();
    if (!sheetId) {
        alert('Sheet ID tidak boleh kosong');
        return;
    }
    
    try {
        await api('/settings', {
            method: 'PUT',
            body: JSON.stringify({ spreadsheetId: sheetId })
        });
        alert('Sheet ID berhasil disimpan!');
    } catch (error) {
        alert('Gagal menyimpan: ' + error.message);
    }
});

// Save Settings
document.getElementById('btn-save-settings').addEventListener('click', async () => {
    try {
        const settings = {
            windowStartDay: parseInt(document.getElementById('window-start').value),
            windowEndDay: parseInt(document.getElementById('window-end').value),
            scheduleStartHour: parseInt(document.getElementById('schedule-start').value),
            scheduleEndHour: parseInt(document.getElementById('schedule-end').value),
            maxPerRun: parseInt(document.getElementById('max-per-run').value)
        };
        
        await api('/settings', {
            method: 'PUT',
            body: JSON.stringify(settings)
        });
        
        alert('Pengaturan berhasil disimpan!');
    } catch (error) {
        alert('Gagal menyimpan: ' + error.message);
    }
});

// Sync Button
document.getElementById('btn-sync').addEventListener('click', async () => {
    const btn = document.getElementById('btn-sync');
    btn.disabled = true;
    btn.textContent = 'Syncing...';
    
    try {
        await api('/send/sync', { method: 'POST' });
        alert('Sync berhasil!');
        loadDashboard();
    } catch (error) {
        alert('Gagal sync: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.textContent = '🔄 Sync dari Google Sheet';
    }
});

// Send All Button
document.getElementById('btn-send-all').addEventListener('click', async () => {
    if (!confirm('Kirim reminder ke semua kendaraan yang akan expired?')) return;
    
    const btn = document.getElementById('btn-send-all');
    btn.disabled = true;
    btn.textContent = 'Mengirim...';
    
    try {
        const result = await api('/send/all', { method: 'POST' });
        alert(`Berhasil mengirim ${result.data?.sent || 0} reminder!`);
        loadDashboard();
    } catch (error) {
        alert('Gagal mengirim: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.textContent = '📤 Kirim Reminder Sekarang';
    }
});

// Search
document.getElementById('btn-search').addEventListener('click', () => {
    const search = document.getElementById('search-input').value.trim();
    loadVehicles(search);
});

document.getElementById('search-input').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        const search = e.target.value.trim();
        loadVehicles(search);
    }
});

// Initial load
loadDashboard();
