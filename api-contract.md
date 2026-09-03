# API Contract

## Overview

RESTful API for Support Ticket Management System. Base path: `/bin/api/tickets`

All endpoints return JSON. Timestamps are ISO 8601 format (UTC).

---

## Endpoints

### 1. List All Tickets (with search and filter)

**Endpoint:** `GET /bin/api/tickets`

**Query Parameters:**
- `search` (optional): Keyword to search in title and description
- `status` (optional): Filter by status (Open, In Progress, Resolved, Closed, Cancelled)
- `page` (optional, Stretch): Page number (default: 1)
- `limit` (optional, Stretch): Results per page (default: 20)

**Request:**
```
GET /bin/api/tickets?search=payment&status=Open
```

**Response (200 OK):**
```json
{
  "success": true,
  "count": 2,
  "tickets": [
    {
      "id": "ticket-001",
      "title": "Payment processing broken",
      "description": "Users cannot complete payment...",
      "priority": "HIGH",
      "status": "Open",
      "assignedTo": "user-123",
      "createdBy": "user-456",
      "createdAt": "2026-09-01T10:30:00Z",
      "updatedAt": "2026-09-01T10:30:00Z"
    },
    {
      "id": "ticket-002",
      "title": "Payment email notifications missing",
      "description": "Users don't receive payment confirmations...",
      "priority": "MEDIUM",
      "status": "Open",
      "assignedTo": "user-789",
      "createdBy": "user-456",
      "createdAt": "2026-09-01T11:15:00Z",
      "updatedAt": "2026-09-01T11:15:00Z"
    }
  ]
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Invalid status value"
}
```

**Response (500 Server Error):**
```json
{
  "success": false,
  "error": "Failed to retrieve tickets"
}
```

---

### 2. Create New Ticket

**Endpoint:** `POST /bin/api/tickets`

**Request:**
```json
{
  "title": "Payment processing broken",
  "description": "Users cannot complete payment transactions in checkout flow",
  "priority": "HIGH",
  "assignedTo": "user-123"
}
```

**Validation Rules:**
- `title`: Required, string, 1-255 characters, non-empty after trim
- `description`: Required, string, 1-5000 characters, non-empty after trim
- `priority`: Required, one of [HIGH, MEDIUM, LOW]
- `assignedTo`: Required, must be a valid user ID in AEM system

**Response (201 Created):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-001",
    "title": "Payment processing broken",
    "description": "Users cannot complete payment transactions in checkout flow",
    "priority": "HIGH",
    "status": "Open",
    "assignedTo": "user-123",
    "createdBy": "current-user-id",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    { "field": "title", "message": "Title is required" },
    { "field": "priority", "message": "Priority must be one of: HIGH, MEDIUM, LOW" }
  ]
}
```

**Response (400 Bad Request - Invalid User):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    { "field": "assignedTo", "message": "User does not exist" }
  ]
}
```

---

### 3. Get Ticket Detail

**Endpoint:** `GET /bin/api/tickets/{id}`

**Path Parameters:**
- `id`: Ticket ID (e.g., ticket-001)

**Request:**
```
GET /bin/api/tickets/ticket-001
```

**Response (200 OK):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-001",
    "title": "Payment processing broken",
    "description": "Users cannot complete payment transactions...",
    "priority": "HIGH",
    "status": "Open",
    "assignedTo": "user-123",
    "createdBy": "user-456",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Ticket not found"
}
```

---

### 4. Update Ticket Fields

**Endpoint:** `PUT /bin/api/tickets/{id}`

**Path Parameters:**
- `id`: Ticket ID

**Request (partial update):**
```json
{
  "title": "Payment processing broken - checkout flow",
  "description": "Users cannot complete payment transactions in checkout flow. Affects 5% of transactions.",
  "priority": "CRITICAL",
  "assignedTo": "user-789"
}
```

**Validation Rules:**
- All fields optional; only provided fields are updated
- `title`: 1-255 characters if provided
- `description`: 1-5000 characters if provided
- `priority`: One of [HIGH, MEDIUM, LOW] if provided
- `assignedTo`: Valid user ID if provided

**Response (200 OK):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-001",
    "title": "Payment processing broken - checkout flow",
    "description": "Users cannot complete payment transactions in checkout flow. Affects 5% of transactions.",
    "priority": "CRITICAL",
    "status": "Open",
    "assignedTo": "user-789",
    "createdBy": "user-456",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T14:45:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    { "field": "assignedTo", "message": "User does not exist" }
  ]
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Ticket not found"
}
```

