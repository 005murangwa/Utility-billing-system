package com.ubs.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long billId;
    private String billReference;
    private Long customerId;
    private String customerFullName;
    private String comment;
    private LocalDateTime createdAt;
}
