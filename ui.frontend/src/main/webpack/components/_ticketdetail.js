(function(window) {
  'use strict';

  const selectors = {
    self: '[data-cmp-is="ticketdetail"]',
    container: '[data-cmp-hook-ticketdetail="container"]',
    loading: '[data-cmp-hook-ticketdetail="loading"]',
    error: '[data-cmp-hook-ticketdetail="error"]',
    title: '[data-cmp-hook-ticketdetail="title"]',
    status: '[data-cmp-hook-ticketdetail="status"]',
    priority: '[data-cmp-hook-ticketdetail="priority"]',
    assignedto: '[data-cmp-hook-ticketdetail="assignedto"]',
    createdby: '[data-cmp-hook-ticketdetail="createdby"]',
    created: '[data-cmp-hook-ticketdetail="created"]',
    updated: '[data-cmp-hook-ticketdetail="updated"]',
    description: '[data-cmp-hook-ticketdetail="description"]',
    viewMode: '[data-cmp-hook-ticketdetail="view-mode"]',
    editMode: '[data-cmp-hook-ticketdetail="edit-mode"]',
    editBtn: '[data-cmp-hook-ticketdetail="edit-btn"]',
    form: '[data-cmp-hook-ticketdetail="form"]',
    formTitle: '[data-cmp-hook-ticketdetail="form-title"]',
    formDescription: '[data-cmp-hook-ticketdetail="form-description"]',
    formPriority: '[data-cmp-hook-ticketdetail="form-priority"]',
    formAssignedto: '[data-cmp-hook-ticketdetail="form-assignedto"]',
    saveBtn: '[data-cmp-hook-ticketdetail="save-btn"]',
    cancelBtn: '[data-cmp-hook-ticketdetail="cancel-btn"]',
    statusSelect: '[data-cmp-hook-ticketdetail="status-select"]',
    statusChangeBtn: '[data-cmp-hook-ticketdetail="status-change-btn"]',
    errorStatus: '[data-cmp-hook-ticketdetail="error-status"]',
    commentsList: '[data-cmp-hook-ticketdetail="comments-list"]',
    commentForm: '[data-cmp-hook-ticketdetail="comment-form"]',
    commentText: '[data-cmp-hook-ticketdetail="comment-text"]',
    addCommentBtn: '[data-cmp-hook-ticketdetail="add-comment-btn"]'
  };

  function TicketDetail(config) {
    this.element = config.element;
    this.ticket = null;
    this.ticketId = this.extractTicketId();

    if (!this.ticketId) {
      this.showError('Could not determine ticket ID');
      return;
    }

    this.cacheElements();
    this.bindEvents();
    this.loadTicket();
  }

  TicketDetail.prototype.extractTicketId = function() {
    // Try to get from page URL
    const pathParts = window.location.pathname.split('/');
    for (let i = pathParts.length - 1; i >= 0; i--) {
      if (pathParts[i].startsWith('ticket-')) {
        return pathParts[i].replace('.html', '');
      }
    }
    return null;
  };

  TicketDetail.prototype.cacheElements = function() {
    this.container = this.element.querySelector(selectors.container);
    this.loadingEl = this.element.querySelector(selectors.loading);
    this.errorEl = this.element.querySelector(selectors.error);
    this.titleEl = this.element.querySelector(selectors.title);
    this.statusEl = this.element.querySelector(selectors.status);
    this.priorityEl = this.element.querySelector(selectors.priority);
    this.assignedtoEl = this.element.querySelector(selectors.assignedto);
    this.createdbyEl = this.element.querySelector(selectors.createdby);
    this.createdEl = this.element.querySelector(selectors.created);
    this.updatedEl = this.element.querySelector(selectors.updated);
    this.descriptionEl = this.element.querySelector(selectors.description);
    this.viewModeEl = this.element.querySelector(selectors.viewMode);
    this.editModeEl = this.element.querySelector(selectors.editMode);
    this.editBtn = this.element.querySelector(selectors.editBtn);
    this.form = this.element.querySelector(selectors.form);
    this.formTitle = this.element.querySelector(selectors.formTitle);
    this.formDescription = this.element.querySelector(selectors.formDescription);
    this.formPriority = this.element.querySelector(selectors.formPriority);
    this.formAssignedto = this.element.querySelector(selectors.formAssignedto);
    this.saveBtn = this.element.querySelector(selectors.saveBtn);
    this.cancelBtn = this.element.querySelector(selectors.cancelBtn);
    this.statusSelect = this.element.querySelector(selectors.statusSelect);
    this.statusChangeBtn = this.element.querySelector(selectors.statusChangeBtn);
    this.errorStatusEl = this.element.querySelector(selectors.errorStatus);
    this.commentsList = this.element.querySelector(selectors.commentsList);
    this.commentForm = this.element.querySelector(selectors.commentForm);
    this.commentText = this.element.querySelector(selectors.commentText);
    this.addCommentBtn = this.element.querySelector(selectors.addCommentBtn);
  };

  TicketDetail.prototype.bindEvents = function() {
    this.editBtn.addEventListener('click', () => this.toggleEditMode());
    this.form.addEventListener('submit', (e) => this.onFormSubmit(e));
    this.cancelBtn.addEventListener('click', () => this.toggleEditMode());
    this.statusChangeBtn.addEventListener('click', () => this.onStatusChange());
    this.commentForm.addEventListener('submit', (e) => this.onCommentSubmit(e));
  };

  TicketDetail.prototype.loadTicket = function() {
    this.showLoading();

    fetch(`/bin/api/tickets?id=${this.ticketId}`)
      .then(response => {
        if (!response.ok) {
          return response.json().then(data => {
            throw new Error(data.error || 'Failed to load ticket');
          });
        }
        return response.json();
      })
      .then(data => {
        if (data.success && data.ticket) {
          this.ticket = data.ticket;
          this.renderTicket();
          this.populateStatusSelect();
          this.renderComments();
          this.hideLoading();
        } else {
          throw new Error('Unexpected response format');
        }
      })
      .catch(error => {
        console.error('Error loading ticket:', error);
        this.showError(error.message || 'Failed to load ticket');
      });
  };

  TicketDetail.prototype.renderTicket = function() {
    if (!this.ticket) return;

    this.titleEl.textContent = this.escapeHtml(this.ticket.title);
    this.statusEl.textContent = this.ticket.status;
    this.statusEl.className = `cmp-ticketdetail__status cmp-ticketdetail__status--${this.ticket.status.toLowerCase().replace(/\s+/g, '-')}`;
    this.priorityEl.textContent = this.ticket.priority;
    this.priorityEl.className = `cmp-ticketdetail__priority cmp-ticketdetail__priority--${this.ticket.priority.toLowerCase()}`;
    this.assignedtoEl.textContent = this.escapeHtml(this.ticket.assignedTo || 'Unassigned');
    this.createdbyEl.textContent = this.escapeHtml(this.ticket.createdBy || 'Unknown');
    this.createdEl.textContent = this.formatDate(this.ticket.createdAt);
    this.updatedEl.textContent = this.formatDate(this.ticket.updatedAt);
    this.descriptionEl.textContent = this.escapeHtml(this.ticket.description);

    // Populate form fields
    this.formTitle.value = this.ticket.title;
    this.formDescription.value = this.ticket.description;
    this.formPriority.value = this.ticket.priority;
    this.formAssignedto.value = this.ticket.assignedTo;

    // Show container
    this.container.style.display = 'block';
  };

  TicketDetail.prototype.populateStatusSelect = function() {
    if (!this.ticket) return;

    const currentStatus = this.ticket.status;
    const validStatuses = this.getValidNextStates(currentStatus);

    // Clear existing options
    this.statusSelect.innerHTML = '<option value="">-- Select New Status --</option>';

    validStatuses.forEach(status => {
      const option = document.createElement('option');
      option.value = status;
      option.textContent = status;
      this.statusSelect.appendChild(option);
    });

    if (validStatuses.length === 0) {
      this.statusChangeBtn.disabled = true;
      this.statusChangeBtn.textContent = 'Terminal State - No Status Change Available';
    }
  };

  TicketDetail.prototype.getValidNextStates = function(currentStatus) {
    // State machine logic (matches backend StateTransitionValidator)
    const transitions = {
      'Open': ['In Progress', 'Cancelled'],
      'In Progress': ['Resolved', 'Cancelled'],
      'Resolved': ['Closed', 'In Progress'],
      'Closed': [],
      'Cancelled': []
    };
    return transitions[currentStatus] || [];
  };

  TicketDetail.prototype.renderComments = function() {
    if (!this.ticket || !this.ticket.comments) {
      this.commentsList.innerHTML = '<p class="cmp-ticketdetail__no-comments">No comments yet.</p>';
      return;
    }

    if (this.ticket.comments.length === 0) {
      this.commentsList.innerHTML = '<p class="cmp-ticketdetail__no-comments">No comments yet.</p>';
      return;
    }

    this.commentsList.innerHTML = '';
    this.ticket.comments.forEach(comment => {
      const commentEl = this.createCommentElement(comment);
      this.commentsList.appendChild(commentEl);
    });
  };

  TicketDetail.prototype.createCommentElement = function(comment) {
    const div = document.createElement('div');
    div.className = 'cmp-ticketdetail__comment';
    div.innerHTML = `
      <div class="cmp-ticketdetail__comment-header">
        <strong class="cmp-ticketdetail__comment-author">${this.escapeHtml(comment.createdBy)}</strong>
        <span class="cmp-ticketdetail__comment-date">${this.formatDate(comment.createdAt)}</span>
      </div>
      <div class="cmp-ticketdetail__comment-body">${this.escapeHtml(comment.message)}</div>
    `;
    return div;
  };

  TicketDetail.prototype.toggleEditMode = function() {
    const isEditMode = this.editModeEl.style.display !== 'none';
    if (isEditMode) {
      this.editModeEl.style.display = 'none';
      this.viewModeEl.style.display = 'block';
      this.editBtn.textContent = 'Edit';
    } else {
      this.editModeEl.style.display = 'block';
      this.viewModeEl.style.display = 'none';
      this.editBtn.textContent = 'Cancel';
    }
    this.clearFormErrors();
  };

  TicketDetail.prototype.onFormSubmit = function(e) {
    e.preventDefault();
    this.clearFormErrors();

    const updates = {
      title: this.formTitle.value || '',
      description: this.formDescription.value || '',
      priority: this.formPriority.value || '',
      assignedTo: this.formAssignedto.value || ''
    };

    // Simple client-side validation
    const errors = {};
    if (!updates.title.trim()) {
      errors.title = 'Title is required';
    }
    if (!updates.description.trim()) {
      errors.description = 'Description is required';
    }
    if (!updates.priority) {
      errors.priority = 'Priority is required';
    }
    if (!updates.assignedTo) {
      errors.assignedTo = 'Assignee is required';
    }

    if (Object.keys(errors).length > 0) {
      this.displayFormErrors(errors);
      return;
    }

    this.saveBtn.disabled = true;
    this.saveBtn.textContent = 'Saving...';

    fetch(`/bin/api/tickets?id=${this.ticketId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updates)
    })
      .then(response => {
        if (!response.ok) {
          return response.json().then(data => {
            throw new Error(data.error || 'Failed to update ticket');
          });
        }
        return response.json();
      })
      .then(data => {
        if (data.success && data.ticket) {
          this.ticket = data.ticket;
          this.renderTicket();
          this.toggleEditMode();
          this.showSuccessMessage('Ticket updated successfully');
        } else {
          throw new Error('Unexpected response format');
        }
      })
      .catch(error => {
        console.error('Error updating ticket:', error);
        this.displayFormErrors({ general: error.message || 'Failed to update ticket' });
      })
      .finally(() => {
        this.saveBtn.disabled = false;
        this.saveBtn.textContent = 'Save Changes';
      });
  };

  TicketDetail.prototype.onStatusChange = function() {
    const newStatus = this.statusSelect.value;

    if (!newStatus) {
      this.showError('Please select a new status');
      return;
    }

    this.statusChangeBtn.disabled = true;
    this.statusChangeBtn.textContent = 'Changing...';

    fetch('/bin/api/tickets?id=' + this.ticketId, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        newStatus: newStatus
      })
    })
      .then(response => {
        if (!response.ok) {
          return response.json().then(data => {
            throw new Error(data.error || 'Failed to change status');
          });
        }
        return response.json();
      })
      .then(data => {
        if (data.success && data.ticket) {
          this.ticket = data.ticket;
          this.renderTicket();
          this.populateStatusSelect();
          this.statusSelect.value = '';
          this.showSuccessMessage('Status changed successfully');
        } else {
          throw new Error('Unexpected response format');
        }
      })
      .catch(error => {
        console.error('Error changing status:', error);
        this.errorStatusEl.textContent = error.message || 'Failed to change status';
        this.errorStatusEl.style.display = 'block';
      })
      .finally(() => {
        this.statusChangeBtn.disabled = false;
        this.statusChangeBtn.textContent = 'Change Status';
      });
  };

  TicketDetail.prototype.onCommentSubmit = function(e) {
    e.preventDefault();

    const message = this.commentText.value || '';

    if (!message.trim()) {
      this.showError('Comment cannot be empty');
      return;
    }

    this.addCommentBtn.disabled = true;
    this.addCommentBtn.textContent = 'Adding...';

    fetch('/bin/api/tickets/comments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        id: this.ticketId,
        message: message.trim()
      })
    })
      .then(response => {
        if (!response.ok) {
          return response.json().then(data => {
            throw new Error(data.error || 'Failed to add comment');
          });
        }
        return response.json();
      })
      .then(data => {
        if (data.success) {
          this.commentText.value = '';
          // Reload comments
          this.loadComments();
          this.showSuccessMessage('Comment added successfully');
        } else {
          throw new Error('Unexpected response format');
        }
      })
      .catch(error => {
        console.error('Error adding comment:', error);
        this.showError(error.message || 'Failed to add comment');
      })
      .finally(() => {
        this.addCommentBtn.disabled = false;
        this.addCommentBtn.textContent = 'Add Comment';
      });
  };

  TicketDetail.prototype.loadComments = function() {
    fetch(`/bin/api/tickets/comments?id=${this.ticketId}`)
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to load comments');
        }
        return response.json();
      })
      .then(data => {
        if (data.success) {
          this.ticket.comments = data.comments;
          this.renderComments();
        }
      })
      .catch(error => {
        console.error('Error loading comments:', error);
      });
  };

  TicketDetail.prototype.displayFormErrors = function(errors) {
    Object.keys(errors).forEach(field => {
      const errorEl = this.element.querySelector(`[data-cmp-hook-ticketdetail="error-${field}"]`);
      if (errorEl) {
        errorEl.textContent = errors[field];
        errorEl.style.display = 'block';
      }
    });
  };

  TicketDetail.prototype.clearFormErrors = function() {
    this.element.querySelectorAll('[data-cmp-hook-ticketdetail^="error-"]').forEach(el => {
      el.textContent = '';
      el.style.display = 'none';
    });
  };

  TicketDetail.prototype.showLoading = function() {
    if (this.loadingEl) this.loadingEl.style.display = 'block';
    if (this.container) this.container.style.display = 'none';
    if (this.errorEl) this.errorEl.style.display = 'none';
  };

  TicketDetail.prototype.hideLoading = function() {
    if (this.loadingEl) this.loadingEl.style.display = 'none';
  };

  TicketDetail.prototype.showError = function(message) {
    if (this.loadingEl) this.loadingEl.style.display = 'none';
    if (this.container) this.container.style.display = 'none';
    if (this.errorEl) {
      this.errorEl.textContent = message || 'An error occurred';
      this.errorEl.style.display = 'block';
    }
  };

  TicketDetail.prototype.showSuccessMessage = function(message) {
    const msgEl = document.createElement('div');
    msgEl.className = 'cmp-ticketdetail__success-message';
    msgEl.setAttribute('role', 'status');
    msgEl.textContent = message;
    this.element.insertBefore(msgEl, this.container);
    setTimeout(() => msgEl.remove(), 3000);
  };

  TicketDetail.prototype.escapeHtml = function(text) {
    if (!text) return '';
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
  };

  TicketDetail.prototype.formatDate = function(dateString) {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);
      return date.toLocaleString();
    } catch (e) {
      return dateString;
    }
  };

  // Auto-initialization
  function initTicketDetail() {
    const elements = document.querySelectorAll(selectors.self);
    elements.forEach(el => {
      if (!el.dataset.initialized) {
        new TicketDetail({ element: el });
        el.dataset.initialized = 'true';
      }
    });
  }

  // Initialize on document ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initTicketDetail);
  } else {
    initTicketDetail();
  }

  // Handle dynamic component insertion
  const observer = new MutationObserver(mutations => {
    mutations.forEach(mutation => {
      mutation.addedNodes.forEach(node => {
        if (node.nodeType === 1 && node.matches && node.matches(selectors.self)) {
          new TicketDetail({ element: node });
        }
        if (node.nodeType === 1 && node.querySelector && node.querySelector(selectors.self)) {
          initTicketDetail();
        }
      });
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true
  });

})(window);
