package com.example.bankapi.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.bankapi.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(JwtService jwtService,
			UserDetailsService userDetailsService, AuthenticationEntryPoint authenticationEntryPoint) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(7).trim();
		try {
			if (token.isEmpty()) {
				throw new IllegalArgumentException("Bearer token is empty");
			}
			String username = jwtService.extractUsername(token);
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				if (!jwtService.isTokenValid(token, userDetails)) {
					SecurityContextHolder.clearContext();
					authenticationEntryPoint.commence(request, response,
							new org.springframework.security.authentication.BadCredentialsException("Invalid token"));
					return;
				}
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(request, response);
		} catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(request, response, exception instanceof AuthenticationException authException
					? authException
					: new org.springframework.security.authentication.BadCredentialsException("Invalid token", exception));
		}
	}
}