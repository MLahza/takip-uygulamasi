/**
 * Mola Takip Uygulaması - Core Logic
 */

// --- State Management ---
let employees = JSON.parse(localStorage.getItem('mola_employees')) || [];
let activeTab = 'active';

// --- Constants ---
const BREAK_DURATION = 1 * 60; // 45 minutes in seconds
// const BREAK_DURATION = 10; // For testing: 10 seconds

// --- DOM Elements ---
const employeeList = document.getElementById('employee-list');
const finishedList = document.getElementById('finished-list');
const employeeModal = document.getElementById('employee-modal');
const employeeForm = document.getElementById('employee-form');
const addEmployeeTrigger = document.getElementById('add-employee-trigger');
const closeModalBtns = document.querySelectorAll('.modal-close');
const navItems = document.querySelectorAll('.nav-item');
const views = document.querySelectorAll('.view');
const currentDateEl = document.getElementById('current-date');
const toast = document.getElementById('notification-toast');
const toastMessage = document.getElementById('toast-message');

// --- Initialization ---
function init() {
    renderDate();
    renderEmployees();
    setupEventListeners();
    startMasterTimer();
    requestNotificationPermission();
    lucide.createIcons();
}

// --- Utilities ---
function renderDate() {
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    currentDateEl.innerText = new Date().toLocaleDateString('tr-TR', options);
}

function saveToStorage() {
    localStorage.setItem('mola_employees', JSON.stringify(employees));
}

function formatTime(seconds) {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function showToast(message) {
    toastMessage.innerText = message;
    toast.classList.add('active');
    setTimeout(() => toast.classList.remove('active'), 4000);
}

// --- Notification API ---
function requestNotificationPermission() {
    if ('Notification' in window) {
        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            Notification.requestPermission();
        }
    }
}

function sendNotification(name) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('Mola Bitti!', {
            body: `${name} için 45 dakikalık mola süresi tamamlandı.`,
            icon: 'https://cdn-icons-png.flaticon.com/512/2972/2972531.png'
        });
    }
    showToast(`${name} molasını tamamladı!`);
}

// --- Core Functions ---
function renderEmployees() {
    // Clear lists
    employeeList.innerHTML = '';
    finishedList.innerHTML = '';

    const activeEmployees = employees.filter(e => !e.molaFinished);
    const finishedEmployees = employees.filter(e => e.molaFinished);

    if (activeEmployees.length === 0) {
        employeeList.innerHTML = `
            <div class="empty-state">
                <i data-lucide="users"></i>
                <p>Henüz çalışan eklenmemiş.</p>
            </div>`;
    } else {
        activeEmployees.forEach(emp => {
            const card = createEmployeeCard(emp);
            employeeList.appendChild(card);
        });
    }

    if (finishedEmployees.length === 0) {
        finishedList.innerHTML = `
            <div class="empty-state">
                <i data-lucide="check-circle"></i>
                <p>Henüz süresi dolan eleman yok.</p>
            </div>`;
    } else {
        finishedEmployees.forEach(emp => {
            const card = createFinishedCard(emp);
            finishedList.appendChild(card);
        });
    }

    lucide.createIcons();
}

function createEmployeeCard(emp) {
    const div = document.createElement('div');
    div.className = 'card';

    const isMolaActive = emp.molaStartTime !== null;
    let remaining = 0;

    if (isMolaActive) {
        const elapsed = Math.floor((Date.now() - emp.molaStartTime) / 1000);
        remaining = Math.max(0, BREAK_DURATION - elapsed);
    }

    div.innerHTML = `
        <div class="card-main">
            <div class="info">
                <h3>${emp.name}</h3>
                <div class="status">
                    <span class="status-dot ${isMolaActive ? 'active' : ''}"></span>
                    <span>${isMolaActive ? 'Molada' : 'Çalışıyor'}</span>
                </div>
            </div>
            <div class="card-actions">
                <button class="btn-icon" onclick="editEmployee('${emp.id}')"><i data-lucide="edit-3"></i></button>
                <button class="btn-icon delete" onclick="deleteEmployee('${emp.id}')"><i data-lucide="trash-2"></i></button>
            </div>
        </div>
        ${isMolaActive ? `
            <div style="display: flex; justify-content: center; margin-bottom: 0.75rem; margin-top: 0.25rem;">
                <div class="timer-badge" id="timer-${emp.id}" style="font-size: 1.75rem; width: 60%; letter-spacing: 2px;">${formatTime(remaining)}</div>
            </div>
        ` : ''}
        <div style="display: flex; gap: 0.5rem; width: 100%;">
            <button class="btn btn-start" onclick="startBreak('${emp.id}')" style="flex: 1; ${isMolaActive ? 'opacity: 0.5; cursor: not-allowed;' : ''}" ${isMolaActive ? 'disabled' : ''}>
                <i data-lucide="play"></i> Başlat
            </button>
            <button class="btn btn-danger" onclick="stopBreak('${emp.id}')" style="flex: 1; justify-content: center; ${!isMolaActive ? 'opacity: 0.5; cursor: not-allowed;' : ''}" ${!isMolaActive ? 'disabled' : ''}>
                <i data-lucide="square"></i> Durdur
            </button>
        </div>
    `;
    return div;
}

