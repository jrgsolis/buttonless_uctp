package com.demo.uctp.service;

import com.demo.uctp.util.PemKeyLoaderUtil;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;

@Service
public class CtpJweService {

    private final String privateKeyPath;

    public CtpJweService(@Value("${ctp.mle.private-key-path:}") String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String decryptJwe(String jwe) throws Exception {

	    try {	
	        if (privateKeyPath == null || privateKeyPath.isBlank()) {
	            throw new IllegalStateException(
	                "No se configuró ctp.mle.private-key-path (ruta al PEM PKCS#8)."
	            );
	        }

	        RSAPrivateKey privateKey =
	                PemKeyLoaderUtil.loadPrivateKey(privateKeyPath);

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

}
