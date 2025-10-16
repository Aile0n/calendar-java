// API Base URL
const API_BASE = '/api/calendar';

// State
let currentDate = new Date();
let entries = [];
let editingIndex = -1;
let darkMode = false;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initializeEventListeners();
    loadConfig();
    loadEntries();
    renderCalendar();
});

// Event Listeners
function initializeEventListeners() {
    // Header buttons
    document.getElementById('darkModeToggle').addEventListener('click', toggleDarkMode);
    document.getElementById('exportBtn').addEventListener('click', exportCalendar);
    document.getElementById('refreshBtn').addEventListener('click', loadEntries);
    
    // Toolbar buttons
    document.getElementById('newEntryBtn').addEventListener('click', openNewEntryModal);
    document.getElementById('importBtn').addEventListener('click', () => document.getElementById('importFile').click());
    document.getElementById('importFile').addEventListener('change', importFile);
    
    // Calendar navigation
    document.getElementById('prevMonth').addEventListener('click', () => changeMonth(-1));
    document.getElementById('nextMonth').addEventListener('click', () => changeMonth(1));
    
    // Modal
    document.querySelector('.close').addEventListener('click', closeModal);
    document.getElementById('cancelBtn').addEventListener('click', closeModal);
    document.getElementById('entryForm').addEventListener('submit', saveEntry);
    
    // Click outside modal to close
    window.addEventListener('click', (e) => {
        const modal = document.getElementById('entryModal');
        if (e.target === modal) {
            closeModal();
        }
    });
}

// Config
async function loadConfig() {
    try {
        const response = await fetch(`${API_BASE}/config`);
        if (response.ok) {
            const config = await response.json();
            darkMode = config.darkMode || false;
            if (darkMode) {
                document.body.classList.add('dark-mode');
            }
        }
    } catch (error) {
        console.error('Fehler beim Laden der Konfiguration:', error);
    }
}

