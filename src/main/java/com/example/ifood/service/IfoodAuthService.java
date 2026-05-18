package com.example.ifood.service;

import com.example.ifood.config.IfoodProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodAuthService {

	private final IfoodProperties properties;
	private final RestClient restClient = RestClient.create();

	private final AtomicReference<CachedToken> cache = new AtomicReference<>();

	public String getAccessToken() {
		if (properties.isMockEnabled()) {
			return "mock-token";
		}

		CachedToken current = cache.get();
		if (current != null && current.isValid()) {
			return current.token();
		}

		return refreshAccessToken();
	}

	public void invalidateCache() {
		cache.set(null);
	}

	@SuppressWarnings("unchecked")
	public synchronized String refreshAccessToken() {
		validarCredenciais();

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grantType", "refresh_token");
		body.add("clientId", properties.getClientId());
		body.add("clientSecret", properties.getClientSecret());
		body.add("refreshToken", properties.getRefreshToken());

		try {
			Map<String, Object> response = restClient.post()
					.uri(properties.authUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(body)
					.retrieve()
					.body(Map.class);

			if (response == null || !response.containsKey("accessToken")) {
				throw new IllegalStateException("Resposta inválida ao renovar token iFood");
			}

			String accessToken = String.valueOf(response.get("accessToken"));
			int expiresIn = response.get("expiresIn") instanceof Number n ? n.intValue() : 21_600;
			Instant expiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 120));

			if (response.containsKey("refreshToken")) {
				log.warn("Novo refresh token retornado — atualize IFOOD_REFRESH_TOKEN no ambiente");
			}

			cache.set(new CachedToken(accessToken, expiresAt));
			log.info("Access token iFood renovado (expira em ~{}s)", expiresIn);
			return accessToken;

		} catch (HttpClientErrorException.Unauthorized e) {
			cache.set(null);
			throw new IllegalStateException(
					"Token iFood inválido (401). Verifique IFOOD_CLIENT_SECRET e IFOOD_REFRESH_TOKEN e reinicie o Spring.",
					e);
		}
	}

	private void validarCredenciais() {
		if (properties.getClientId() == null || properties.getClientId().isBlank()) {
			throw new IllegalStateException("IFOOD_CLIENT_ID não configurado");
		}
		if (properties.getClientSecret() == null || properties.getClientSecret().isBlank()
				|| properties.getClientSecret().contains("sua-secret")) {
			throw new IllegalStateException(
					"IFOOD_CLIENT_SECRET inválido. Use o Client Secret real do portal (não 'sua-secret').");
		}
		if (properties.getRefreshToken() == null || properties.getRefreshToken().isBlank()
				|| properties.getRefreshToken().contains("SEU_REFRESH")) {
			throw new IllegalStateException(
					"IFOOD_REFRESH_TOKEN inválido. Rode o fluxo OAuth (userCode → portal → token) novamente.");
		}
	}

	private record CachedToken(String token, Instant expiresAt) {
		boolean isValid() {
			return Instant.now().isBefore(expiresAt);
		}
	}

}
