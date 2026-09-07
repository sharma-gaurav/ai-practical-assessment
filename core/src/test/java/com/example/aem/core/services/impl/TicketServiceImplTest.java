package com.example.aem.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.aem.core.exceptions.InvalidUserException;
import com.example.aem.core.services.CommentService;
import com.example.aem.core.services.StateTransitionValidator;
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;
import com.example.aem.core.testcontext.TestSystemResolverService;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Tests {@link TicketServiceImpl} against a JCR-backed mock repository, exercising the
 * real persistence path rather than mocking the service under test.
 */
@ExtendWith(AemContextExtension.class)
class TicketServiceImplTest {

    private final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    private TicketService ticketService;
    private TestSystemResolverService resolverService;

    @BeforeEach
    void setUp() {
        resolverService = new TestSystemResolverService(context)
            .addUser("admin")
            .addUser("agent")
            .withCurrentUser("admin");

        context.registerService(SystemResourceResolverService.class, resolverService);
        context.registerInjectActivateService(new StateTransitionValidatorImpl());
        context.registerInjectActivateService(new CommentServiceImpl());
        ticketService = context.registerInjectActivateService(new TicketServiceImpl());
    }

    private Map<String, Object> createTicket() {
        return ticketService.create("Login broken", "Users cannot sign in", "HIGH", "admin");
    }

    // ===================== CREATE =====================

    @Test
    void createReturnsTicketWithGeneratedIdAndOpenStatus() {
        Map<String, Object> ticket = createTicket();

        assertNotNull(ticket.get("id"));
        assertTrue(((String) ticket.get("id")).startsWith("ticket-"));
        assertEquals("Login broken", ticket.get("title"));
        assertEquals("Users cannot sign in", ticket.get("description"));
        assertEquals("HIGH", ticket.get("priority"));
        assertEquals("Open", ticket.get("status"));
        assertEquals("admin", ticket.get("assignedTo"));
        assertEquals("admin", ticket.get("createdBy"));
        assertNotNull(ticket.get("createdAt"));
        assertNotNull(ticket.get("updatedAt"));
    }

    @Test
    void createPersistsTicketSoItCanBeReadBack() {
        String id = (String) createTicket().get("id");

        Map<String, Object> read = ticketService.read(id);

        assertEquals(id, read.get("id"));
        assertEquals("Login broken", read.get("title"));
        assertEquals("Open", read.get("status"));
    }

    @Test
    void createGeneratesUniqueIdsForEachTicket() {
        String first = (String) createTicket().get("id");
        String second = (String) createTicket().get("id");

        assertTrue(!first.equals(second), "Ticket ids must be unique");
    }

