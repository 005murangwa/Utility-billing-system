package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.CommentResponse;
import com.ubs.billing.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .billId(comment.getBill().getId())
                .billReference(comment.getBill().getBillReference())
                .customerId(comment.getBill().getCustomer().getId())
                .customerFullName(comment.getBill().getCustomer().getFullName())
                .comment(comment.getComment())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
