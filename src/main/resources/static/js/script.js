// API Base URL
const API_BASE_URL = '/api';

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeAnimations();
    loadStatistics();
    setupSmoothScroll();
    setupNavigation();
    updateAuthUI();
});
// ===== Auth UI =====
function isLoggedIn() {
    try { return !!JSON.parse(localStorage.getItem('pc_user')); } catch { return false; }
}

function updateAuthUI() {
    const btn = document.getElementById('login-button');
    const profileLink = document.getElementById('profile-link');
    if (!btn) return;
    if (isLoggedIn()) {
        btn.innerHTML = '<i class="fas fa-sign-out-alt"></i> Logout';
        btn.onclick = logout;
        if (profileLink) profileLink.style.display = '';
    } else {
        btn.innerHTML = '<i class="fas fa-sign-in-alt"></i> Login';
        btn.onclick = showLoginModal;
        if (profileLink) profileLink.style.display = 'none';
    }
}

function logout() {
    localStorage.removeItem('pc_user');
    updateAuthUI();
    showToast('Logged out', 'success');
}


// ===== Statistics Animation =====
function loadStatistics() {
    // Simulate loading statistics with animation
    animateCounter('donor-count', 0, 1247, 2000);
    animateCounter('donation-count', 0, 3891, 2000);
    animateCounter('response-time', 0, 15, 2000);
}

function animateCounter(elementId, start, end, duration) {
    const element = document.getElementById(elementId);
    const range = end - start;
    const increment = range / (duration / 16);
    let current = start;
    
    const timer = setInterval(() => {
        current += increment;
        if (current >= end) {
            element.textContent = Math.round(end);
            clearInterval(timer);
        } else {
            element.textContent = Math.round(current);
        }
    }, 16);
}

// ===== Navigation =====
function setupNavigation() {
    // Active link highlighting on scroll
    window.addEventListener('scroll', () => {
        const sections = document.querySelectorAll('section[id]');
        const navLinks = document.querySelectorAll('.nav-link');
        
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.clientHeight;
            if (window.pageYOffset >= sectionTop - 100) {
                current = section.getAttribute('id');
            }
        });
        
        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    });
}

function toggleMenu() {
    const navMenu = document.querySelector('.nav-menu');
    navMenu.style.display = navMenu.style.display === 'flex' ? 'none' : 'flex';
}

// ===== Smooth Scrolling =====
function setupSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
}

