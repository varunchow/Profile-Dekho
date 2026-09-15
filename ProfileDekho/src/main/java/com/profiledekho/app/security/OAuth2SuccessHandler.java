package com.profiledekho.app.security;

import com.profiledekho.app.model.User;
import com.profiledekho.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String clientName = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("GOOGLE");

        String username;
        String email;
        String name;
        String provider;

        if (clientName.contains("google")) {
            provider = "google";
            email = oauth2User.getAttribute("email");
            username = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9_]", "_");
            name = oauth2User.getAttribute("name");
        } else if (clientName.contains("github")) {
            provider = "github";
            username = oauth2User.getAttribute("login");
            email = oauth2User.getAttribute("email");
            if (email == null) {
                email = username + "@github.com";
            }
            name = oauth2User.getAttribute("name");
            if (name == null) {
                name = username;
            }
        } else {
            provider = "unknown";
            email = oauth2User.getAttribute("email");
            username = oauth2User.getAttribute("login");
            name = oauth2User.getAttribute("name");
        }

        // Find or create user
        Optional<User> optUser = userRepository.findByIdentifier(username);
        User user;

        if (optUser.isPresent()) {
            user = optUser.get();
            user.setLastLogin(Instant.now().toString());
        } else {
            user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setName(name);
            user.setProvider(provider);
            user.setCreatedAt(Instant.now().toString());
            user.setLastLogin(Instant.now().toString());
        }

        user.setAvatar(oauth2User.getAttribute("avatar_url"));
        userRepository.save(user);

        // Generate JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("provider", provider);
        claims.put("name", name);

        String accessToken = jwtUtil.generateAccessToken(username, claims);
        String refreshToken = jwtUtil.generateRefreshToken(username);

        // Redirect to frontend with token
        String redirectUrl = String.format("%s/auth-callback?token=%s&refreshToken=%s&username=%s&email=%s&name=%s",
                frontendUrl, accessToken, refreshToken, username, email, name);

        response.sendRedirect(redirectUrl);
    }
}
