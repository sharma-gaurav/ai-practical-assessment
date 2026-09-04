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

    /**
     * Retrieves a ticket by ID
     *
     * @param ticketId the ticket ID
     * @return Map containing ticket data
     * @throws IllegalArgumentException if ticketId is null or empty
     * @throws RuntimeException if ticket not found
     */
    Map<String, Object> read(String ticketId);

    /**
     * Updates ticket fields (title, description, priority, assignedTo)
     *
     * @param ticketId the ticket ID
     * @param updates Map containing fields to update
     * @return Map containing updated ticket data
     * @throws IllegalArgumentException if updates are invalid
     * @throws RuntimeException if ticket not found
     */
    Map<String, Object> update(String ticketId, Map<String, Object> updates);

    /**
     * Changes the ticket status (validates transition via StateTransitionValidator)
     *
     * @param ticketId the ticket ID
     * @param newStatus the new status
     * @throws IllegalArgumentException if newStatus is invalid
     * @throws IllegalStateException if transition is not allowed
     * @throws RuntimeException if ticket not found
     */
    void changeStatus(String ticketId, String newStatus);
}
