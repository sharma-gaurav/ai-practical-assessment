(function(window) {
  const FormValidator = {
    validateTicketForm: function(formData) {
      const errors = {};

      if (!formData.title || formData.title.trim() === '') {
        errors.title = 'Title is required';
      } else if (formData.title.length > 255) {
        errors.title = 'Title must not exceed 255 characters';
      }

      if (!formData.description || formData.description.trim() === '') {
        errors.description = 'Description is required';
      } else if (formData.description.length > 5000) {
        errors.description = 'Description must not exceed 5000 characters';
      }

      if (!formData.priority) {
        errors.priority = 'Priority is required';
      } else if (!['HIGH', 'MEDIUM', 'LOW'].includes(formData.priority)) {
        errors.priority = 'Priority must be one of: HIGH, MEDIUM, LOW';
      }

      if (!formData.assignedto || formData.assignedto.trim() === '') {
        errors.assignedto = 'Assignee is required';
      }

      return Object.keys(errors).length === 0 ? null : errors;
    },

    displayErrors: function(errors) {
      if (!errors) {
        return;
      }

      FormValidator.clearErrors();

      Object.keys(errors).forEach(function(fieldName) {
        const fieldSelector = '[data-cmp-hook-ticketcreate="' + fieldName + '"]';
        const field = document.querySelector(fieldSelector);

        if (field) {
          const formGroup = field.closest('.cmp-ticketcreate__form-group');
          if (formGroup) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'cmp-ticketcreate__error-message';
            errorDiv.setAttribute('role', 'alert');
            errorDiv.textContent = errors[fieldName];
            formGroup.appendChild(errorDiv);

            // Add error class to form group
            formGroup.classList.add('cmp-ticketcreate__form-group--error');
          }
        }
      });
    },

    clearErrors: function() {
      const errorMessages = document.querySelectorAll('.cmp-ticketcreate__error-message');
      errorMessages.forEach(function(el) {
        el.remove();
      });

      const formGroups = document.querySelectorAll('.cmp-ticketcreate__form-group--error');
      formGroups.forEach(function(el) {
        el.classList.remove('cmp-ticketcreate__form-group--error');
      });
    }
  };

  window.FormValidator = FormValidator;
})(window);
