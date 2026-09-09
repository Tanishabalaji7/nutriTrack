/**
 * NutriTrack AI — Main Application Orchestrator
 */
const App = {
    async init() {
        console.log('Initializing NutriTrack AI Application...');

        // 1. Initialize Auth Portal (Login / Registration)
        if (window.AuthModule) {
            await window.AuthModule.init();
        }

        // 2. Navigation Tabs
        this.bindNavigation();

        // 3. Mouse Parallax for Floating Food Elements
        this.bindFoodParallax();
    },

    bindNavigation() {
        const navButtons = document.querySelectorAll('.nav-item');
        navButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.dataset.tab;
                if (!targetTab) return;

                // Update nav classes
                navButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                // Update panes
                document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
                const activePane = document.getElementById(`tab-${targetTab}`);
                if (activePane) {
                    activePane.classList.add('active');
                }

                // Refresh specific tab data if opened
                if (targetTab === 'analytics' && window.ChartsModule) {
                    window.ChartsModule.refresh();
                } else if (targetTab === 'food-diary' && window.TrackerModule) {
                    window.TrackerModule.refresh();
                } else if (targetTab === 'profile-settings' && window.ProfileModule) {
                    window.ProfileModule.init();
                }
            });
        });
    },

    bindFoodParallax() {
        document.addEventListener('mousemove', (e) => {
            const x = (e.clientX / window.innerWidth) - 0.5;
            const y = (e.clientY / window.innerHeight) - 0.5;

            const foods = document.querySelectorAll('.food-element');
            foods.forEach((food, idx) => {
                const factor = (idx + 1) * 12;
                food.style.transform = `translate(${x * factor}px, ${y * factor}px)`;
            });
        });
    },

    showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>✨</span> <div>${message}</div>`;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }
};

window.App = App;

// Bootstrap on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
