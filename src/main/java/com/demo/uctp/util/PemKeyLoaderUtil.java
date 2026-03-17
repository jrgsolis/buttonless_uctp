package com.demo.uctp.util;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import java.io.FileReader;
import java.io.StringReader;
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
            return loadPrivateKey(pemParser, p.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException("No fue posible cargar la llave privada PEM desde: " + p.toAbsolutePath(), e);
        }
    }

	public static RSAPrivateKey loadPrivateKeyFromPemString(String pem) {
		if (pem == null || pem.isBlank()) {
			throw new RuntimeException("PEM vacío para llave privada.");
		}

		// Allows Railway/env-var styles where newlines are escaped as "\n"
		String normalized = pem.trim().replace("\\n", "\n");

		try (PEMParser pemParser = new PEMParser(new StringReader(normalized))) {
			return loadPrivateKey(pemParser, "inline-pem");
		} catch (Exception e) {
			throw new RuntimeException("No fue posible cargar la llave privada PEM desde contenido en memoria.", e);
		}
	}

	private static RSAPrivateKey loadPrivateKey(PEMParser pemParser, String sourceLabel) {
		try {
			Object object = pemParser.readObject();

			JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

			PrivateKey privateKey;

			if (object instanceof PrivateKeyInfo privateKeyInfo) {
				privateKey = converter.getPrivateKey(privateKeyInfo);
			} else {
				throw new IllegalArgumentException("Formato PEM no soportado para llave privada (" + sourceLabel + "): " + object);
			}

			return (RSAPrivateKey) privateKey;
		} catch (Exception e) {
			throw new RuntimeException("No fue posible parsear la llave privada PEM (" + sourceLabel + ").", e);
		}
	}
}
