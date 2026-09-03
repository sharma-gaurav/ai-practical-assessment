# Design Notes

## Architecture Overview

**Three-Tier Architecture:**

```
Frontend (Vanilla JavaScript in AEM)
    ↓ HTTP/JSON
Backend API (Sling Servlets)
    ↓ OSGi Dependency Injection
Business Logic (OSGi Services)
    ↓ JCR API
Persistence (AEM JCR Pages/Content)
```

- **Frontend:** Vanilla JavaScript with HTML/CSS deployed as AEM client libraries and authoring dialogs
- **Backend API:** Sling Servlets mapped to `/bin/api/tickets/*` paths, handling HTTP requests/responses
- **Business Logic:** OSGi services (TicketService, StateTransitionValidator, CommentService) with dependency injection
- **Persistence:** All data stored in AEM JCR using page-based content structure

## Frontend Design

### Component Structure

**Pages in AEM:**
- `/content/ai-practical-assessment/tickets` — Container page for ticket list and filtering UI
- `/content/ai-practical-assessment/create-ticket` — Standalone page for creating new tickets
- `/content/ai-practical-assessment/tickets/<ticket-id>` — Individual ticket detail page, opens in edit mode

**Vanilla JavaScript Modules:**
```
TicketList (on /tickets page)
├── TicketSearch (search input, filter dropdown)
├── TicketTable (displays all tickets)
└── Event handlers for search, filter, pagination

TicketDetail (on /tickets/<ticket-id>, edit mode)
├── TicketForm (edit title, description, priority, assignee)
├── StateTransitionControl (dropdown for status change)
├── CommentSection
│   ├── CommentList (all comments)
│   └── CommentForm (add new comment)
└── Event handlers for form submission, validation

TicketCreate (on /create-ticket page)
├── TicketCreateForm (form with all ticket fields)
├── Input validation handlers
└── Form submission and redirect logic
```

### Page Structure in AEM

**Ticket List Page:** `/content/ai-practical-assessment/tickets`
- Contains HTML for TicketSearch component (search input, filter dropdown)
- Contains table element for TicketList with JavaScript to fetch and render
- Clicking a ticket navigates to `/tickets/{ticket-id}`
- JavaScript module: `ticket-list.js` initializes on page load

**Create Ticket Page:** `/content/ai-practical-assessment/create-ticket`
- Contains HTML form for TicketCreateForm
- JavaScript module: `ticket-create.js` handles form validation and submission
- Form submits to backend API `/bin/api/tickets` (POST)
- On success, redirects to `/tickets` (list view)

**Ticket Detail Page:** `/content/ai-practical-assessment/tickets/{ticket-id}`
- Auto-generated/programmatically created for each new ticket
- Opens in AEM edit mode
- Contains HTML for TicketDetail component
- JavaScript module: `ticket-detail.js` fetches ticket, renders form, handles edits
- Edit form submits to `/bin/api/tickets/{ticket-id}` (PUT)
- Comment form submits to `/bin/api/tickets/{ticket-id}/comments` (POST)

### State Management

- **DOM State:** Use data attributes and element properties to track current state
- **Form State:** Use FormData API for capturing input values
- **Application State:** Store current user, filters, search term in JavaScript variables
- **API Calls:** Use Fetch API with error/loading states
- **DOM Updates:** Direct DOM manipulation with querySelector, innerHTML (with sanitization)

### Key Design Decisions

1. **Search & Filter Placement:** Search bar at top of `/tickets` page, always visible
2. **Detail View:** Dedicated page at `/tickets/<ticket-id>` with edit mode, browser back returns to list
3. **Status Transition:** Dropdown showing only valid next states, warning message if invalid attempted
4. **Comments:** Reverse chronological order (newest first), immutable (no edit/delete in Core)
5. **Error Messages:** Inline field errors for forms, alert/banner for API errors, dialog for state-machine errors
6. **Loading States:** Show loading spinner while fetching, disable form buttons during submission
7. **No Framework Dependencies:** Pure vanilla JS for simplicity and direct AEM integration

## Frontend Implementation Pattern

### Module Structure

Each page has a main JavaScript module:

