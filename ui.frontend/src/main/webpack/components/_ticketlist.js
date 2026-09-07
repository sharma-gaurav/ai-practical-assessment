window.TicketList = (function() {
    'use strict';

    const COMPONENT_SELECTOR = '[data-cmp-is="ticketlist"]';
    const API_ENDPOINT = '/bin/api/tickets';
    const DEFAULT_LIMIT = 20;

    function init() {
        const components = document.querySelectorAll(COMPONENT_SELECTOR);
        components.forEach(component => {
            if (!component.dataset.initialized) {
                initializeComponent(component);
                component.dataset.initialized = 'true';
            }
        });
    }

    function initializeComponent(component) {
        const searchInput = component.querySelector('[data-cmp-hook-ticketlist="search"]');
        const filterSelect = component.querySelector('[data-cmp-hook-ticketlist="filter"]');
        const clearBtn = component.querySelector('[data-cmp-hook-ticketlist="clear"]');

        // Store state
        const state = {
            search: '',
            status: '',
            page: 0,
            tickets: []
        };

        // Load initial tickets
        loadTickets(component, state);

        // Event listeners
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                state.search = e.target.value.trim();
                state.page = 0;
                searchTimeout = setTimeout(() => {
                    loadTickets(component, state);
                }, 300);
            });
        }

        if (filterSelect) {
            filterSelect.addEventListener('change', (e) => {
                state.status = e.target.value;
                state.page = 0;
                loadTickets(component, state);
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                state.search = '';
                state.status = '';
                state.page = 0;
                if (searchInput) searchInput.value = '';
                if (filterSelect) filterSelect.value = '';
                loadTickets(component, state);
            });
        }
    }

    function loadTickets(component, state) {
        const loadingEl = component.querySelector('[data-cmp-hook-ticketlist="loading"]');
        const errorEl = component.querySelector('[data-cmp-hook-ticketlist="error"]');
        const emptyEl = component.querySelector('[data-cmp-hook-ticketlist="empty"]');
        const tableEl = component.querySelector('[data-cmp-hook-ticketlist="table"]');
        const tbodyEl = component.querySelector('[data-cmp-hook-ticketlist="tbody"]');

        // Show loading
        if (loadingEl) loadingEl.style.display = 'block';
        if (errorEl) errorEl.style.display = 'none';
        if (emptyEl) emptyEl.style.display = 'none';
        if (tableEl) tableEl.style.display = 'none';

        const params = new URLSearchParams();
        if (state.search) params.append('search', state.search);
        if (state.status) params.append('status', state.status);
        params.append('page', state.page);
        params.append('limit', DEFAULT_LIMIT);

        fetch(`${API_ENDPOINT}?${params.toString()}`)
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.error || `HTTP error! status: ${response.status}`);
                    }).catch(err => {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                if (loadingEl) loadingEl.style.display = 'none';

                if (!data.success) {
                    throw new Error(data.error || 'Failed to fetch tickets');
                }

                state.tickets = data.tickets || [];

                if (state.tickets.length === 0) {
                    if (emptyEl) emptyEl.style.display = 'block';
                    if (tableEl) tableEl.style.display = 'none';
                } else {
                    if (emptyEl) emptyEl.style.display = 'none';
                    if (tableEl) tableEl.style.display = 'table';
                    renderTickets(tbodyEl, state.tickets);
                }
            })
            .catch(error => {
                console.error('Error loading tickets:', error);
                if (loadingEl) loadingEl.style.display = 'none';
                if (tableEl) tableEl.style.display = 'none';
                if (emptyEl) emptyEl.style.display = 'none';
                if (errorEl) {
                    errorEl.textContent = error.message || 'Failed to load tickets. Please try again.';
                    errorEl.style.display = 'block';
                }
            });
    }

    function renderTickets(tbodyEl, tickets) {
        if (!tbodyEl) return;

        tbodyEl.innerHTML = '';

        tickets.forEach(ticket => {
            const row = createTicketRow(ticket);
            tbodyEl.appendChild(row);
        });
    }

    function createTicketRow(ticket) {
        const row = document.createElement('tr');
        row.className = 'cmp-ticketlist__row';
        row.setAttribute('data-ticket-id', ticket.id);

        const statusClass = `cmp-ticketlist__status--${ticket.status.toLowerCase().replace(/\s+/g, '-')}`;
        const priorityClass = `cmp-ticketlist__priority--${ticket.priority.toLowerCase()}`;

        const truncateDescription = (desc, maxLength = 100) => {
            if (!desc) return '';
            return desc.length > maxLength ? desc.substring(0, maxLength) + '...' : desc;
        };

        row.innerHTML = `
            <td class="cmp-ticketlist__cell cmp-ticketlist__cell--id">${escapeHtml(ticket.id)}</td>
            <td class="cmp-ticketlist__cell cmp-ticketlist__cell--title">${escapeHtml(ticket.title)}</td>
            <td class="cmp-ticketlist__cell cmp-ticketlist__cell--description">${escapeHtml(truncateDescription(ticket.description))}</td>
            <td class="cmp-ticketlist__cell cmp-ticketlist__cell--priority">
                <span class="cmp-ticketlist__priority ${priorityClass}">${escapeHtml(ticket.priority)}</span>
            </td>
            <td class="cmp-ticketlist__cell cmp-ticketlist__cell--status">
                <span class="cmp-ticketlist__status ${statusClass}">${escapeHtml(ticket.status)}</span>
            </td>
        `;

        // Make row clickable to open detail view
        row.style.cursor = 'pointer';
        row.addEventListener('click', () => {
            window.location.href = `/content/ai-practical-assessment/tickets/${ticket.id}.html`;
        });

        return row;
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    // Auto-initialize on DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Handle dynamic insertion (AEM Page Editor)
    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === 1 && node.matches && node.matches(COMPONENT_SELECTOR)) {
                    init();
                }
                if (node.nodeType === 1 && node.querySelector && node.querySelector(COMPONENT_SELECTOR)) {
                    init();
                }
            });
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    return {
        init: init
    };
})();
