package com.demo.uctp.service;

import com.demo.uctp.util.HttpSignatureUtil;
import com.demo.uctp.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SessionsService {

  @Value("${vas.host}")
  private String host;

  @Value("${vas.sessions.path}")
  private String sessionsPath;

  @Value("${merchant.id}")
  private String merchantId;

  @Value("${merchant.keyId}")
  private String keyId;

  @Value("${merchant.secretKey}")
  private String secretKeyBase64;

  public record SessionsResponse(String captureContextJwt,
                                 String clientLibrary,
                                 String clientLibraryIntegrity,
                                 Map<String, Object> decodedJwtPayload,
                                 JsonNode rawSessionsResponse) {}

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final HttpClient http = HttpClient.newHttpClient();

	  public SessionsResponse createUctpSession(String currency, String totalAmount, String country, String locale, String origin) {

		  String o = (origin == null) ? "" : origin.trim();
		  if (o.isBlank() || !o.startsWith("https://")) {
		    throw new IllegalArgumentException(
		        "Origin must start with https:// (CyberSource UCTP targetOrigins requires HTTPS). Got: " + origin
		    );
		  }
		  
		  // 1) Build body EXACT to your working example
		  Map<String, Object> body = new LinkedHashMap<>();
		  body.put("version", "0.6");
		  body.put("targetOrigins", List.of(o));

	  Map<String, Object> amountDetails = new LinkedHashMap<>();
	  amountDetails.put("totalAmount", totalAmount);
	  amountDetails.put("currency", currency);

	  Map<String, Object> orderInformation = new LinkedHashMap<>();
	  orderInformation.put("amountDetails", amountDetails);

	  Map<String, Object> data = new LinkedHashMap<>();
	  data.put("orderInformation", orderInformation);

	  body.put("data", data);

	  body.put("allowedCardNetworks", List.of("VISA", "MASTERCARD", "AMEX"));
	  body.put("country", country);
	  body.put("locale", locale);
	  body.put("billingType", "FULL");
	  
	  // Debug: confirma lo que mandas a Cybersource
	  System.out.println("[SessionsService] targetOrigins = " + body.get("targetOrigins"));


	  try {
	    // 2) Serialize body
	    String json = MAPPER.writeValueAsString(body);
	    byte[] bodyBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

	    // 3) HTTP Signature headers
	    String date = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);

	    String digestB64 = HttpSignatureUtil.digestSha256Base64(bodyBytes);
	    String digestHeader = "SHA-256=" + digestB64;

	    // request-target MUST be: "post /uctp/v1/sessions" (path only, lowercase method)
	    String requestTarget = "post " + sessionsPath;

	    String signingString =
	        "host: " + host + "\n" +
	        "date: " + date + "\n" +
	        "request-target: " + requestTarget + "\n" +
	        "digest: " + digestHeader + "\n" +
	        "v-c-merchant-id: " + merchantId;

	    // merchant.secretKey MUST be Base64 (per most CyberSource setups)
	    byte[] secretKeyBytes = Base64.getDecoder().decode(secretKeyBase64);

	    String signatureB64 = HttpSignatureUtil.hmacSha256Base64(secretKeyBytes, signingString);

	    String signatureHeader =
	        "keyid=\"" + keyId + "\"," +
	        " algorithm=\"HmacSHA256\"," +
	        " headers=\"host date request-target digest v-c-merchant-id\"," +
	        " signature=\"" + signatureB64 + "\"";

	    // 4) Build request
	    URI uri = URI.create("https://" + host + sessionsPath);

	    HttpRequest req = HttpRequest.newBuilder()
	        .uri(uri)
	        .header("Content-Type", "application/json")
	        .header("Accept", "application/json")
	        // DO NOT set Host manually (restricted header in Java HttpClient)
	        .header("Date", date)
	        .header("Digest", digestHeader)
	        .header("v-c-merchant-id", merchantId)
	        .header("Signature", signatureHeader)
	        .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
	        .build();

	    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

	    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
	      throw new RuntimeException("Sessions API failed. HTTP " + resp.statusCode() + ": " + resp.body());
	    }

	    // 5) Parse response: can be JWT (text/plain) or JSON with captureContext field
	    String raw = resp.body() == null ? "" : resp.body().trim();

	    String jwt;
	    JsonNode sessionsRespNode = null;

	    if (raw.startsWith("eyJ") && raw.contains(".")) {
	      // JWT returned directly
	      jwt = raw;
	    } else {
	      // JSON returned
	      sessionsRespNode = MAPPER.readTree(raw);

	      jwt = null;
	      if (sessionsRespNode.hasNonNull("captureContext")) jwt = sessionsRespNode.get("captureContext").asText();
	      if (jwt == null && sessionsRespNode.hasNonNull("jwt")) jwt = sessionsRespNode.get("jwt").asText();
	      if (jwt == null && sessionsRespNode.hasNonNull("token")) jwt = sessionsRespNode.get("token").asText();

	      // sometimes nested
	      if (jwt == null && sessionsRespNode.has("session")
	          && sessionsRespNode.get("session").hasNonNull("captureContext")) {
	        jwt = sessionsRespNode.get("session").get("captureContext").asText();
	      }

	      if (jwt == null) {
	        throw new RuntimeException("No captureContext/JWT field found in JSON /sessions response: " + raw);
	      }
	    }

	    // 6) Decode JWT payload (no signature verification in this demo)
	    Map<String, Object> payload = JwtUtil.decodeJwtPayload(jwt);

	    // 7) Extract clientLibrary + integrity from payload (common path)
	    String clientLibrary = null;
	    String clientLibraryIntegrity = null;

	    try {
	      Object ctxObj = payload.get("ctx");
	      if (ctxObj instanceof List<?> ctxList && !ctxList.isEmpty()) {
	        Object first = ctxList.get(0);
	        if (first instanceof Map<?, ?> firstMap) {
	          Object dataObj2 = firstMap.get("data");
	          if (dataObj2 instanceof Map<?, ?> dataMap) {
	            Object cl = dataMap.get("clientLibrary");
	            Object cli = dataMap.get("clientLibraryIntegrity");
	            clientLibrary = cl != null ? cl.toString() : null;
	            clientLibraryIntegrity = cli != null ? cli.toString() : null;
	          }
	        }
	      }
	    } catch (Exception ignored) { }

	    // If response was JWT-only, keep a node for logging
	    JsonNode rawNode = sessionsRespNode != null
	        ? sessionsRespNode
	        : MAPPER.createObjectNode().put("rawJwt", jwt);

	    return new SessionsResponse(jwt, clientLibrary, clientLibraryIntegrity, payload, rawNode);

	  } catch (Exception e) {
	    throw new RuntimeException("Failed to create UCTP session: " + e.getMessage(), e);
	  }
	}
  }
