# 🥗 NutriTrack AI — Personal Nutrition & Calorie Tracker

A modern, full-stack clinical & sports nutrition tracking platform powered by **Java REST Backend**, **SQLite Database**, and **Google Gemini AI**.

---

## ✨ Features
- **📊 Interactive Calorie & Macro Dashboard**: Track daily intake, remaining deficits, protein, carbs, fats, and water hydration.
- **🤖 NutriBot Conversational AI**: Powered by Google Gemini 3.7 Flash for personalized diet advice, recipe ideas, workout nutrition, and natural language food logging.
- **📅 Interactive Viewable Calendar**: Browse through months and days with an interactive `#FFBFA9` themed calendar widget.
- **🥗 Comprehensive Verified Foods Database**: 190+ whole foods, including authentic regional Indian dishes, Mediterranean bowls, and global staples.
- **💧 Smart Hydration Tracking**: Real-time water logging with daily hydration goal tracking.
- **📈 Weekly Trends & Analytics**: Historical intake charts and breakdown metrics.

---

## 🛠️ Tech Stack
- **Backend**: Java 21 / Java HTTP Server, JSON (org.json), SQLite JDBC
- **Database**: SQLite (`backend/data/nutrition_tracker.db`)
- **AI Engine**: Google Gemini API (`gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash`)
- **Frontend**: Vanilla HTML5, CSS3 Glassmorphism (`#B95E82`, `#F39F9F`, `#FFECC0`, `#FBFFB1`, `#FFBFA9`), JavaScript ES6+

---

## 🚀 Quick Start (Local Setup)

### 1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/nutritracker-ai.git
cd nutritracker-ai
```

### 2. Configure Environment Variables:
Copy `.env.example` to `.env` and add your Google Gemini API key:
```bash
cp .env.example .env
# Edit .env and set GEMINI_API_KEY=your_key_here
```

### 3. Run the Application:
```bash
chmod +x start.sh
./start.sh
```
Open **http://localhost:8080** in your browser!

---

## 🌐 Deploy as a Live Website (Free Cloud Hosting)

### Method 1: Deploy on Render (Recommended)
1. Push your repository to **GitHub**.
2. Go to **[Render.com](https://render.com)** and click **New + Web Service**.
3. Select your GitHub repository.
4. Choose **Docker** as the Environment (Render automatically detects the `Dockerfile`).
5. Under **Environment Variables**, add:
   - `GEMINI_API_KEY` = `your_google_gemini_api_key`
6. Click **Deploy Web Service** — you'll get a free live HTTPS URL (e.g. `https://nutritracker.onrender.com`)!

### Method 2: Deploy on Railway
1. Go to **[Railway.app](https://railway.app)**.
2. Click **New Project** > **Deploy from GitHub Repo**.
3. Add the `GEMINI_API_KEY` environment variable in Railway's Settings tab.
4. Railway will automatically build the `Dockerfile` and give you a public domain.
