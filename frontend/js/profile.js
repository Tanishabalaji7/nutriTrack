/**
 * NutriTrack AI — Profile & Energy Calculation Module
 */
const ProfileModule = {
    currentProfile: null,

    async init() {
        await this.loadProfile();
        this.bindEvents();
    },

    async loadProfile() {
        try {
            this.currentProfile = await Api.getProfile();
            this.renderProfileForm();
            this.renderSidebarUser();
            this.renderBioStatsSummary();
        } catch (err) {
            console.error('Error loading profile:', err);
        }
    },

    renderProfileForm() {
        if (!this.currentProfile) return;
        const p = this.currentProfile;

        const nameEl = document.getElementById('input-profile-name');
        const ageEl = document.getElementById('input-profile-age');
        const genderEl = document.getElementById('input-profile-gender');
        const heightEl = document.getElementById('input-profile-height');
        const weightEl = document.getElementById('input-profile-weight');
        const actEl = document.getElementById('input-profile-activity');
        const goalEl = document.getElementById('input-profile-goal');
        const dietEl = document.getElementById('input-profile-diet');
        const waterEl = document.getElementById('input-profile-water');
        const customCalEl = document.getElementById('input-profile-custom-cal');
        const geminiKeyEl = document.getElementById('input-profile-gemini-key');

        if (nameEl) nameEl.value = p.name || 'Alex Morgan';
        if (ageEl) ageEl.value = p.age || 28;
        if (genderEl) genderEl.value = p.gender || 'female';
        if (heightEl) heightEl.value = p.heightCm || 168;
        if (weightEl) weightEl.value = p.weightKg || 64;
        if (actEl) actEl.value = p.activityLevel || 'moderate';
        if (goalEl) goalEl.value = p.goal || 'maintain';
        if (dietEl) dietEl.value = p.dietaryPref || 'omnivore';
        if (waterEl) waterEl.value = p.waterGoalLiters || 2.5;
        if (customCalEl) customCalEl.value = p.customCalorieTarget || 0;
        if (geminiKeyEl) geminiKeyEl.value = p.geminiApiKey || '';
    },

    renderSidebarUser() {
        if (!this.currentProfile) return;
        const p = this.currentProfile;

        const nameEl = document.getElementById('sidebar-user-name');
        const metaEl = document.getElementById('sidebar-user-meta');
        const avatarEl = document.getElementById('avatar-initials');

        if (nameEl) nameEl.textContent = p.name;
        if (metaEl) {
            const goalLabel = p.goal === 'weight_loss' ? 'Deficit' : (p.goal === 'muscle_gain' ? 'Surplus' : 'Maintain');
            metaEl.textContent = `Age ${p.age} • ${goalLabel}`;
        }
        if (avatarEl && p.name) {
            const parts = p.name.split(' ');
            const initials = parts.length > 1 ? (parts[0][0] + parts[1][0]).toUpperCase() : p.name.substring(0, 2).toUpperCase();
            avatarEl.textContent = initials;
        }
    },

    renderBioStatsSummary() {
        if (!this.currentProfile) return;
        const p = this.currentProfile;

        // Compute BMR via Mifflin-St Jeor
        let bmr = (10 * p.weightKg) + (6.25 * p.heightCm) - (5 * p.age);
        if (p.gender === 'male') bmr += 5;
        else bmr -= 161;

        // Activity Multiplier
        let mult = 1.55;
        if (p.activityLevel === 'sedentary') mult = 1.2;
        else if (p.activityLevel === 'light') mult = 1.375;
        else if (p.activityLevel === 'active') mult = 1.725;
        else if (p.activityLevel === 'very_active') mult = 1.9;

        const tdee = bmr * mult;

        // Target Calories
        let targetCal = tdee;
        if (p.customCalorieTarget > 0) {
            targetCal = p.customCalorieTarget;
        } else if (p.goal === 'weight_loss') {
            targetCal = Math.max(1200, tdee - Math.min(500, tdee * 0.2));
        } else if (p.goal === 'muscle_gain') {
            targetCal = tdee + 380;
        }

        // Protein (g)
        let protG = Math.round(p.weightKg * (p.goal === 'muscle_gain' ? 2.0 : (p.goal === 'weight_loss' ? 1.8 : 1.4)));
        if (p.age >= 65) protG = Math.max(protG, Math.round(p.weightKg * 1.4));

        // Fat (g)
        const fatCalPercent = p.dietaryPref === 'keto' ? 0.70 : 0.28;
        const fatG = Math.round((targetCal * fatCalPercent) / 9);

        // Carbs (g)
        const carbG = p.dietaryPref === 'keto' ? 30 : Math.round(Math.max(50, targetCal - ((protG * 4) + (fatG * 9))) / 4);

        // Age Bracket Name
        let ageBracket = 'Young Adult (19-30)';
        if (p.age <= 3) ageBracket = 'Toddler (1-3)';
        else if (p.age <= 8) ageBracket = 'Child (4-8)';
        else if (p.age <= 18) ageBracket = 'Adolescent (9-18)';
        else if (p.age <= 50) ageBracket = 'Adult (31-50)';
        else if (p.age <= 70) ageBracket = 'Mature (51-70)';
        else ageBracket = 'Senior (71+)';

        const bmrEl = document.getElementById('bio-bmr-display');
        const tdeeEl = document.getElementById('bio-tdee-display');
        const targetEl = document.getElementById('bio-target-display');
        const protEl = document.getElementById('bio-protein-display');
        const carbEl = document.getElementById('bio-carbs-display');
        const fatEl = document.getElementById('bio-fats-display');
        const ageEl = document.getElementById('bio-age-bracket-display');

        if (bmrEl) bmrEl.textContent = `${Math.round(bmr)} kcal`;
        if (tdeeEl) tdeeEl.textContent = `${Math.round(tdee)} kcal`;
        if (targetEl) targetEl.textContent = `${Math.round(targetCal)} kcal / day`;
        if (protEl) protEl.textContent = `${protG} g / day`;
        if (carbEl) carbEl.textContent = `${carbG} g / day`;
        if (fatEl) fatEl.textContent = `${fatG} g / day`;
        if (ageEl) ageEl.textContent = ageBracket;
    },

    bindEvents() {
        const form = document.getElementById('profile-form');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this.handleSaveProfile();
            });
        }
    },

    async handleSaveProfile() {
        const statusEl = document.getElementById('profile-save-status');
        if (statusEl) statusEl.textContent = 'Saving to SQLite...';

        try {
            const updated = {
                name: document.getElementById('input-profile-name').value.trim(),
                age: parseInt(document.getElementById('input-profile-age').value) || 28,
                gender: document.getElementById('input-profile-gender').value,
                heightCm: parseFloat(document.getElementById('input-profile-height').value) || 168,
                weightKg: parseFloat(document.getElementById('input-profile-weight').value) || 64,
                activityLevel: document.getElementById('input-profile-activity').value,
                goal: document.getElementById('input-profile-goal').value,
                dietaryPref: document.getElementById('input-profile-diet').value,
                waterGoalLiters: parseFloat(document.getElementById('input-profile-water')?.value) || 2.5,
                customCalorieTarget: parseInt(document.getElementById('input-profile-custom-cal')?.value) || 0,
                geminiApiKey: (document.getElementById('input-profile-gemini-key') && document.getElementById('input-profile-gemini-key').value.trim()) 
                    || (this.currentProfile && this.currentProfile.geminiApiKey) 
                    || ''
            };

            this.currentProfile = await Api.updateProfile(updated);
            this.renderSidebarUser();
            this.renderBioStatsSummary();

            if (statusEl) {
                statusEl.textContent = '✓ Profile saved!';
                setTimeout(() => { statusEl.textContent = ''; }, 3000);
            }

            // Trigger global refresh for dashboard, chatbot, and charts
            if (window.TrackerModule) window.TrackerModule.refresh();
            if (window.ChartsModule) window.ChartsModule.refresh();
            if (window.ChatbotModule) window.ChatbotModule.checkGeminiStatus();

            App.showToast('Profile & Gemini API key updated successfully! 🎯✨');
        } catch (err) {
            console.error('Error saving profile:', err);
            if (statusEl) statusEl.textContent = 'Failed to save.';
        }
    }
};

window.ProfileModule = ProfileModule;
