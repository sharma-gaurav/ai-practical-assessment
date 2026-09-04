package com.example.aem.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

import com.example.aem.core.services.CommentService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=GET,POST",
        "sling.servlet.paths=/bin/api/tickets/comments",
        "service.ranking=2000"
    }
)
public class CommentServlet implements Servlet {

    private static final Logger logger = LoggerFactory.getLogger(CommentServlet.class);
    private static final Gson gson = new Gson();

    @Reference
    private CommentService commentService;

    @Override
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException {
        // No-op
    }

    @Override
    public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
            throws javax.servlet.ServletException, IOException {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        if ("GET".equals(slingRequest.getMethod())) {
            doGet(slingRequest, slingResponse);
        } else if ("POST".equals(slingRequest.getMethod())) {
            doPost(slingRequest, slingResponse);
        } else {
            slingResponse.sendError(SlingHttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // Extract ticket ID from query parameter or request path
            String ticketId = request.getParameter("id");

            if (StringUtils.isBlank(ticketId)) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Ticket ID is required", null);
                return;
            }

            try {
                List<Map<String, Object>> comments = commentService.getComments(ticketId);
                response.setStatus(SlingHttpServletResponse.SC_OK);
                writeSuccessResponse(response, comments);
            } catch (Exception e) {
                logger.error("Error retrieving comments for ticket: {}", ticketId, e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to retrieve comments", null);
            }
        } catch (Exception e) {
            logger.error("Unexpected error in CommentServlet GET", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Server error", null);
        }
    }

    private void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            // Read and parse JSON from request body
            String requestBody = request.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

            JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);

            String ticketId = requestJson.has("id") ? requestJson.get("id").getAsString() : null;
            String message = requestJson.has("message") ? requestJson.get("message").getAsString() : null;

            // Validate inputs
            Map<String, String> errors = new HashMap<>();
            if (StringUtils.isBlank(ticketId)) {
                errors.put("id", "Ticket ID is required");
            }
            if (StringUtils.isBlank(message)) {
                errors.put("message", "Comment message is required");
            }

            if (!errors.isEmpty()) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, "Validation failed", errors);
                return;
            }

            try {
                String createdBy = request.getResourceResolver().getUserID();
                Map<String, Object> comment = commentService.addComment(ticketId, message.trim(), createdBy);
                response.setStatus(SlingHttpServletResponse.SC_CREATED);
                response.setContentType("application/json");
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.addProperty("success", true);
                jsonResponse.add("comment", gson.toJsonTree(comment));
                response.getWriter().write(gson.toJson(jsonResponse));
            } catch (IllegalArgumentException e) {
                logger.warn("Validation error: {}", e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                writeErrorResponse(response, e.getMessage(), null);
            } catch (Exception e) {
                logger.error("Error adding comment to ticket: {}", ticketId, e);
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeErrorResponse(response, "Failed to add comment", null);
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in request body", e);
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            writeErrorResponse(response, "Invalid JSON in request body", null);
        } catch (Exception e) {
            logger.error("Unexpected error in CommentServlet POST", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorResponse(response, "Server error", null);
        }
    }

    private void writeSuccessResponse(SlingHttpServletResponse response, List<Map<String, Object>> comments) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", true);
        jsonResponse.add("comments", gson.toJsonTree(comments));
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
        return "Comment Servlet";
    }

    @Override
    public void destroy() {
        // No-op
    }
}
