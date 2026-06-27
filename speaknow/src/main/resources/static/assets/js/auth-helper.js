(function() {
    const API_BASE_URL = '/api';
    
    // Check if user is already authenticated
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    
    if (token && user) {
        return; // Already authenticated
    }
    
    // Check if we are on login or register pages, do not auto-login there to allow manual overrides
    const path = window.location.pathname;
    if (path.includes('login.html') || path.includes('register.html')) {
        return;
    }
    
    // Generate guest username if not exists
    let guestUsername = localStorage.getItem('guest_username');
    if (!guestUsername) {
        guestUsername = 'guest_' + Math.random().toString(36).substring(2, 9);
        localStorage.setItem('guest_username', guestUsername);
    }
    const guestEmail = `${guestUsername}@speaknow.ai`;
    const guestPassword = 'GuestPassword123!';
    
    // Inject stylesheet for guest loading screen
    const style = document.createElement('style');
    style.id = 'guest-auth-style';
    style.textContent = `
        .guest-auth-loader {
            position: fixed;
            inset: 0;
            background: #fafafa;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            z-index: 100000;
            font-family: 'Inter', system-ui, sans-serif;
            transition: opacity 0.3s ease;
            color: #292524;
        }
        .dark .guest-auth-loader {
            background: #0c0a09;
            color: #f5f5f5;
        }
        .guest-spinner {
            width: 40px;
            height: 40px;
            border: 3px solid #e7e5e4;
            border-top-color: #292524;
            border-radius: 50%;
            animation: guest-spin 1s linear infinite;
            margin-bottom: 16px;
        }
        .dark .guest-spinner {
            border: 3px solid #292524;
            border-top-color: #a8a29e;
        }
        @keyframes guest-spin {
            to { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(style);
    
    // Create guest loader element
    const loader = document.createElement('div');
    loader.className = 'guest-auth-loader';
    loader.innerHTML = `
        <div class="guest-spinner"></div>
        <div style="font-size: 14px; font-weight: 600; letter-spacing: -0.01em;">Preparing your learning space...</div>
        <div style="font-size: 11px; color: #a8a29e; margin-top: 6px;">Setting up a guest account</div>
    `;
    document.body.appendChild(loader);
    
    // Auto register and login guest
    async function doAutoLogin() {
        try {
            // First, try register
            let res = await fetch(`${API_BASE_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: guestUsername,
                    email: guestEmail,
                    password: guestPassword,
                    name: 'Guest Learner'
                })
            });
            
            let data;
            if (res.ok) {
                data = await res.json();
            } else {
                // If registration failed (likely already registered), login
                res = await fetch(`${API_BASE_URL}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: guestUsername,
                        password: guestPassword
                    })
                });
                if (res.ok) {
                    data = await res.json();
                } else {
                    throw new Error('Guest authentication failed');
                }
            }
            
            // Save authentication details
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            localStorage.setItem('userId', data.user.id);
            
            // Fade out loader and reload
            loader.style.opacity = '0';
            setTimeout(() => {
                loader.remove();
                window.location.reload();
            }, 300);
            
        } catch (e) {
            console.error('Guest auth failed:', e);
            loader.innerHTML = `
                <div style="color: #dc2626; font-weight: 600; margin-bottom: 8px;">Connection Error</div>
                <div style="font-size: 13px; color: #78716c; margin-bottom: 16px; text-align: center; max-width: 250px;">Failed to connect to the learning server. Please ensure the backend is running.</div>
                <button id="guest-retry-btn" style="background: #292524; color: white; border: none; padding: 10px 20px; border-radius: 99px; cursor: pointer; font-size: 12px; font-weight: 600; transition: background 0.2s;">Retry Connecting</button>
            `;
            const btn = document.getElementById('guest-retry-btn');
            if (btn) {
                btn.addEventListener('click', () => window.location.reload());
            }
        }
    }
    
    // Run doAutoLogin
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', doAutoLogin);
    } else {
        doAutoLogin();
    }
})();
