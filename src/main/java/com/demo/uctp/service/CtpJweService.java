package com.demo.uctp.service;

import com.demo.uctp.util.PemKeyLoaderUtil;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.interfaces.RSAPrivateKey;

@Service
public class CtpJweService {

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
            try {
                byte[] decoded = Base64.getDecoder().decode(privateKeyPemBase64.trim());
                String pem = new String(decoded, StandardCharsets.UTF_8);
                return PemKeyLoaderUtil.loadPrivateKeyFromPemString(pem);
            } catch (Exception e) {
                throw new IllegalStateException("No fue posible decodificar ctp.mle.private-key-pem-base64.", e);
            }
        }

        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            return PemKeyLoaderUtil.loadPrivateKeyFromPemString(privateKeyPem);
        }

        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            return PemKeyLoaderUtil.loadPrivateKey(privateKeyPath);
        }

        throw new IllegalStateException(
            "Configura una llave privada MLE (PEM PKCS#8) via ctp.mle.private-key-path, " +
            "ctp.mle.private-key-pem, o ctp.mle.private-key-pem-base64."
        );
    }

}
