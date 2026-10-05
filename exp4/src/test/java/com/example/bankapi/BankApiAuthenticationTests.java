package com.example.bankapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BankApiAuthenticationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Test
	void validCredentialsReturnTokenAndAllowAccountAccess() throws Exception {
		String token = loginAndGetToken();

		mockMvc.perform(get("/account/details").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountNumber").value("XXXXXX1234"))
				.andExpect(jsonPath("$.accountHolder").value("Test User"))
				.andExpect(jsonPath("$.balance").value(50000.0));
	}

	@Test
	void missingTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/account/details"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	@Test
	void invalidTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/account/details").header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));

		String[] tokenParts = loginAndGetToken().split("\\.");
		String changedSignature = (tokenParts[2].startsWith("A") ? "B" : "A") + tokenParts[2].substring(1);
		String tamperedToken = tokenParts[0] + "." + tokenParts[1] + "." + changedSignature;
		mockMvc.perform(get("/account/details").header("Authorization", "Bearer " + tamperedToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	@Test
	void expiredTokenReturnsUnauthorized() throws Exception {
		SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
		Instant now = Instant.now();
		String expiredToken = Jwts.builder()
				.subject("user")
				.issuedAt(Date.from(now.minusSeconds(120)))
				.expiration(Date.from(now.minusSeconds(60)))
				.signWith(key)
				.compact();

		mockMvc.perform(get("/account/details").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	@Test
	void invalidCredentialsReturnUnauthorized() throws Exception {
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"user\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
	}

	private String loginAndGetToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"user\",\"password\":\"password\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.get("token").asText();
	}
}