---

### 5. Change Ticket Status

**Endpoint:** `PUT /bin/api/tickets/{id}/status`

**Path Parameters:**
- `id`: Ticket ID

**Request:**
```json
{
  "status": "In Progress"
}
```

**Validation Rules:**
- `status`: Required, one of [Open, In Progress, Resolved, Closed, Cancelled]
- Transition must be valid according to state machine:
  - Open → [In Progress, Cancelled]
  - In Progress → [Resolved, Cancelled]
  - Resolved → [Closed]
  - Closed → (terminal, no transitions)
  - Cancelled → (terminal, no transitions)

**Response (200 OK - Valid Transition):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-001",
    "title": "Payment processing broken",
    "description": "...",
    "priority": "HIGH",
    "status": "In Progress",
    "assignedTo": "user-123",
    "createdBy": "user-456",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T14:45:00Z"
  }
}
```

**Response (409 Conflict - Invalid Transition):**
```json
{
  "success": false,
  "error": "Invalid state transition",
  "currentStatus": "Open",
  "requestedStatus": "Resolved",
  "validNextStates": ["In Progress", "Cancelled"],
  "message": "Cannot transition from Open to Resolved. Valid transitions: In Progress, Cancelled"
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Ticket not found"
}
```

---

### 6. Get Comments for a Ticket

**Endpoint:** `GET /bin/api/tickets/{id}/comments`

**Path Parameters:**
- `id`: Ticket ID

**Request:**
```
GET /bin/api/tickets/ticket-001/comments
```

**Response (200 OK):**
```json
{
  "success": true,
  "count": 2,
  "comments": [
    {
      "id": "comment-002",
      "message": "Fix deployed, testing in production",
      "createdBy": "user-789",
      "createdAt": "2026-09-01T14:30:00Z"
    },
    {
      "id": "comment-001",
      "message": "I can reproduce this on staging",
      "createdBy": "user-123",
      "createdAt": "2026-09-01T11:00:00Z"
    }
  ]
}
```

**Response (200 OK - No Comments):**
```json
{
  "success": true,
  "count": 0,
  "comments": []
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Ticket not found"
}
```

---

### 7. Add Comment to a Ticket

**Endpoint:** `POST /bin/api/tickets/{id}/comments`

**Path Parameters:**
- `id`: Ticket ID

**Request:**
```json
{
  "message": "I can reproduce this on staging environment with steps: 1. Login 2. Add item to cart 3. Click checkout"
}
```

**Validation Rules:**
- `message`: Required, string, 1-2000 characters, non-empty after trim

**Response (201 Created):**
```json
{
  "success": true,
  "comment": {
    "id": "comment-001",
    "message": "I can reproduce this on staging environment with steps: 1. Login 2. Add item to cart 3. Click checkout",
    "createdBy": "current-user-id",
    "createdAt": "2026-09-01T11:00:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": [
    { "field": "message", "message": "Message is required and cannot be empty" }
  ]
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Ticket not found"
}
```

---

## Response Format

All responses follow this structure:

**Successful Response:**
```json
{
  "success": true,
  "data": { /* endpoint-specific data */ }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Error message",
  "details": [ /* optional validation details */ ]
}
```

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 400 | Bad Request - Validation error or invalid input |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Invalid state transition |
| 500 | Server Error - Internal error |

## Authentication & Authorization

- Requests must be made by an authenticated AEM user
- Current user ID is automatically captured from `ResourceResolver.getUserID()`
- No role-based authorization in Core; all users can view/edit all tickets

## Error Handling

- Frontend should check `success: true/false` and display errors from `error` and `details` fields
- For state machine errors (409), display `message` and `validNextStates` to user
- For validation errors (400), display field-level messages from `details` array
- Never commit secrets (keys, tokens) in responses

## Rate Limiting

Not implemented in Core. Stretch goal: Implement rate limiting per user.

## Pagination (Stretch)

Query parameters:
- `page`: 1-indexed page number
- `limit`: Results per page (min 1, max 100, default 20)

Response includes:
```json
{
  "success": true,
  "page": 1,
  "limit": 20,
  "total": 45,
  "totalPages": 3,
  "tickets": [...]
}
```

## CORS Headers

If frontend is on different origin, enable CORS headers in Sling Servlets.
