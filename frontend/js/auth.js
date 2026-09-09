/**
 * NutriTrack AI — Authentication & Onboarding Module
 */
const AuthModule = {
    currentUser: null,

    async init() {
        this.bindEvents();
        this.checkExistingSession();
    },

    checkExistingSession() {
        const saved = localStorage.getItem('nutritrack_user');
        if (saved) {
            try {
                this.currentUser = JSON.parse(saved);
                this.showDashboard();
                return;
            } catch (e) {
                localStorage.removeItem('nutritrack_user');
            }
        }
        this.showLoginPortal();
    },

    showLoginPortal() {
        const overlay = document.getElementById('auth-portal-overlay');
        const layout = document.getElementById('main-app-layout');
        if (overlay) overlay.style.display = 'flex';
        if (layout) layout.style.display = 'none';
    },

    showDashboard() {
        const overlay = document.getElementById('auth-portal-overlay');
        const layout = document.getElementById('main-app-layout');
        if (overlay) overlay.style.display = 'none';
        if (layout) layout.style.display = 'flex';

        // Re-bootstrap user-specific components
        if (window.ProfileModule) window.ProfileModule.init();
        if (window.TrackerModule) window.TrackerModule.init();
        if (window.ChatbotModule) window.ChatbotModule.init();
        if (window.ChartsModule) window.ChartsModule.init();
    },

    getCurrentUserId() {
        return this.currentUser ? this.currentUser.id : 1;
    },

    bindEvents() {
        // Auth Tab Switching (Sign In vs Create Fresh Account)
        document.querySelectorAll('.auth-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.auth-tab-btn').forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.auth-tab-pane').forEach(p => p.classList.remove('active'));

                btn.classList.add('active');
                const target = btn.dataset.authTab;
                document.getElementById(`auth-tab-${target}`)?.classList.add('active');
            });
        });

        // 1. Sign In Form
        document.getElementById('login-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleLogin();
        });

        // 2. Signup Form
        document.getElementById('signup-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleSignup();
        });

        // 3. Google 1-Click Fast Access Button
        document.getElementById('btn-google-signin')?.addEventListener('click', async () => {
            await this.handleGoogleLogin();
        });

        // 4. Logout / Switch User
        document.getElementById('btn-logout')?.addEventListener('click', () => {
            this.handleLogout();
        });
    },

    async handleLogin() {
        const email = document.getElementById('login-email')?.value.trim();
        const password = document.getElementById('login-password')?.value;
        const errEl = document.getElementById('login-error-msg');
        if (errEl) errEl.textContent = '';

        try {
            const res = await Api.login(email, password);
            if (res.success && res.user) {
                this.currentUser = res.user;
                localStorage.setItem('nutritrack_user', JSON.stringify(res.user));
                App.showToast(`Welcome back, ${res.user.name}! 🌟`);
                this.showDashboard();
            } else {
                if (errEl) errEl.textContent = res.error || 'Invalid email or password.';
            }
        } catch (err) {
            console.error('Login error:', err);
            if (errEl) errEl.textContent = 'Server connection error. Please retry.';
        }
    },

    async handleSignup() {
        const errEl = document.getElementById('signup-error-msg');
        if (errEl) errEl.textContent = '';

        const name = document.getElementById('signup-name')?.value.trim();
        const email = document.getElementById('signup-email')?.value.trim();
        const password = document.getElementById('signup-password')?.value;
        const age = parseInt(document.getElementById('signup-age')?.value) || 28;
        const gender = document.getElementById('signup-gender')?.value || 'female';
        const heightCm = parseFloat(document.getElementById('signup-height')?.value) || 170;
        const weightKg = parseFloat(document.getElementById('signup-weight')?.value) || 65;
        const goal = document.getElementById('signup-goal')?.value || 'maintain';
        const activityLevel = document.getElementById('signup-activity')?.value || 'moderate';

        const signupPayload = {
            name, email, password, age, gender, heightCm, weightKg, goal, activityLevel,
            dietaryPref: 'omnivore', waterGoalLiters: 2.5, customCalorieTarget: 0
        };

        try {
            const res = await Api.signup(signupPayload);
            if (res.success && res.user) {
                this.currentUser = res.user;
                localStorage.setItem('nutritrack_user', JSON.stringify(res.user));
                App.showToast(`Account created! Welcome, ${res.user.name}! 🚀`);
                this.showDashboard();
            } else {
                if (errEl) errEl.textContent = res.error || 'Failed to create account.';
            }
        } catch (err) {
            console.error('Signup error:', err);
            if (errEl) errEl.textContent = 'Failed to create account. Please retry.';
        }
    },

    async handleGoogleLogin() {
        try {
            const res = await Api.googleLogin('Alex Morgan (Google Account)', 'alex.google@example.com');
            if (res.success && res.user) {
                this.currentUser = res.user;
                localStorage.setItem('nutritrack_user', JSON.stringify(res.user));
                App.showToast(`Signed in with Google! Welcome, ${res.user.name}! 🌟`);
                this.showDashboard();
            }
        } catch (err) {
            console.error('Google login error:', err);
        }
    },

    handleLogout() {
        localStorage.removeItem('nutritrack_user');
        this.currentUser = null;
        App.showToast('Logged out successfully.');
        this.showLoginPortal();
    }
};

window.AuthModule = AuthModule;
