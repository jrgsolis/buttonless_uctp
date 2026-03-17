package com.demo.uctp.service;

import com.demo.uctp.util.PemKeyLoaderUtil;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.interfaces.RSAPrivateKey;

@Service
public class CtpJweService {

    private static final Logger log = LoggerFactory.getLogger(CtpJweService.class);

    private final String privateKeyPath;
    private final String privateKeyPem;
    private final String privateKeyPemBase64;

    public CtpJweService(
        @Value("${ctp.mle.private-key-path:}") String privateKeyPath,
        @Value("${ctp.mle.private-key-pem:}") String privateKeyPem,
        @Value("${ctp.mle.private-key-pem-base64:}") String privateKeyPemBase64
    ) {
        this.privateKeyPath = privateKeyPath;
        this.privateKeyPem = privateKeyPem;
        this.privateKeyPemBase64 = privateKeyPemBase64;
    }

    @PostConstruct
    public void logConfig() {
        boolean hasPemB64 = privateKeyPemBase64 != null && !privateKeyPemBase64.isBlank();
        boolean hasPem = privateKeyPem != null && !privateKeyPem.isBlank();
        boolean hasPath = privateKeyPath != null && !privateKeyPath.isBlank();
        log.info("MLE private key configured? pemBase64={} pem={} path={}", hasPemB64, hasPem, hasPath);
    }

    public String decryptJwe(String jwe) throws Exception {

	    try {	
	        RSAPrivateKey privateKey = resolvePrivateKey();

       /* JWEObject jweObject = JWEObject.parse(jwe);

        jweObject.decrypt(new RSADecrypter(privateKey));

        String payload = jweObject.getPayload().toString();

        SignedJWT jwt = SignedJWT.parse(payload);

        return jwt.getJWTClaimsSet().getClaims();*/
        
        
        JWEObject jweObject = JWEObject.parse(jwe);
        jweObject.decrypt(new RSADecrypter(privateKey));

        return jweObject.getPayload().toString();
	        
	        
	     } catch (Exception e) {
	    	 throw new RuntimeException("No fue posible desencriptar el JWE", e);
	     }
	    
	    }

    private RSAPrivateKey resolvePrivateKey() {
        if (privateKeyPemBase64 != null && !privateKeyPemBase64.isBlank()) {
            log.info("Using MLE private key source: pemBase64");
            try {
                String raw = privateKeyPemBase64.trim();
                if (raw.contains("BEGIN PRIVATE KEY") || raw.contains("BEGIN RSA PRIVATE KEY")) {
                    throw new IllegalStateException(
                        "ctp.mle.private-key-pem-base64 parece contener PEM en texto plano. " +
                        "Usa ctp.mle.private-key-pem (CTP_MLE_PRIVATE_KEY_PEM) o pega un Base64 real."
                    );
                }

                // Railway UIs / copy-paste sometimes introduce whitespace/newlines; ignore them.
                String compact = raw.replaceAll("\\s+", "");
                byte[] decoded = Base64.getMimeDecoder().decode(compact);
                String pem = new String(decoded, StandardCharsets.UTF_8);
                return PemKeyLoaderUtil.loadPrivateKeyFromPemString(pem);
            } catch (Exception e) {
                throw new IllegalStateException("No fue posible decodificar ctp.mle.private-key-pem-base64.", e);
            }
        }

        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            log.info("Using MLE private key source: pem");
            return PemKeyLoaderUtil.loadPrivateKeyFromPemString(privateKeyPem);
        }

        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            log.info("Using MLE private key source: path");
            return PemKeyLoaderUtil.loadPrivateKey(privateKeyPath);
        }

        throw new IllegalStateException(
            "Configura una llave privada MLE (PEM PKCS#8) via ctp.mle.private-key-path, " +
            "ctp.mle.private-key-pem, o ctp.mle.private-key-pem-base64."
        );
    }

}