**ticket-list.js:**
```javascript
class TicketListManager {
  constructor() {
    this.tickets = [];
    this.searchTerm = '';
    this.statusFilter = '';
    this.init();
  }

  init() {
    this.cacheElements();
    this.bindEvents();
    this.loadTickets();
  }

  cacheElements() {
    this.searchInput = document.getElementById('search-input');
    this.statusFilter = document.getElementById('status-filter');
    this.ticketTable = document.getElementById('ticket-table');
    this.loadingSpinner = document.getElementById('loading');
  }

  bindEvents() {
    this.searchInput.addEventListener('input', e => this.onSearch(e));
    this.statusFilter.addEventListener('change', e => this.onFilterChange(e));
  }

  onSearch(event) {
    this.searchTerm = event.target.value;
    this.loadTickets();
  }

  onFilterChange(event) {
    this.statusFilter = event.target.value;
    this.loadTickets();
  }

  async loadTickets() {
    this.showLoading();
    try {
      const params = new URLSearchParams();
      if (this.searchTerm) params.append('search', this.searchTerm);
      if (this.statusFilter) params.append('status', this.statusFilter);
      
      const response = await fetch(`/bin/api/tickets?${params}`);
      const data = await response.json();
      
      this.tickets = data.tickets;
      this.render();
    } catch (error) {
      this.showError('Failed to load tickets');
    } finally {
      this.hideLoading();
    }
  }

  render() {
    this.ticketTable.innerHTML = this.tickets.map(ticket => `
      <tr>
        <td><a href="/tickets/${ticket.id}">${this.escapeHtml(ticket.title)}</a></td>
        <td>${ticket.status}</td>
        <td>${ticket.priority}</td>
        <td>${this.escapeHtml(ticket.assignedTo || 'Unassigned')}</td>
      </tr>
    `).join('');
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  showLoading() { this.loadingSpinner.style.display = 'block'; }
  hideLoading() { this.loadingSpinner.style.display = 'none'; }
  showError(msg) { alert(msg); }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => new TicketListManager());
} else {
  new TicketListManager();
}
```

**ticket-detail.js** and **ticket-create.js** follow similar patterns.

### Form Validation

```javascript
class FormValidator {
  static validateTicketForm(formData) {
    const errors = {};
    
    if (!formData.title || formData.title.trim() === '') {
      errors.title = 'Title is required';
    }
    if (!formData.description || formData.description.trim() === '') {
      errors.description = 'Description is required';
    }
    if (!formData.priority) {
      errors.priority = 'Priority is required';
    }
    if (!formData.assignedTo) {
      errors.assignedTo = 'Assignee is required';
    }
    
    return Object.keys(errors).length === 0 ? null : errors;
  }

  static displayErrors(errors) {
    document.querySelectorAll('.error-message').forEach(el => el.remove());
    for (const [field, message] of Object.entries(errors)) {
      const input = document.getElementById(field);
      const errorEl = document.createElement('div');
      errorEl.className = 'error-message';
      errorEl.textContent = message;
      input.parentNode.appendChild(errorEl);
    }
  }

  static clearErrors() {
    document.querySelectorAll('.error-message').forEach(el => el.remove());
  }
}
```

## Backend Design

### OSGi Services Architecture

```
TicketService (interface)
├── create(title, description, priority, assignee) → Ticket
├── read(ticketId) → Ticket
├── update(ticketId, fields) → Ticket
├── delete(ticketId) → void
├── list() → List<Ticket>
├── search(keyword) → List<Ticket>
└── filterByStatus(status) → List<Ticket>

StateTransitionValidator (interface)
├── isValidTransition(currentStatus, nextStatus) → boolean
├── getValidNextStates(currentStatus) → Set<String>
└── validateTransition(currentStatus, nextStatus) → void throws InvalidTransitionException

CommentService (interface)
├── addComment(ticketId, message, createdBy) → Comment
└── getComments(ticketId) → List<Comment>
```

### Service Implementations

**TicketService Implementation:**
- Injected with `ResourceResolver` (via OSGi Dependency Injection)
- Uses `ResourceResolver.getResource()` to access JCR nodes under `/content/ai-practical-assessment/tickets`
- Implements CRUD operations using JCR API
- Performs input validation (title/description not empty, priority valid)
- Throws custom exceptions for business logic violations

**StateTransitionValidator Implementation:**
- Hardcoded state machine rules (no database lookup)
- Transition matrix stored as static final Map/Set
- Validates before any status update
- Provides helpful error messages with valid next states

