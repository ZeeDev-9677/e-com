package com.shop.user.service;

import com.shop.common.security.JwtProperties;
import com.shop.user.domain.Role;
import com.shop.user.domain.User;
import com.shop.user.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtProperties props;

    public TokenResponse issue(User u) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.expirationMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer()).issuedAt(now).expiresAt(exp)
                .subject(u.getId().toString())
                .claim("email", u.getEmail())
                .claim("roles", u.getRoles().stream().map(Role::name).toList())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", props.expirationMinutes() * 60);
    }
}
