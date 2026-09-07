package com.example.aem.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.services.StateTransitionValidator;
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=GET,POST,PUT",
        "sling.servlet.paths=/bin/api/tickets",
        "service.ranking=3000"
    }
)
public class TicketOperationServlet implements Servlet {

    private static final Logger logger = LoggerFactory.getLogger(TicketOperationServlet.class);
    private static final Gson gson = new Gson();
    private static final String TICKETS_PATH = "/content/ai-practical-assessment/tickets";
    private static final String SERVICE_USER = "ai-practical-assessment-ticketservice";

    @Reference
    private TicketService ticketService;

    @Reference
    private StateTransitionValidator stateTransitionValidator;

    @Reference
    private SystemResourceResolverService systemResolverService;

    @Override
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException {
        // No-op
    }

    @Override
    public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
            throws javax.servlet.ServletException, IOException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        String method = slingRequest.getMethod();

        if ("GET".equals(method)) {
            doGet(slingRequest, slingResponse);
        } else if ("POST".equals(method)) {
            doPost(slingRequest, slingResponse);
        } else if ("PUT".equals(method)) {
            doPut(slingRequest, slingResponse);
        } else {
            slingResponse.sendError(SlingHttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String requestBody = request.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);

            String title = requestJson.has("title") ? requestJson.get("title").getAsString() : null;
            String description = requestJson.has("description") ? requestJson.get("description").getAsString() : null;
            String priority = requestJson.has("priority") ? requestJson.get("priority").getAsString() : null;
            String assignedTo = requestJson.has("assignedto") ? requestJson.get("assignedto").getAsString() : null;

            // Validate input
            if (title == null || title.trim().isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", Map.of("title", "Title is required"));
                return;
            }
            if (description == null || description.trim().isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", Map.of("description", "Description is required"));
                return;
            }
            if (priority == null || (!priority.equals("HIGH") && !priority.equals("MEDIUM") && !priority.equals("LOW"))) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", Map.of("priority", "Priority must be one of: HIGH, MEDIUM, LOW"));
                return;
            }
            if (assignedTo == null || assignedTo.trim().isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", Map.of("assignedto", "Assignee is required"));
                return;
            }

