package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = createUser(
                "repository-user",
                "repository-user@example.com"
        );

        userRepository.save(user);

        Optional<User> result =
                userRepository.findByEmail("repository-user@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername())
                .isEqualTo("repository-user");
        assertThat(result.get().getEmail())
                .isEqualTo("repository-user@example.com");
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> result =
                userRepository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
    }

    private User createUser(String username, String email) {
        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(UserRole.SALES);
        user.setActive(true);

        return user;
    }
}