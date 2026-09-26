/**
 * NutriTrack AI — SVG Interactive Analytics & Trend Visualizer
 */
const ChartsModule = {
    weeklyData: [],

    async init() {
        await this.loadData();
    },

    async refresh() {
        await this.loadData();
    },

    async loadData() {
        try {
            const res = await Api.getWeeklyTrends();
            this.weeklyData = res.weeklyData || [];
            this.renderCalorieBarChart();
            this.renderMacroAreaChart();
        } catch (err) {
            console.error('Error loading weekly trends:', err);
        }
    },

    renderCalorieBarChart() {
        const container = document.getElementById('weekly-calorie-chart');
        if (!container || this.weeklyData.length === 0) return;

        const data = this.weeklyData;
        const width = 520;
        const height = 260;
        const padding = { top: 30, right: 20, bottom: 40, left: 50 };

        const chartW = width - padding.left - padding.right;
        const chartH = height - padding.top - padding.bottom;

        const maxVal = Math.max(2500, ...data.map(d => Math.max(d.calories, d.targetCalories))) * 1.1;

        const barWidth = 32;
        const slotWidth = chartW / data.length;

        let barsSvg = '';
        let labelsSvg = '';
        let targetLinesSvg = '';

        data.forEach((d, idx) => {
            const x = padding.left + (idx * slotWidth) + (slotWidth - barWidth) / 2;
            const barH = (d.calories / maxVal) * chartH;
            const y = padding.top + chartH - barH;

            const targetY = padding.top + chartH - ((d.targetCalories / maxVal) * chartH);

            const isMet = d.calories > 0 && Math.abs(d.calories - d.targetCalories) < 300;
            const barFill = d.calories === 0 ? 'rgba(255,255,255,0.06)' : (isMet ? 'url(#bar-gradient-green)' : 'url(#bar-gradient-cyan)');

            barsSvg += `
                <rect x="${x}" y="${y}" width="${barWidth}" height="${barH}" rx="6" fill="${barFill}" opacity="0.9">
                    <title>${d.dayName} (${d.date}): ${d.calories} kcal / Target: ${d.targetCalories} kcal</title>
                </rect>
                <text x="${x + barWidth / 2}" y="${y - 8}" fill="#FFF" font-size="11" font-weight="700" text-anchor="middle" font-family="'Outfit', sans-serif">
                    ${d.calories > 0 ? d.calories : ''}
                </text>
            `;

            // Target marker line
            targetLinesSvg += `
                <line x1="${x - 4}" y1="${targetY}" x2="${x + barWidth + 4}" y2="${targetY}" stroke="#F43F5E" stroke-width="2" stroke-dasharray="3,3" opacity="0.8">
                    <title>Target: ${d.targetCalories} kcal</title>
                </line>
            `;

            labelsSvg += `
                <text x="${x + barWidth / 2}" y="${height - 14}" fill="#94A3B8" font-size="12" font-weight="600" text-anchor="middle" font-family="'Outfit', sans-serif">
                    ${d.dayName}
                </text>
            `;
        });

        container.innerHTML = `
            <svg class="chart-svg" viewBox="0 0 ${width} ${height}">
                <defs>
                    <linearGradient id="bar-gradient-green" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#B95E82" />
                        <stop offset="100%" stop-color="#F39F9F" />
                    </linearGradient>
                    <linearGradient id="bar-gradient-cyan" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#4FB7B3" />
                        <stop offset="100%" stop-color="#D8FFC5" />
                    </linearGradient>
                </defs>
                <!-- Grid Lines -->
                <line x1="${padding.left}" y1="${padding.top}" x2="${width - padding.right}" y2="${padding.top}" stroke="rgba(185,94,130,0.12)" />
                <line x1="${padding.left}" y1="${padding.top + chartH/2}" x2="${width - padding.right}" y2="${padding.top + chartH/2}" stroke="rgba(185,94,130,0.12)" />
                <line x1="${padding.left}" y1="${padding.top + chartH}" x2="${width - padding.right}" y2="${padding.top + chartH}" stroke="#E9FF97" stroke-width="2" />

                <!-- Legend -->
                <text x="${width - padding.right}" y="18" fill="#B95E82" font-size="11" font-weight="800" text-anchor="end">--- Target (TDEE)</text>

                ${barsSvg}
                ${targetLinesSvg}
                ${labelsSvg}
            </svg>
        `;
    },

    renderMacroAreaChart() {
        const container = document.getElementById('weekly-macro-chart');
        if (!container || this.weeklyData.length === 0) return;

        const data = this.weeklyData;
        const width = 520;
        const height = 260;
        const padding = { top: 30, right: 20, bottom: 40, left: 40 };

        const chartW = width - padding.left - padding.right;
        const chartH = height - padding.top - padding.bottom;

        const maxMacro = Math.max(150, ...data.map(d => Math.max(d.protein, d.carbs, d.fat))) * 1.2;
        const stepX = chartW / Math.max(1, data.length - 1);

        const getPoints = (key) => data.map((d, i) => `${padding.left + i * stepX},${padding.top + chartH - ((d[key] / maxMacro) * chartH)}`).join(' ');

        const proteinPts = getPoints('protein');
        const carbsPts = getPoints('carbs');
        const fatPts = getPoints('fat');

        let labelsSvg = '';
        data.forEach((d, i) => {
            const x = padding.left + i * stepX;
            labelsSvg += `
                <text x="${x}" y="${height - 14}" fill="#94A3B8" font-size="12" font-weight="600" text-anchor="middle" font-family="'Outfit', sans-serif">
                    ${d.dayName}
                </text>
            `;
        });

        container.innerHTML = `
            <svg class="chart-svg" viewBox="0 0 ${width} ${height}">
                <!-- Grid Lines -->
                <line x1="${padding.left}" y1="${padding.top}" x2="${width - padding.right}" y2="${padding.top}" stroke="rgba(255,255,255,0.06)" />
                <line x1="${padding.left}" y1="${padding.top + chartH/2}" x2="${width - padding.right}" y2="${padding.top + chartH/2}" stroke="rgba(255,255,255,0.06)" />
                <line x1="${padding.left}" y1="${padding.top + chartH}" x2="${width - padding.right}" y2="${padding.top + chartH}" stroke="rgba(255,255,255,0.12)" />

                <!-- Legends -->
                <g transform="translate(${padding.left}, 16)">
                    <circle cx="0" cy="0" r="5" fill="#4FB7B3" stroke="#E9FF97" stroke-width="1.5" />
                    <text x="10" y="4" fill="#154740" font-size="11" font-weight="800">Protein (g)</text>

                    <circle cx="100" cy="0" r="5" fill="#EA580C" stroke="#E9FF97" stroke-width="1.5" />
                    <text x="110" y="4" fill="#EA580C" font-size="11" font-weight="800">Carbs (g)</text>

                    <circle cx="190" cy="0" r="5" fill="#D97706" stroke="#E9FF97" stroke-width="1.5" />
                    <text x="200" y="4" fill="#D97706" font-size="11" font-weight="800">Fat (g)</text>
                </g>

                <!-- Polyline Series -->
                <polyline fill="none" stroke="#4FB7B3" stroke-width="3.5" points="${proteinPts}" stroke-linecap="round" stroke-linejoin="round" />
                <polyline fill="none" stroke="#EA580C" stroke-width="3.5" points="${carbsPts}" stroke-linecap="round" stroke-linejoin="round" />
                <polyline fill="none" stroke="#D97706" stroke-width="3.5" points="${fatPts}" stroke-linecap="round" stroke-linejoin="round" />

                <!-- Dots -->
                ${data.map((d, i) => {
                    const x = padding.left + i * stepX;
                    const pY = padding.top + chartH - ((d.protein / maxMacro) * chartH);
                    const cY = padding.top + chartH - ((d.carbs / maxMacro) * chartH);
                    const fY = padding.top + chartH - ((d.fat / maxMacro) * chartH);
                    return `
                        <circle cx="${x}" cy="${pY}" r="5" fill="#4FB7B3" stroke="#E9FF97" stroke-width="1.5"><title>${d.dayName} Protein: ${d.protein}g</title></circle>
                        <circle cx="${x}" cy="${cY}" r="5" fill="#EA580C" stroke="#E9FF97" stroke-width="1.5"><title>${d.dayName} Carbs: ${d.carbs}g</title></circle>
                        <circle cx="${x}" cy="${fY}" r="5" fill="#D97706" stroke="#E9FF97" stroke-width="1.5"><title>${d.dayName} Fat: ${d.fat}g</title></circle>
                    `;
                }).join('')}

                ${labelsSvg}
            </svg>
        `;
    }
};

window.ChartsModule = ChartsModule;
