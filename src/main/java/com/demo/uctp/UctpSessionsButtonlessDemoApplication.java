package com.demo.uctp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

@SpringBootApplication
public class UctpSessionsButtonlessDemoApplication {
 
	public static void main(String[] args) {
		
		 Security.addProvider(new BouncyCastleProvider());

	    
	        
    SpringApplication.run(UctpSessionsButtonlessDemoApplication.class, args);
  
	}


}
