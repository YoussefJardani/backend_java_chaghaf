package ma.chaghaf.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        String phone
    ) {}

    public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        String role
    ) {}

    public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String role,
        String avatar,
        Boolean active
    ) {}
}
