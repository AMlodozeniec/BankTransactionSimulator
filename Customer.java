import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import javax.crypto.Cipher;

public class Customer{
	private DataInputStream din;
	private DataOutputStream dout;
	private BufferedReader br;
	private String psystemDomain;
	private int psystemPort;
	private Socket client;
	private String accountId;
	
	PrivateKeyReader privateKeyReader;
	PublicKeyReader publicKeyReader;
	
	final String PUP_FILE = "Pup.der";
	final String PUA_FILE = "Pua.der";
	final String PUT_FILE = "Put.der";
	final String PUB_FILE = "Pub.der";
	
	final String PRA_FILE = "Pra.der";
	final String PRT_FILE = "Prt.der";

	final int CHUNK_SIZE = 64;
	
	public void connect(){
		try{
			System.out.println("Attempting to connect to psystem");
			client = new Socket(psystemDomain, psystemPort);
			System.out.println("Successfully connected to purchasing system at port " + psystemPort);
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
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
	
	public void exception(Exception ex){
		ex.printStackTrace();
		System.exit(1);
	}
	
	public void displayItems(DataInputStream din){
		try{
			String temp = null;
			while((temp = din.readUTF()) != null){
				if(temp.equals("\n")){
					break;
				}
				System.out.println(temp);
			}
			
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public void verifyAccount(BufferedReader userInput, DataInputStream din, DataOutputStream dout){
		boolean verified = false;
		try{
			while(!verified){
				String passwd;
				System.out.print("Enter ID: ");
				accountId = userInput.readLine();
				System.out.print("Enter password: ");
				passwd = userInput.readLine();
				String hash = encryptPassword(passwd);
				//System.out.println("Hashed password " + hash);
				dout.writeUTF(accountId);
				dout.writeUTF(hash);
				String c;
				c = din.readUTF();
				if(c.equals("error")){
					System.out.println("The password is incorrect");
				}
				else{
					displayItems(din);
					verified = true;
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public String encrypt(PublicKey key, String str) throws Exception{
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");   
    		cipher.init(Cipher.ENCRYPT_MODE, key);  
			byte[] utf8 = str.getBytes("UTF8");
			byte[] enc = cipher.doFinal(utf8);
			
			return new sun.misc.BASE64Encoder().encode(enc);
	}
	
	public byte[] decrypt(PrivateKey key, byte[] ciphertext) throws Exception{
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(ciphertext);
		
	}	

	public String generateDigitalSignature(PrivateKey key, byte[] data) throws Exception{
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(key);
		sig.update(data);
		byte[] signatureBytes = sig.sign();
		return new sun.misc.BASE64Encoder().encode(signatureBytes);
	}
	
	public void encryptInfo(String itemNum, String creditCardNumber, DataOutputStream dout){
		try{
			publicKeyReader = new PublicKeyReader();		
			privateKeyReader = new PrivateKeyReader();		
			
			PublicKey pup = publicKeyReader.get(PUP_FILE);
			PublicKey pub = publicKeyReader.get(PUB_FILE);
			PrivateKey pra = privateKeyReader.get(PRA_FILE);
			PrivateKey prt = privateKeyReader.get(PRT_FILE);
			
			
			String encryptedItemNum= encrypt(pup, itemNum);
			byte[] encryptedData = encryptedItemNum.getBytes("UTF8");
			
			String ds = null;
			
			//Generate digital signature
			if(accountId.equals("alice")){
				ds = generateDigitalSignature(pra, encryptedData);
			}
			else if(accountId.equals("tom")){
				ds = generateDigitalSignature(prt, encryptedData);
			}
			else{
				System.out.println("wrong accountId");
				System.exit(1);
			}
			String encryptedName = encrypt(pub, accountId);
			String encryptedCreditCard = encrypt(pub, creditCardNumber);
			
			dout.writeUTF(encryptedItemNum);
			dout.writeUTF(ds);
			dout.writeUTF(encryptedName);
	//		System.out.println("ds is " + ds);
			dout.writeUTF(encryptedCreditCard);
		}
		catch(Exception ex){
			exception(ex);
		}	
	}
	
	public void selectItem(BufferedReader userInput, DataInputStream din,
DataOutputStream dout){
		try{
			System.out.print("Please enter the item #: ");
			String itemNum, creditCardNumber;
			itemNum = userInput.readLine();
			//dout.writeUTF(itemNum);
			System.out.print("Please enter your credit card number: ");
			creditCardNumber = userInput.readLine();
			
			encryptInfo(itemNum, creditCardNumber, dout);
			String bankResponse;
			while((bankResponse = din.readUTF()) != null){
				//System.out.println(bankResponse);
				if(bankResponse.equals("ok")){
					System.out.println("We will process your order soon");
	//				client.close();
					break;
				}
				else if(bankResponse.equals("error")){
					System.out.println("Wrong credit card number");
	//				client.close();
					break;
				}
			}
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public void run(){
		try{
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			DataInputStream din = new DataInputStream(client.getInputStream());
			DataOutputStream dout = new DataOutputStream(client.getOutputStream());
			verifyAccount(userInput, din, dout);
			selectItem(userInput, din, dout);
			client.close();
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public void checkArgs(String[] args){
		if(args.length != 2){
			System.err.println("Usage: java Customer <purchasing-system-domain> <purchasing-system-port>");
			System.exit(1);
		}
		psystemDomain = args[0];
		//if(!psystemDomain.equals("localhost")){
		//	System.err.println("psystem domain name must be 'localhost'");
		//	System.exit(1);
		//}
		
		psystemPort = Integer.parseInt(args[1]);
		if(psystemPort != 9844){
			System.err.println("psystemPort # must be 9844");
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws Exception{
		Customer client = new Customer();
		client.checkArgs(args);
		client.connect();
		client.run();
	}
}


