package com.sharehub.auth;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final AdminWhitelistRepository adminWhitelistRepository;

    public GitHubOAuth2UserService(AdminWhitelistRepository adminWhitelistRepository) {
        this.adminWhitelistRepository = adminWhitelistRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);
        return enrichUser(user);
    }

    OAuth2User enrichUser(OAuth2User user) {
        Object loginValue = user.getAttribute("login");
        String login = loginValue == null ? null : String.valueOf(loginValue);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
        if (adminWhitelistRepository.isAdmin(login)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new DefaultOAuth2User(authorities, user.getAttributes(), "login");
    }
}
