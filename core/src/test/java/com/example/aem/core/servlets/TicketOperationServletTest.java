package com.example.aem.core.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;
import com.example.aem.core.services.impl.CommentServiceImpl;
import com.example.aem.core.services.impl.StateTransitionValidatorImpl;
import com.example.aem.core.services.impl.TicketServiceImpl;
import com.example.aem.core.testcontext.TestSystemResolverService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Drives {@link TicketOperationServlet} end-to-end through Sling Mocks: real servlet,
 * real service wiring, JCR-backed mock repository. Asserts on HTTP status codes and
 * response payloads, which is the contract the frontend depends on.
 */
@ExtendWith(AemContextExtension.class)
class TicketOperationServletTest {

    private static final Gson GSON = new Gson();

    private final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    private Servlet servlet;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        context.registerService(SystemResourceResolverService.class,
            new TestSystemResolverService(context).addUser("admin").addUser("agent").withCurrentUser("admin"));
        context.registerInjectActivateService(new StateTransitionValidatorImpl());
        context.registerInjectActivateService(new CommentServiceImpl());
        ticketService = context.registerInjectActivateService(new TicketServiceImpl());
        servlet = context.registerInjectActivateService(new TicketOperationServlet());
    }

    private String newTicketId() {
        return (String) ticketService.create("Login broken", "Cannot sign in", "HIGH", "admin").get("id");
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

    private MockSlingHttpServletResponse response() {
        return new MockSlingHttpServletResponse();
    }

    // ===================== GET: list =====================

    @Test
    void getWithoutIdReturnsListEnvelope() {
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request("GET"), response);

        assertEquals(200, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
        assertNotNull(json.get("tickets"));
    }

    @Test
    void getListEchoesPaginationParameters() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("page", "2", "limit", "5"));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(2, json.get("page").getAsInt());
        assertEquals(5, json.get("limit").getAsInt());
    }

    @Test
    void getListDefaultsPaginationWhenAbsent() {
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request("GET"), response);

        assertEquals(0, json.get("page").getAsInt());
        assertEquals(20, json.get("limit").getAsInt());
    }

    @Test
    void getListAcceptsSearchAndStatusFilterTogether() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("search", "login", "status", "Open"));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
    }

    @Test
    void getListToleratesSearchTermContainingQuote() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("search", "it's broken"));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus(), "A quote in the search term must not break the request");
        assertTrue(json.get("success").getAsBoolean());
    }

    // ===================== GET: detail =====================

    @Test
    void getWithIdReturnsTicketDetail() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", id));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
        JsonObject ticket = json.getAsJsonObject("ticket");
        assertEquals(id, ticket.get("id").getAsString());
        assertEquals("Login broken", ticket.get("title").getAsString());
        assertEquals("Open", ticket.get("status").getAsString());
    }

    @Test
    void getDetailExposesEveryFieldTheDetailViewRenders() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", id));

        JsonObject ticket = call(request, response()).getAsJsonObject("ticket");

        for (String field : new String[] { "id", "title", "description", "priority", "status",
            "assignedTo", "createdBy", "createdAt", "updatedAt", "comments" }) {
            assertTrue(ticket.has(field), "Detail response is missing field: " + field);
        }
    }

    @Test
    void getUnknownIdReturnsNotFound() {
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", "ticket-doesnotexist"));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(404, response.getStatus());
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("Ticket not found", json.get("error").getAsString());
    }

    // ===================== POST: create =====================

    @Test
    void postCreatesTicketAndReturns201() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"New issue\",\"description\":\"Details here\","
            + "\"priority\":\"MEDIUM\",\"assignedto\":\"admin\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(201, response.getStatus());
        assertTrue(json.get("success").getAsBoolean());
        JsonObject ticket = json.getAsJsonObject("ticket");
        assertEquals("New issue", ticket.get("title").getAsString());
        assertEquals("MEDIUM", ticket.get("priority").getAsString());
        assertEquals("Open", ticket.get("status").getAsString(), "New tickets must start Open");
    }

    @Test
    void postMissingTitleReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"description\":\"Details\",\"priority\":\"HIGH\",\"assignedto\":\"admin\"}")
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", json.get("error").getAsString());
        assertEquals("Title is required", json.getAsJsonObject("details").get("title").getAsString());
    }

    @Test
    void postBlankTitleIsRejected() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"   \",\"description\":\"Details\","
            + "\"priority\":\"HIGH\",\"assignedto\":\"admin\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        call(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    void postMissingDescriptionReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"Title\",\"priority\":\"HIGH\",\"assignedto\":\"admin\"}")
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Description is required",
            json.getAsJsonObject("details").get("description").getAsString());
    }

    @Test
    void postInvalidPriorityReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"Title\",\"description\":\"Details\","
            + "\"priority\":\"URGENT\",\"assignedto\":\"admin\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(json.getAsJsonObject("details").get("priority").getAsString().contains("HIGH"));
    }

    @Test
    void postMissingAssigneeReturnsFieldLevelError() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"Title\",\"description\":\"Details\",\"priority\":\"HIGH\"}")
            .getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Assignee is required",
            json.getAsJsonObject("details").get("assignedto").getAsString());
    }

    @Test
    void postUnknownAssigneeIsRejectedWithHelpfulMessage() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent(("{\"title\":\"Title\",\"description\":\"Details\","
            + "\"priority\":\"HIGH\",\"assignedto\":\"ghost\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertFalse(json.get("success").getAsBoolean());
        assertTrue(json.get("error").getAsString().contains("ghost"),
            "Error should name the rejected user, was: " + json.get("error").getAsString());
    }

    @Test
    void postMalformedJsonReturnsBadRequest() {
        MockSlingHttpServletRequest request = request("POST");
        request.setContent("{not valid json".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("Invalid JSON in request body", json.get("error").getAsString());
    }

    // ===================== PUT: field update =====================

    @Test
    void putUpdatesFieldsAndReturnsUpdatedTicket() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"title\":\"Renamed\",\"priority\":\"LOW\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        JsonObject ticket = json.getAsJsonObject("ticket");
        assertEquals("Renamed", ticket.get("title").getAsString());
        assertEquals("LOW", ticket.get("priority").getAsString());
    }

    @Test
    void putWithoutIdReturnsBadRequest() {
        MockSlingHttpServletRequest request = request("PUT");
        request.setContent("{\"title\":\"Renamed\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(json.get("error").getAsString().contains("Ticket ID required"));
    }

    @Test
    void putWithNoRecognisedFieldsReturnsBadRequest() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"unknownField\":\"x\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("No fields to update", json.get("error").getAsString());
    }

    @Test
    void putOnUnknownTicketReturnsNotFound() {
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", "ticket-doesnotexist"));
        request.setContent("{\"title\":\"Renamed\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(404, response.getStatus());
        assertEquals("Ticket not found", json.get("error").getAsString());
    }

    @Test
    void putWithInvalidPriorityReturnsBadRequest() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"priority\":\"URGENT\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        call(request, response);

        assertEquals(400, response.getStatus());
    }

    // ===================== PUT: status change =====================

    @Test
    void putNewStatusPerformsValidTransition() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"newStatus\":\"In Progress\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("In Progress", json.getAsJsonObject("ticket").get("status").getAsString());
    }

    @Test
    void putInvalidTransitionReturns409WithAllowedNextStates() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"newStatus\":\"Resolved\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(409, response.getStatus(),
            "An invalid transition must be a 409 Conflict, not a 500");
        assertEquals("Invalid status transition", json.get("error").getAsString());

        String allowed = json.getAsJsonObject("details").get("status").getAsString();
        assertTrue(allowed.contains("In Progress"), "Allowed states must be listed, was: " + allowed);
        assertTrue(allowed.contains("Cancelled"), "Allowed states must be listed, was: " + allowed);
    }

    @Test
    void putTransitionOutOfTerminalStateReturns409() {
        String id = newTicketId();
        ticketService.changeStatus(id, "Cancelled");

        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"newStatus\":\"In Progress\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        call(request, response);

        assertEquals(409, response.getStatus());
    }

    @Test
    void putBlankStatusReturnsBadRequest() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", id));
        request.setContent("{\"newStatus\":\"\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(json.get("error").getAsString().contains("Status is required"));
    }

    @Test
    void putStatusChangeOnUnknownTicketReturnsNotFound() {
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Map.of("id", "ticket-doesnotexist"));
        request.setContent("{\"newStatus\":\"In Progress\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(404, response.getStatus());
        assertEquals("Ticket not found", json.get("error").getAsString());
    }

    // ===================== method routing =====================

    @Test
    void unsupportedMethodIsRejected() throws Exception {
        MockSlingHttpServletRequest request = request("DELETE");
        MockSlingHttpServletResponse response = response();

        servlet.service(request, response);

        assertEquals(405, response.getStatus());
    }

    @Test
    void responsesDeclareJsonContentType() {
        String id = newTicketId();
        MockSlingHttpServletRequest request = request("GET");
        request.setParameterMap(Map.of("id", id));
        MockSlingHttpServletResponse response = response();

        call(request, response);

        assertTrue(response.getContentType().contains("application/json"),
            "Content type was: " + response.getContentType());
    }

    @Test
    void servletDescribesItself() {
        assertNotNull(servlet.getServletInfo());
    }

    // ===================== end-to-end lifecycle =====================

    @Test
    void fullLifecycleFromCreationThroughClosureOverHttp() {
        // Create
        MockSlingHttpServletRequest create = request("POST");
        create.setContent(("{\"title\":\"Lifecycle\",\"description\":\"End to end\","
            + "\"priority\":\"HIGH\",\"assignedto\":\"admin\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse createResponse = response();
        String id = call(create, createResponse).getAsJsonObject("ticket").get("id").getAsString();
        assertEquals(201, createResponse.getStatus());

        // Advance through every valid transition
        for (String status : new String[] { "In Progress", "Resolved", "Closed" }) {
            MockSlingHttpServletRequest transition = request("PUT");
            transition.setParameterMap(Map.of("id", id));
            transition.setContent(("{\"newStatus\":\"" + status + "\"}").getBytes(StandardCharsets.UTF_8));
            MockSlingHttpServletResponse transitionResponse = response();

            JsonObject json = call(transition, transitionResponse);

            assertEquals(200, transitionResponse.getStatus(), "Transition to " + status + " should succeed");
            assertEquals(status, json.getAsJsonObject("ticket").get("status").getAsString());
        }

        // Closed is terminal
        MockSlingHttpServletRequest reopen = request("PUT");
        reopen.setParameterMap(Map.of("id", id));
        reopen.setContent("{\"newStatus\":\"Open\"}".getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse reopenResponse = response();

        call(reopen, reopenResponse);

        assertEquals(409, reopenResponse.getStatus(), "Closed must be terminal");
    }

    @Test
    void everyInvalidTransitionIsRejectedOverHttp() {
        Map<String, String[]> invalidTransitions = Map.of(
            "Open", new String[] { "Resolved", "Closed" },
            "In Progress", new String[] { "Open", "Closed" },
            "Resolved", new String[] { "Open", "Cancelled" });

        for (Map.Entry<String, String[]> entry : invalidTransitions.entrySet()) {
            for (String target : entry.getValue()) {
                String id = newTicketId();
                // Walk the ticket to the starting state under test.
                if ("In Progress".equals(entry.getKey())) {
                    ticketService.changeStatus(id, "In Progress");
                } else if ("Resolved".equals(entry.getKey())) {
                    ticketService.changeStatus(id, "In Progress");
                    ticketService.changeStatus(id, "Resolved");
                }

                MockSlingHttpServletRequest request = request("PUT");
                request.setParameterMap(Map.of("id", id));
                request.setContent(("{\"newStatus\":\"" + target + "\"}").getBytes(StandardCharsets.UTF_8));
                MockSlingHttpServletResponse response = response();

                call(request, response);

                assertEquals(409, response.getStatus(),
                    entry.getKey() + " -> " + target + " must be rejected with 409");
            }
        }
    }

    @Test
    void everyValidTransitionSucceedsOverHttp() {
        // Open -> Cancelled
        String cancelled = newTicketId();
        assertTransitionSucceeds(cancelled, "Cancelled");

        // Open -> In Progress -> Cancelled
        String inProgressCancelled = newTicketId();
        assertTransitionSucceeds(inProgressCancelled, "In Progress");
        assertTransitionSucceeds(inProgressCancelled, "Cancelled");

        // Open -> In Progress -> Resolved -> In Progress (reopen)
        String reopened = newTicketId();
        assertTransitionSucceeds(reopened, "In Progress");
        assertTransitionSucceeds(reopened, "Resolved");
        assertTransitionSucceeds(reopened, "In Progress");

        // Open -> In Progress -> Resolved -> Closed
        String closed = newTicketId();
        assertTransitionSucceeds(closed, "In Progress");
        assertTransitionSucceeds(closed, "Resolved");
        assertTransitionSucceeds(closed, "Closed");
    }

    private void assertTransitionSucceeds(String ticketId, String newStatus) {
        MockSlingHttpServletRequest request = request("PUT");
        request.setParameterMap(Collections.singletonMap("id", ticketId));
        request.setContent(("{\"newStatus\":\"" + newStatus + "\"}").getBytes(StandardCharsets.UTF_8));
        MockSlingHttpServletResponse response = response();

        JsonObject json = call(request, response);

        assertEquals(200, response.getStatus(), "Transition to " + newStatus + " should succeed");
        assertEquals(newStatus, json.getAsJsonObject("ticket").get("status").getAsString());
    }
}
