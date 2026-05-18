(function () {
    const state = {
        subscriptions: [],
        menuItems: [],
        quantities: {}
    };

    function roleKind() {
        const title = document.title.toLowerCase();
        if (title.includes('admin')) return 'admin';
        if (title.includes('manager')) return 'manager';
        return 'employee';
    }

    function tableWrapClass() {
        return roleKind() === 'admin' ? 'table-wrap' : 'tablewrap';
    }

    function money(value) {
        return `Rs. ${Number(value || 0).toFixed(2)}`;
    }

    function safeText(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function getSubscriptionId(sub) {
        return sub.subscriptionId || sub.id;
    }

    function activeSubscriptions() {
        return state.subscriptions.filter(s => (s.status || 'ACTIVE') === 'ACTIVE');
    }

    function injectStyles() {
        if (document.getElementById('meal-order-styles')) return;
        const style = document.createElement('style');
        style.id = 'meal-order-styles';
        style.textContent = `
            .meal-order-layout{display:grid;grid-template-columns:minmax(260px,360px) minmax(0,1fr);gap:18px;align-items:start}
            .meal-order-panel{background:var(--panel);border:1px solid var(--border);border-radius:12px;overflow:hidden;margin-bottom:20px}
            .meal-order-body{padding:18px}
            .meal-order-card-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px}
            .meal-item-card{background:var(--bg3);border:1px solid var(--border);border-radius:8px;padding:14px;display:flex;flex-direction:column;gap:10px;min-height:150px}
            .meal-item-card:hover{border-color:var(--border2)}
            .meal-item-title{font-weight:700;color:var(--text);font-size:14px}
            .meal-item-desc{color:var(--text2);font-size:12px;line-height:1.45;min-height:34px}
            .meal-item-meta{display:flex;justify-content:space-between;align-items:center;color:var(--text3);font-size:12px}
            .qty-control{display:flex;align-items:center;gap:6px;margin-top:auto}
            .qty-control input{width:58px;text-align:center;padding:7px 6px}
            .meal-summary{display:flex;justify-content:space-between;gap:12px;padding-top:14px;margin-top:14px;border-top:1px solid var(--border);font-size:13px;color:var(--text2)}
            .meal-summary strong{color:var(--text)}
            .meal-muted{color:var(--text3);font-size:12px;line-height:1.5}
            .meal-inline-actions{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
            @media (max-width: 900px){.meal-order-layout{grid-template-columns:1fr}}
        `;
        document.head.appendChild(style);
    }

    function addNavItem() {
        if (document.querySelector('[data-meal-order-nav]')) return;
        const navItems = Array.from(document.querySelectorAll('.nav-item'));
        if (!navItems.length) return;
        if (navItems.some(item => item.textContent.trim().toLowerCase().includes('meal orders'))) return;

        const nav = document.createElement('div');
        nav.className = 'nav-item';
        nav.dataset.mealOrderNav = 'true';
        nav.innerHTML = roleKind() === 'employee'
            ? '<span class="icon">[]</span> Meal Orders'
            : '<span>[]</span> Meal Orders';
        nav.addEventListener('click', function () {
            if (roleKind() === 'admin') {
                window.showView('meal-order', { target: nav });
            } else {
                window.showView('meal-order', nav);
            }
            window.loadMealOrderWorkspace();
        });

        const after = navItems.filter(item => /subscription/i.test(item.textContent)).pop()
            || navItems.filter(item => /restaurant/i.test(item.textContent)).pop()
            || navItems[navItems.length - 1];
        after.insertAdjacentElement('afterend', nav);
    }

    function addView() {
        const content = document.querySelector('.content') || document.getElementById('content');
        if (!content) return;

        const wrapClass = tableWrapClass();
        let view = document.getElementById('meal-order-view');
        if (!view) {
            view = document.createElement('div');
            view.id = 'meal-order-view';
            view.className = 'view';
            content.appendChild(view);
        }
        if (view.dataset.mealOrderReady === 'true') return;
        view.dataset.mealOrderReady = 'true';
        view.innerHTML = `
            <div class="meal-order-layout">
                <div class="meal-order-panel">
                    <div class="section-head">
                        <div>
                            <div class="section-title">Meal Order</div>
                            <div class="section-sub">Choose an active subscription and order menu items.</div>
                        </div>
                    </div>
                    <div class="meal-order-body">
                        <div class="form-group full">
                            <label>Active Subscription</label>
                            <select id="meal-order-subscription" onchange="loadMealMenuForSelectedSubscription()">
                                <option value="">Loading subscriptions...</option>
                            </select>
                        </div>
                        <div id="meal-order-subscription-meta" class="meal-muted" style="margin-top:10px">Select a subscription to view menu items.</div>
                        <div class="meal-summary">
                            <span>Selected items</span>
                            <strong id="meal-order-count">0</strong>
                        </div>
                        <div class="meal-summary" style="margin-top:0;border-top:0;padding-top:8px">
                            <span>Total</span>
                            <strong id="meal-order-total">Rs. 0.00</strong>
                        </div>
                        <div class="meal-inline-actions" style="margin-top:16px">
                            <button class="btn btn-primary" onclick="placeMealOrder()">Place Order</button>
                            <button class="btn btn-secondary" onclick="clearMealQuantities()">Clear</button>
                        </div>
                    </div>
                </div>

                <div>
                    <div class="meal-order-panel">
                        <div class="section-head">
                            <div>
                                <div class="section-title">Menu Items</div>
                                <div class="section-sub">Available items for the selected restaurant and meal slot.</div>
                            </div>
                            <button class="btn btn-secondary btn-sm" onclick="loadMealMenuForSelectedSubscription()">Refresh</button>
                        </div>
                        <div class="meal-order-body">
                            <div id="meal-menu-list" class="meal-order-card-grid">
                                <div class="empty-state">Select a subscription to load menu items</div>
                            </div>
                        </div>
                    </div>

                    <div class="meal-order-panel">
                        <div class="section-head">
                            <div>
                                <div class="section-title">My Orders</div>
                                <div class="section-sub">Recent meal orders and item details.</div>
                            </div>
                            <button class="btn btn-secondary btn-sm" onclick="loadMealOrders()">Refresh</button>
                        </div>
                        <div class="${wrapClass}">
                            <table>
                                <thead>
                                    <tr><th>ID</th><th>Restaurant</th><th>Slot</th><th>Items</th><th>Total</th><th>Status</th><th>Placed</th></tr>
                                </thead>
                                <tbody id="meal-orders-list">
                                    <tr><td colspan="7" class="empty-state">Loading...</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    async function loadMealSubscriptions() {
        const select = document.getElementById('meal-order-subscription');
        select.innerHTML = '<option value="">Loading subscriptions...</option>';
        state.subscriptions = await apiFetch('/subscription/user') || [];
        const active = activeSubscriptions();
        if (!active.length) {
            select.innerHTML = '<option value="">No active subscriptions</option>';
            document.getElementById('meal-menu-list').innerHTML =
                '<div class="empty-state">Create an active meal subscription before ordering.</div>';
            updateSelectedSubscriptionMeta();
            return;
        }
        select.innerHTML = '<option value="">Select a subscription</option>' + active.map(s => {
            const id = getSubscriptionId(s);
            const label = `${s.restaurantName || 'Restaurant'} - ${s.mealSlot || 'Slot'} (${s.scheduleType || 'Schedule'})`;
            return `<option value="${id}">${safeText(label)}</option>`;
        }).join('');
        if (active.length === 1) {
            select.value = String(getSubscriptionId(active[0]));
            await loadMealMenuForSelectedSubscription();
        } else {
            updateSelectedSubscriptionMeta();
        }
    }

    function selectedSubscription() {
        const id = Number(document.getElementById('meal-order-subscription')?.value || 0);
        return state.subscriptions.find(s => Number(getSubscriptionId(s)) === id);
    }

    function updateSelectedSubscriptionMeta() {
        const meta = document.getElementById('meal-order-subscription-meta');
        const sub = selectedSubscription();
        if (!sub) {
            meta.textContent = 'Select a subscription to view menu items.';
            return;
        }
        meta.textContent = `${sub.restaurantName || 'Restaurant'} - ${sub.mealSlot || 'Meal'} - ${sub.scheduleType || 'Schedule'}${sub.nextDeliveryTime ? ' - next delivery ' + new Date(sub.nextDeliveryTime).toLocaleString() : ''}`;
    }

    async function loadMealMenuForSelectedSubscription() {
        const list = document.getElementById('meal-menu-list');
        const sub = selectedSubscription();
        state.quantities = {};
        updateMealSummary();
        updateSelectedSubscriptionMeta();
        if (!sub) {
            list.innerHTML = '<div class="empty-state">Select a subscription to load menu items</div>';
            return;
        }
        if (!sub.restaurantId || !sub.mealSlot) {
            list.innerHTML = '<div class="empty-state">This subscription is missing restaurant or meal slot data.</div>';
            return;
        }
        list.innerHTML = '<div class="empty-state">Loading menu items...</div>';
        try {
            state.menuItems = await apiFetch(`/menu-items/restaurant/${sub.restaurantId}?slot=${encodeURIComponent(sub.mealSlot)}`) || [];
            renderMealMenu();
        } catch (error) {
            list.innerHTML = '<div class="empty-state">Could not load menu items.</div>';
            showAlert(`Could not load menu: ${error.message}`, 'error');
        }
    }

    function renderMealMenu() {
        const list = document.getElementById('meal-menu-list');
        if (!state.menuItems.length) {
            list.innerHTML = '<div class="empty-state">No available menu items for this meal slot.</div>';
            return;
        }
        list.innerHTML = state.menuItems.map(item => `
            <div class="meal-item-card">
                <div class="meal-item-title">${safeText(item.name || 'Menu item')}</div>
                <div class="meal-item-desc">${safeText(item.description || 'No description added')}</div>
                <div class="meal-item-meta">
                    <span>${safeText(item.mealSlot || '')}</span>
                    <strong>${money(item.price)}</strong>
                </div>
                <div class="qty-control">
                    <button class="btn btn-secondary btn-sm" onclick="setMealQuantity(${item.id}, -1)">-</button>
                    <input id="meal-qty-${item.id}" type="number" min="0" value="0" onchange="setMealQuantity(${item.id}, this.value, true)">
                    <button class="btn btn-secondary btn-sm" onclick="setMealQuantity(${item.id}, 1)">+</button>
                </div>
            </div>
        `).join('');
    }

    function setMealQuantity(menuItemId, value, absolute) {
        const current = Number(state.quantities[menuItemId] || 0);
        const next = absolute ? Number(value || 0) : current + Number(value || 0);
        state.quantities[menuItemId] = Math.max(0, next);
        const input = document.getElementById(`meal-qty-${menuItemId}`);
        if (input) input.value = state.quantities[menuItemId];
        updateMealSummary();
    }

    function updateMealSummary() {
        const selected = Object.entries(state.quantities)
            .filter(([, quantity]) => Number(quantity) > 0);
        const count = selected.reduce((total, [, quantity]) => total + Number(quantity), 0);
        const total = selected.reduce((sum, [id, quantity]) => {
            const item = state.menuItems.find(i => Number(i.id) === Number(id));
            return sum + Number(item?.price || 0) * Number(quantity);
        }, 0);
        document.getElementById('meal-order-count').textContent = count;
        document.getElementById('meal-order-total').textContent = money(total);
    }

    function clearMealQuantities() {
        state.quantities = {};
        state.menuItems.forEach(item => {
            const input = document.getElementById(`meal-qty-${item.id}`);
            if (input) input.value = 0;
        });
        updateMealSummary();
    }

    async function placeMealOrder() {
        const sub = selectedSubscription();
        if (!sub) {
            showAlert('Please select an active subscription', 'error');
            return;
        }
        const items = Object.entries(state.quantities)
            .filter(([, quantity]) => Number(quantity) > 0)
            .map(([menuItemId, quantity]) => ({ menuItemId: Number(menuItemId), quantity: Number(quantity) }));
        if (!items.length) {
            showAlert('Please select at least one menu item', 'error');
            return;
        }
        try {
            await apiFetch('/orders', {
                method: 'POST',
                body: JSON.stringify({ subscriptionId: getSubscriptionId(sub), items })
            });
            showAlert('Meal order placed successfully', 'success');
            clearMealQuantities();
            await loadMealOrders();
        } catch (error) {
            showAlert(`Order failed: ${error.message}`, 'error');
        }
    }

    async function loadMealOrders() {
        const tbody = document.getElementById('meal-orders-list');
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Loading orders...</td></tr>';
        try {
            const orders = await apiFetch('/orders/my') || [];
            if (!orders.length) {
                tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No meal orders yet</td></tr>';
                return;
            }
            tbody.innerHTML = orders.map(order => {
                const items = order.items || [];
                const total = items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0);
                const details = items.map(item => `${safeText(item.menuItemName || 'Item')} x ${item.quantity}`).join('<br>');
                const statusClass = String(order.status || '').toLowerCase();
                return `<tr>
                    <td>#${order.id}</td>
                    <td>${safeText(order.restaurantName || '-')}</td>
                    <td>${safeText(order.mealSlot || '-')}</td>
                    <td>${details || '-'}</td>
                    <td>${money(total)}</td>
                    <td><span class="chip ${statusClass}">${safeText(order.status || '-')}</span></td>
                    <td>${order.placedAt ? new Date(order.placedAt).toLocaleString() : '-'}</td>
                </tr>`;
            }).join('');
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Could not load meal orders</td></tr>';
        }
    }

    async function loadMealOrderWorkspace() {
        await loadMealSubscriptions();
        await loadMealOrders();
    }

    function init() {
        if (!window.apiFetch || !window.showView) return;
        injectStyles();
        addView();
        addNavItem();
    }

    window.loadMealOrderWorkspace = loadMealOrderWorkspace;
    window.loadMealMenuForSelectedSubscription = loadMealMenuForSelectedSubscription;
    window.setMealQuantity = setMealQuantity;
    window.clearMealQuantities = clearMealQuantities;
    window.placeMealOrder = placeMealOrder;
    window.loadMealOrders = loadMealOrders;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