function toggleDarkMode() {
    darkMode = !darkMode;
    document.body.classList.toggle('dark-mode');
    
    // Save to backend
    fetch(`${API_BASE}/config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ darkMode })
    }).catch(error => console.error('Fehler beim Speichern der Konfiguration:', error));
}

// Entries Management
async function loadEntries() {
    setStatus('Lade Termine...');
    try {
        const response = await fetch(`${API_BASE}/entries`);
        if (response.ok) {
            entries = await response.json();
            renderEventsList();
            renderCalendar();
            setStatus(`Status: ${entries.length} Termine geladen`);
        } else {
            setStatus('Fehler beim Laden der Termine');
        }
    } catch (error) {
        console.error('Fehler beim Laden der Termine:', error);
        setStatus('Fehler beim Laden der Termine');
    }
}

function setStatus(message) {
    document.getElementById('statusLabel').textContent = message;
}

// Calendar Rendering
function renderCalendar() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    // Update month display
    const monthNames = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
                        'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];
    document.getElementById('currentMonth').textContent = `${monthNames[month]} ${year}`;
    
    // Get days in month
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();
    
    // Clear grid
    const grid = document.getElementById('calendarGrid');
    grid.innerHTML = '';
    
    // Add day headers
    const dayNames = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];
    dayNames.forEach(name => {
        const header = document.createElement('div');
        header.className = 'day-name';
        header.textContent = name;
        grid.appendChild(header);
    });
    
    // Add empty cells for days before first day of month
    for (let i = 0; i < startingDayOfWeek; i++) {
        grid.appendChild(document.createElement('div'));
    }
    
    // Add days
    for (let day = 1; day <= daysInMonth; day++) {
        const dayElement = createDayElement(year, month, day);
        grid.appendChild(dayElement);
    }
}

function createDayElement(year, month, day) {
    const dayElement = document.createElement('div');
    dayElement.className = 'calendar-day';
    
    const date = new Date(year, month, day);
    const today = new Date();
    
    // Check if it's today
    if (date.toDateString() === today.toDateString()) {
        dayElement.classList.add('today');
    }
    
    // Check for events on this day
    const dayEntries = getEntriesForDay(date);
    if (dayEntries.length > 0) {
        dayElement.classList.add('has-events');
    }
    
    // Day number
    const dayNumber = document.createElement('div');
    dayNumber.className = 'day-number';
    dayNumber.textContent = day;
    dayElement.appendChild(dayNumber);
    
    // Event indicators
    if (dayEntries.length > 0) {
        const eventsIndicator = document.createElement('div');
        eventsIndicator.className = 'day-events';
        for (let i = 0; i < Math.min(dayEntries.length, 3); i++) {
            const dot = document.createElement('span');
            dot.className = 'day-event-dot';
            eventsIndicator.appendChild(dot);
        }
        if (dayEntries.length > 3) {
            eventsIndicator.appendChild(document.createTextNode(` +${dayEntries.length - 3}`));
        }
        dayElement.appendChild(eventsIndicator);
    }
    
    dayElement.addEventListener('click', () => showDayEvents(date, dayEntries));
    
    return dayElement;
}

function getEntriesForDay(date) {
    return entries.filter(entry => {
        const startDate = new Date(entry.start);
        return startDate.getFullYear() === date.getFullYear() &&
               startDate.getMonth() === date.getMonth() &&
               startDate.getDate() === date.getDate();
    });
}

function showDayEvents(date, dayEntries) {
    if (dayEntries.length === 0) {
        openNewEntryModal(date);
    } else {
        // Scroll to events list
        document.querySelector('.events-section').scrollIntoView({ behavior: 'smooth' });
    }
}

function changeMonth(delta) {
    currentDate.setMonth(currentDate.getMonth() + delta);
    renderCalendar();
}

// Events List Rendering
function renderEventsList() {
    const list = document.getElementById('eventsList');
    list.innerHTML = '';
    
    if (entries.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📭</div>
                <p>Keine Termine vorhanden.</p>
                <p>Klicken Sie auf "Neuer Termin", um einen Termin zu erstellen.</p>
            </div>
        `;
        return;
    }
    
    // Sort entries by start date
    const sortedEntries = [...entries].sort((a, b) => 
        new Date(a.start) - new Date(b.start)
    );
    
    sortedEntries.forEach((entry, index) => {
        const eventItem = createEventItem(entry, index);
        list.appendChild(eventItem);
    });
}

function createEventItem(entry, index) {
    const item = document.createElement('div');
    item.className = 'event-item';
    
    if (entry.category) {
        const categoryClass = entry.category.toLowerCase().replace(/\s+/g, '-');
        item.classList.add(`category-${categoryClass}`);
    }
    
    const startDate = new Date(entry.start);
    const endDate = new Date(entry.end);
    
    item.innerHTML = `
        <div class="event-title">${escapeHtml(entry.title)}</div>
        <div class="event-time">
            📅 ${formatDate(startDate)} ${formatTime(startDate)} - ${formatTime(endDate)}
        </div>
        ${entry.description ? `<div class="event-description">${escapeHtml(entry.description)}</div>` : ''}
        ${entry.category ? `<span class="event-category">${escapeHtml(entry.category)}</span>` : ''}
        ${entry.reminderMinutesBefore ? `<span class="event-category">🔔 ${entry.reminderMinutesBefore} Min. vorher</span>` : ''}
        <div class="event-actions">
            <button class="btn btn-primary" onclick="editEntry(${index})">✏️ Bearbeiten</button>
            <button class="btn btn-secondary" onclick="deleteEntry(${index})" style="background-color: var(--danger-color);">🗑️ Löschen</button>
        </div>
    `;
    
    return item;
}

// Modal Management
function openNewEntryModal(date = null) {
    editingIndex = -1;
    document.getElementById('modalTitle').textContent = 'Neuer Termin';
    document.getElementById('entryForm').reset();
    
    // Set default dates
    const today = date || new Date();
    const tomorrow = new Date(today);
    tomorrow.setHours(today.getHours() + 1);
    
    document.getElementById('entryStartDate').value = formatDateInput(today);
    document.getElementById('entryEndDate').value = formatDateInput(today);
    document.getElementById('entryStartTime').value = '09:00';
    document.getElementById('entryEndTime').value = '10:00';
    
    document.getElementById('entryModal').classList.add('show');
}

