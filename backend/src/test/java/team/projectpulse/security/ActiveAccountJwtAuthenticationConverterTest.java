package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ActiveAccountJwtAuthenticationConverterTest {

    @Test
    void acceptsAnEnabledAccountWhenTheJwtUserIdIsLong() {
        UserRepository userRepository = mock(UserRepository.class);
        Student student = new Student("john", "John", "Smith", "j.smith@abc.edu", "password", true, "student");
        given(userRepository.findById(4)).willReturn(Optional.of(student));
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("userId", 4L)
                .claim("authorities", "ROLE_student")
                .build();

        assertThat(new ActiveAccountJwtAuthenticationConverter(userRepository).convert(jwt)).isNotNull();
    }
}
