# 🥗 NutriTrack AI — Personal Nutrition & Calorie Tracker

[![Java 21](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-8E75C2?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

A modern, full-stack clinical & sports nutrition tracking platform powered by a lightweight **Java REST Backend**, an embedded **SQLite Database**, and **Google Gemini AI** for smart conversational diet assistance.

---

## ✨ Features

- **📊 Interactive Calorie & Macro Dashboard**: Real-time tracking of calories, remaining daily deficit, macronutrients (Protein, Carbs, Fats), and micronutrients.
- **🤖 NutriBot Conversational AI**: Powered by Google Gemini (`gemini-3.7-flash` / `gemini-3.6-flash` / `gemini-3.5-flash`) for diet advice, recipe recommendations, workout nutrition, and natural-language food logging.
- **📅 Interactive Calendar Navigation**: Browse through days and months with a custom-styled interactive calendar widget.
- **🥗 Comprehensive Food Library**: 190+ whole and regional foods (Indian dishes, Mediterranean meals, and global staples) with instant autocomplete search.
- **💧 Smart Hydration Tracking**: Real-time water logging with daily hydration goals and progress animations.
- **📈 Weekly Trends & Visual Analytics**: SVG charts showing caloric intake vs. TDEE goals and macro distribution over time.
- **🎨 Glassmorphic & Neon Interface**: Refined color scheme with warm ivory navigation (`#FCF9EA`), golden amber unselected borders (`#E9A319`), soft olive accents (`#91AC67`), and luminous neon highlights (`#E9FF97`).

---

## 🏗️ Architecture & Project Structure

```
nutritrack-ai/
├── backend/
│   ├── data/                 # SQLite database storage (runtime)
│   ├── lib/                  # Bundled dependencies (SQLite JDBC, org.json, SLF4J)
│   └── src/com/nutritracker/
│       ├── controller/       # HTTP Request Handlers & API Routing (ApiController.java)
│       ├── dao/              # Database Access Objects (UserDao, FoodDao, MealLogDao, etc.)
│       ├── database/         # SQLite Connection & Seed Initialization (DatabaseManager.java)
│       ├── model/            # Domain Models (UserProfile, FoodItem, MealLog, etc.)
│       ├── server/           # Java Embedded HTTP Server (NutritionTrackerServer.java)
│       └── service/          # Business Logic (GeminiService, MealLogService, etc.)
├── frontend/
│   ├── assets/               # Static assets & icons
│   ├── css/
│   │   └── styles.css        # Glassmorphic UI styling with neon borders
│   ├── js/
│   │   ├── api.js            # Frontend REST API client
│   │   ├── app.js            # Application controller & state management
│   │   ├── auth.js           # Authentication & session handling
│   │   ├── chatbot.js        # NutriBot chat integration
│   │   ├── charts.js         # Interactive SVG analytics charts
│   │   ├── profile.js        # User profile, TDEE, and macro calculator
│   │   └── tracker.js        # Meal logging, food library search, and hydration
│   └── index.html            # Main single-page application entry point
├── Dockerfile                # Multi-stage Docker build for containerized deployment
├── render.yaml               # Render Cloud deployment blueprint
├── pom.xml                   # Maven project descriptor
├── start.sh                  # One-click startup script (macOS / Linux)
├── start.bat                 # One-click startup script (Windows)
├── .env.example              # Template for environment variables
├── .gitignore                # Standard repository exclusion rules
└── README.md                 # Project documentation
```

---

## 🚀 Quick Start (Local Setup)

### Prerequisites
- **Java Development Kit (JDK 21 or higher)**
- **Git**

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/nutritrack-ai.git
cd nutritrack-ai
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env` and insert your Google Gemini API key:
```bash
cp .env.example .env
```
Inside `.env`:
```ini
GEMINI_API_KEY=your_google_gemini_api_key_here
```
*(Note: If you do not provide an API key, all core tracking and food logging features will work fully, and NutriBot will operate with fallback intelligent nutritional responses).*

### 3. Run the Application

#### On macOS / Linux:
```bash
chmod +x start.sh
./start.sh
```

#### On Windows:
Double-click `start.bat` or run:
```cmd
start.bat
```

Once started, open **[http://localhost:8080](http://localhost:8080)** in your browser!

---

## 🐳 Docker Setup

You can build and run the application in a lightweight container:

```bash
# Build the Docker image
docker build -t nutritrack-ai .

# Run the container
docker run -p 8080:8080 -e GEMINI_API_KEY="your_api_key" nutritrack-ai
```

Access the app at **[http://localhost:8080](http://localhost:8080)**.

---

## 🌐 Cloud Deployment

### Deploy on Render (Recommended)
1. Push your repository to **GitHub**.
2. Log into **[Render.com](https://render.com)** and choose **New + Web Service**.
3. Link your GitHub repository.
4. Set the runtime environment to **Docker** (Render will use `Dockerfile` and `render.yaml` automatically).
5. Add your `GEMINI_API_KEY` under the **Environment Variables** section.
6. Click **Deploy Web Service** to get a live HTTPS URL!

---

## 📡 REST API Reference

| Endpoint | Method | Description |
| :--- | :---: | :--- |
| `/api/foods` | `GET` | Search the food database (`?q=apple&category=Fruits`) |
| `/api/foods` | `POST` | Create a custom food item |
| `/api/foods/categories` | `GET` | Fetch list of food categories |
| `/api/logs/daily` | `GET` | Fetch daily summary for a date (`?date=YYYY-MM-DD`) |
| `/api/logs` | `POST` | Log a food item with meal category and quantity |
| `/api/logs/{id}` | `DELETE` | Remove a logged food item |
| `/api/water` | `POST` | Log water consumption in mL |
| `/api/water/reset` | `POST` | Reset logged water for a date |
| `/api/analytics/weekly` | `GET` | Retrieve 7-day intake trends and macros |
| `/api/chat` | `POST` | Send message to NutriBot AI |
| `/api/chat/history` | `GET` | Retrieve recent chat conversation |
| `/api/profile` | `GET` / `PUT` | Read and update user profile & metrics |
| `/api/auth/login` | `POST` | User authentication |
| `/api/auth/signup` | `POST` | User registration |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
