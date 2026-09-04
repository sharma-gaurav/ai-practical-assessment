package com.example.aem.core.services;

import java.util.List;
import java.util.Map;

public interface CommentService {

    Map<String, Object> addComment(String ticketId, String message, String createdBy);

    List<Map<String, Object>> getComments(String ticketId);
}
