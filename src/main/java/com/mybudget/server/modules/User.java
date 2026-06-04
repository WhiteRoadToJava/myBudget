package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.Role;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    // use the email regex pattern to validate the email format, use the email as the username for authentication.
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-ZA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Invalid email format"
    )
    private String username;

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()\\-_=+{};:,<.>])(?=.{8,})"
            + ".*$",
            message = "Password must be at least 8 characters long and contain at least "
            + "one uppercase letter, one number, and one special character"
    )
    private String password;

    private Set<Role> roles;
    private String firstName;
    private String lastName;
    private String phone;
    private String expireDate;
    @CreatedDate
    private String createdAt;

    public User() {
    }

    public User(String username, String password, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

}
