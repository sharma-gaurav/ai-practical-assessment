# API Contract

## Overview

RESTful API for Support Ticket Management System. Base path: `/bin/api/tickets`

All endpoints return JSON. Timestamps are ISO 8601 format (UTC).

**Important:** The API uses two consolidated servlet paths for all ticket operations:
- `/bin/api/tickets` — Handles ticket listing, creation, detail retrieval, field updates, and status changes
- `/bin/api/tickets/comments` — Handles comment operations

---

## Core Endpoints

### 1. List All Tickets (with search and filter)

**Endpoint:** `GET /bin/api/tickets`

**Query Parameters:**
- `search` (optional): Keyword to search in title and description (case-insensitive)
- `status` (optional): Filter by status (Open, In Progress, Resolved, Closed, Cancelled)
- `page` (optional): Page number (default: 0)
- `limit` (optional): Results per page (default: 20)

**Request:**
```
GET /bin/api/tickets?search=login&status=Open&page=0&limit=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "tickets": [
    {
      "id": "ticket-a1b2c3d4",
      "title": "Mobile login issue",
      "description": "Users unable to log in from mobile devices...",
      "priority": "HIGH",
      "status": "Open",
      "assignedTo": "admin",
      "createdBy": "admin",
      "createdAt": "2026-09-01T10:30:00Z",
      "updatedAt": "2026-09-01T10:30:00Z"
    }
  ],
  "page": 0,
  "limit": 20,
  "total": 1
}
```

**Response (500 Server Error):**
```json
{
  "success": false,
  "error": "Failed to fetch tickets: [error details]"
}
```

---

### 2. Create New Ticket

**Endpoint:** `POST /bin/api/tickets`

**Request:**
```json
{
  "title": "Fix login issue",
  "description": "Users unable to log in after update",
  "priority": "HIGH",
  "assignedto": "admin"
}
```

**Validation Rules:**
- `title`: Required, string, 1-255 characters, non-empty after trim
- `description`: Required, string, 1-5000 characters, non-empty after trim
- `priority`: Required, one of [HIGH, MEDIUM, LOW]
- `assignedto`: Required, must be a valid user in AEM system

**Response (201 Created):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-a1b2c3d4",
    "title": "Fix login issue",
    "description": "Users unable to log in after update",
    "priority": "HIGH",
    "status": "Open",
    "assignedTo": "admin",
    "createdBy": "current-user",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Response (400 Bad Request - Validation Error):**
```json
{
  "success": false,
  "error": "Validation failed",
  "details": {
    "title": "Title is required"
  }
}
```

**Response (400 Bad Request - Invalid User):**
```json
{
  "success": false,
  "error": "User does not exist: admin"
}
```

---

### 3. Get Ticket Detail

**Endpoint:** `GET /bin/api/tickets?id=<ticket-id>`

**Query Parameters:**
- `id`: Ticket ID (e.g., ticket-a1b2c3d4)

**Request:**
```
GET /bin/api/tickets?id=ticket-a1b2c3d4
```

**Response (200 OK):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-a1b2c3d4",
    "title": "Fix login issue",
    "description": "Users unable to log in after update",
    "priority": "HIGH",
    "status": "Open",
    "assignedTo": "admin",
    "createdBy": "admin",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z",
    "comments": [
      {
        "id": "comment-001",
        "message": "Working on this",
        "createdBy": "admin",
        "createdAt": "2026-09-01T11:00:00Z"
      }
    ]
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

**Endpoint:** `PUT /bin/api/tickets?id=<ticket-id>`

**Query Parameters:**
- `id`: Ticket ID (required)

**Request (partial update):**
```json
{
  "title": "Fixed: login issue resolved",
  "priority": "MEDIUM"
}
```

**Validation Rules:**
- All fields optional; only provided fields are updated
- `title`: 1-255 characters if provided, non-empty after trim
- `description`: 1-5000 characters if provided, non-empty after trim
- `priority`: One of [HIGH, MEDIUM, LOW] if provided
- `assignedTo`: Valid user ID if provided

