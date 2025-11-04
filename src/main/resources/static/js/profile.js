// Simple API helper mirroring script.js
async function apiCall(endpoint, method = 'GET', data = null) {
  const options = { method, headers: { 'Content-Type': 'application/json' } };
  if (data && method !== 'GET') options.body = JSON.stringify(data);
  const res = await fetch(`/api${endpoint}`, options);
  if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
  return res.json();
}

function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast ${type}`;
  toast.style.display = 'block';
  setTimeout(() => { toast.style.display = 'none'; }, 3000);
}

function getSession() {
  try { return JSON.parse(localStorage.getItem('pc_user')); } catch { return null; }
}

document.addEventListener('DOMContentLoaded', async () => {
  const session = getSession();
  if (!session) { window.location.href = '/'; return; }
  const userId = session.id || 0;
  document.getElementById('profile-role').textContent = session.role || 'USER';

  try {
    const user = await apiCall(`/users/${userId}`);
    // Populate fields
    document.getElementById('pf-fullName').value = user.fullName || '';
    document.getElementById('pf-email').value = user.email || '';
    document.getElementById('pf-phone').value = user.phoneNumber || '';
    if (user.bloodGroup) document.getElementById('pf-blood').value = user.bloodGroup;
    document.getElementById('pf-city').value = user.city || '';
    document.getElementById('pf-state').value = user.state || '';
    document.getElementById('pf-pincode').value = user.pincode || '';
    document.getElementById('pf-address').value = user.address || '';
  } catch (e) {
    // Backend not rebuilt yet; fill from session if available
    document.getElementById('pf-email').value = session.email || '';
    document.getElementById('pf-fullName').value = session.username || '';
  }

  document.getElementById('profile-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      fullName: document.getElementById('pf-fullName').value.trim(),
      phoneNumber: document.getElementById('pf-phone').value.trim(),
      bloodGroup: document.getElementById('pf-blood').value,
      address: document.getElementById('pf-address').value.trim(),
      city: document.getElementById('pf-city').value.trim(),
      state: document.getElementById('pf-state').value.trim(),
      pincode: document.getElementById('pf-pincode').value.trim()
    };

    try {
      await apiCall(`/users/${userId}`, 'PUT', payload);
      showToast('Profile updated', 'success');
    } catch (err) {
      // Fallback if backend not available
      const sess = getSession() || {}; 
      sess.username = payload.fullName || sess.username; 
      localStorage.setItem('pc_user', JSON.stringify(sess));
      showToast('Saved locally (demo). Rebuild server to persist.', 'success');
    }
  });
});