function createFinishedCard(emp) {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.borderColor = 'var(--primary)';

    div.innerHTML = `
        <div class="card-main">
            <div class="info">
                <h3>${emp.name}</h3>
                <div class="status">
                    <i data-lucide="check" style="color:var(--primary); width:16px;"></i>
                    <span>Mola Tamamlandı</span>
                </div>
            </div>
        </div>
        <div style="display: flex; gap: 0.5rem; width: 100%; margin-top: 0.5rem;">
            <button class="btn btn-secondary" onclick="resetBreak('${emp.id}')" style="flex: 1; justify-content: center; padding: 0.5rem 0.75rem; font-size: 0.85rem;">
                <i data-lucide="rotate-ccw" style="width: 16px; height: 16px;"></i> Sıfırla
            </button>
            <button class="btn btn-danger" onclick="deleteEmployee('${emp.id}')" style="flex: 1; justify-content: center; padding: 0.5rem 0.75rem; font-size: 0.85rem;">
                <i data-lucide="trash-2" style="width: 16px; height: 16px;"></i> Sil
            </button>
        </div>
    `;
    return div;
}

// --- Actions ---
window.startBreak = (id) => {
    const emp = employees.find(e => e.id === id);
    if (emp) {
        emp.molaStartTime = Date.now();
        saveToStorage();
        renderEmployees();
    }
};

window.stopBreak = (id) => {
    const emp = employees.find(e => e.id === id);
    if (emp) {
        emp.molaStartTime = null;
        emp.molaFinished = false;
        saveToStorage();
        renderEmployees();
    }
};

window.resetBreak = (id) => {
    const emp = employees.find(e => e.id === id);
    if (emp) {
        emp.molaStartTime = null;
        emp.molaFinished = false;
        saveToStorage();
        renderEmployees();
    }
};

window.deleteEmployee = (id) => {
    if (confirm('Bu çalışanı silmek istediğinize emin misiniz?')) {
        employees = employees.filter(e => e.id !== id);
        saveToStorage();
        renderEmployees();
    }
};

window.editEmployee = (id) => {
    const emp = employees.find(e => e.id === id);
    if (emp) {
        document.getElementById('employee-id').value = emp.id;
        document.getElementById('employee-name').value = emp.name;
        document.getElementById('modal-title').innerText = 'Çalışanı Düzenle';
        employeeModal.classList.add('active');
    }
};

// --- Time Tracking ---
function startMasterTimer() {
    setInterval(() => {
        let changed = false;
        employees.forEach(emp => {
            if (emp.molaStartTime && !emp.molaFinished) {
                const elapsed = Math.floor((Date.now() - emp.molaStartTime) / 1000);
                const remaining = Math.max(0, BREAK_DURATION - elapsed);

                // Update UI directly for performance if in active view
                const timerEl = document.getElementById(`timer-${emp.id}`);
                if (timerEl) {
                    timerEl.innerText = formatTime(remaining);
                }

                if (remaining === 0) {
                    emp.molaFinished = true;
                    changed = true;
                    sendNotification(emp.name);
                }
            }
        });

        if (changed) {
            saveToStorage();
            renderEmployees();
        }
    }, 1000);
}

// --- Event Listeners ---
function setupEventListeners() {
    addEmployeeTrigger.addEventListener('click', () => {
        document.getElementById('employee-id').value = '';
        document.getElementById('employee-name').value = '';
        document.getElementById('modal-title').innerText = 'Çalışan Ekle';
        employeeModal.classList.add('active');
    });

    closeModalBtns.forEach(btn => {
        btn.addEventListener('click', () => employeeModal.classList.remove('active'));
    });

    employeeForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const id = document.getElementById('employee-id').value;
        const name = document.getElementById('employee-name').value;

        if (id) {
            // Edit
            const emp = employees.find(e => e.id === id);
            if (emp) emp.name = name;
        } else {
            // Add
            employees.push({
                id: Date.now().toString(),
                name: name,
                molaStartTime: null,
                molaFinished: false
            });
        }

        saveToStorage();
        renderEmployees();
        employeeModal.classList.remove('active');
    });

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const viewId = item.getAttribute('data-view');

            // UI Toggle
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');

            views.forEach(view => {
                view.classList.remove('active');
                if (view.id === `view-${viewId}`) view.classList.add('active');
            });

            activeTab = viewId;
        });
    });
}

// Start the app
init();