function editEntry(index) {
    editingIndex = index;
    const entry = entries[index];
    
    document.getElementById('modalTitle').textContent = 'Termin bearbeiten';
    
    const startDate = new Date(entry.start);
    const endDate = new Date(entry.end);
    
    document.getElementById('entryTitle').value = entry.title || '';
    document.getElementById('entryDescription').value = entry.description || '';
    document.getElementById('entryStartDate').value = formatDateInput(startDate);
    document.getElementById('entryStartTime').value = formatTimeInput(startDate);
    document.getElementById('entryEndDate').value = formatDateInput(endDate);
    document.getElementById('entryEndTime').value = formatTimeInput(endDate);
    document.getElementById('entryCategory').value = entry.category || '';
    document.getElementById('entryReminder').value = entry.reminderMinutesBefore || '';
    
    document.getElementById('entryModal').classList.add('show');
}

function closeModal() {
    document.getElementById('entryModal').classList.remove('show');
    editingIndex = -1;
}

async function saveEntry(e) {
    e.preventDefault();
    
    const title = document.getElementById('entryTitle').value.trim();
    const description = document.getElementById('entryDescription').value.trim();
    const startDate = document.getElementById('entryStartDate').value;
    const startTime = document.getElementById('entryStartTime').value;
    const endDate = document.getElementById('entryEndDate').value;
    const endTime = document.getElementById('entryEndTime').value;
    const category = document.getElementById('entryCategory').value.trim();
    const reminder = document.getElementById('entryReminder').value;
    
    // Combine date and time
    const start = `${startDate}T${startTime}`;
    const end = `${endDate}T${endTime}`;
    
    const entry = {
        title,
        description: description || null,
        start,
        end,
        category: category || null,
        reminderMinutesBefore: reminder ? parseInt(reminder) : null
    };
    
    try {
        const url = editingIndex >= 0 
            ? `${API_BASE}/entries/${editingIndex}`
            : `${API_BASE}/entries`;
        
        const method = editingIndex >= 0 ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(entry)
        });
        
        if (response.ok) {
            setStatus('Termin gespeichert');
            closeModal();
            await loadEntries();
        } else {
            alert('Fehler beim Speichern des Termins');
        }
    } catch (error) {
        console.error('Fehler beim Speichern:', error);
        alert('Fehler beim Speichern des Termins');
    }
}

async function deleteEntry(index) {
    if (!confirm('Möchten Sie diesen Termin wirklich löschen?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/entries/${index}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            setStatus('Termin gelöscht');
            await loadEntries();
        } else {
            alert('Fehler beim Löschen des Termins');
        }
    } catch (error) {
        console.error('Fehler beim Löschen:', error);
        alert('Fehler beim Löschen des Termins');
    }
}

// Import/Export
async function importFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    
    const formData = new FormData();
    formData.append('file', file);
    
    setStatus('Importiere...');
    
    try {
        const response = await fetch(`${API_BASE}/import`, {
            method: 'POST',
            body: formData
        });
        
        if (response.ok) {
            const message = await response.text();
            setStatus(message);
            await loadEntries();
        } else {
            const error = await response.text();
            alert('Fehler beim Import: ' + error);
            setStatus('Import fehlgeschlagen');
        }
    } catch (error) {
        console.error('Fehler beim Import:', error);
        alert('Fehler beim Import: ' + error.message);
        setStatus('Import fehlgeschlagen');
    }
    
    // Reset file input
    e.target.value = '';
}

async function exportCalendar() {
    try {
        const response = await fetch(`${API_BASE}/export`);
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'calendar-export.ics';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            setStatus('Kalender exportiert');
        } else {
            alert('Fehler beim Export');
        }
    } catch (error) {
        console.error('Fehler beim Export:', error);
        alert('Fehler beim Export');
    }
}

// Utility Functions
function formatDate(date) {
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
}

function formatTime(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
}

function formatDateInput(date) {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function formatTimeInput(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
