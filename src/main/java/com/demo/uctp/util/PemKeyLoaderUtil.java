package com.demo.uctp.util;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

public class PemKeyLoaderUtil {

	/* String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
*/
	
	public static RSAPrivateKey loadPrivateKey(String path) {
		Path p;
		try {
			p = Path.of(path);
		} catch (Exception e) {
			throw new RuntimeException("Ruta inválida para llave privada PEM: " + path, e);
		}

		if (!Files.exists(p) || !Files.isRegularFile(p)) {
			throw new RuntimeException("No existe el archivo PEM en: " + p.toAbsolutePath());
		}

        try (PEMParser pemParser = new PEMParser(new FileReader(path))) {
            Object object = pemParser.readObject();

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            PrivateKey privateKey;

            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                privateKey = converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new IllegalArgumentException("Formato PEM no soportado para llave privada: " + object);
            }

            return (RSAPrivateKey) privateKey;

        } catch (Exception e) {
            throw new RuntimeException("No fue posible cargar la llave privada PEM desde: " + p.toAbsolutePath(), e);
        }
    }
}
