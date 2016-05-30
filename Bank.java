import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import javax.crypto.Cipher;

public class Bank{
	private int bankPort;
	HashMap<Account, String> balances;

	final String PUP_FILE = "Pup.der";
	//final String PUA_FILE = "Pua.der";
	//final String PUT_FILE = "Put.der";
	final String PUB_FILE = "Pub.der";
	
	final String PRB_FILE = "Prb.der";
	
	public void exception(Exception ex){
		ex.printStackTrace();
		System.exit(1);
	}
	
	public String decrypt(PrivateKey key, String ciphertext) throws Exception{
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(ciphertext);
			byte[] utf8 = cipher.doFinal(dec);
			
			return new String(utf8, "UTF8");
	}
	
	public void readBalance(){
		try{
			balances = new HashMap<Account, String>();
			FileReader input = new FileReader("balance");
			BufferedReader br = new BufferedReader(input);
			String name, creditCard, balance, line;
			String[] accountContents;
			while((line = br.readLine()) != null){
				accountContents = line.split(",");
				name = accountContents[0];
				creditCard = accountContents[1];
				balance = accountContents[2];
				Account account = new Account(name, creditCard);
				balances.put(account, balance);
			}
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public boolean verifyCreditCard(String name, String creditCard){
		boolean retVal = false;
		try{
			readBalance();
			for(Map.Entry<Account, String> me: balances.entrySet()){
				if(me.getKey().getName().equals(name)){
					if(me.getKey().getCreditCard().equals(creditCard)){
						retVal = true;
					}
				}
			}
		}
		catch(Exception ex){
			exception(ex);
		}
		return retVal;
	}
	
	public String decryptPublic(PublicKey key, String ciphertext) throws Exception{
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(ciphertext);
			byte[] utf8 = cipher.doFinal(dec);
			
			return new String(utf8, "UTF8");
	}
	
	public void updateBalance(String name, String price){
		try{
			FileReader input = new FileReader("balance");
			BufferedReader br = new BufferedReader(input);
			String line;
			String[] info;
			int balance;
			StringBuilder sb = new StringBuilder();
			
			while((line = br.readLine()) != null){
				if(line.contains(name)){
					info = line.split(",");
					sb.append(info[0] + "," + info[1] + ",");
					//System.out.println("Stringbuilder " + sb.toString());
					balance = Integer.parseInt(info[2]);
					//System.out.println("Old balance " + balance);
					
					balance += Integer.parseInt(price);
					//System.out.println("New balance " + balance);
					sb.append(String.valueOf(balance) + "\n");
				}
				else{
					sb.append(line + "\n");
				}
			}
			File f = new File("balance");
			Writer writer = new FileWriter(f);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(sb.toString());
			bufferedWriter.close();
			
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public void decryptInfo(DataInputStream din, DataOutputStream dout){
		try{
			PublicKeyReader publicKeyReader = new PublicKeyReader();
			PrivateKeyReader privateKeyReader = new PrivateKeyReader();
			
			PublicKey pub = publicKeyReader.get(PUB_FILE);
			PublicKey pup = publicKeyReader.get(PUP_FILE);
			PrivateKey prb = privateKeyReader.get(PRB_FILE);
			
			String name, creditCard, price;
			
			name = din.readUTF();
			name = decrypt(prb, name);
			//System.out.println("Name is " + name);
			
			creditCard = din.readUTF();
			creditCard = decrypt(prb, creditCard);
			//System.out.println("creditcard is " + creditCard);
			
			
			if(verifyCreditCard(name, creditCard)){
				dout.writeUTF("ok");
				price = din.readUTF();
				price = decryptPublic(pup, price);
				//System.out.println("Price is " + price);
				updateBalance(name, price);
			}
			else{
				dout.writeUTF("error");
				//send "error" to psystem
			}
			
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public void run(){
		try{
			while(true){
				ServerSocket bankSocket = new ServerSocket(bankPort);
				System.out.println("Bank server is online");
				Socket pSystemClient = bankSocket.accept();
				System.out.println("PSystem is connected to Bank");

				DataInputStream din = new DataInputStream(pSystemClient.getInputStream());
				DataOutputStream dout = new DataOutputStream(pSystemClient.getOutputStream());
				
				decryptInfo(din, dout);
				bankSocket.close();
				System.out.println("Bank server has closed");
			}		
			
		}
		catch(Exception ex){
			exception(ex);
		}
	}
	
	public void checkArgs(String[] args){
		if(args.length != 1){
			System.err.println("Usage: java Bank 9000");
			System.exit(1);
		}
		bankPort = Integer.parseInt(args[0]);
		if(bankPort != 9000){
			System.err.println("BankPort # must be 9000");
			System.exit(1);
		}
	}	
	
	public static void main(String[] args){
		Bank bank = new Bank();
		bank.checkArgs(args);
		bank.run();
	}
}

class Account{
	private String name;
	private String creditCard;
	
	public Account(String nameIn, String creditCardIn){
		name = nameIn;
		creditCard = creditCardIn;
	}
	
	public String getName(){
		return name;
	}
	
	public String getCreditCard(){
		return creditCard;
	}
}
