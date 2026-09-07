package com.example.aem.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.aem.core.services.CommentService;
import com.example.aem.core.services.SystemResourceResolverService;
import com.example.aem.core.services.TicketService;
import com.example.aem.core.testcontext.TestSystemResolverService;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Tests {@link CommentServiceImpl} against a JCR-backed mock repository.
 */
@ExtendWith(AemContextExtension.class)
class CommentServiceImplTest {

    private final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    private CommentService commentService;
    private TicketService ticketService;
    private String ticketId;

    @BeforeEach
    void setUp() {
        context.registerService(SystemResourceResolverService.class,
            new TestSystemResolverService(context).addUser("admin").withCurrentUser("admin"));
        context.registerInjectActivateService(new StateTransitionValidatorImpl());
        commentService = context.registerInjectActivateService(new CommentServiceImpl());
        ticketService = context.registerInjectActivateService(new TicketServiceImpl());

        ticketId = (String) ticketService.create("Login broken", "Cannot sign in", "HIGH", "admin").get("id");
    }

    // ===================== ADD =====================

    @Test
    void addCommentReturnsPersistedCommentData() {
        Map<String, Object> comment = commentService.addComment(ticketId, "Looking into it", "admin");

        assertNotNull(comment.get("id"));
        assertTrue(((String) comment.get("id")).startsWith("comment-"));
        assertEquals("Looking into it", comment.get("message"));
        assertEquals("admin", comment.get("createdBy"));
        assertNotNull(comment.get("createdAt"));
    }

    @Test
    void addCommentTrimsSurroundingWhitespace() {
        Map<String, Object> comment = commentService.addComment(ticketId, "   padded   ", "admin");

        assertEquals("padded", comment.get("message"));
    }

    @Test
    void addCommentTimestampUsesIso8601Format() {
        Map<String, Object> comment = commentService.addComment(ticketId, "Note", "admin");

        assertTrue(((String) comment.get("createdAt")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void addCommentGeneratesUniqueIds() {
        String first = (String) commentService.addComment(ticketId, "One", "admin").get("id");
        String second = (String) commentService.addComment(ticketId, "Two", "admin").get("id");

        assertTrue(!first.equals(second));
    }

    @Test
    void addCommentRejectsNullTicketId() {
        assertThrows(IllegalArgumentException.class,
            () -> commentService.addComment(null, "Message", "admin"));
    }

    @Test
    void addCommentRejectsEmptyTicketId() {
        assertThrows(IllegalArgumentException.class,
            () -> commentService.addComment("", "Message", "admin"));
    }

    @Test
    void addCommentRejectsNullMessage() {
        assertThrows(IllegalArgumentException.class,
            () -> commentService.addComment(ticketId, null, "admin"));
    }

    @Test
    void addCommentRejectsWhitespaceOnlyMessage() {
        assertThrows(IllegalArgumentException.class,
            () -> commentService.addComment(ticketId, "    ", "admin"));
    }

    @Test
    void addCommentFailsForUnknownTicket() {
        assertThrows(RuntimeException.class,
            () -> commentService.addComment("ticket-doesnotexist", "Message", "admin"));
    }

    // ===================== GET =====================

    @Test
    void getCommentsReturnsEmptyListWhenNoneAdded() {
        List<Map<String, Object>> comments = commentService.getComments(ticketId);

        assertNotNull(comments);
        assertTrue(comments.isEmpty());
    }

    @Test
    void getCommentsReturnsEmptyListForUnknownTicket() {
        List<Map<String, Object>> comments = commentService.getComments("ticket-doesnotexist");

        assertTrue(comments.isEmpty(), "An unknown ticket should yield no comments rather than an error");
    }

    @Test
    void getCommentsReturnsAllAddedComments() {
        commentService.addComment(ticketId, "First", "admin");
        commentService.addComment(ticketId, "Second", "admin");
        commentService.addComment(ticketId, "Third", "admin");

        assertEquals(3, commentService.getComments(ticketId).size());
    }

    @Test
    void getCommentsReturnsChronologicalOrderOldestFirst() {
        commentService.addComment(ticketId, "First", "admin");
        commentService.addComment(ticketId, "Second", "admin");
        commentService.addComment(ticketId, "Third", "admin");

        List<Map<String, Object>> comments = commentService.getComments(ticketId);

        assertEquals("First", comments.get(0).get("message"));
        assertEquals("Second", comments.get(1).get("message"));
        assertEquals("Third", comments.get(2).get("message"));
    }

    @Test
    void getCommentsPreservesAuthorPerComment() {
        commentService.addComment(ticketId, "From admin", "admin");
        commentService.addComment(ticketId, "From agent", "agent");

        List<Map<String, Object>> comments = commentService.getComments(ticketId);

        assertEquals("admin", comments.get(0).get("createdBy"));
        assertEquals("agent", comments.get(1).get("createdBy"));
    }

    @Test
    void getCommentsRejectsNullTicketId() {
        assertThrows(IllegalArgumentException.class, () -> commentService.getComments(null));
    }

    @Test
    void getCommentsRejectsEmptyTicketId() {
        assertThrows(IllegalArgumentException.class, () -> commentService.getComments(""));
    }

    @Test
    void commentsAreVisibleThroughTicketRead() {
        commentService.addComment(ticketId, "Visible via ticket", "admin");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comments =
            (List<Map<String, Object>>) ticketService.read(ticketId).get("comments");

        assertEquals(1, comments.size());
        assertEquals("Visible via ticket", comments.get(0).get("message"));
    }

    @Test
    void commentsPersistIndependentlyOfServiceInstance() {
        commentService.addComment(ticketId, "Durable", "admin");

        CommentService freshService = context.registerInjectActivateService(new CommentServiceImpl());

        assertEquals(1, freshService.getComments(ticketId).size());
    }

    @Test
    void commentsOnOneTicketDoNotLeakToAnother() {
        String otherTicketId =
            (String) ticketService.create("Other", "Other description", "LOW", "admin").get("id");
        commentService.addComment(ticketId, "Belongs to first", "admin");

        assertEquals(1, commentService.getComments(ticketId).size());
        assertTrue(commentService.getComments(otherTicketId).isEmpty());
    }
}