    @Test
    void createTimestampsUseIso8601Format() {
        Map<String, Object> ticket = createTicket();

        assertTrue(((String) ticket.get("createdAt")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
            "createdAt should be ISO-8601 UTC, was: " + ticket.get("createdAt"));
    }

    @Test
    void createRejectsUnknownAssignee() {
        InvalidUserException e = assertThrows(InvalidUserException.class,
            () -> ticketService.create("Title", "Description", "HIGH", "ghost"));

        assertTrue(e.getMessage().contains("ghost"));
    }

    @Test
    void createRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
            () -> ticketService.create("   ", "Description", "HIGH", "admin"));
    }

    @Test
    void createRejectsTitleOverMaxLength() {
        String tooLong = "x".repeat(256);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.create(tooLong, "Description", "HIGH", "admin"));

        assertTrue(e.getMessage().contains("255"));
    }

    @Test
    void createAcceptsTitleAtMaxLength() {
        Map<String, Object> ticket =
            ticketService.create("x".repeat(255), "Description", "HIGH", "admin");

        assertEquals(255, ((String) ticket.get("title")).length());
    }

    @Test
    void createRejectsDescriptionOverMaxLength() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.create("Title", "x".repeat(5001), "HIGH", "admin"));

        assertTrue(e.getMessage().contains("5000"));
    }

    @Test
    void createRejectsBlankPriority() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.create("Title", "Description", "  ", "admin"));

        assertTrue(e.getMessage().contains("Priority is required"));
    }

    @Test
    void createRejectsUnrecognisedPriority() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.create("Title", "Description", "URGENT", "admin"));

        assertTrue(e.getMessage().contains("HIGH"));
    }

    @Test
    void createAcceptsAllValidPriorities() {
        for (String priority : new String[] { "HIGH", "MEDIUM", "LOW" }) {
            Map<String, Object> ticket =
                ticketService.create("Title", "Description", priority, "admin");
            assertEquals(priority, ticket.get("priority"));
        }
    }

    // ===================== READ =====================

    @Test
    void readRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.read("  "));
    }

    @Test
    void readReportsNotFoundForUnknownId() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> ticketService.read("ticket-doesnotexist"));

        assertTrue(e.getMessage().contains("not found"),
            "Message must contain 'not found' so the servlet maps it to 404, was: " + e.getMessage());
    }

    @Test
    void readIncludesCommentsCollection() {
        String id = (String) createTicket().get("id");

        Map<String, Object> read = ticketService.read(id);

        assertNotNull(read.get("comments"));
    }

    // ===================== UPDATE =====================

    @Test
    void updateAppliesTitleChange() {
        String id = (String) createTicket().get("id");

        Map<String, Object> updated = ticketService.update(id, Map.of("title", "Login fixed"));

        assertEquals("Login fixed", updated.get("title"));
        assertEquals("Login fixed", ticketService.read(id).get("title"));
    }

    @Test
    void updateAppliesMultipleFieldsAtOnce() {
        String id = (String) createTicket().get("id");

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", "New title");
        updates.put("description", "New description");
        updates.put("priority", "LOW");
        updates.put("assignedTo", "agent");

        Map<String, Object> updated = ticketService.update(id, updates);

        assertEquals("New title", updated.get("title"));
        assertEquals("New description", updated.get("description"));
        assertEquals("LOW", updated.get("priority"));
        assertEquals("agent", updated.get("assignedTo"));
    }

    @Test
    void updateLeavesUnspecifiedFieldsUntouched() {
        String id = (String) createTicket().get("id");

        ticketService.update(id, Map.of("priority", "LOW"));

        Map<String, Object> read = ticketService.read(id);
        assertEquals("Login broken", read.get("title"));
        assertEquals("Users cannot sign in", read.get("description"));
    }

    @Test
    void updateDoesNotChangeStatus() {
        String id = (String) createTicket().get("id");

        ticketService.update(id, Map.of("title", "Renamed"));

        assertEquals("Open", ticketService.read(id).get("status"));
    }

    @Test
    void updateRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> ticketService.update("  ", Map.of("title", "x")));
    }

    @Test
    void updateRejectsEmptyUpdateMap() {
        String id = (String) createTicket().get("id");

        assertThrows(IllegalArgumentException.class, () -> ticketService.update(id, Map.of()));
    }

    @Test
    void updateRejectsNullUpdateMap() {
        String id = (String) createTicket().get("id");

        assertThrows(IllegalArgumentException.class, () -> ticketService.update(id, null));
    }

    @Test
    void updateRejectsBlankTitle() {
        String id = (String) createTicket().get("id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.update(id, Map.of("title", " ")));

        assertTrue(e.getMessage().contains("Title is required"));
    }

    @Test
    void updateRejectsInvalidPriority() {
        String id = (String) createTicket().get("id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.update(id, Map.of("priority", "URGENT")));

        assertTrue(e.getMessage().contains("HIGH"));
    }

    @Test
    void updateRejectsUnknownAssignee() {
        String id = (String) createTicket().get("id");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.update(id, Map.of("assignedTo", "ghost")));

        assertTrue(e.getMessage().contains("ghost"));
    }

    @Test
    void updateReportsAllValidationErrorsTogether() {
        String id = (String) createTicket().get("id");

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", "");
        updates.put("priority", "NOPE");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ticketService.update(id, updates));

        assertTrue(e.getMessage().contains("title"));
        assertTrue(e.getMessage().contains("priority"));
    }

    @Test
    void updateReportsNotFoundForUnknownId() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> ticketService.update("ticket-doesnotexist", Map.of("title", "x")));

        assertTrue(e.getMessage().contains("not found"));
    }

    // ===================== STATUS CHANGE =====================

    @Test
    void changeStatusAdvancesOpenToInProgress() {
        String id = (String) createTicket().get("id");

        ticketService.changeStatus(id, "In Progress");

        assertEquals("In Progress", ticketService.read(id).get("status"));
    }

    @Test
    void changeStatusWalksTheFullHappyPath() {
        String id = (String) createTicket().get("id");

        ticketService.changeStatus(id, "In Progress");
        ticketService.changeStatus(id, "Resolved");
        ticketService.changeStatus(id, "Closed");

        assertEquals("Closed", ticketService.read(id).get("status"));
    }

    @Test
    void changeStatusSupportsReopeningFromResolved() {
        String id = (String) createTicket().get("id");
        ticketService.changeStatus(id, "In Progress");
        ticketService.changeStatus(id, "Resolved");

        ticketService.changeStatus(id, "In Progress");

        assertEquals("In Progress", ticketService.read(id).get("status"));
    }

    @Test
    void changeStatusRejectsSkippingInProgress() {
        String id = (String) createTicket().get("id");

        assertThrows(IllegalStateException.class, () -> ticketService.changeStatus(id, "Resolved"));
        assertEquals("Open", ticketService.read(id).get("status"), "status must be unchanged");
    }

    @Test
    void changeStatusRejectsTransitionOutOfClosedTerminalState() {
        String id = (String) createTicket().get("id");
        ticketService.changeStatus(id, "In Progress");
        ticketService.changeStatus(id, "Resolved");
        ticketService.changeStatus(id, "Closed");

        assertThrows(IllegalStateException.class, () -> ticketService.changeStatus(id, "Open"));
    }

    @Test
    void changeStatusRejectsTransitionOutOfCancelledTerminalState() {
        String id = (String) createTicket().get("id");
        ticketService.changeStatus(id, "Cancelled");

        assertThrows(IllegalStateException.class, () -> ticketService.changeStatus(id, "In Progress"));
    }

    @Test
    void changeStatusErrorNamesTheAllowedNextStates() {
        String id = (String) createTicket().get("id");

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> ticketService.changeStatus(id, "Closed"));

        assertTrue(e.getMessage().contains("In Progress"));
        assertTrue(e.getMessage().contains("Cancelled"));
    }

    @Test
    void changeStatusRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.changeStatus(" ", "Open"));
    }

    @Test
    void changeStatusRejectsBlankStatus() {
        String id = (String) createTicket().get("id");

        assertThrows(IllegalArgumentException.class, () -> ticketService.changeStatus(id, " "));
    }

    @Test
    void changeStatusReportsNotFoundForUnknownId() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> ticketService.changeStatus("ticket-doesnotexist", "In Progress"));

        assertTrue(e.getMessage().contains("not found"));
    }

    // ===================== PERSISTENCE =====================

    @Test
    void ticketsSurviveResolverLifecycleAndRemainReadable() {
        // Each service call acquires and releases its own resolver via the resolver service,
        // so reading through a fresh service instance proves the data lives in the repository
        // rather than in service-local state.
        String id = (String) createTicket().get("id");
        ticketService.changeStatus(id, "In Progress");

        TicketService freshService = context.registerInjectActivateService(new TicketServiceImpl());
        Map<String, Object> read = freshService.read(id);

        assertEquals(id, read.get("id"));
        assertEquals("Login broken", read.get("title"));
        assertEquals("In Progress", read.get("status"));
    }

    @Test
    void allServiceDependenciesAreWired() {
        assertNotNull(context.getService(TicketService.class));
        assertNotNull(context.getService(CommentService.class));
        assertNotNull(context.getService(StateTransitionValidator.class));
        assertNotNull(context.getService(SystemResourceResolverService.class));
    }
}
