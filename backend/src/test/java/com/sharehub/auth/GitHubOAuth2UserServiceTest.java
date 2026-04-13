package com.sharehub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class GitHubOAuth2UserServiceTest {

    @Test
    void shouldMarkWhitelistedGithubUserAsAdmin() {
        AdminWhitelistRepository repository = mock(AdminWhitelistRepository.class);
        when(repository.isAdmin("octocat")).thenReturn(true);

        GitHubOAuth2UserService service = new GitHubOAuth2UserService(repository);
        OAuth2User user = new DefaultOAuth2User(
            Set.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("login", "octocat", "name", "Octo Cat"),
            "login"
        );

        OAuth2User loaded = service.enrichUser(user);
        assertThat(loaded.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }
}
