/**
 * NutriTrack AI — REST API Client (Connecting to Java Backend)
 */
// 🔧 DEPLOYMENT CONFIG
// For local dev:   keep as 'http://localhost:8080/api'
// For production:  replace with your Render/Railway backend URL
//                  e.g. 'https://nutritrack-ai.onrender.com/api'
const BACKEND_URL = window.__NUTRITRACK_BACKEND__ 
    || (window.location.origin && window.location.origin !== 'null'
        ? `${window.location.origin}/api`
        : '/api');

const API_BASE_URL = BACKEND_URL;

const Api = {
    getHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        if (window.AuthModule && window.AuthModule.getCurrentUserId()) {
            headers['X-User-Id'] = window.AuthModule.getCurrentUserId().toString();
        }
        return headers;
    },

    // 1. Authentication
    async login(email, password) {
        const res = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        return await res.json();
    },

    async signup(payload) {
        const res = await fetch(`${API_BASE_URL}/auth/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        return await res.json();
    },

    async googleLogin(name, email) {
        const res = await fetch(`${API_BASE_URL}/auth/google`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email })
        });
        return await res.json();
    },

    // 2. User Profile
    async getProfile() {
        const res = await fetch(`${API_BASE_URL}/profile`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to fetch user profile');
        return await res.json();
    },

    async updateProfile(profileData) {
        const res = await fetch(`${API_BASE_URL}/profile`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(profileData)
        });
        if (!res.ok) throw new Error('Failed to update user profile');
        return await res.json();
    },

    // 3. Google Gemini API Status & Key
    async getGeminiStatus() {
        const res = await fetch(`${API_BASE_URL}/gemini/status`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to fetch Gemini status');
        return await res.json();
    },

    async saveGeminiKey(apiKey) {
        const res = await fetch(`${API_BASE_URL}/gemini/key`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ apiKey })
        });
        if (!res.ok) throw new Error('Failed to save Gemini API key');
        return await res.json();
    },

    // 4. Foods Database
    async searchFoods(query = '', category = '') {
        const params = new URLSearchParams();
        if (query) params.append('q', query);
        if (category && category !== 'All') params.append('category', category);
        const res = await fetch(`${API_BASE_URL}/foods?${params.toString()}`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to search food items');
        return await res.json();
    },

    async createCustomFood(foodData) {
        const res = await fetch(`${API_BASE_URL}/foods`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify(foodData)
        });
        if (!res.ok) throw new Error('Failed to create custom food');
        return await res.json();
    },

    // 5. Meal Logs
    async getDailySummary(dateStr) {
        const res = await fetch(`${API_BASE_URL}/logs/daily?date=${dateStr}`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to fetch daily summary');
        return await res.json();
    },

    async logMeal(foodId, mealType, quantity, dateStr, notes = '') {
        const res = await fetch(`${API_BASE_URL}/logs`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ foodId, mealType, quantity, date: dateStr, notes })
        });
        if (!res.ok) throw new Error('Failed to log meal item');
        return await res.json();
    },

    async deleteMealLog(logId) {
        const res = await fetch(`${API_BASE_URL}/logs/${logId}`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to delete meal log');
        return await res.json();
    },

    // 6. Water Hydration
    async logWater(amountMl, dateStr) {
        const res = await fetch(`${API_BASE_URL}/water`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ amountMl, date: dateStr })
        });
        if (!res.ok) throw new Error('Failed to log water');
        return await res.json();
    },

    // 7. Weekly Trends
    async getWeeklyTrends() {
        const res = await fetch(`${API_BASE_URL}/analytics/weekly`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to fetch weekly trends');
        return await res.json();
    },

    // 8. NutriBot AI Chat (powered by Gemini)
    async sendChatMessage(message) {
        const res = await fetch(`${API_BASE_URL}/chat`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ message })
        });
        if (!res.ok) throw new Error('Failed to communicate with NutriBot AI');
        return await res.json();
    },

    async getChatHistory() {
        const res = await fetch(`${API_BASE_URL}/chat/history`, {
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to fetch chat history');
        return await res.json();
    },

    async clearChatHistory() {
        const res = await fetch(`${API_BASE_URL}/chat/history`, {
            method: 'DELETE',
            headers: this.getHeaders()
        });
        if (!res.ok) throw new Error('Failed to clear chat history');
        return await res.json();
    }
};
