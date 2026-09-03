(function(window) {
  const selectors = {
    self: '[data-cmp-is="ticketcreate"]',
    form: '[data-cmp-hook-ticketcreate="form"]',
    titleInput: '[data-cmp-hook-ticketcreate="title"]',
    descriptionInput: '[data-cmp-hook-ticketcreate="description"]',
    prioritySelect: '[data-cmp-hook-ticketcreate="priority"]',
    assignedToSelect: '[data-cmp-hook-ticketcreate="assignedto"]',
    submitButton: '[data-cmp-hook-ticketcreate="submit"]'
  };

  function TicketCreate(config) {
    this.element = config.element;
    this.form = this.element.querySelector(selectors.form);
    this.titleInput = this.element.querySelector(selectors.titleInput);
    this.descriptionInput = this.element.querySelector(selectors.descriptionInput);
    this.prioritySelect = this.element.querySelector(selectors.prioritySelect);
    this.assignedToSelect = this.element.querySelector(selectors.assignedToSelect);
    this.submitButton = this.element.querySelector(selectors.submitButton);

    if (this.form) {
      this.form.addEventListener('submit', (e) => {
        this.onFormSubmit(e);
      });
    }

    // Mark as initialized
    this.element.removeAttribute('data-cmp-is');
  }

  TicketCreate.prototype.onFormSubmit = function(e) {
    e.preventDefault();

    FormValidator.clearErrors();

    const formData = {
      title: this.titleInput.value || '',
      description: this.descriptionInput.value || '',
      priority: this.prioritySelect.value || '',
      assignedto: this.assignedToSelect.value || ''
    };

    const errors = FormValidator.validateTicketForm(formData);

    if (errors) {
      FormValidator.displayErrors(errors);
      return;
    }

    this.submitButton.disabled = true;
    this.submitButton.textContent = 'Creating...';

    const payload = {
      title: formData.title,
      description: formData.description,
      priority: formData.priority,
      assignedto: formData.assignedto
    };

    fetch('/bin/api/tickets', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    .then(function(response) {
      if (!response.ok) {
        return response.json().then(function(data) {
          throw new Error(data.error || 'Failed to create ticket');
        });
      }
      return response.json();
    })
    .then(function(data) {
      if (data.success && data.ticket) {
        // Show success message
        const successMsg = document.createElement('div');
        successMsg.className = 'cmp-ticketcreate__success-message';
        successMsg.setAttribute('role', 'status');
        successMsg.textContent = 'Ticket created successfully! Redirecting...';
        this.form.parentNode.insertBefore(successMsg, this.form);

        // Redirect after a short delay
        setTimeout(function() {
          window.location.href = '/content/ai-practical-assessment/tickets.html';
        }, 1500);
      } else {
        throw new Error('Unexpected response from server');
      }
    }.bind(this))
    .catch(function(error) {
      console.error('Error creating ticket:', error);
      this.submitButton.disabled = false;
      this.submitButton.textContent = 'Create Ticket';

      const errorMsg = document.createElement('div');
      errorMsg.className = 'cmp-ticketcreate__error-message';
      errorMsg.setAttribute('role', 'alert');
      errorMsg.textContent = error.message || 'Failed to create ticket. Please try again.';
      this.form.parentNode.insertBefore(errorMsg, this.form);
    }.bind(this));
  };

  // Auto-initialization
  function initTicketCreate() {
    const elements = document.querySelectorAll(selectors.self);
    elements.forEach(function(el) {
      new TicketCreate({ element: el });
    });
  }

  // Initialize on document ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initTicketCreate);
  } else {
    initTicketCreate();
  }

  // Handle dynamic component insertion (AEM Page Editor)
  const observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
      if (mutation.addedNodes.length) {
        mutation.addedNodes.forEach(function(node) {
          if (node.nodeType === 1 && node.matches && node.matches(selectors.self)) {
            new TicketCreate({ element: node });
          }
        });
      }
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true
  });

})(window);
