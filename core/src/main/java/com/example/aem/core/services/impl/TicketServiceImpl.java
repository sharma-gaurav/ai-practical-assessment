package com.example.aem.core.services.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.PersistenceException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.exceptions.InvalidUserException;
import com.example.aem.core.services.CommentService;
import com.example.aem.core.services.StateTransitionValidator;
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;

@Component(service = TicketService.class)
public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);
    private static final String TICKETS_ROOT_PATH = "/content/ai-practical-assessment/tickets";
    private static final String SERVICE_USER = "ai-practical-assessment-ticketservice";

    @Reference
    private SystemResourceResolverService systemResolverService;

    @Reference
    private StateTransitionValidator stateTransitionValidator;

    @Reference
    private CommentService commentService;

    @Override
    public Map<String, Object> create(String title, String description, String priority, String assignedTo) {
        logger.debug("Creating ticket with title: {}, priority: {}, assignedTo: {}", title, priority, assignedTo);

        // Validate inputs
        validateInputs(title, description, priority, assignedTo);

        // Verify assigned user exists using AEM User Management API
        if (!userExists(assignedTo)) {
            throw new InvalidUserException("User does not exist: " + assignedTo);
        }

        try {
            // Generate unique ticket ID
            String ticketId = "ticket-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Create ticket page in JCR
            Map<String, Object> ticket = createTicketPage(ticketId, title, description, priority, assignedTo);

            logger.info("Ticket created successfully with ID: {}", ticketId);
            return ticket;

        } catch (PersistenceException e) {
            logger.error("Failed to persist ticket", e);
            throw new RuntimeException("Failed to create ticket: " + e.getMessage(), e);
        } catch (RepositoryException e) {
            logger.error("Repository error while creating ticket", e);
            throw new RuntimeException("Repository error: " + e.getMessage(), e);
        }
    }

    private void validateInputs(String title, String description, String priority, String assignedTo) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("Title is required");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title must not exceed 255 characters");
        }

        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("Description is required");
        }
        if (description.length() > 5000) {
            throw new IllegalArgumentException("Description must not exceed 5000 characters");
        }

        if (StringUtils.isBlank(priority)) {
            throw new IllegalArgumentException("Priority is required");
        }
        if (!isValidPriority(priority)) {
            throw new IllegalArgumentException("Priority must be one of: HIGH, MEDIUM, LOW");
        }

        if (StringUtils.isBlank(assignedTo)) {
            throw new IllegalArgumentException("Assignee is required");
        }
    }

    private boolean isValidPriority(String priority) {
        return "HIGH".equals(priority) || "MEDIUM".equals(priority) || "LOW".equals(priority);
    }

    private boolean userExists(String userId) {
        try {
            return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    logger.debug("Session is null for user check: {}", userId);
                    return false;
                }
                if (session instanceof JackrabbitSession) {
                    JackrabbitSession jrSession = (JackrabbitSession) session;
                    UserManager userManager = jrSession.getUserManager();
                    boolean exists = userManager.getAuthorizable(userId) != null;
                    logger.debug("User {} exists: {}", userId, exists);
                    return exists;
                }
                logger.debug("Session is not JackrabbitSession for user check: {}", userId);
                return false;
            });
        } catch (Exception e) {
            logger.error("Error checking if user exists: {}", userId, e);
            return false;
        }
    }

    private Map<String, Object> createTicketPage(String ticketId, String title, String description,
            String priority, String assignedTo) throws PersistenceException, RepositoryException {
        // Use system resource resolver for ticket creation and user details
        return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
            // Get current authenticated user
            String createdBy = resolver.getUserID();

            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new RuntimeException("Cannot adapt ResourceResolver to Session");
            }

            // Get or create parent node
            Node ticketsRoot = getOrCreateNode(session, TICKETS_ROOT_PATH);

            // Create ticket page node
            Node ticketNode = ticketsRoot.addNode(ticketId, "cq:Page");

            // Create jcr:content node with properties
            Node contentNode = ticketNode.addNode("jcr:content", "cq:PageContent");

            // Set properties
            Calendar now = Calendar.getInstance();

            contentNode.setProperty("cq:template",
                    "/conf/ai-practical-assessment/settings/wcm/templates/ticket-content");
            contentNode.setProperty("sling:resourceType", "ai-practical-assessment/components/page");
            contentNode.setProperty("jcr:title", title);
            contentNode.setProperty("jcr:description", description);
            contentNode.setProperty("cq:lastModified", now);
            contentNode.setProperty("cq:lastModifiedBy", createdBy);
            contentNode.setProperty("description", description);
            contentNode.setProperty("priority", priority);
            contentNode.setProperty("status", "Open");
            contentNode.setProperty("assignedTo", assignedTo);
            contentNode.setProperty("createdBy", createdBy);
            contentNode.setProperty("createdAt", now);
            contentNode.setProperty("updatedAt", now);

            // Save changes
            session.save();

            // Return ticket data as map
            Map<String, Object> ticket = new HashMap<>();
            ticket.put("id", ticketId);
            ticket.put("title", title);
            ticket.put("description", description);
            ticket.put("priority", priority);
            ticket.put("status", "Open");
            ticket.put("assignedTo", assignedTo);
            ticket.put("createdBy", createdBy);
            ticket.put("createdAt", ISO8601.format(now));
            ticket.put("updatedAt", ISO8601.format(now));

            return ticket;
        });
    }

    private Node getOrCreateNode(Session session, String path) throws RepositoryException {
        try {
            return session.getNode(path);
        } catch (javax.jcr.PathNotFoundException e) {
            logger.debug("Creating missing node at path: {}", path);
            String parentPath = path.substring(0, path.lastIndexOf('/'));
            if (!"/".equals(parentPath) && !parentPath.isEmpty()) {
                getOrCreateNode(session, parentPath);
            }

            Node parentNode = session.getNode(parentPath.isEmpty() || "/".equals(parentPath) ? "/" : parentPath);
            String nodeName = path.substring(path.lastIndexOf('/') + 1);
            return parentNode.addNode(nodeName, "nt:unstructured");
        }
    }

    @Override
    public Map<String, Object> read(String ticketId) {
        logger.debug("Reading ticket with ID: {}", ticketId);

        if (StringUtils.isBlank(ticketId)) {
            throw new IllegalArgumentException("Ticket ID is required");
        }

        try {
            return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    throw new RuntimeException("Cannot adapt ResourceResolver to Session");
                }

                String ticketPath = TICKETS_ROOT_PATH + "/" + ticketId;
                try {
                    Node ticketNode = session.getNode(ticketPath);
                    Node contentNode = ticketNode.getNode("jcr:content");

                    // Build ticket map
                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("id", ticketId);
                    ticket.put("title", contentNode.getProperty("jcr:title").getString());
                    ticket.put("description", contentNode.getProperty("description").getString());
                    ticket.put("priority", contentNode.getProperty("priority").getString());
                    ticket.put("status", contentNode.getProperty("status").getString());
                    ticket.put("assignedTo", contentNode.getProperty("assignedTo").getString());
                    ticket.put("createdBy", contentNode.getProperty("createdBy").getString());
                    ticket.put("createdAt", ISO8601.format(contentNode.getProperty("createdAt").getDate()));
                    ticket.put("updatedAt", ISO8601.format(contentNode.getProperty("updatedAt").getDate()));

                    // Include comments
                    ticket.put("comments", commentService.getComments(ticketId));

                    return ticket;
                } catch (javax.jcr.PathNotFoundException e) {
                    throw new RuntimeException("Ticket not found: " + ticketId, e);
                }
            });
        } catch (Exception e) {
            logger.error("Error reading ticket: {}", ticketId, e);
            throw new RuntimeException("Failed to read ticket: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> update(String ticketId, Map<String, Object> updates) {
        logger.debug("Updating ticket with ID: {}", ticketId);

        if (StringUtils.isBlank(ticketId)) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No updates provided");
        }

        // Validate update fields
        Map<String, String> errors = new HashMap<>();
        if (updates.containsKey("title")) {
            String title = (String) updates.get("title");
            if (StringUtils.isBlank(title)) {
                errors.put("title", "Title is required");
            } else if (title.length() > 255) {
                errors.put("title", "Title must not exceed 255 characters");
            }
        }

        if (updates.containsKey("description")) {
            String description = (String) updates.get("description");
            if (StringUtils.isBlank(description)) {
                errors.put("description", "Description is required");
            } else if (description.length() > 5000) {
                errors.put("description", "Description must not exceed 5000 characters");
            }
        }

        if (updates.containsKey("priority")) {
            String priority = (String) updates.get("priority");
            if (StringUtils.isBlank(priority)) {
                errors.put("priority", "Priority is required");
            } else if (!isValidPriority(priority)) {
                errors.put("priority", "Priority must be one of: HIGH, MEDIUM, LOW");
            }
        }

        if (updates.containsKey("assignedTo")) {
            String assignedTo = (String) updates.get("assignedTo");
            if (StringUtils.isBlank(assignedTo)) {
                errors.put("assignedTo", "Assignee is required");
            } else if (!userExists(assignedTo)) {
                errors.put("assignedTo", "User does not exist: " + assignedTo);
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + errors);
        }

        try {
            return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    throw new RuntimeException("Cannot adapt ResourceResolver to Session");
                }

                String ticketPath = TICKETS_ROOT_PATH + "/" + ticketId;
                try {
                    Node ticketNode = session.getNode(ticketPath);
                    Node contentNode = ticketNode.getNode("jcr:content");

                    Calendar now = Calendar.getInstance();
                    contentNode.setProperty("cq:lastModified", now);
                    contentNode.setProperty("cq:lastModifiedBy", resolver.getUserID());
                    contentNode.setProperty("updatedAt", now);

                    // Apply updates
                    if (updates.containsKey("title")) {
                        contentNode.setProperty("jcr:title", (String) updates.get("title"));
                    }
                    if (updates.containsKey("description")) {
                        contentNode.setProperty("description", (String) updates.get("description"));
                    }
                    if (updates.containsKey("priority")) {
                        contentNode.setProperty("priority", (String) updates.get("priority"));
                    }
                    if (updates.containsKey("assignedTo")) {
                        contentNode.setProperty("assignedTo", (String) updates.get("assignedTo"));
                    }

                    session.save();

                    // Return updated ticket
                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("id", ticketId);
                    ticket.put("title", contentNode.getProperty("jcr:title").getString());
                    ticket.put("description", contentNode.getProperty("description").getString());
                    ticket.put("priority", contentNode.getProperty("priority").getString());
                    ticket.put("status", contentNode.getProperty("status").getString());
                    ticket.put("assignedTo", contentNode.getProperty("assignedTo").getString());
                    ticket.put("createdBy", contentNode.getProperty("createdBy").getString());
                    ticket.put("createdAt", ISO8601.format(contentNode.getProperty("createdAt").getDate()));
                    ticket.put("updatedAt", ISO8601.format(contentNode.getProperty("updatedAt").getDate()));

                    return ticket;
                } catch (javax.jcr.PathNotFoundException e) {
                    throw new RuntimeException("Ticket not found: " + ticketId, e);
                }
            });
        } catch (Exception e) {
            logger.error("Error updating ticket: {}", ticketId, e);
            throw new RuntimeException("Failed to update ticket: " + e.getMessage(), e);
        }
    }

    @Override
    public void changeStatus(String ticketId, String newStatus) {
        logger.debug("Changing status for ticket: {} to: {}", ticketId, newStatus);

        if (StringUtils.isBlank(ticketId)) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (StringUtils.isBlank(newStatus)) {
            throw new IllegalArgumentException("Status is required");
        }

        try {
            systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    throw new RuntimeException("Cannot adapt ResourceResolver to Session");
                }

                String ticketPath = TICKETS_ROOT_PATH + "/" + ticketId;
                try {
                    Node ticketNode = session.getNode(ticketPath);
                    Node contentNode = ticketNode.getNode("jcr:content");

                    String currentStatus = contentNode.getProperty("status").getString();

                    // Validate transition
                    stateTransitionValidator.validateTransition(currentStatus, newStatus);

                    // Update status
                    Calendar now = Calendar.getInstance();
                    contentNode.setProperty("status", newStatus);
                    contentNode.setProperty("cq:lastModified", now);
                    contentNode.setProperty("cq:lastModifiedBy", resolver.getUserID());
                    contentNode.setProperty("updatedAt", now);

                    session.save();
                    logger.info("Ticket {} status changed from {} to {}", ticketId, currentStatus, newStatus);

                    return null;
                } catch (javax.jcr.PathNotFoundException e) {
                    throw new RuntimeException("Ticket not found: " + ticketId, e);
                }
            });
        } catch (IllegalStateException e) {
            logger.warn("Invalid status transition for ticket {}: {}", ticketId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error changing status for ticket: {}", ticketId, e);
            throw new RuntimeException("Failed to change status: " + e.getMessage(), e);
        }
    }

    /**
     * ISO8601 formatter utility
     */
    private static class ISO8601 {
        static String format(Calendar cal) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.format(cal.getTime());
        }
    }
}
