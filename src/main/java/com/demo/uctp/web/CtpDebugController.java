package com.demo.uctp.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demo.uctp.service.CtpJweService;

@RestController
@RequestMapping("/api/debug")
public class CtpDebugController {

    private final CtpJweService ctpJweService;

    public CtpDebugController(CtpJweService ctpJweService) {
        this.ctpJweService = ctpJweService;
    }

    @PostMapping("/decrypt-jwe")
    public ResponseEntity<?> decryptJwe(@RequestBody Map<String, String> body) {
        try {
            String jwe = body.get("jwe");

            System.out.println(">>> /api/debug/decrypt-jwe INVOKED");
            
            if (jwe == null || jwe.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "El campo 'jwe' es requerido"
                ));
            }

            String decryptedPayload = ctpJweService.decryptJwe(jwe);
            
            System.out.println("Decrypted JWE: " + decryptedPayload);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "decryptedPayload", decryptedPayload
            ));

        } catch (Exception e) {
	        	e.printStackTrace();

	            String cause = null;
	            Throwable t = e;
	            // walk down a few levels for the most useful root message
	            for (int i = 0; i < 6 && t != null; i++) {
	                if (t.getCause() == null) break;
	                t = t.getCause();
	            }
	            if (t != null && t != e) {
	                cause = t.getMessage();
	            } else if (e.getCause() != null) {
	                cause = e.getCause().getMessage();
	            }
	        	
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "exception",
                e.getClass().getName(),
                "cause", cause != null ? String.valueOf(cause) : "null"
                
            ));
        }
    }
}
