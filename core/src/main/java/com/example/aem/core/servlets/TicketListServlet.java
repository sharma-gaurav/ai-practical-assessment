package com.example.aem.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.services.SystemResourceResolverService;
import com.google.gson.Gson;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/api/tickets/list",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET
    }
)
public class TicketListServlet extends SlingAllMethodsServlet {

    private static final Logger logger = LoggerFactory.getLogger(TicketListServlet.class);
    private static final String TICKETS_PATH = "/content/ai-practical-assessment/tickets";
    private static final String SERVICE_USER = "ai-practical-assessment-ticketservice";
    private static final Gson gson = new Gson();

    @Reference
    private SystemResourceResolverService systemResolverService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String searchKeyword = request.getParameter("search");
        String statusFilter = request.getParameter("status");
        String pageStr = request.getParameter("page");
        String limitStr = request.getParameter("limit");

        int page = StringUtils.isNotBlank(pageStr) ? Integer.parseInt(pageStr) : 0;
        int limit = StringUtils.isNotBlank(limitStr) ? Integer.parseInt(limitStr) : 20;

        try {
            List<Map<String, Object>> tickets = fetchTickets(searchKeyword, statusFilter, page, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("tickets", tickets);
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", tickets.size());

            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(result));

        } catch (Exception e) {
            logger.error("Error fetching tickets", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

                QueryManager qm = session.getWorkspace().getQueryManager();
                String queryString = buildQuery(searchKeyword, statusFilter);

                Query query = qm.createQuery(queryString, Query.JCR_SQL2);
                QueryResult result = query.execute();
                NodeIterator nodeIterator = result.getNodes();

                while (nodeIterator.hasNext()) {
                    Node ticketNode = nodeIterator.nextNode();
                    Map<String, Object> ticket = extractTicketData(ticketNode);
                    if (ticket != null) {
                        tickets.add(ticket);
                    }
                }

            } catch (RepositoryException e) {
                logger.error("Repository error while fetching tickets", e);
                throw e;
            }

            return tickets;
        });
    }

    private String buildQuery(String searchKeyword, String statusFilter) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM [cq:Page] ")
             .append("WHERE ISDESCENDANTNODE([").append(TICKETS_PATH).append("]) ");

        if (StringUtils.isNotBlank(statusFilter)) {
            query.append("AND [jcr:content/status] = '").append(statusFilter).append("' ");
        }

        if (StringUtils.isNotBlank(searchKeyword)) {
            String keyword = "%" + searchKeyword + "%";
            query.append("AND (")
                 .append("[jcr:content/jcr:title] LIKE '").append(keyword).append("' ")
                 .append("OR [jcr:content/description] LIKE '").append(keyword).append("') ");
        }

        query.append("ORDER BY [jcr:content/createdAt] DESC");
        return query.toString();
    }

    private Map<String, Object> extractTicketData(Node ticketNode) throws RepositoryException {
        try {
            if (!ticketNode.hasNode("jcr:content")) {
                return null;
            }

            Node contentNode = ticketNode.getNode("jcr:content");
            Map<String, Object> ticket = new HashMap<>();

            ticket.put("id", ticketNode.getName());
            ticket.put("title", contentNode.getProperty("jcr:title").getString());
            ticket.put("description", getPropertyValue(contentNode, "description", ""));
            ticket.put("priority", getPropertyValue(contentNode, "priority", "MEDIUM"));
            ticket.put("status", getPropertyValue(contentNode, "status", "Open"));
            ticket.put("assignedTo", getPropertyValue(contentNode, "assignedTo", ""));
            ticket.put("createdBy", getPropertyValue(contentNode, "createdBy", ""));
            ticket.put("createdAt", getPropertyValue(contentNode, "createdAt", ""));
            ticket.put("updatedAt", getPropertyValue(contentNode, "updatedAt", ""));

            return ticket;

        } catch (RepositoryException e) {
            logger.error("Error extracting ticket data from node: " + ticketNode.getPath(), e);
            return null;
        }
    }

    private String getPropertyValue(Node node, String propertyName, String defaultValue) {
        try {
            if (node.hasProperty(propertyName)) {
                return node.getProperty(propertyName).getString();
            }
        } catch (RepositoryException e) {
            logger.debug("Property not found: " + propertyName, e);
        }
        return defaultValue;
    }
}