**CommentService Implementation:**
- Creates JCR child nodes under ticket's comments folder
- Automatically sets createdBy from current user context
- Stores createdAt as system timestamp

### Sling Servlet Endpoints

```
GET  /bin/api/tickets
POST /bin/api/tickets
GET  /bin/api/tickets/{id}
PUT  /bin/api/tickets/{id}
PUT  /bin/api/tickets/{id}/status
GET  /bin/api/tickets/{id}/comments
POST /bin/api/tickets/{id}/comments
```

- Each servlet handles single resource/method
- Input validation and error handling
- Consistent JSON response format
- HTTP status codes: 200 (OK), 201 (Created), 400 (Bad Request), 404 (Not Found), 409 (Conflict), 500 (Server Error)

## Database Design

### AEM Content Structure

**Root Container:** `/content/ai-practical-assessment`
```
/content/ai-practical-assessment/
├── tickets (container page - list and filter)
│   ├── ticket-001 (cq:Page for individual ticket)
│   │   └── jcr:content (properties)
│   │       ├── jcr:title = "Payment processing broken"
│   │       ├── description = "Users cannot complete payment..."
│   │       ├── priority = "HIGH"
│   │       ├── status = "Open"
│   │       ├── assignedTo = "user-123"
│   │       ├── createdBy = "user-456"
│   │       ├── createdAt = "2026-09-01T10:30:00Z"
│   │       ├── updatedAt = "2026-09-01T10:30:00Z"
│   │       └── comments (folder)
│   │           ├── comment-001 (nt:unstructured)
│   │           │   ├── message = "I can reproduce this on staging"
│   │           │   ├── createdBy = "user-123"
│   │           │   └── createdAt = "2026-09-01T11:00:00Z"
│   │           └── comment-002 (nt:unstructured)
│   │               ├── message = "Fix deployed, testing in production"
│   │               ├── createdBy = "user-789"
│   │               └── createdAt = "2026-09-01T14:30:00Z"
│   ├── ticket-002 (cq:Page)
│   │   └── jcr:content
│   │       └── ...
│   └── ticket-003 (cq:Page)
│       └── jcr:content
│           └── ...
├── create-ticket (cq:Page for creation form)
│   └── jcr:content (contains TicketCreateForm component)
└── [other AEM pages/content]
```

### JCR Node Types

**Tickets as cq:Page:**
- Use `cq:Page` node type for each ticket
- Leverages AEM's page management, versioning, publishing
- Properties stored in `jcr:content` child node
- Automatically assigned name/ID when page is created

**Node Type Definition for jcr:content:**
```
[nt:page] > nt:hierarchyNode
  + jcr:content (nt:unstructured) 
    - jcr:title (string) (required)
    - description (string) (required)
    - priority (string) = 'MEDIUM' (required)
    - status (string) = 'Open' (required)
    - assignedTo (string)
    - createdBy (string) (required)
    - createdAt (date) (required)
    - updatedAt (date)
  + comments (nt:unstructured)
    + * (nt:unstructured)
      - message (string)
      - createdBy (string)
      - createdAt (date)
```

### Query Strategy

**Using JCR Query Language (SQL2):**

List all tickets:
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND NOT NAME([*]) = 'jcr:content'
```

Search by keyword:
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND (
    [jcr:content/jcr:title] LIKE '%keyword%' 
    OR [jcr:content/description] LIKE '%keyword%'
  )
```

Filter by status:
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND [jcr:content/status] = 'Open'
```

Combined search and filter:
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND [jcr:content/status] = 'Open'
  AND (
    [jcr:content/jcr:title] LIKE '%keyword%' 
    OR [jcr:content/description] LIKE '%keyword%'
  )
```

### Data Constraints

- **Ticket ID:** JCR node name (auto-generated or derived from title)
- **Title:** Required, string, max 255 chars
- **Description:** Required, string, max 5000 chars
- **Priority:** Enum (HIGH, MEDIUM, LOW)
- **Status:** Enum (Open, In Progress, Resolved, Closed, Cancelled)
- **AssignedTo:** Reference to user (string, validated against user list)
- **CreatedBy:** Reference to user, set at creation, immutable
- **CreatedAt:** Timestamp, set at creation, immutable
- **UpdatedAt:** Timestamp, updated on any modification

## Validation Strategy

