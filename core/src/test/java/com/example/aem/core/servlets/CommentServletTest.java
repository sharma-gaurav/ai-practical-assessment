package com.example.aem.core.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.aem.core.services.CommentService;
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;
import com.example.aem.core.services.impl.CommentServiceImpl;
import com.example.aem.core.services.impl.StateTransitionValidatorImpl;
import com.example.aem.core.services.impl.TicketServiceImpl;
import com.example.aem.core.testcontext.TestSystemResolverService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Drives {@link CommentServlet} end-to-end through Sling Mocks against a JCR-backed
 * mock repository.
 */
@ExtendWith(AemContextExtension.class)
class CommentServletTest {

    private static final Gson GSON = new Gson();

    private final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    private Servlet servlet;
    private CommentService commentService;
    private String ticketId;

    @BeforeEach
    void setUp() {
        context.registerService(SystemResourceResolverService.class,
            new TestSystemResolverService(context).addUser("admin").withCurrentUser("admin"));
        context.registerInjectActivateService(new StateTransitionValidatorImpl());
        commentService = context.registerInjectActivateService(new CommentServiceImpl());
        TicketService ticketService = context.registerInjectActivateService(new TicketServiceImpl());
        servlet = context.registerInjectActivateService(new CommentServlet());

        ticketId = (String) ticketService.create("Login broken", "Cannot sign in", "HIGH", "admin").get("id");
    }

    private MockSlingHttpServletRequest request(String method) {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setMethod(method);
        return request;
    }

    private JsonObject call(MockSlingHttpServletRequest request, MockSlingHttpServletResponse response) {
        try {
            servlet.service(request, response);
        } catch (Exception e) {
            throw new AssertionError("Servlet threw instead of writing an error response", e);
        }
        String body = response.getOutputAsString();
        assertNotNull(body);
        assertFalse(body.isEmpty(), "Servlet must always write a response body");
        return GSON.fromJson(body, JsonObject.class);
    }

    // ===================== GET =====================

    @Test
    void getReturnsEmptyArrayWhenTicketHasNoComments() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", ticketId));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(0, json.getAsJsonArray("comments").size());
    }

    @Test
    void getReturnsCommentsInChronologicalOrder() {
        commentService.addComment(ticketId, "First", "admin");
        commentService.addComment(ticketId, "Second", "admin");

        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", ticketId));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonArray comments = call(request, response).getAsJsonArray("comments");

        assertEquals(2, comments.size());
        assertEquals("First", comments.get(0).getAsJsonObject().get("message").getAsString());
        assertEquals("Second", comments.get(1).getAsJsonObject().get("message").getAsString());
    }

    @Test
    void getExposesAuthorAndTimestampPerComment() {
        commentService.addComment(ticketId, "Note", "admin");

        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", ticketId));

        JsonObject comment = call(request, new MockSlingHttpServletResponse())
            .getAsJsonArray("comments").get(0).getAsJsonObject();

        assertEquals("admin", comment.get("createdBy").getAsString());
        assertNotNull(comment.get("createdAt"));
        assertNotNull(comment.get("id"));
    }

    @Test
    void getWithoutIdReturnsBadRequest() {
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request("GET"), response);

        assertEquals(400, response.getStatus());
        assertEquals("Ticket ID is required", json.get("error").getAsString());
    }

    @Test
    void getWithBlankIdReturnsBadRequest() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", "   "));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        call(request, response);

        assertEquals(400, response.getStatus());
    }

    // ===================== POST =====================

    @Test
    void postAddsCommentAndReturns201() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"id\":\"" + ticketId + "\",\"message\":\"Looking into it\"}")
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(201, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
        JsonObject comment = json.getAsJsonObject("comment");
        assertEquals("Looking into it", comment.get("message").getAsString());
        assertNotNull(comment.get("id"));
        assertNotNull(comment.get("createdAt"));
    }

    @Test
    void postedCommentIsPersistedAndReadableBack() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"id\":\"" + ticketId + "\",\"message\":\"Durable\"}")
            .getBytes(StandardCharsets.UTF_8));

        call(request, new MockSlingHttpServletResponse());

        assertEquals(1, commentService.getComments(ticketId).size());
        assertEquals("Durable", commentService.getComments(ticketId).get(0).get("message"));
    }

    @Test
    void postRecordsAuthorFromRequestUser() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"id\":\"" + ticketId + "\",\"message\":\"Attributed\"}")
            .getBytes(StandardCharsets.UTF_8));

        JsonObject comment = call(request, new MockSlingHttpServletResponse()).getAsJsonObject("comment");

        assertNotNull(comment.get("createdBy"), "Comment must record who created it");
    }

    @Test
    void postMissingIdReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent("{\"message\":\"Orphan comment\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", json.get("error").getAsString());
        assertTrue(json.getAsJsonArray("details").toString().contains("Ticket ID is required"));
    }

    @Test
    void postMissingMessageReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"id\":\"" + ticketId + "\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(json.getAsJsonArray("details").toString().contains("Comment message is required"));
    }

    @Test
    void postWhitespaceOnlyMessageIsRejected() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"id\":\"" + ticketId + "\",\"message\":\"    \"}")
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        call(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    void postReportsBothMissingFieldsAtOnce() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        String details = json.getAsJsonArray("details").toString();
        assertTrue(details.contains("Ticket ID is required"));
        assertTrue(details.contains("Comment message is required"));
    }

    @Test
    void postMalformedJsonReturnsBadRequest() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent("{not json".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Invalid JSON in request body", json.get("error").getAsString());
    }

    @Test
    void postToUnknownTicketReportsFailure() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent("{\"id\":\"ticket-doesnotexist\",\"message\":\"Hello\"}"
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        JsonObject json = call(request, response);

        assertFalse(json.get("success").getAsBoolean());
        assertTrue(response.getStatus() >= 400);
    }

    // ===================== routing =====================

    @Test
    void unsupportedMethodIsRejected() throws Exception {
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        servlet.service(request("DELETE"), response);

        assertEquals(405, response.getStatus());
    }

    @Test
    void responsesDeclareJsonContentType() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", ticketId));
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        call(request, response);

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void servletDescribesItself() {
        assertNotNull(servlet.getServletInfo());
    }
}
