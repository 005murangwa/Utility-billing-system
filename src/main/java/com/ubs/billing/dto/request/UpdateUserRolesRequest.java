package com.ubs.billing.dto.request;

import com.ubs.billing.validation.ValidStaffRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to assign or update staff roles")
public class UpdateUserRolesRequest {

    @NotEmpty(message = "At least one role is required")
    @ValidStaffRole
    @Schema(description = "Staff roles to assign", example = "[\"ROLE_OPERATOR\"]")
    private Set<String> roles;
}
