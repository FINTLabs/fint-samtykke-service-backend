package no.fintlabs;


import no.vigoiks.resourceserver.security.FintJwtUserConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;


@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Value("${fint.integration.service.authorized-role:${fint.integration.service.authorized-role:rolle}}")
    private String authorizedRole;
    @Value("${fint.integration.service.authorized-org-id:vigo.no}")
    private String authorizedOrgId;
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/**")
                        //.permitAll()
                        .access(accessDecisionManager())
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt()
                        .jwtAuthenticationConverter(new FintJwtUserConverter()));
        return http.build();
    }

    @Bean
    public ReactiveAuthorizationManager<AuthorizationContext> accessDecisionManager() {
        return (mono, context) -> mono
                .map(auth -> {
                    AuthorizationDecision decision = new AuthorizationDecision(false);
                    if (auth instanceof JwtAuthenticationToken) {
                        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
                        boolean hasRole = jwtAuth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_" + authorizedRole));
                        boolean hasAuthority = jwtAuth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ORGID_" + authorizedOrgId));
                        if (hasRole && hasAuthority) {
                            decision = new AuthorizationDecision(true);
                        }
                    }
                    return decision;
                });
    }
}