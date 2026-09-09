/**
 * NutriTrack AI — Daily Calorie Tracker & Meal Logging Module
 */
const TrackerModule = {
    currentDate: new Date().toISOString().split('T')[0],
    calViewYear: new Date().getFullYear(),
    calViewMonth: new Date().getMonth(),
    dailyData: null,
    selectedFood: null,
    currentMealType: 'lunch',

    async init() {
        const parts = this.currentDate.split('-');
        this.calViewYear = parseInt(parts[0]);
        this.calViewMonth = parseInt(parts[1]) - 1;

        this.updateDateUI();
        await this.loadDailyData();
        this.bindEvents();
    },

    async refresh() {
        await this.loadDailyData();
    },

    updateDateUI() {
        const label = document.getElementById('formatted-date-label');
        const monthBadge = document.getElementById('mini-cal-month');
        const dayBadge = document.getElementById('mini-cal-day');

        const parts = this.currentDate.split('-');
        const dateObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));

        // Update Mini Calendar Badge
        if (monthBadge) {
            monthBadge.textContent = dateObj.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();
        }
        if (dayBadge) {
            dayBadge.textContent = String(dateObj.getDate()).padStart(2, '0');
        }

        // Update Header Date Label
        if (label) {
            const todayStr = new Date().toISOString().split('T')[0];
            if (this.currentDate === todayStr) {
                label.textContent = 'Today, ' + this.formatShortDate(this.currentDate);
            } else {
                label.textContent = this.formatFullDate(this.currentDate);
            }
        }

        this.renderCalendarPopover();
    },

    formatShortDate(dateStr) {
        const parts = dateStr.split('-');
        const date = new Date(parts[0], parts[1] - 1, parts[2]);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    },

    formatFullDate(dateStr) {
        const parts = dateStr.split('-');
        const date = new Date(parts[0], parts[1] - 1, parts[2]);
        return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    },

    renderCalendarPopover() {
        const monthLabel = document.getElementById('cal-current-month-label');
        const yearLabel = document.getElementById('cal-current-year-label');
        const monthsStrip = document.getElementById('cal-months-strip');
        const daysGrid = document.getElementById('cal-days-grid');

        if (!monthLabel || !daysGrid) return;

        const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
        const shortMonths = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

        monthLabel.textContent = months[this.calViewMonth];
        yearLabel.textContent = this.calViewYear;

        // Render Months Strip
        if (monthsStrip) {
            monthsStrip.innerHTML = '';
            shortMonths.forEach((mName, idx) => {
                const pill = document.createElement('div');
                pill.className = `cal-month-pill ${idx === this.calViewMonth ? 'active' : ''}`;
                pill.textContent = mName;
                pill.dataset.month = idx;
                pill.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.calViewMonth = idx;
                    this.renderCalendarPopover();
                });
                monthsStrip.appendChild(pill);
            });
        }

        // Render Days Grid
        daysGrid.innerHTML = '';

        const firstDayIndex = new Date(this.calViewYear, this.calViewMonth, 1).getDay();
        const daysInCurrentMonth = new Date(this.calViewYear, this.calViewMonth + 1, 0).getDate();
        const daysInPrevMonth = new Date(this.calViewYear, this.calViewMonth, 0).getDate();

        const todayObj = new Date();
        const todayStr = `${todayObj.getFullYear()}-${String(todayObj.getMonth() + 1).padStart(2, '0')}-${String(todayObj.getDate()).padStart(2, '0')}`;

        // Previous Month Trailing Days
        for (let i = firstDayIndex - 1; i >= 0; i--) {
            const dayNum = daysInPrevMonth - i;
            const prevMonth = this.calViewMonth === 0 ? 11 : this.calViewMonth - 1;
            const prevYear = this.calViewMonth === 0 ? this.calViewYear - 1 : this.calViewYear;
            const dateStr = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;

            const cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.textContent = dayNum;
            cell.dataset.date = dateStr;
            cell.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectDate(dateStr);
            });
            daysGrid.appendChild(cell);
        }

        // Current Month Days
        for (let d = 1; d <= daysInCurrentMonth; d++) {
            const dateStr = `${this.calViewYear}-${String(this.calViewMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

            const cell = document.createElement('div');
            cell.className = 'cal-day-cell';
            cell.textContent = d;
            cell.dataset.date = dateStr;

            if (dateStr === this.currentDate) {
                cell.classList.add('selected');
            }
            if (dateStr === todayStr) {
                cell.classList.add('is-today');
            }

            cell.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectDate(dateStr);
            });
            daysGrid.appendChild(cell);
        }

        // Next Month Leading Days to fill total grid
        const totalRendered = firstDayIndex + daysInCurrentMonth;
        const totalGridSlots = totalRendered <= 35 ? 35 : 42;
        const remainingSlots = totalGridSlots - totalRendered;

        for (let n = 1; n <= remainingSlots; n++) {
            const nextMonth = this.calViewMonth === 11 ? 0 : this.calViewMonth + 1;
            const nextYear = this.calViewMonth === 11 ? this.calViewYear + 1 : this.calViewYear;
            const dateStr = `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(n).padStart(2, '0')}`;

            const cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.textContent = n;
            cell.dataset.date = dateStr;
            cell.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectDate(dateStr);
            });
            daysGrid.appendChild(cell);
        }
    },

    selectDate(dateStr) {
        this.currentDate = dateStr;
        const parts = dateStr.split('-');
        this.calViewYear = parseInt(parts[0]);
        this.calViewMonth = parseInt(parts[1]) - 1;

        this.updateDateUI();
        this.closeCalendar();
        this.loadDailyData();
        if (window.ChartsModule) window.ChartsModule.refresh();
    },

    toggleCalendar() {
        const popover = document.getElementById('calendar-popover');
        const trigger = document.getElementById('date-display-trigger');
        if (!popover) return;

        const isOpen = popover.classList.contains('open');
        if (isOpen) {
            this.closeCalendar();
        } else {
            this.openCalendar();
        }
    },

    openCalendar() {
        const popover = document.getElementById('calendar-popover');
        const trigger = document.getElementById('date-display-trigger');
        if (!popover) return;

        const parts = this.currentDate.split('-');
        this.calViewYear = parseInt(parts[0]);
        this.calViewMonth = parseInt(parts[1]) - 1;

        this.renderCalendarPopover();
        popover.classList.add('open');
        trigger?.classList.add('active');
        popover.setAttribute('aria-hidden', 'false');
    },

    closeCalendar() {
        const popover = document.getElementById('calendar-popover');
        const trigger = document.getElementById('date-display-trigger');
        if (!popover) return;

        popover.classList.remove('open');
        trigger?.classList.remove('active');
        popover.setAttribute('aria-hidden', 'true');
    },

    async loadDailyData() {
        try {
            this.dailyData = await Api.getDailySummary(this.currentDate);
            this.renderDashboard();
            this.renderFullMealDiary();
        } catch (err) {
            console.error('Error loading daily summary:', err);
        }
    },

    renderDashboard() {
        if (!this.dailyData) return;
        const data = this.dailyData;
        const analysis = data.analysis;

        // 1. Calorie Radial Progress Ring
        const targetCal = analysis.targetCalories || 2000;
        const eatenCal = analysis.consumedCalories || 0;
        const remainingCal = analysis.remainingCalories || 0;
        const bmr = analysis.bmr || 1500;

        const remEl = document.getElementById('calories-remaining-val');
        if (remEl) {
            remEl.textContent = Math.abs(Math.round(remainingCal)).toLocaleString();
            const unitEl = remEl.nextElementSibling;
            if (unitEl) unitEl.textContent = remainingCal >= 0 ? 'kcal left' : 'kcal over target';
        }

        const targetSubEl = document.getElementById('calories-target-sub');
        if (targetSubEl) targetSubEl.textContent = `Target: ${Math.round(targetCal).toLocaleString()} kcal`;

        const eatenStatEl = document.getElementById('stat-eaten-cal');
        if (eatenStatEl) eatenStatEl.textContent = `${Math.round(eatenCal)} kcal`;

        const targetStatEl = document.getElementById('stat-target-cal');
        if (targetStatEl) targetStatEl.textContent = `${Math.round(targetCal)} kcal`;

        const bmrStatEl = document.getElementById('stat-bmr-cal');
        if (bmrStatEl) bmrStatEl.textContent = `${Math.round(bmr)} kcal`;

        // SVG Ring animation (Circumference = 2 * PI * 92 ≈ 578)
        const circle = document.getElementById('calorie-progress-circle');
        if (circle) {
            const circumference = 578;
            const progress = Math.min(1.0, eatenCal / Math.max(1, targetCal));
            const offset = circumference - (progress * circumference);
            circle.style.strokeDashoffset = offset;
        }

        // Score Pill
        const heroScore = document.getElementById('hero-score-val');
        if (heroScore) heroScore.textContent = `${analysis.nutritionScore}/100`;

        // 2. Macro Progress Bars
        const macros = analysis.macros;
        if (macros) {
            // Protein
            const pTarget = macros.protein.target;
            const pEaten = macros.protein.consumed;
            document.getElementById('protein-eaten-val').textContent = `${pEaten}g`;
            document.getElementById('protein-target-val').textContent = `${pTarget}g`;
            const pPct = Math.min(100, Math.round((pEaten / Math.max(1, pTarget)) * 100));
            document.getElementById('protein-progress-bar').style.width = `${pPct}%`;

            // Carbs
            const cTarget = macros.carbs.target;
            const cEaten = macros.carbs.consumed;
            document.getElementById('carbs-eaten-val').textContent = `${cEaten}g`;
            document.getElementById('carbs-target-val').textContent = `${cTarget}g`;
            const cPct = Math.min(100, Math.round((cEaten / Math.max(1, cTarget)) * 100));
            document.getElementById('carbs-progress-bar').style.width = `${cPct}%`;

            // Fats
            const fTarget = macros.fat.target;
            const fEaten = macros.fat.consumed;
            document.getElementById('fats-eaten-val').textContent = `${fEaten}g`;
            document.getElementById('fats-target-val').textContent = `${fTarget}g`;
            const fPct = Math.min(100, Math.round((fEaten / Math.max(1, fTarget)) * 100));
            document.getElementById('fats-progress-bar').style.width = `${fPct}%`;
        }

        // Mini Metrics
        const waterLogged = data.waterLoggedMl || 0;
        const waterGoal = data.waterGoalMl || 2500;
        document.getElementById('mini-water-val').textContent = `${waterLogged} / ${waterGoal} ml`;

        const fiberLogged = analysis.micros?.fiber?.consumed || 0;
        const fiberTarget = analysis.micros?.fiber?.target || 30;
        document.getElementById('mini-fiber-val').textContent = `${fiberLogged} / ${fiberTarget}g`;

        // Age Banner & Alerts
        const ageRule = analysis.ageRule;
        const ageTitleEl = document.getElementById('dash-age-bracket-title');
        const ageFocusEl = document.getElementById('dash-age-focus-text');
        if (ageRule && ageTitleEl) {
            ageTitleEl.textContent = `Age-Stratified Profile: ${ageRule.bracketName}`;
            if (ageFocusEl) ageFocusEl.textContent = ageRule.physiologicalFocus;
        }

        const alertsContainer = document.getElementById('dash-health-alerts-container');
        if (alertsContainer) {
            alertsContainer.innerHTML = '';
            const alerts = analysis.healthAlerts || [];
            const recs = analysis.recommendations || [];

            alerts.forEach(alt => {
                const tag = document.createElement('span');
                tag.className = 'alert-tag';
                tag.textContent = alt;
                alertsContainer.appendChild(tag);
            });

            if (recs.length > 0 && alerts.length < 3) {
                const tag = document.createElement('span');
                tag.className = 'alert-tag positive';
                tag.textContent = recs[0];
                alertsContainer.appendChild(tag);
            }
        }

        // 3. Render Meal Category Buckets (Breakfast, Lunch, Dinner, Snack)
        this.renderMealBuckets(data.mealBuckets, data.mealCalTotals);
    },

    renderMealBuckets(buckets, calTotals) {
        const mealTypes = ['breakfast', 'lunch', 'dinner', 'snack'];

        mealTypes.forEach(type => {
            const listEl = document.getElementById(`meal-list-${type}`);
            const calEl = document.getElementById(`cal-tag-${type}`);
            const total = calTotals && calTotals[type] ? calTotals[type] : 0;

            if (calEl) calEl.textContent = `${Math.round(total)} kcal`;

            if (listEl) {
                const items = (buckets && buckets[type]) ? buckets[type] : [];
                if (items.length === 0) {
                    listEl.innerHTML = '<div class="empty-meal-placeholder">No items logged yet.</div>';
                } else {
                    listEl.innerHTML = '';
                    items.forEach(log => {
                        const row = document.createElement('div');
                        row.className = 'meal-item-row';
                        row.innerHTML = `
                            <div>
                                <span class="meal-item-name">${log.food ? log.food.name : 'Food'}</span>
                                <span class="meal-item-sub">${log.quantity}x (${Math.round((log.food ? log.food.servingSize : 100) * log.quantity)}${log.food ? log.food.servingUnit : 'g'}) • ${log.computedProtein || 0}g P</span>
                            </div>
                            <div class="meal-item-right">
                                <span class="meal-item-cals">${Math.round(log.computedCalories || 0)} kcal</span>
                                <button class="btn-del-item" data-log-id="${log.id}" title="Remove Item">&times;</button>
                            </div>
                        `;
                        listEl.appendChild(row);
                    });
                }
            }
        });
    },

    renderFullMealDiary() {
        const container = document.getElementById('full-diary-container');
        if (!container || !this.dailyData) return;

        const data = this.dailyData;
        const buckets = data.mealBuckets || {};
        const mealTypes = [
            { key: 'breakfast', name: 'Breakfast', emoji: '🌅' },
            { key: 'lunch', name: 'Lunch', emoji: '☀️' },
            { key: 'dinner', name: 'Dinner', emoji: '🌙' },
            { key: 'snack', name: 'Snacks & Treats', emoji: '🍎' }
        ];

        container.innerHTML = '';

        mealTypes.forEach(m => {
            const items = buckets[m.key] || [];
            let totalCals = 0, totalP = 0, totalC = 0, totalF = 0;

            items.forEach(item => {
                totalCals += item.computedCalories || 0;
                totalP += item.computedProtein || 0;
                totalC += item.computedCarbs || 0;
                totalF += item.computedFat || 0;
            });

            const sec = document.createElement('div');
            sec.className = 'diary-meal-section';
            sec.innerHTML = `
                <div class="diary-meal-header">
                    <div class="meal-title-group">
                        <span class="meal-emoji">${m.emoji}</span>
                        <div>
                            <h4>${m.name}</h4>
                            <span class="section-sub">${items.length} items logged • ${Math.round(totalCals)} kcal (P: ${Math.round(totalP)}g, C: ${Math.round(totalC)}g, F: ${Math.round(totalF)}g)</span>
                        </div>
                    </div>
                    <button class="btn btn-secondary btn-sm" data-quick-add="${m.key}">+ Add ${m.name}</button>
                </div>
                ${items.length === 0 ? '<div class="empty-meal-placeholder" style="padding: 18px;">No items logged for ' + m.name.toLowerCase() + '.</div>' : `
                    <table class="diary-meal-items-table">
                        <thead>
                            <tr>
                                <th>Food Item</th>
                                <th>Portion</th>
                                <th>Calories</th>
                                <th>Protein</th>
                                <th>Carbs</th>
                                <th>Fat</th>
                                <th>Fiber</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${items.map(i => `
                                <tr>
                                    <td><strong>${i.food ? i.food.name : 'Item'}</strong></td>
                                    <td>${i.quantity}x (${Math.round((i.food?.servingSize || 100) * i.quantity)} ${i.food?.servingUnit || 'g'})</td>
                                    <td><strong class="highlight-green">${Math.round(i.computedCalories || 0)} kcal</strong></td>
                                    <td>${i.computedProtein || 0}g</td>
                                    <td>${i.computedCarbs || 0}g</td>
                                    <td>${i.computedFat || 0}g</td>
                                    <td>${i.computedFiber || 0}g</td>
                                    <td><button class="btn-del-item" data-log-id="${i.id}" title="Delete">&times;</button></td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `}
            `;
            container.appendChild(sec);
        });
    },

    bindEvents() {
        // Date switchers & Interactive Viewable Calendar
        document.getElementById('btn-prev-day')?.addEventListener('click', (e) => {
            e.stopPropagation();
            this.shiftDate(-1);
        });
        document.getElementById('btn-next-day')?.addEventListener('click', (e) => {
            e.stopPropagation();
            this.shiftDate(1);
        });

        // Trigger Interactive Calendar Popover
        document.getElementById('date-display-trigger')?.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggleCalendar();
        });

        // Month Navigation Buttons inside Calendar
        document.getElementById('cal-prev-month')?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (this.calViewMonth === 0) {
                this.calViewMonth = 11;
                this.calViewYear--;
            } else {
                this.calViewMonth--;
            }
            this.renderCalendarPopover();
        });

        document.getElementById('cal-next-month')?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (this.calViewMonth === 11) {
                this.calViewMonth = 0;
                this.calViewYear++;
            } else {
                this.calViewMonth++;
            }
            this.renderCalendarPopover();
        });

        // Calendar Quick Shortcuts
        document.getElementById('cal-btn-yesterday')?.addEventListener('click', (e) => {
            e.stopPropagation();
            this.shiftDate(-1);
            this.closeCalendar();
        });

        document.getElementById('cal-btn-today')?.addEventListener('click', (e) => {
            e.stopPropagation();
            const today = new Date().toISOString().split('T')[0];
            this.selectDate(today);
        });

        document.getElementById('cal-btn-tomorrow')?.addEventListener('click', (e) => {
            e.stopPropagation();
            this.shiftDate(1);
            this.closeCalendar();
        });

        // Click outside calendar to close
        document.addEventListener('click', (e) => {
            const container = document.getElementById('date-navigator-container');
            if (container && !container.contains(e.target)) {
                this.closeCalendar();
            }
        });

        // Quick Water Buttons
        document.getElementById('btn-quick-water')?.addEventListener('click', async () => {
            await Api.logWater(250, this.currentDate);
            App.showToast('💧 Added +250ml Water!');
            this.loadDailyData();
            if (window.ChartsModule) window.ChartsModule.refresh();
        });

        // Modal Open / Close
        document.getElementById('btn-open-log-modal')?.addEventListener('click', () => this.openLogModal());
        document.getElementById('btn-open-log-modal-2')?.addEventListener('click', () => this.openLogModal());
        document.getElementById('btn-open-log-modal-3')?.addEventListener('click', () => this.openLogModal());
        document.getElementById('btn-close-log-modal')?.addEventListener('click', () => this.closeLogModal());

        // Delegate Add button from meal cards
        document.addEventListener('click', (e) => {
            const addBtn = e.target.closest('[data-add-meal]') || e.target.closest('[data-quick-add]');
            if (addBtn) {
                const meal = addBtn.dataset.addMeal || addBtn.dataset.quickAdd;
                this.openLogModal(meal);
            }

            const delBtn = e.target.closest('.btn-del-item');
            if (delBtn) {
                const logId = delBtn.dataset.logId;
                if (logId) this.deleteMeal(logId);
            }
        });

        // Modal Tabs
        document.querySelectorAll('.modal-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.modal-tab-btn').forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.modal-tab-pane').forEach(p => p.classList.remove('active'));
                btn.classList.add('active');
                const target = btn.dataset.modalTab;
                document.getElementById(`modal-tab-${target}`)?.classList.add('active');
            });
        });

        // Search Food Inputs
        const searchInput = document.getElementById('food-search-input');
        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', () => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => this.performFoodSearch(), 250);
            });
        }

        // Category filter chips
        document.querySelectorAll('.cat-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('.cat-chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                this.performFoodSearch();
            });
        });

        // Stepper Quantity Multiplier
        document.getElementById('btn-qty-minus')?.addEventListener('click', () => this.adjustQuantity(-0.25));
        document.getElementById('btn-qty-plus')?.addEventListener('click', () => this.adjustQuantity(0.25));
        document.getElementById('input-log-quantity')?.addEventListener('input', () => this.updateLivePortionPreview());

        // Confirm Log Button
        document.getElementById('btn-confirm-log-meal')?.addEventListener('click', () => this.handleConfirmLog());
        document.getElementById('btn-cancel-selection')?.addEventListener('click', () => this.cancelSelection());

        // Custom Food Form
        document.getElementById('custom-food-form')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleCreateCustomFood();
        });
    },

    shiftDate(days) {
        const parts = this.currentDate.split('-');
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        d.setDate(d.getDate() + days);
        this.currentDate = d.toISOString().split('T')[0];
        this.updateDateUI();
        this.loadDailyData();
    },

    openLogModal(mealType = 'lunch') {
        this.currentMealType = mealType;
        const selectMeal = document.getElementById('select-log-meal-type');
        if (selectMeal) selectMeal.value = mealType;

        const modal = document.getElementById('food-log-modal');
        if (modal) modal.classList.add('open');
        this.performFoodSearch();
    },

    closeLogModal() {
        const modal = document.getElementById('food-log-modal');
        if (modal) modal.classList.remove('open');
        this.cancelSelection();
    },

    async performFoodSearch() {
        const q = document.getElementById('food-search-input')?.value || '';
        const activeCatChip = document.querySelector('.cat-chip.active');
        const cat = activeCatChip ? activeCatChip.dataset.cat : 'All';

        const listEl = document.getElementById('food-results-list');
        if (!listEl) return;

        listEl.innerHTML = '<div style="padding: 12px; text-align: center; color: var(--text-muted);">Searching SQLite database...</div>';

        try {
            const data = await Api.searchFoods(q, cat);
            const foods = data.foods || [];

            if (foods.length === 0) {
                listEl.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted);">No matching food items found. Try another query or create custom food.</div>';
                return;
            }

            listEl.innerHTML = '';
            foods.forEach(f => {
                const item = document.createElement('div');
                item.className = 'food-search-item';
                item.innerHTML = `
                    <div>
                        <strong>${f.name}</strong>
                        <span class="meal-item-sub">${f.servingSize} ${f.servingUnit} • ${f.category}</span>
                    </div>
                    <div>
                        <span class="meal-item-cals">${f.calories} kcal</span>
                        <span class="meal-item-sub"> (P: ${f.proteinG}g, C: ${f.carbsG}g, F: ${f.fatG}g)</span>
                    </div>
                `;
                item.addEventListener('click', () => this.selectFoodForLogging(f, item));
                listEl.appendChild(item);
            });
        } catch (err) {
            console.error('Error searching foods:', err);
        }
    },

    selectFoodForLogging(food, itemEl) {
        this.selectedFood = food;
        document.querySelectorAll('.food-search-item').forEach(i => i.classList.remove('selected'));
        if (itemEl) itemEl.classList.add('selected');

        const box = document.getElementById('selected-portion-box');
        if (box) box.classList.remove('hidden');

        document.getElementById('selected-food-name').textContent = food.name;
        document.getElementById('selected-food-unit-info').textContent = 
            `Base: ${food.servingSize}${food.servingUnit} = ${food.calories} kcal | ${food.proteinG}g P | ${food.carbsG}g C | ${food.fatG}g F`;

        document.getElementById('input-log-quantity').value = '1.0';
        this.updateLivePortionPreview();
    },

    cancelSelection() {
        this.selectedFood = null;
        document.getElementById('selected-portion-box')?.classList.add('hidden');
        document.querySelectorAll('.food-search-item').forEach(i => i.classList.remove('selected'));
    },

    adjustQuantity(delta) {
        const input = document.getElementById('input-log-quantity');
        if (!input) return;
        let val = parseFloat(input.value) || 1.0;
        val = Math.max(0.25, Math.min(10.0, val + delta));
        input.value = val.toFixed(2);
        this.updateLivePortionPreview();
    },

    updateLivePortionPreview() {
        if (!this.selectedFood) return;
        const q = parseFloat(document.getElementById('input-log-quantity')?.value) || 1.0;

        document.getElementById('prev-cal').textContent = Math.round(this.selectedFood.calories * q);
        document.getElementById('prev-prot').textContent = (Math.round(this.selectedFood.proteinG * q * 10) / 10).toFixed(1);
        document.getElementById('prev-carbs').textContent = (Math.round(this.selectedFood.carbsG * q * 10) / 10).toFixed(1);
        document.getElementById('prev-fat').textContent = (Math.round(this.selectedFood.fatG * q * 10) / 10).toFixed(1);
    },

    async handleConfirmLog() {
        if (!this.selectedFood) return;
        const mealType = document.getElementById('select-log-meal-type').value;
        const quantity = parseFloat(document.getElementById('input-log-quantity').value) || 1.0;

        try {
            await Api.logMeal(this.selectedFood.id, mealType, quantity, this.currentDate);
            App.showToast(`Logged ${this.selectedFood.name} to ${mealType.toUpperCase()}! 🥗`);
            this.closeLogModal();
            await this.loadDailyData();
            if (window.ChartsModule) window.ChartsModule.refresh();
        } catch (err) {
            console.error('Error logging food:', err);
        }
    },

    async handleCreateCustomFood() {
        try {
            const custom = {
                name: document.getElementById('cust-name').value.trim(),
                category: document.getElementById('cust-category').value,
                servingSize: parseFloat(document.getElementById('cust-serving-size').value) || 100,
                servingUnit: document.getElementById('cust-serving-unit').value.trim() || 'g',
                calories: parseFloat(document.getElementById('cust-calories').value) || 0,
                proteinG: parseFloat(document.getElementById('cust-protein').value) || 0,
                carbsG: parseFloat(document.getElementById('cust-carbs').value) || 0,
                fatG: parseFloat(document.getElementById('cust-fat').value) || 0,
                fiberG: parseFloat(document.getElementById('cust-fiber').value) || 0,
                sodiumMg: parseFloat(document.getElementById('cust-sodium').value) || 0,
                calciumMg: parseFloat(document.getElementById('cust-calcium').value) || 0,
                isCustom: true
            };

            const created = await Api.createCustomFood(custom);
            App.showToast(`Created custom food: "${created.name}" ✨`);

            // Switch to search tab and auto-select
            document.querySelector('.modal-tab-btn[data-modal-tab="search-foods"]').click();
            document.getElementById('food-search-input').value = created.name;
            this.performFoodSearch();
        } catch (err) {
            console.error('Error creating custom food:', err);
        }
    },

    async deleteMeal(logId) {
        try {
            await Api.deleteMealLog(logId);
            App.showToast('Meal log deleted.');
            await this.loadDailyData();
            if (window.ChartsModule) window.ChartsModule.refresh();
        } catch (err) {
            console.error('Error deleting meal log:', err);
        }
    }
};

window.TrackerModule = TrackerModule;
