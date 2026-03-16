package com.demo.uctp.web;

import com.demo.uctp.service.SessionsService;
import com.demo.uctp.service.SessionsService.SessionsResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionsController {

  private final SessionsService service;

  public SessionsController(SessionsService service) {
    this.service = service;
  }

  /**
   * LEGACY (keeps backwards compatibility):
   * Creates a UCTP session via query params.
   */
  @GetMapping(value = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
  public SessionsResponse createSessionGet(
      @RequestParam(name = "currency", defaultValue = "USD") String currency,
      @RequestParam(name = "totalAmount", defaultValue = "21.00") String totalAmount,
      @RequestParam(name = "country", defaultValue = "US") String country,
      @RequestParam(name = "locale", defaultValue = "en_US") String locale,
      // You may pass http://localhost:8080 for dev, or https://... for prod
      @RequestParam(name = "origin", defaultValue = "https://localhost:8080") String origin
  ) {
    String resolvedOrigin = normalizeOrigin(origin);

    if (!isAllowedOrigin(resolvedOrigin)) {
      throw new IllegalArgumentException("origin must be https://... or http://localhost[:port]. Got: " + resolvedOrigin);
    }

	    // ✅ Normaliza y valida (nunca 0)
	    String normalizedAmount = normalizeAmount(totalAmount, "21.00");
	    
	    return service.createUctpSession(currency, normalizedAmount, country, locale, resolvedOrigin);
	  }

  @PostMapping(
		    value = "/sessions",
		    consumes = MediaType.APPLICATION_JSON_VALUE,
		    produces = MediaType.APPLICATION_JSON_VALUE
		)
		public SessionsResponse createSessionPost(@RequestBody SessionsRequest req, HttpServletRequest httpReq) {

		  // --- Amount / currency ---
		  String currency = safeCurrency(req.data, "USD");
		  String totalAmountRaw = safeAmount(req.data, "21.00");
		  String totalAmount = normalizeAmount(totalAmountRaw, "21.00"); // valida > 0

		  String country = (req.country != null && !req.country.isBlank()) ? req.country : "US";
		  String locale  = (req.locale != null && !req.locale.isBlank()) ? req.locale : "en_US";

		  // --- Resolve origin (priority: Origin header) ---
		  String originHeader = httpReq.getHeader("Origin");
		  String origin = normalizeOrigin(originHeader);

		  // Optional: if you're behind proxy / tunnel, these can help debug
		  String xfProto = httpReq.getHeader("X-Forwarded-Proto");
		  String xfHost  = httpReq.getHeader("X-Forwarded-Host");

		  // Fallback: body.targetOrigins if no Origin header
		  if (origin == null) {
		    origin = firstAnyAllowedOrigin(req.targetOrigins);
		  }

		  if (origin == null) {
		    throw new IllegalArgumentException("No Origin header and no valid targetOrigins provided.");
		  }

		  if (!isAllowedOrigin(origin)) {
		    throw new IllegalArgumentException("Origin must be https://... or http://localhost[:port]. Got: " + origin);
		  }

		  // ✅ Logs to prove what we're using (TEMPORARY but super useful now)
		  System.out.println("[SessionsController] Origin header: " + originHeader);
		  System.out.println("[SessionsController] X-Forwarded-Proto: " + xfProto);
		  System.out.println("[SessionsController] X-Forwarded-Host: " + xfHost);
		  System.out.println("[SessionsController] req.targetOrigins: " + req.targetOrigins);
		  System.out.println("[SessionsController] origin USED: " + origin);
		  System.out.println("[SessionsController] amountRaw=" + totalAmountRaw + " amountNormalized=" + totalAmount + " currency=" + currency);

		  // IMPORTANT: now the service MUST use this origin to build targetOrigins in the Cybersource request
		  return service.createUctpSession(currency, totalAmount, country, locale, origin);
		}
  // -------- Helpers --------

  /**
   * Accept:
   * - https://... (any host)  ✅ (prod)
   * - http://localhost[:port] ✅ (dev only)
   */
	  private static boolean isAllowedOrigin(String origin) {
		  if (origin == null || origin.isBlank()) return false;
		  return origin.startsWith("https://");
		}

  /**
   * Returns the first origin in the list that is allowed by isAllowedOrigin.
   */
  private static String firstAnyAllowedOrigin(List<String> targetOrigins) {
	  if (targetOrigins == null || targetOrigins.isEmpty()) return null;
	  for (String o : targetOrigins) {
	    String norm = normalizeOrigin(o);
	    if (norm != null && isAllowedOrigin(norm)) return norm;
	  }
	  return null;
	}

  /**
   * Normalize:
   * - trims
   * - if a full URL is provided (with path/query), keep only scheme://host[:port]
   * - strip trailing slash
   */
  private static String normalizeOrigin(String maybeUrl) {
	  if (maybeUrl == null) return null;
	  String s = maybeUrl.trim();
	  if (s.isBlank()) return null;

	  int schemeIdx = s.indexOf("://");
	  if (schemeIdx > 0) {
	    int slashAfterHost = s.indexOf("/", schemeIdx + 3);
	    if (slashAfterHost > 0) s = s.substring(0, slashAfterHost);
	    if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
	  }
	  return s;
	}

  private static String safeCurrency(SessionsRequest.Data data, String def) {
    try {
      String v = data.orderInformation.amountDetails.currency;
      return (v == null || v.isBlank()) ? def : v;
    } catch (Exception e) {
      return def;
    }
  }

  private static String safeAmount(SessionsRequest.Data data, String def) {
    try {
      String v = data.orderInformation.amountDetails.totalAmount;
      return (v == null || v.isBlank()) ? def : v;
    } catch (Exception e) {
      return def;
    }
  }

  
  private static String normalizeAmount(String amountStr, String def) {
	  String v = (amountStr == null) ? "" : amountStr.trim();
	  if (v.isBlank()) v = def;

	  v = v.replace(",", ".").replace("$", "").trim();

	  try {
	    double d = Double.parseDouble(v);
	    if (d <= 0.0) throw new IllegalArgumentException("Cart total must be greater than zero. Add an item before creating a session.");
	    return String.format(java.util.Locale.US, "%.2f", d);
	  } catch (NumberFormatException e) {
	    throw new IllegalArgumentException("Invalid totalAmount: " + amountStr);
	  }
	}
  
  
  // -------- Request DTOs --------
  // Keep them lenient to avoid breaking if you add more fields later.

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SessionsRequest {
    public String version;
    public List<String> targetOrigins;
    public Data data;
    public List<String> allowedCardNetworks;
    public String country;
    public String locale;
    public String billingType;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
      public OrderInformation orderInformation;

      @JsonIgnoreProperties(ignoreUnknown = true)
      public static class OrderInformation {
        public AmountDetails amountDetails;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AmountDetails {
          public String totalAmount;
          public String currency;
        }
      }
    }
  }
}
