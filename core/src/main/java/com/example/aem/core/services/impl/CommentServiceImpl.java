package com.example.aem.core.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.services.CommentService;
import com.example.aem.core.services.SystemResourceResolverService;

@Component(service = CommentService.class)
public class CommentServiceImpl implements CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final String TICKETS_ROOT_PATH = "/content/ai-practical-assessment/tickets";
    private static final String SERVICE_USER = "ai-practical-assessment-ticketservice";
    private static final String COMMENTS_FOLDER = "comments";

    @Reference
    private SystemResourceResolverService systemResolverService;

    @Override
    public Map<String, Object> addComment(String ticketId, String message, String createdBy) {
        logger.debug("Adding comment to ticket: {}", ticketId);

        if (ticketId == null || ticketId.isEmpty()) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment message is required");
        }

        return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new RuntimeException("Cannot adapt ResourceResolver to Session");
            }

            String ticketPath = TICKETS_ROOT_PATH + "/" + ticketId;
            Node ticketNode = session.getNode(ticketPath);
            Node jcrContentNode = ticketNode.getNode("jcr:content");

            // Get or create comments folder
            Node commentsNode;
            if (jcrContentNode.hasNode(COMMENTS_FOLDER)) {
                commentsNode = jcrContentNode.getNode(COMMENTS_FOLDER);
            } else {
                commentsNode = jcrContentNode.addNode(COMMENTS_FOLDER, "nt:unstructured");
            }

            // Create comment node
            String commentId = "comment-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Node commentNode = commentsNode.addNode(commentId, "nt:unstructured");

            // Set comment properties
            Calendar now = Calendar.getInstance();
            commentNode.setProperty("message", message.trim());
            commentNode.setProperty("createdBy", createdBy);
            commentNode.setProperty("createdAt", now);

            // Save changes
            session.save();

            // Return comment data as map
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", commentId);
            comment.put("message", message.trim());
            comment.put("createdBy", createdBy);
            comment.put("createdAt", ISO8601.format(now));

            return comment;
        });
    }

    @Override
    public List<Map<String, Object>> getComments(String ticketId) {
        logger.debug("Fetching comments for ticket: {}", ticketId);

        if (ticketId == null || ticketId.isEmpty()) {
            throw new IllegalArgumentException("Ticket ID is required");
        }

        try {
            return systemResolverService.executeWithSystemResolver(SERVICE_USER, resolver -> {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    throw new RuntimeException("Cannot adapt ResourceResolver to Session");
                }

                List<Map<String, Object>> comments = new ArrayList<>();

                try {
                    String ticketPath = TICKETS_ROOT_PATH + "/" + ticketId;
                    Node ticketNode = session.getNode(ticketPath);
                    Node jcrContentNode = ticketNode.getNode("jcr:content");

                    if (jcrContentNode.hasNode(COMMENTS_FOLDER)) {
                        Node commentsNode = jcrContentNode.getNode(COMMENTS_FOLDER);
                        NodeIterator nodeIterator = commentsNode.getNodes();

                        // Collect comments in chronological order (oldest first)
                        while (nodeIterator.hasNext()) {
                            Node commentNode = nodeIterator.nextNode();
                            Map<String, Object> comment = new HashMap<>();
                            comment.put("id", commentNode.getName());
                            comment.put("message", commentNode.getProperty("message").getString());
                            comment.put("createdBy", commentNode.getProperty("createdBy").getString());
                            comment.put("createdAt",
                                    ISO8601.format(commentNode.getProperty("createdAt").getDate()));
                            comments.add(comment);
                        }
                    }
                } catch (javax.jcr.PathNotFoundException e) {
                    logger.debug("Ticket or comments folder not found: {}", ticketId);
                    // Return empty list if ticket or comments folder doesn't exist
                }

                return comments;
            });
        } catch (Exception e) {
            logger.error("Error fetching comments for ticket: {}", ticketId, e);
            throw new RuntimeException("Failed to fetch comments: " + e.getMessage(), e);
        }
    }

    private static class ISO8601 {
        static String format(Calendar cal) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.format(cal.getTime());
        }
    }
}
