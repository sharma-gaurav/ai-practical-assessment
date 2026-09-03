package com.example.aem.core.services;

import java.util.Map;

public interface TicketService {

    /**
     * Creates a new ticket in the JCR repository
     *
     * @param title the ticket title (required, max 255 chars)
     * @param description the ticket description (required, max 5000 chars)
     * @param priority the ticket priority (HIGH, MEDIUM, LOW)
     * @param assignedTo the user ID to assign the ticket to (required)
     * @return Map containing ticket data including id, title, description, priority, status, assignedTo,
     *         createdBy, createdAt, updatedAt
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws com.example.aem.core.exceptions.InvalidUserException if assignedTo user doesn't exist
     */
    Map<String, Object> create(String title, String description, String priority, String assignedTo);
}
