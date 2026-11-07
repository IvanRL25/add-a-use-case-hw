package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;
    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    public ActiveAccountJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");
        this.delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = jwt.getClaim("userId");
        if (userId == null || this.userRepository.findById(userId).filter(user -> user.isEnabled()).isEmpty()) {
            throw new InvalidBearerTokenException("The account is deactivated or unavailable.");
        }
        return this.delegate.convert(jwt);
    }
}
