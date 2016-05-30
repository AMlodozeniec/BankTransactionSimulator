import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import javax.crypto.Cipher;

public class Psystem{
	int pSystemPort;
	String bankDomain;
	int bankIp;
	int bankPort; 
	HashMap<String, String> accounts;
	HashMap<String, Item> items;
	
	final String PUP_FILE = "Pup.der";
	//final String PUB_FILE = "Pub.der";
	final String PUA_FILE = "Pua.der";
	final String PUT_FILE = "Put.der";
	
	final String PRP_FILE = "Prp.der";
	
	public Psystem(){
	}
	
	public void run(){
		try{
			while(true){
				ServerSocket psystemSocket = new ServerSocket(pSystemPort);
				System.out.println("pSystemServer is running");
				Socket clientSocket = psystemSocket.accept();
				System.out.println("Client is connected to pSystem");
			
				readResponse(psystemSocket,clientSocket);
				System.out.println("pSystemServer has closed");
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public void readAccounts(){
		try{
			accounts = new HashMap<String, String>();
			FileReader input = new FileReader("password");
			BufferedReader br = new BufferedReader(input);
			String line, id, password;
			while((line = br.readLine()) != null){
				if(line.contains("ID: ")){
					id = line.substring(4,line.length());
					line = br.readLine();
					password = line.substring(10,line.length());
					accounts.put(id,password);
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public void readItems(){
		try{
			items = new HashMap<String, Item>();
			
			FileReader f = new FileReader("item");
			BufferedReader br = new BufferedReader(f);
			String line;
			String[] itemContents;
			String itemNum, name, price;
			while((line = br.readLine()) != null){
				itemContents = line.split(",");
				itemNum = itemContents[0];
				name = itemContents[1];
				price = itemContents[2].substring(1,itemContents[2].length());
				Item item = new Item(name,price);
				items.put(itemNum, item);
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public void sendItemContents(DataOutputStream dout){
		try{
			FileReader f = new FileReader("item");
			BufferedReader br = new BufferedReader(f);
			String line;
			while((line = br.readLine()) != null){
				dout.writeUTF(line);
			}
			dout.writeUTF("\n");
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);	
		}
	}
	
	public boolean verifyAccount(DataInputStream din, DataOutputStream dout){
		boolean retVal = false;
		try{
			while(!retVal){
				String id, passwd;
				id = din.readUTF();
				passwd = din.readUTF();
				for(Map.Entry<String, String> me: accounts.entrySet()){
					if(me.getKey().equals(id)){
						if(me.getValue().equals(passwd)){
								dout.writeUTF("correct");
								retVal = true;
								sendItemContents(dout);
						}
						else{
							dout.writeUTF("error");
						}
						break;
					}		
						
				}
			}
		}
		
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
		return retVal;
	}
	
	public void exception(Exception ex){
		ex.printStackTrace();
		System.exit(1);
	}


	public String encrypt(PrivateKey key, String str) throws Exception{
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");   
    		cipher.init(Cipher.ENCRYPT_MODE, key);  
			byte[] utf8 = str.getBytes("UTF8");
			byte[] enc = cipher.doFinal(utf8);
			
			return new sun.misc.BASE64Encoder().encode(enc);
	}


    public String decrypt(PrivateKey key, String ciphertext) throws Exception{
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(ciphertext);
			byte[] utf8 = cipher.doFinal(dec);
			
			return new String(utf8, "UTF8");
    }
	
	public String readEncryptedInfo(DataInputStream din) throws Exception{
		StringBuilder sb = new StringBuilder();
		String line;
		while(!(line = din.readUTF()).equals("done sending")){
			sb.append(line);
		}
		return sb.toString();
	}	
	
	public void verifyDigitalSignature(String ds, byte[] data, PublicKey
key) throws Exception{
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initVerify(key);
		sig.update(data);
		
		sig.verify(new sun.misc.BASE64Decoder().decodeBuffer(ds));
	} 
	
	public String findItem(String itemNum){
		String price = null;
		try{
			for(Map.Entry<String, Item> me: items.entrySet()){
				if(me.getKey().equals(itemNum)){
					price = me.getValue().getPrice();
				}
			}
		}
		catch(Exception ex){
			exception(ex);
		}
		return price;
	}
	
	public void decryptTransaction(DataInputStream din, DataOutputStream dout){
		try{
			PrivateKeyReader privateKeyReader = new PrivateKeyReader();
			PublicKeyReader publicKeyReader = new PublicKeyReader();
			
			PrivateKey prp = privateKeyReader.get(PRP_FILE);
			
			PublicKey pua = publicKeyReader.get(PUA_FILE);
			PublicKey put = publicKeyReader.get(PUT_FILE);
			
			String itemNum, name, creditCardNum, ds;
			
			itemNum = din.readUTF();
			byte[] data = itemNum.getBytes("UTF8");
		//	System.out.println("item received");
			
			ds = din.readUTF();
		//	System.out.println("ds received");
			for(Map.Entry<String, String> me: accounts.entrySet()){
					if(me.getKey().equals("alice")){
						verifyDigitalSignature(ds, data, pua);
					}
					else{
						verifyDigitalSignature(ds, data, put);
					}	
			}
	
			//System.out.println("DS: verified");
			
			itemNum = decrypt(prp, itemNum);
			//System.out.println("itemNum " + itemNum);
			
			name = din.readUTF();		
			//name = decrypt(prp, name);
			//System.out.println("Received name");

			
			creditCardNum = din.readUTF();
			//creditCardNum = decrypt(prb, creditCardNum);
			//System.out.println("Received creditCardNum");
			
			String price = findItem(itemNum);
			//System.out.println("Price is " + price);
			
			String encryptedPrice = encrypt(prp, price);
			connectToBank(name, creditCardNum, encryptedPrice, dout);
			
		}
		catch(Exception ex){
			exception(ex);
		}	
	}
	
	public String connectToBank(String name, String creditCard, String price,
DataOutputStream dout){
		String bankResponse = "";
		try{
			System.out.println("Attempting to connect to Bank");
			Socket bankClient = new Socket(bankDomain, bankPort);
			System.out.println("Successfully connected to bank");
			
			DataInputStream bankDin = new DataInputStream(bankClient.getInputStream());
			DataOutputStream bankDout = new DataOutputStream(bankClient.getOutputStream());
			
			bankDout.writeUTF(name);
			bankDout.writeUTF(creditCard);
			bankDout.writeUTF(price);
				
			bankResponse = bankDin.readUTF();
			//System.out.println("Bankresponse is " + bankResponse);
			dout.writeUTF(bankResponse);
		}
		catch(Exception ex){
			exception(ex);
		}
		return bankResponse;
	}
	
	public void readResponse(ServerSocket server, Socket client){	
		try{
			DataInputStream din = new DataInputStream(client.getInputStream());
			DataOutputStream dout= new DataOutputStream(client.getOutputStream());
			verifyAccount(din, dout);
			//System.out.println("Verified account");
			sendItemContents(dout);
			decryptTransaction(din, dout);
			server.close();
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public void checkArgs(String[] args){
		if(args.length != 3){
			System.err.println("Usage: java Psystem <purchasing-system-port> <bank-ip> <bank-port>");
			System.exit(1);
		}
		pSystemPort = Integer.parseInt(args[0]);
		if(pSystemPort != 9844){
			System.err.println("PsystemPort # must be 9844");
			System.exit(1);
		}
		bankDomain = args[1]; //localhost
		
		bankPort = Integer.parseInt(args[2]);
		if(bankPort != 9000){
			System.err.println("BankPort # must be 9000");
			System.exit(1);
		}
	}
	
	
	public static void main(String[] args){
		Psystem pSystem = new Psystem();
		pSystem.checkArgs(args);
		pSystem.readAccounts();
		pSystem.readItems();
		pSystem.run();
	}
}

class Item{
	private String name;
	private String price;
	
	public Item(String nameIn, String priceIn){
		name = nameIn;
		price = priceIn;
	}
		
	public String getName(){
		return name;
	}
	
	public String getPrice(){
		return price;
	}
}