**Response (200 OK):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-a1b2c3d4",
    "title": "Fixed: login issue resolved",
    "description": "Users unable to log in after update",
    "priority": "MEDIUM",
    "status": "Open",
    "assignedTo": "admin",
    "createdBy": "admin",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T14:45:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation failed"
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

**Endpoint:** `PUT /bin/api/tickets?id=<ticket-id>`

**Query Parameters:**
- `id`: Ticket ID (required)

**Request:**
```json
{
  "newStatus": "In Progress"
}
```

**Validation Rules:**
- `newStatus`: Required, one of [Open, In Progress, Resolved, Closed, Cancelled]
- Transition must be valid according to state machine:
  - Open → [In Progress, Cancelled]
  - In Progress → [Resolved, Cancelled, Open]
  - Resolved → [Closed]
  - Closed → (terminal, no transitions)
  - Cancelled → (terminal, no transitions)

**Response (200 OK - Valid Transition):**
```json
{
  "success": true,
  "ticket": {
    "id": "ticket-a1b2c3d4",
    "title": "Fix login issue",
    "description": "Users unable to log in after update",
    "priority": "HIGH",
    "status": "In Progress",
    "assignedTo": "admin",
    "createdBy": "admin",
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T14:45:00Z"
  }
}
```

**Response (409 Conflict - Invalid Transition):**
```json
{
  "success": false,
  "error": "Invalid status transition",
  "details": {
    "status": "Invalid transition. Allowed next states: [In Progress, Cancelled]"
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

## Comment Endpoints

### 6. Get Comments for a Ticket

**Endpoint:** `GET /bin/api/tickets/comments?id=<ticket-id>`

**Query Parameters:**
- `id`: Ticket ID (required)

**Request:**
```
GET /bin/api/tickets/comments?id=ticket-a1b2c3d4
```

**Response (200 OK):**
```json
{
  "success": true,
  "comments": [
    {
      "id": "comment-001",
      "message": "I can reproduce this",
      "createdBy": "admin",
      "createdAt": "2026-09-01T11:00:00Z"
    },
    {
      "id": "comment-002",
      "message": "Fix deployed",
      "createdBy": "admin",
      "createdAt": "2026-09-01T14:30:00Z"
    }
  ]
}
```

**Response (200 OK - No Comments):**
```json
{
  "success": true,
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

**Endpoint:** `POST /bin/api/tickets/comments`

The ticket ID is supplied in the request body (not as a query parameter).

**Request:**
```json
{
  "id": "ticket-a1b2c3d4",
  "message": "Working on this issue"
}
```

**Validation Rules:**
- `id`: Required, must be a valid ticket ID
- `message`: Required, non-empty after trim

**Response (201 Created):**
```json
{
  "success": true,
  "comment": {
    "id": "comment-003",
    "message": "Working on this issue",
    "createdBy": "current-user",
    "createdAt": "2026-09-01T15:00:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation failed"
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
  "details": { /* optional validation details */ }
}
```

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 400 | Bad Request - Validation error or invalid input |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Invalid state transition or write conflict |
| 500 | Server Error - Internal error |

## Authentication & Authorization

- Requests must be made by an authenticated AEM user (use `-u admin:admin` with curl)
- Current user ID is automatically captured from `ResourceResolver.getUserID()`
- No role-based authorization; all authenticated users can view/edit all tickets

## Error Handling

- Frontend should always check `success: true/false`
- Display error message from `error` field
- For validation errors, check `details` object for field-level messages
- For 409 Conflict on status change, display the allowed states from `details.status`
- Implement retry logic with exponential backoff for 409 Conflict responses (transient JCR write conflicts)

## Search & Filter

- Search is **case-insensitive** — wrapped with LOWER() in query
- Search terms with special characters (e.g., "bug/crash") are **SQL-escaped** — single quotes replaced with ''
- Search and status filter can be combined in one request
- Empty search string returns all tickets (matching status filter if provided)

## Retry Strategy

On 409 Conflict responses (write conflicts), implement exponential backoff:
- Retry 1: 100ms delay
- Retry 2: 200ms delay
- Retry 3: 400ms delay
- Max 3 retries before failing

This handles transient JCR write conflicts caused by concurrent updates.
