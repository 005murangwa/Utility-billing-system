package com.ubs.billing.controller;

import com.ubs.billing.dto.request.CreateCommentRequest;
import com.ubs.billing.dto.response.CommentResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.service.CommentService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR', 'CUSTOMER')")
@Tag(name = "Comments", description = "Bill comment endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Create comment", description = "Adds a comment to a bill. Customers may only comment on their own bills.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.createComment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentById(id)));
    }

    @GetMapping
    @Operation(
            summary = "Get comments",
            description = "Returns paginated comments with optional filters by bill, customer, and search text."
    )
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Search in comment text, username, or bill reference")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<CommentResponse> response = commentService.getComments(billId, customerId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bill/{billId}")
    @Operation(summary = "Get bill comments", description = "Returns paginated comments for a specific bill")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getBillComments(
            @PathVariable Long billId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<CommentResponse> response = commentService.getBillComments(billId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
