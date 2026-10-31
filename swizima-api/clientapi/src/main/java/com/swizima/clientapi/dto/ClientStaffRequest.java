package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a client staff member.
 */
public record ClientStaffRequest(
        @NotBlank(message = "Surname is required")
        @Size(max = 100)
        String surname,

        @NotBlank(message = "Given name is required")
        @Size(max = 150)
        String givenName,

        Gender gender,

        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,30}$", message = "Phone number is invalid")
        String phoneNumber,

        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,30}$", message = "Alternative phone number is invalid")
        String alternativePhone,

        @Email(message = "Email is invalid")
        @Size(max = 150)
        String email,

        @Size(max = 100)
        String position,

        @Pattern(regexp = "^$|^[A-Za-z0-9]{8,20}$", message = "NIN must be 8-20 letters or digits")
        String nin,

        String summary
) {
}
