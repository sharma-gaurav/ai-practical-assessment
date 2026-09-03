package com.example.aem.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.exceptions.InvalidUserException;
import com.example.aem.core.services.TicketService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=POST",
        "sling.servlet.paths=/bin/api/tickets",
        "service.ranking=1000"
    }
)
public class TicketCreateServlet implements Servlet {

    private static final Logger logger = LoggerFactory.getLogger(TicketCreateServlet.class);
    private static final Gson gson = new Gson();

    @Reference
    private TicketService ticketService;

    @Override
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException {
        // No-op
    }

    @Override
    public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
            throws javax.servlet.ServletException, IOException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        if ("POST".equals(slingRequest.getMethod())) {
            doPost(slingRequest, slingResponse);
        } else {
            slingResponse.sendError(SlingHttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // 1. Read and parse JSON from request body
            String requestBody = request.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);

            // 2. Extract parameters from JSON
            String title = requestJson.has("title") ? requestJson.get("title").getAsString() : null;
            String description = requestJson.has("description") ? requestJson.get("description").getAsString() : null;
            String priority = requestJson.has("priority") ? requestJson.get("priority").getAsString() : null;
            String assignedTo = requestJson.has("assignedto") ? requestJson.get("assignedto").getAsString() : null;

            // 3. Validate input
            Map<String, String> errors = validateInput(title, description, priority, assignedTo);
            if (!errors.isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", errors);
                return;
            }

            // 4. Call service to create ticket
            try {
                Map<String, Object> ticket = ticketService.create(title.trim(), description.trim(), priority, assignedTo);
                response.setStatus(SlingHttpServletResponse.SC_CREATED);
                writeSuccessResponse(response, ticket);
            } catch (InvalidUserException e) {
                logger.warn("Invalid user provided: {}", assignedTo);
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                Map<String, String> fieldErrors = new HashMap<>();
                fieldErrors.put("assignedto", "User does not exist");
                writeErrorResponse(response, "Validation failed", fieldErrors);
            } catch (IllegalArgumentException e) {
                logger.warn("Validation error: {}", e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                Map<String, String> fieldErrors = new HashMap<>();
                fieldErrors.put("general", e.getMessage());
                writeErrorResponse(response, "Validation failed", fieldErrors);
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
            logger.error("Error processing request", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Failed to process request", null);
        }
    }

    private Map<String, String> validateInput(String title, String description, String priority, String assignedTo) {
        Map<String, String> errors = new HashMap<>();

        if (StringUtils.isBlank(title)) {
            errors.put("title", "Title is required");
        } else if (title.length() > 255) {
            errors.put("title", "Title must not exceed 255 characters");
        }

        if (StringUtils.isBlank(description)) {
            errors.put("description", "Description is required");
        } else if (description.length() > 5000) {
            errors.put("description", "Description must not exceed 5000 characters");
        }

        if (StringUtils.isBlank(priority)) {
            errors.put("priority", "Priority is required");
        } else if (!isValidPriority(priority)) {
            errors.put("priority", "Priority must be one of: HIGH, MEDIUM, LOW");
        }

        if (StringUtils.isBlank(assignedTo)) {
            errors.put("assignedto", "Assignee is required");
        }

        return errors;
    }

    private boolean isValidPriority(String priority) {
        return "HIGH".equals(priority) || "MEDIUM".equals(priority) || "LOW".equals(priority);
    }

    private void writeSuccessResponse(SlingHttpServletResponse response, Map<String, Object> ticket) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", true);
        jsonResponse.add("ticket", gson.toJsonTree(ticket));
        response.getWriter().write(gson.toJson(jsonResponse));
    }

    private void writeErrorResponse(SlingHttpServletResponse response, String message, Map<String, String> details) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", false);
        jsonResponse.addProperty("error", message);

        if (details != null && !details.isEmpty()) {
            jsonResponse.add("details", gson.toJsonTree(
                details.entrySet().stream()
                    .map(e -> {
                        JsonObject detail = new JsonObject();
                        detail.addProperty("field", e.getKey());
                        detail.addProperty("message", e.getValue());
                        return detail;
                    })
                    .toArray()
            ));
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }

    @Override
    public javax.servlet.ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return "Ticket Creation Servlet";
    }

    @Override
    public void destroy() {
        // No-op
    }
}
