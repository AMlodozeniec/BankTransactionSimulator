Name: Adam Mlodozeniec
Email: amlodoz1@binghamton.edu

Programming language: Java
Platform: Linux

Tested on bingsuns: Yes

How to compile: make
How to execute: 
		(1) First, run Bank.java with 'java Bank 9000'
		(2) Then, run Psystem.java with 'java Psystem 9844 <domain-name> 9000'
		(3) Finally, run Customer with 'java Customer <domain-name> 9844'
		Note: <domain-name> should be same for both

Optional: Project was done alone for extra credit

Code for performing encryption/decryption:
	Creating public keys: 
		
    openssl rsa -in aliceKeys.pem -pubout -outform DER -out Pua.der
    openssl rsa -in bankKeys.pem -pubout -outform DER -out Pub.der
    openssl rsa -in tomKeys.pem -pubout -outform DER -out Put.der
    openssl rsa -in psystemKeys.pem -pubout -outform DER -out Pup.der
	
	Creating private keys:

	openssl pkcs8 -topk8 -inform PEM -outform DER -in bankKeys.pem -out Prb.der -nocrypt
    openssl pkcs8 -topk8 -inform PEM -outform DER -in aliceKeys.pem -out Pra.der -nocrypt
 	openssl pkcs8 -topk8 -inform PEM -outform DER -in psystemKeys.pem -out Prp.der -nocrypt
 	openssl pkcs8 -topk8 -inform PEM -outform DER -in tomKeys.pem -out Prt.der -nocrypt

	Functions for encryption:
			
		(Using public key) 
			public String encrypt(PublicKey key, String str) throws Exception{
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc = cipher.doFinal(utf8);

            return new sun.misc.BASE64Encoder().encode(enc);
    	}
			
			
			
		(Using private key)
		    public String encrypt(PrivateKey key, String str) throws Exception{
            	Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
            	cipher.init(Cipher.ENCRYPT_MODE, key);  
            	byte[] utf8 = str.getBytes("UTF8");
            	byte[] enc = cipher.doFinal(utf8);
            
            	return new sun.misc.BASE64Encoder().encode(enc);
    		}
		
	Function for encrypting password
		
			public String encryptPassword(String password){
		        String sha1 = "";
		        try{
		            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		            crypt.reset();
		            crypt.update(password.getBytes("UTF-8"));
		            sha1 = byteToHex(crypt.digest());
		        }
		        catch(Exception ex){
		            ex.printStackTrace();
		            System.exit(1);
		        }
		        return sha1;
		    }

   		 public String byteToHex(byte[] hash){
   		     Formatter formatter = new Formatter();
   		     for(byte b : hash){
   		         formatter.format("%02x", b);
   		     }
   		     String result = formatter.toString();
   		     formatter.close();
   		     return result;
   		 }
	
	Functions for decryption:
		(Using private key)
		public String decrypt(PrivateKey key, String ciphertext) throws Exception{
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(ciphertext);
            byte[] utf8 = cipher.doFinal(dec);

            return new String(utf8, "UTF8");
    	}
		
		(Using public key) 
		public String encrypt(PublicKey key, String str) throws Exception{
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc = cipher.doFinal(utf8);

            return new sun.misc.BASE64Encoder().encode(enc);
    	}
		
			