            try {
                Map<String, Object> ticket = ticketService.create(title.trim(), description.trim(), priority, assignedTo);
                response.setStatus(SlingHttpServletResponse.SC_CREATED);
                writeSuccessResponse(response, ticket);
            } catch (IllegalArgumentException e) {
                logger.warn("Validation error: {}", e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, e.getMessage(), null);
            } catch (Exception e) {
                logger.error("Failed to create ticket", e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to create ticket", null);
            }

        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in request body", e);
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "Invalid JSON in request body", null);
        } catch (Exception e) {
            logger.error("Unexpected error in TicketOperationServlet POST", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Server error", null);
        }
    }

    private void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String ticketId = request.getParameter("id");

            // If id parameter provided, get specific ticket detail
            if (ticketId != null && !ticketId.isEmpty()) {
                getTicketDetail(ticketId, response);
            } else {
                // Otherwise, list all tickets with optional search/filter
                listTickets(request, response);
            }
        } catch (Exception e) {
            logger.error("Unexpected error in TicketOperationServlet GET", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Server error", null);
        }
    }

    private void getTicketDetail(String ticketId, SlingHttpServletResponse response) throws IOException {
        try {
            Map<String, Object> ticket = ticketService.read(ticketId);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            writeSuccessResponse(response, ticket);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.debug("Ticket not found: {}", ticketId);
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                writeErrorResponse(response, "Ticket not found", null);
            } else {
                logger.error("Error reading ticket: {}", ticketId, e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to read ticket", null);
            }
        }
    }

    private void listTickets(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String searchKeyword = request.getParameter("search");
            String statusFilter = request.getParameter("status");
            String pageStr = request.getParameter("page");
            String limitStr = request.getParameter("limit");

            int page = StringUtils.isNotBlank(pageStr) ? Integer.parseInt(pageStr) : 0;
            int limit = StringUtils.isNotBlank(limitStr) ? Integer.parseInt(limitStr) : 20;

            List<Map<String, Object>> tickets = fetchTickets(searchKeyword, statusFilter, page, limit);

            response.setContentType("application/json");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("tickets", tickets);
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", tickets.size());

            response.getWriter().write(gson.toJson(result));

        } catch (Exception e) {
            logger.error("Error fetching tickets", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to fetch tickets: " + e.getMessage());
            response.getWriter().write(gson.toJson(error));
        }
    }

    private List<Map<String, Object>> fetchTickets(String searchKeyword, String statusFilter, int page, int limit)
            throws RepositoryException {

        return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
            List<Map<String, Object>> tickets = new ArrayList<>();

            try {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    logger.error("Cannot adapt ResourceResolver to Session");
                    return Collections.emptyList();
                }

                // Build JCR SQL2 query to fetch all cq:Page nodes under tickets path
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([")
                    .append(TICKETS_PATH).append("])");

                if (StringUtils.isNotBlank(statusFilter)) {
                    queryBuilder.append(" AND [jcr:content/status] = '").append(statusFilter).append("'");
                }

                if (StringUtils.isNotBlank(searchKeyword)) {
                    String escapedKeyword = searchKeyword.replace("'", "''").toLowerCase();
                    queryBuilder.append(" AND (LOWER([jcr:content/jcr:title]) LIKE '%").append(escapedKeyword)
                        .append("%' OR LOWER([jcr:content/description]) LIKE '%").append(escapedKeyword).append("%')");
                }

                logger.debug("Executing query: {}", queryBuilder.toString());

                QueryManager qm = session.getWorkspace().getQueryManager();
                Query query = qm.createQuery(queryBuilder.toString(), Query.JCR_SQL2);
                QueryResult result = query.execute();
                NodeIterator iterator = result.getNodes();

                logger.debug("Query returned {} nodes", iterator.getSize());

                while (iterator.hasNext()) {
                    Node ticketNode = iterator.nextNode();

                    // Skip jcr:content nodes - we only want cq:Page nodes
                    if ("jcr:content".equals(ticketNode.getName())) {
                        continue;
                    }

                    try {
                        Node contentNode = ticketNode.getNode("jcr:content");

                        Map<String, Object> ticket = new HashMap<>();
                        ticket.put("id", ticketNode.getName());
                        ticket.put("title", contentNode.hasProperty("jcr:title") ?
                            contentNode.getProperty("jcr:title").getString() : "");
                        ticket.put("description", contentNode.hasProperty("description") ?
                            contentNode.getProperty("description").getString() : "");
                        ticket.put("priority", contentNode.hasProperty("priority") ?
                            contentNode.getProperty("priority").getString() : "");
                        ticket.put("status", contentNode.hasProperty("status") ?
                            contentNode.getProperty("status").getString() : "Open");
                        ticket.put("assignedTo", contentNode.hasProperty("assignedTo") ?
                            contentNode.getProperty("assignedTo").getString() : null);
                        tickets.add(ticket);
                    } catch (Exception e) {
                        logger.warn("Error processing ticket node: {}", ticketNode.getName(), e);
                    }
                }

                logger.debug("Fetched {} tickets", tickets.size());
            } catch (Exception e) {
                logger.error("Error executing query", e);
            }

            return tickets;
        });
    }

    private void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String ticketId = request.getParameter("id");

            if (ticketId == null || ticketId.isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Ticket ID required (use ?id=<ticket-id>)", null);
                return;
            }

            String requestBody = request.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);

            // Determine operation: status change or field update
            if (requestJson.has("newStatus")) {
                handleStatusChange(ticketId, requestJson, response);
            } else {
                handleFieldUpdate(ticketId, requestJson, response);
            }

        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in request body", e);
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "Invalid JSON in request body", null);
        } catch (Exception e) {
            logger.error("Unexpected error in TicketOperationServlet PUT", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Server error", null);
        }
    }

    private void handleFieldUpdate(String ticketId, JsonObject requestJson,
            SlingHttpServletResponse response) throws IOException {
        Map<String, Object> updates = new HashMap<>();

        if (requestJson.has("title")) {
            updates.put("title", requestJson.get("title").getAsString());
        }
        if (requestJson.has("description")) {
            updates.put("description", requestJson.get("description").getAsString());
        }
        if (requestJson.has("priority")) {
            updates.put("priority", requestJson.get("priority").getAsString());
        }
        if (requestJson.has("assignedTo")) {
            updates.put("assignedTo", requestJson.get("assignedTo").getAsString());
        }

        if (updates.isEmpty()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "No fields to update", null);
            return;
        }

        try {
            Map<String, Object> updatedTicket = ticketService.update(ticketId, updates);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            writeSuccessResponse(response, updatedTicket);
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating ticket: {}", e.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "Validation failed", null);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.debug("Ticket not found: {}", ticketId);
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                writeErrorResponse(response, "Ticket not found", null);
            } else {
                logger.error("Error updating ticket: {}", ticketId, e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to update ticket", null);
            }
        }
    }

    private void handleStatusChange(String ticketId, JsonObject requestJson,
            SlingHttpServletResponse response) throws IOException {
        String newStatus = requestJson.has("newStatus") ?
            requestJson.get("newStatus").getAsString() : null;

        if (newStatus == null || newStatus.isEmpty()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "Status is required in request body", null);
            return;
        }

        try {
            ticketService.changeStatus(ticketId, newStatus);
            Map<String, Object> updatedTicket = ticketService.read(ticketId);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            writeSuccessResponse(response, updatedTicket);
        } catch (IllegalStateException e) {
            logger.warn("Invalid status transition: {}", e.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_CONFLICT);
            Map<String, String> details = new HashMap<>();
            details.put("status", e.getMessage());
            writeErrorResponse(response, "Invalid status transition", details);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.debug("Ticket not found: {}", ticketId);
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                writeErrorResponse(response, "Ticket not found", null);
            } else {
                logger.error("Error changing status for ticket: {}", ticketId, e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to change status", null);
            }
        }
    }

    private void writeSuccessResponse(SlingHttpServletResponse response,
            Map<String, Object> ticket) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", true);
        jsonResponse.add("ticket", gson.toJsonTree(ticket));
        response.getWriter().write(gson.toJson(jsonResponse));
    }

    private void writeErrorResponse(SlingHttpServletResponse response, String message,
            Map<String, String> details) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", false);
        jsonResponse.addProperty("error", message);

        if (details != null && !details.isEmpty()) {
            jsonResponse.add("details", gson.toJsonTree(details));
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }

    @Override
    public javax.servlet.ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return "Ticket Servlet (GET/POST/PUT) - List, Create, Read, Update, Status Change";
    }

    @Override
    public void destroy() {
        // No-op
    }
}