function scrollToSection(sectionId) {
    const section = document.getElementById(sectionId);
    if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

// ===== Search Donors =====
async function searchDonors() {
    const bloodGroup = document.getElementById('blood-group').value;
    const location = document.getElementById('location').value;
    const radius = document.getElementById('radius').value;
    
    if (!bloodGroup) {
        showToast('Please select a blood group', 'error');
        return;
    }
    
    if (!location) {
        showToast('Please select a district/city', 'error');
        return;
    }
    
    const resultsContainer = document.getElementById('donor-results');
    resultsContainer.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Searching for donors...</div>';
    
    try {
        // Call backend API (include radius)
        const response = await fetch(`/api/donors/search?bloodGroup=${bloodGroup}&city=${location}&radius=${radius}`);
        if (!response.ok) throw new Error('API error');
        const donors = await response.json();
        displayDonorResults(donors);
    } catch (error) {
        console.error('Error searching donors:', error);
        showToast('Error searching donors. Please try again.', 'error');
        resultsContainer.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-circle"></i><p>Error loading results</p></div>';
    }
}

function generateMockDonors(bloodGroup) {
    const names = ['Rajesh Kumar', 'Priya Sharma', 'Amit Patel', 'Sneha Reddy', 'Vikram Singh'];
    const donors = [];
    
    for (let i = 0; i < 5; i++) {
        donors.push({
            id: i + 1,
            name: names[i],
            bloodGroup: bloodGroup,
            location: 'Mumbai, Maharashtra',
            distance: Math.floor(Math.random() * 20) + 1,
            totalDonations: Math.floor(Math.random() * 15) + 1,
            lastDonation: '2 months ago',
            available: Math.random() > 0.3
        });
    }
    
    return donors;
}

function displayDonorResults(donors) {
    const resultsContainer = document.getElementById('donor-results');
    // Store latest donor results globally for bulk SMS use
    window.latestDonorResults = donors || [];
    
    if (!donors || donors.length === 0) {
        resultsContainer.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-users"></i>
                <p>No donors found matching your criteria</p>
            </div>
        `;
        return;
    }
    
    resultsContainer.innerHTML = donors.map(donor => `
        <div class="donor-card">
            <div class="donor-avatar">${donor.bloodGroup || donor.name.charAt(0)}</div>
            <div class="donor-info">
                <h4>${donor.name}</h4>
                <div class="donor-meta">
                    <span><i class="fas fa-map-marker-alt"></i> ${donor.city || donor.location || 'Unknown'}</span>
                    <span><i class="fas fa-road"></i> ${donor.distance} km away</span>
                    <span><i class="fas fa-heart"></i> ${donor.totalDonations} donations</span>
                    ${donor.phone ? `<span><i class="fas fa-phone"></i> ${formatPhoneDisplay(donor.phone)}</span>` : ''}
                </div>
            </div>
            <div class="donor-actions" style="display:flex; gap:8px; align-items:center;">
                <a class="btn-primary" ${donor.phone ? `href="tel:${sanitizePhone(donor.phone)}"` : ''} ${donor.phone ? '' : 'style="pointer-events:none;opacity:.6;"'}>
                    <i class="fas fa-phone"></i> Call
                </a>
                <button class="btn-secondary" onclick="copyPhone('${donor.phone || ''}')" ${donor.phone ? '' : 'disabled'}>
                    <i class="fas fa-copy"></i> Copy
                </button>
            </div>
        </div>
    `).join('');
    
    showToast(`Found ${donors.length} donor(s) in your area`, 'success');
}

function contactDonor(donorId) {
    showToast('Contact request sent to donor!', 'success');
    // Implement actual contact logic here
}

// ===== Emergency Request =====
async function submitEmergencyRequest() {
    const patientName = document.getElementById('patient-name').value;
    const contactNumber = document.getElementById('contact-number').value;
    const bloodGroup = document.getElementById('emergency-blood-group').value;
    const unitsRequired = document.getElementById('units-required').value;
    const hospitalLocation = document.getElementById('hospital-location').value;
    const details = document.getElementById('emergency-details').value;
    
    // Validation
    if (!patientName || !contactNumber || !bloodGroup || !hospitalLocation) {
        showToast('Please fill all required fields', 'error');
        return;
    }
    
    const requestData = {
        patientName,
        contactNumber,
        requiredBloodGroup: bloodGroup,
        unitsRequired: parseInt(unitsRequired),
        hospitalLocation,
        urgencyLevel: 'CRITICAL',
        additionalDetails: details
    };
    
    try {
        showToast('Sending emergency request...', 'success');
        const response = await fetch(`/api/donors/emergency`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        });
        if (response.ok) {
            const result = await response.json();
            showToast(result.message || 'Emergency request sent successfully!', 'success');
            clearEmergencyForm();
        } else {
            throw new Error('Failed to submit request');
        }
    } catch (error) {
        console.error('Error submitting emergency request:', error);
        showToast('Error submitting request. Please try again.', 'error');
    }
}

function clearEmergencyForm() {
    document.getElementById('patient-name').value = '';
    document.getElementById('contact-number').value = '';
    document.getElementById('emergency-blood-group').value = '';
    document.getElementById('units-required').value = '1';
    document.getElementById('hospital-location').value = '';
    document.getElementById('emergency-details').value = '';
}

// ===== Fast2SMS single-send (trial-friendly) =====
async function sendFast2Sms() {
    // Elements for manual override/display
    const smsNumberEl = document.getElementById('sms-number');
    const smsMessageEl = document.getElementById('sms-message');

    // Gather data from emergency form
    const patientName = (document.getElementById('patient-name').value || '').trim();
    const contactNumberForm = (document.getElementById('contact-number').value || '').replace(/\D/g, '').slice(-10);
    const bloodGroup = (document.getElementById('emergency-blood-group').value || '').replace('_','+');
    const units = (document.getElementById('units-required').value || '1').trim();
    const hospital = (document.getElementById('hospital-location').value || '').trim();

    // Construct default message
    const defaultMsg = patientName && hospital && bloodGroup
        ? `URGENT: ${patientName} needs ${units} unit(s) of ${bloodGroup} at ${hospital}. Contact: ${contactNumberForm}. - PulseConnect`
        : '';

    // If fields are empty, prefill them from the form so user can see/edit
    if (smsNumberEl && (!smsNumberEl.value || !smsNumberEl.value.trim())) {
        smsNumberEl.value = contactNumberForm || '';
    }
    if (smsMessageEl && (!smsMessageEl.value || !smsMessageEl.value.trim())) {
        smsMessageEl.value = defaultMsg;
    }

    const number = (smsNumberEl ? smsNumberEl.value : contactNumberForm || '').replace(/\D/g, '').slice(-10);
    const message = (smsMessageEl ? smsMessageEl.value : defaultMsg || '').trim();

    // Validate
    if (!message) {
        showToast('Enter an SMS message (or fill the emergency form to auto-fill)', 'error');
        return;
    }
    if (!number || number.length !== 10) {
        showToast('Enter a valid 10-digit mobile number', 'error');
        return;
    }

    try {
        const res = await fetch(`/sendSMS`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message, number })
        });
        const data = await res.json().catch(() => ({}));
        if (res.ok && data.status === 'success') {
            showToast(data.info || 'SMS sent successfully via Fast2SMS', 'success');
        } else {
            const info = data.info || `HTTP ${res.status}`;
            showToast(`Failed to send SMS: ${info}`, 'error');
        }
    } catch (err) {
        console.error('Fast2SMS error', err);
        showToast('Network error sending SMS', 'error');
    }
}

// Send Fast2SMS bulk to donors currently displayed in results
async function sendFast2SmsBulkFromResults() {
    const donors = Array.isArray(window.latestDonorResults) ? window.latestDonorResults : [];
    if (!donors.length) {
        showToast('No donors loaded. Search donors first.', 'error');
        return;
    }

    // Build message based on current emergency form inputs
    const patientName = (document.getElementById('patient-name').value || '').trim();
    const contactNumber = (document.getElementById('contact-number').value || '').replace(/\D/g, '').slice(-10);
    const bloodGroup = (document.getElementById('emergency-blood-group').value || '').replace('_','+');
    const units = (document.getElementById('units-required').value || '1').trim();
    const hospital = (document.getElementById('hospital-location').value || '').trim();

    if (!patientName || !hospital || !bloodGroup) {
        showToast('Fill required emergency form fields first', 'error');
        return;
    }
    if (!contactNumber || contactNumber.length !== 10) {
        showToast('Enter a valid 10-digit contact number', 'error');
        return;
    }

    // Collect up to 200 donor phone numbers
    const numbers = donors
        .map(d => (d.phone || '').replace(/\D/g, '').slice(-10))
        .filter(n => n && n.length === 10);
    if (!numbers.length) {
        showToast('No donor numbers available to notify', 'error');
        return;
    }
    const limited = numbers.slice(0, 200);

    // Allow override from SMS message textbox if present
    const smsMessageEl = document.getElementById('sms-message');
    const defaultMessage = `URGENT: ${patientName} needs ${units} unit(s) of ${bloodGroup} at ${hospital}. Contact: ${contactNumber}. - PulseConnect`;
    const message = (smsMessageEl && smsMessageEl.value && smsMessageEl.value.trim()) ? smsMessageEl.value.trim() : defaultMessage;

    try {
        const res = await fetch(`/sendSMS/bulk`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message, numbers: limited })
        });
        const data = await res.json().catch(() => ({}));
        if (res.ok && data.status === 'success') {
            showToast(data.info || `Sent to ${data.sent || limited.length} donor(s)`, 'success');
        } else {
            const info = data.info || `HTTP ${res.status}`;
            showToast(`Bulk SMS failed: ${info}`, 'error');
        }
    } catch (err) {
        console.error('Fast2SMS bulk error', err);
        showToast('Network error sending bulk SMS', 'error');
    }
}

// ===== Modal Functions =====
function showLoginModal() {
    closeAllModals();
    document.getElementById('login-modal').style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function showRegisterModal() {
    closeAllModals();
    document.getElementById('register-modal').style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
    document.body.style.overflow = 'auto';
}

function closeAllModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.style.display = 'none';
    });
    document.body.style.overflow = 'auto';
}

// Close modal when clicking outside
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        closeAllModals();
    }
}

// ===== Toast Notifications =====
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.style.display = 'block';
    
    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}

// ===== Animations =====
function initializeAnimations() {
    // Intersection Observer for scroll animations
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, { threshold: 0.1 });
    
    document.querySelectorAll('.feature-card, .donor-card').forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'all 0.6s ease-out';
        observer.observe(card);
    });
}

// ===== Form Submissions =====
document.addEventListener('submit', async function(e) {
    if (!e.target.classList.contains('modal-form')) return;
    e.preventDefault();

    const modal = e.target.closest('.modal');
    const isLogin = modal.id === 'login-modal';

    try {
        if (isLogin) {
            const username = document.getElementById('login-username').value.trim();
            const password = document.getElementById('login-password').value;
            if (!username || !password) {
                showToast('Please enter username/email and password', 'error');
                return;
            }

            const res = await apiCall(`/auth/login`, 'POST', { username, password });
            // store basic session info
            localStorage.setItem('pc_user', JSON.stringify(res));
            showToast(res.message || 'Logged in successfully', 'success');
            closeAllModals();
            updateAuthUI();
        } else {
            // Registration
            const fullName = document.getElementById('reg-fullname').value.trim();
            const email = document.getElementById('reg-email').value.trim();
            const phone = (document.getElementById('reg-phone').value || '').replace(/\D/g, '').slice(-10);
            const bloodGroup = document.getElementById('reg-blood-group').value;
            const city = document.getElementById('reg-city').value;
            const pincode = (document.getElementById('reg-pincode').value || '').trim();
            const address = document.getElementById('reg-address').value.trim();
            const password = document.getElementById('reg-password').value;

            if (!fullName || !email || !phone || !bloodGroup || !city || !pincode || !address || !password) {
                showToast('Please fill all required fields', 'error');
                return;
            }

            const payload = {
                username: email,
                email,
                password,
                fullName,
                phoneNumber: phone,
                bloodGroup,
                address,
                city,
                state: 'Telangana',
                pincode,
                role: 'DONOR',
                preferredLanguage: 'en'
            };

            await apiCall(`/auth/register`, 'POST', payload);
            showToast('Registration successful. You can now login.', 'success');
            closeAllModals();
            showLoginModal();
        }
    } catch (err) {
        console.error(err);
        const msg = (err && err.message) ? err.message : '';
        if (isLogin && msg.includes('status: 404')) {
            // Demo fallback: simulate a successful login so UI can be used before backend rebuild
            const username = document.getElementById('login-username').value.trim();
            const fake = { token: 'demo-token', id: 0, username, email: username, role: 'DONOR', message: 'Logged in (demo)' };
            localStorage.setItem('pc_user', JSON.stringify(fake));
            showToast('Logged in (demo mode)', 'success');
            closeAllModals();
            updateAuthUI();
        } else if (!isLogin && msg.includes('status: 404')) {
            showToast('Registration UI submitted (demo). Backend will be enabled after server rebuild.', 'success');
            closeAllModals();
            showLoginModal();
        } else {
            showToast(typeof err === 'string' ? err : 'Operation failed. Please try again.', 'error');
        }
    }
});

// ===== Utility Functions =====
function formatBloodGroup(bloodGroup) {
    return bloodGroup.replace('_', ' ');
}

function sanitizePhone(phone) {
    return (phone || '').toString().replace(/\D/g, '');
}

function formatPhoneDisplay(phone) {
    const p = sanitizePhone(phone);
    if (p.length === 10) return `+91 ${p.slice(0,5)} ${p.slice(5)}`;
    return phone || '';
}

function copyPhone(phone) {
    const p = sanitizePhone(phone);
    if (!p) { showToast('No phone number available', 'error'); return; }
    navigator.clipboard.writeText(p).then(() => {
        showToast('Phone number copied', 'success');
    }).catch(() => showToast('Failed to copy', 'error'));
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
    });
}

// ===== API Integration Helper =====
async function apiCall(endpoint, method = 'GET', data = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    if (data && method !== 'GET') {
        options.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        if (!response.ok) {
            let body = '';
            try { body = await response.text(); } catch {}
            const msg = body && body.length < 300 ? body : `HTTP error ${response.status}`;
            throw new Error(msg);
        }
        return await response.json();
    } catch (error) {
        console.error('API call error:', error);
        throw error;
    }
}

// ===== Blood Group Compatibility =====
const bloodGroupCompatibility = {
    'O_NEGATIVE': ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
    'O_POSITIVE': ['O+', 'A+', 'B+', 'AB+'],
    'A_NEGATIVE': ['A-', 'A+', 'AB-', 'AB+'],
    'A_POSITIVE': ['A+', 'AB+'],
    'B_NEGATIVE': ['B-', 'B+', 'AB-', 'AB+'],
    'B_POSITIVE': ['B+', 'AB+'],
    'AB_NEGATIVE': ['AB-', 'AB+'],
    'AB_POSITIVE': ['AB+']
};

function getCompatibleBloodGroups(bloodGroup) {
    return bloodGroupCompatibility[bloodGroup] || [];
}

// ===== Real-time Updates (WebSocket - for future implementation) =====
function initializeWebSocket() {
    // WebSocket connection for real-time updates
    // const ws = new WebSocket('ws://localhost:8080/ws');
    // ws.onmessage = (event) => {
    //     const data = JSON.parse(event.data);
    //     handleRealtimeUpdate(data);
    // };
}

function handleRealtimeUpdate(data) {
    if (data.type === 'EMERGENCY_ALERT') {
        showToast('New emergency blood request received!', 'error');
    }
}

// ===== Keyboard Shortcuts =====
document.addEventListener('keydown', function(e) {
    // Escape key to close modals
    if (e.key === 'Escape') {
        closeAllModals();
    }
});

console.log('%c🩸 Pulse Connect - Smart Blood Donor Directory', 'color: #dc143c; font-size: 20px; font-weight: bold;');
console.log('%cPowered by Java 21 + Spring Boot 3.4.0', 'color: #4299e1; font-size: 14px;');
