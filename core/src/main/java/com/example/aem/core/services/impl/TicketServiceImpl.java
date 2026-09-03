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
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;

@Component(service = TicketService.class)
public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);
    private static final String TICKETS_ROOT_PATH = "/content/ai-practical-assessment/tickets";
    private static final String SERVICE_USER = "ai-practical-assessment-ticketservice";

    @Reference
    private SystemResourceResolverService systemResolverService;

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
            Node contentNode = ticketNode.addNode("jcr:content", "nt:unstructured");

            // Set properties
            Calendar now = Calendar.getInstance();

            contentNode.setProperty("jcr:title", title);
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