### Input Validation (Frontend)

- **Required Fields:** title, description, priority, assignee
- **Field Formats:** Priority must be in [HIGH, MEDIUM, LOW], assignee must be valid user
- **Length Limits:** title ≤ 255, description ≤ 5000
- **User Feedback:** Real-time validation errors under fields

### Backend Validation

- **Mandatory:** Revalidate all frontend inputs (never trust client)
- **Business Rules:** State transition validation, assignee existence, ticket existence
- **Error Responses:** HTTP 400 with JSON error object listing all validation failures
- **Idempotency:** Re-submitting same create/update request should succeed or fail consistently

### Validation at API Level

```json
// Error Response Example
{
  "error": true,
  "message": "Validation failed",
  "details": [
    { "field": "title", "message": "Title is required" },
    { "field": "priority", "message": "Priority must be one of: HIGH, MEDIUM, LOW" }
  ]
}
```

## Error Handling Strategy

### Categories

1. **Validation Errors (400 Bad Request):**
   - Missing required fields
   - Invalid field formats
   - Constraint violations
   - User-friendly field-level messages

2. **Authentication/Authorization Errors (403 Forbidden):**
   - User not authenticated
   - User lacks permission to update ticket
   - User cannot assign to another user

3. **Not Found Errors (404 Not Found):**
   - Ticket does not exist
   - Assignee user does not exist
   - Attempt to add comment to non-existent ticket

4. **Conflict Errors (409 Conflict):**
   - Invalid state transition
   - Concurrent modification (if implemented)
   - Resource already exists

5. **Server Errors (500 Internal Server Error):**
   - JCR connection failure
   - Unexpected exception in business logic
   - Log full stack trace, return generic message to client

### Frontend Error Handling

- **Form Validation:** Show inline errors, prevent submission
- **API Errors:** Show alert/banner with error message
- **State Machine Errors:** Dialog with message and valid next states
- **Network Errors:** Retry option, clear indication of connectivity issue

## Testing Strategy

Refer to [test-strategy.md](test-strategy.md) for detailed test plans.

**Quick Summary:**
- **Unit Tests:** Service methods, state machine rules
- **Integration Tests:** Sling Models, full CRUD with JCR
- **API Tests:** Endpoint behavior, error cases
- **E2E Tests:** User workflows (create → list → search → update)
- **Validation:** Input validation, boundary values
- **State Machine:** All transitions, edge cases

## Security Considerations

1. **Input Validation:** Prevent JCR injection attacks via query escaping
2. **Authorization:** Use AEM's service users, check user permissions
3. **CSRF Protection:** Use AEM's form tokens (auto-handled by Sling)
4. **XSS Prevention:** Use textContent instead of innerHTML, escape HTML before display
5. **Sensitive Data:** No passwords or API keys in code/logs
6. **Access Control:** Ensure only authorized users can update tickets

## Performance Considerations

1. **JCR Queries:** Use indexes for title, status, priority to speed up search/filter
2. **Pagination:** Implement limit/offset for large result sets (Stretch goal)
3. **Caching:** Cache frequently accessed tickets (e.g., recent tickets)
4. **Lazy Loading:** Load comments only when detail view opens
5. **Asset Optimization:** Minify JS/CSS (handled by Maven build)

## Trade-offs & Decisions

| Decision | Rationale | Alternatives Considered |
|----------|-----------|------------------------|
| **Page-Based Content Structure** | Leverages AEM's page hierarchy, natural fit for AEM, enables versioning/publishing | Asset-based (less AEM-native), custom nodes (more complex) |
| **Dedicated Pages for List/Create/Detail** | Clean separation of concerns, each page has specific purpose | Single SPA with routing (harder to manage in AEM context) |
| **Vanilla JavaScript** | No framework dependencies, direct DOM control, simpler deployment in AEM | React (more complex bundle), jQuery (outdated) |
| **Sling Servlets for API** | AEM-native, easy service injection | Spring Boot (not idiomatic in AEM), Express.js (external) |
| **OSGi Services** | Dependency injection, lifecycle management | Static utility classes (hard to test), Spring beans (not OSGi) |
| **Simple State Machine** | Hardcoded transitions (easy to understand and test) | Database-driven (more flexible but complex) |
| **Immutable Comments** | Simpler logic, audit trail clarity | Editable (requires more validation, conflict handling) |
