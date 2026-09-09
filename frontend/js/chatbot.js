/**
 * NutriTrack AI — Conversational AI Nutritionist Bot Module (Powered by Google Gemini)
 */
const ChatbotModule = {
    history: [],
    geminiConfigured: false,

    async init() {
        await this.loadHistory();
        await this.checkGeminiStatus();
        this.bindEvents();
    },

    async checkGeminiStatus() {
        try {
            const status = await Api.getGeminiStatus();
            this.geminiConfigured = status.configured;
            this.updateGeminiUI(status);
        } catch (err) {
            console.warn('Could not check Gemini status:', err);
        }
    },

    updateGeminiUI(status) {
        const livePill = document.getElementById('chat-gemini-live-pill');
        const modelLabel = status.model ? status.model.replace('gemini-', 'Gemini ').replace('-flash', ' Flash') : 'Gemini 3.7 Flash';
        if (livePill) {
            if (status.configured) {
                livePill.innerHTML = `<span class="pulse-dot" style="background:#22c55e;"></span> ${modelLabel} Active`;
                livePill.classList.add('active');
            } else {
                livePill.innerHTML = '<span class="pulse-dot" style="background:#22c55e;"></span> NutriBot AI Active';
                livePill.classList.add('active');
            }
        }
    },

    async loadHistory() {
        try {
            const data = await Api.getChatHistory();
            this.history = data.history || [];
            if (this.history.length === 0) {
                // Initial Welcome Greeting
                this.history.push({
                    sender: 'bot',
                    message: "Hello! I am **NutriBot AI**, your smart personal nutritionist and diet coach powered by **Google Gemini**. 🍏\n\nAsk me **anything** — from custom Indian/global recipes, daily calorie deficits, and metabolic science to workouts and natural language food logging!\n\nTry asking:\n• *\"Suggest a high-protein vegetarian Indian dinner\"*\n• *\"I ate 2 rotis and palak paneer for lunch\"*\n• *\"How many calories do I have left today?\"*\n• *\"Explain the metabolic benefits of millets like Ragi and Bajra\"*",
                    actionType: 'general'
                });
            }
            this.renderMessages();
        } catch (err) {
            console.error('Error loading chat history:', err);
        }
    },

    renderMessages() {
        const fullContainer = document.getElementById('full-chat-messages');
        const floatContainer = document.getElementById('floating-chat-messages');

        if (fullContainer) {
            fullContainer.innerHTML = '';
            this.history.forEach(m => fullContainer.appendChild(this.createMessageElement(m)));
            fullContainer.scrollTop = fullContainer.scrollHeight;
        }

        if (floatContainer) {
            floatContainer.innerHTML = '';
            this.history.forEach(m => floatContainer.appendChild(this.createMessageElement(m)));
            floatContainer.scrollTop = floatContainer.scrollHeight;
        }
    },

    createMessageElement(msg) {
        const div = document.createElement('div');
        div.className = `chat-msg ${msg.sender}`;

        const isGemini = msg.actionType === 'gemini_ai';
        const formattedHtml = this.formatMarkdown(msg.message);

        div.innerHTML = `
            ${msg.sender === 'bot' ? `
                <div class="bot-avatar" style="width: 28px; height: 28px; font-size: 0.9rem;" title="${isGemini ? 'Google Gemini AI' : 'NutriBot'}">
                    ${isGemini ? '✨' : '🤖'}
                </div>
            ` : ''}
            <div class="chat-bubble ${isGemini ? 'gemini-bubble' : ''}">
                ${isGemini ? '<div class="gemini-msg-tag">✨ Gemini AI</div>' : ''}
                ${formattedHtml}
            </div>
        `;
        return div;
    },

    formatMarkdown(text) {
        if (!text) return '';
        let escaped = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // Headers
        escaped = escaped.replace(/^### (.*$)/gim, '<h4 style="margin: 8px 0 4px; font-size: 14px; font-weight: 700; color: #521830;">$1</h4>');
        escaped = escaped.replace(/^## (.*$)/gim, '<h3 style="margin: 10px 0 6px; font-size: 15px; font-weight: 800; color: #521830;">$1</h3>');

        // Bold
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        // Italic
        escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
        // Bullets
        escaped = escaped.replace(/^• (.*)$/gm, '<div style="margin-left: 10px; margin-top: 2px;">• $1</div>');
        escaped = escaped.replace(/^- (.*)$/gm, '<div style="margin-left: 10px; margin-top: 2px;">• $1</div>');
        // Line breaks
        escaped = escaped.replace(/\n/g, '<br>');

        return escaped;
    },

    bindEvents() {
        // Full Tab Chat Form
        document.getElementById('full-chat-form')?.addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('full-chat-input');
            if (input && input.value.trim()) {
                const text = input.value.trim();
                input.value = '';
                this.sendMessage(text);
            }
        });

        // Floating Chat Form
        document.getElementById('floating-chat-form')?.addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('floating-chat-input');
            if (input && input.value.trim()) {
                const text = input.value.trim();
                input.value = '';
                this.sendMessage(text);
            }
        });

        // Prompt Chips Click Handlers
        document.addEventListener('click', (e) => {
            const chip = e.target.closest('.chip-btn[data-prompt]');
            if (chip) {
                const prompt = chip.dataset.prompt;
                this.sendMessage(prompt);
            }
        });

        // Clear Chat History
        document.getElementById('btn-clear-chat')?.addEventListener('click', async () => {
            if (confirm('Clear all conversation history?')) {
                await Api.clearChatHistory();
                this.history = [];
                await this.loadHistory();
                App.showToast('Chat history cleared.');
            }
        });

        // Floating Chat Toggle & Minimize
        const floatToggle = document.getElementById('btn-toggle-floating-chat');
        const floatContainer = document.getElementById('floating-chat-container');
        const floatMinBtn = document.getElementById('btn-minimize-chat');

        floatToggle?.addEventListener('click', () => {
            floatContainer?.classList.toggle('open');
        });

        floatMinBtn?.addEventListener('click', () => {
            floatContainer?.classList.remove('open');
        });
    },

    async sendMessage(text) {
        // Append user message immediately
        const userMsg = { sender: 'user', message: text, timestamp: new Date().toISOString() };
        this.history.push(userMsg);
        this.renderMessages();

        // Add loading placeholder with shimmer animation
        const botLoading = { 
            sender: 'bot', 
            message: this.geminiConfigured ? '✨ Asking Gemini AI...' : 'Thinking & analyzing nutrition data...', 
            isLoading: true 
        };
        this.history.push(botLoading);
        this.renderMessages();

        try {
            const res = await Api.sendChatMessage(text);
            // Remove loading
            this.history.pop();

            const botMsg = {
                sender: 'bot',
                message: res.reply,
                actionType: res.actionType,
                metadata: res.metadata,
                timestamp: new Date().toISOString()
            };
            this.history.push(botMsg);
            this.renderMessages();

            // If action logged food or water, refresh tracker & charts
            if (res.actionType === 'logged_food' || res.actionType === 'logged_water') {
                if (window.TrackerModule) window.TrackerModule.refresh();
                if (window.ChartsModule) window.ChartsModule.refresh();
                App.showToast(res.actionType === 'logged_water' ? 'Hydration logged! 💧' : 'Food logged to meal diary! 🥗');
            }

        } catch (err) {
            console.error('Error sending chat message:', err);
            this.history.pop();
            this.history.push({
                sender: 'bot',
                message: "Sorry, I had trouble reaching the backend server. Please check your connection.",
                actionType: 'error'
            });
            this.renderMessages();
        }
    }
};

window.ChatbotModule = ChatbotModule;
