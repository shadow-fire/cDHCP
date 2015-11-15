import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class represents a C-DHCP server. 
 * @author Tanushree & Mrunal
 * @version 1.0
 */
public class DHCPServer 
{
	private static final int MAX_BUFFER_SIZE = 1024; 
	private int listenPort = 1067;
	
	private static LinkedList<String> ipPool= new LinkedList<String>();//to maintain a pool of available IP addresses.
	private static HashMap<String,String> ipInfo = new HashMap<String,String>();//to store the paramters allocated to client.
	
	private static DatagramPacket p;
	private static String subnet1,subnet2;//The C-DHCP server is initialized with 2 subnets.
	private static String CMD;
	
	
	/*Constructor*/
	public DHCPServer() 
	{
		//Initialising the C-DHCP server with 2 sub-nets and a maximum of 25 class C IP addresses
		try {
			
			BufferedReader br = new BufferedReader(new FileReader("input.txt"));
			subnet1 = br.readLine();
			subnet2 = br.readLine();
			String line= br.readLine();
			
			while(line != null)
			{
				ipPool.add(line);
				line = br.readLine();
			}
			br.close();
			} 
		catch(FileNotFoundException fe) 
		{
			System.out.println("File not found");
		} 
		catch(IOException ioe) 
		{
			System.out.println("Can‘t read from file");
		}

		//The first ip address in ipPool is assigned to the server.
		String s_ip = ipPool.get(0);
		ipPool.remove(s_ip);
		System.out.println("Server ip: "+s_ip+"\n");
		
		 CMD = "netsh int ip set address name = \"Wireless Network Connection\" source = static addr = "+s_ip+" mask = 255.255.255.0";
		 try {
			 Runtime.getRuntime().exec(CMD);
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		 
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(listenPort);
			p = new DatagramPacket(new byte[MAX_BUFFER_SIZE], 50);
			
			System.out.println("Listening on port " + listenPort + "...");
			while (true) 
			{
				//Server gets DHCP discovery msg
				socket.receive(p);
				
				/*Thread ser = new Thread(){
					public void run(){
						
					}
				};
				ser.start();*/
				
				System.out.println("Connection established from " + p.getAddress());
				System.out.println("\nDHCP Discovery: " + Arrays.toString(p.getData()));
				
				byte op = p.getData()[0];//retrieving opcode from the DHCPDISCOVER message
				
				if(op ==1)System.out.println("\nDHCP release recieved");
				if(op ==5)System.out.println("\nRequest is for IP");
				if(op ==9)System.out.println("\nRequest is for Subnet");
				if(op ==13)System.out.println("\nRequest is for IP and Subnet");
				
				
				byte[] xid = new byte[4];
				for (int i=0; i < 4; i++) xid[i] = p.getData()[4+i];
				
				byte[] mac = new byte[6];
				for (int i=0; i < 6; i++)mac[i]=p.getData()[24+i];
				
				byte[] ip = new byte[4];
				for (int i=0; i < 4; i++) ip[i]=p.getData()[16+i];
				
				if(op == 1)//DHCPRELEASE message sent by the client.
				{
					/* Removing the configuration information from ipInfo Hash Map and adding the ip back to the ipPool*/
					ipInfo.remove(DHCPMessage.byteArrayToHex(xid)+","+ DHCPMessage.byteArrayToHex(mac));
					String ip_free = ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3]; 
					ipPool.add(ip_free);
					
					p.getData()[0] = (byte)(p.getData()[0] + 1);//BOOTREPLY
					
					for (int i=0; i < 4; i++) p.getData()[12+i]=p.getData()[16+i]=0;//setting yiaddr to 0.0.0.0
					
					byte[] s_ip_b = InetAddress.getByName(s_ip).getAddress();
					for (int i=0; i < 4; i++) p.getData()[20+i]= s_ip_b[i];
					
					p.getData()[36] = (byte) 5;//DHCPNACK
					
					socket.send(p);
					System.out.println("DHCP Ack: " + Arrays.toString(p.getData()));
				}
				
				
				if(op == 9)//Client requests for subnet mask.
				{
					p.getData()[0] = (byte)(p.getData()[0] + 1);//BOOTREPLY_S
					p.getData()[36] = (byte)2;//message type = DHCPOFFER
					
					//Set siaddr field of DHCPOFFER message
					byte[] s_ip_b;
					try {
						s_ip_b = InetAddress.getByName(s_ip).getAddress();
						for (int i=0; i < 4; i++) 
							p.getData()[20+i]= s_ip_b[i];
						
					} catch (UnknownHostException e1) {
						
						e1.printStackTrace();
					}
					
					
					int c=0;
					byte[] ciaddr = new byte[4];
					for (int i=0; i < 4; i++) 
					{
						ciaddr[i] = p.getData()[12+i];
						if(p.getData()[12+i] == 0)
							c++;
					}
					
					if(c == 4)//ciaddr == 0.0.0.0 (invalid)
					{
						p.getData()[36] = (byte) 6;//message type = DHCPNACK.
						socket.send(p);
						System.out.println("DHCP Nack: " + Arrays.toString(p.getData()));
					}
					else
					{
						byte[] subnet = new byte[4];
						subnet = getSubnetMask(ciaddr[3]);
						
						int d =0;
						 for(int i=0;i<4;i++)
						 {
							 if(subnet[i] == 0)
								 d++;
						 }
						 
						 System.out.println("Subnet Mask=" + (subnet[0]& 0xFF)+"."+(subnet[1]& 0xFF)+"."+(subnet[2]& 0xFF)+"."+(subnet[3]& 0xFF)+"\n");
						 
						 if(d == 4)//If the client's ip doesnt lie in one of the 2 subnets offered by the server, a DHCPNACK is sent to the client.
						 {
							 p.getData()[36]=(byte)6;
							 socket.send(p);
							 System.out.println("DHCP Nack: " + Arrays.toString(p.getData()));
							 System.exit(0);
							 
						 }
							 
						 
						 for (int i =0;i<4;i++) p.getData()[39+i] = subnet[i];//setting the subnet mask in the options field.
						 socket.send(p);
						 System.out.println("DHCP Offer: " + Arrays.toString(p.getData()));
						 
						 socket.receive(p);
						 System.out.println("DHCP Request: " + Arrays.toString(p.getData()));
						 
						 	p.getData()[0] = (byte) (p.getData()[0]+1);//BOOTPREPLY_S
							p.getData()[10] = (byte) 128;//set broadcast bit to 1.
							p.getData()[36]= (byte) 5;//message type = DHCPACK.
							socket.send(p);
							System.out.println("DHCP Ack: " + Arrays.toString(p.getData()));
					}
					
				}
				if(op == 13 | op == 5)//request for IP address or IP address and subnet mask
				{
					if(ipPool.size() >1)//IP address is available with the server to be allocated to the client.
					{
						//Sending DHCPOFFER to client
						offerMsg(op,s_ip);
						p.setPort(1068);
						socket.send(p);
						System.out.println("DHCP Offer: " + Arrays.toString(p.getData()));
						
						//Receiving DHCPREQUEST from the client
						socket.receive(p);
						System.out.println("DHCP Request: " + Arrays.toString(p.getData()));
						
						
						//Server sending DHCPACK to client
						p.getData()[0] = (byte) (p.getData()[0]+1);//BOOTPREPLY
						p.getData()[10] = (byte) 128;//set broadcast bit to 1.
						p.getData()[36]= (byte) (p.getData()[36]+2);//message type = DHCPACK.
					
						String key = DHCPMessage.byteArrayToHex(xid)+","+ DHCPMessage.byteArrayToHex(mac) ;// key = xid,hardware address.
						
						byte[] ipaddr = new byte[4];
						for (int i=0; i < 4; i++) ipaddr[i]=p.getData()[16+i];
						
						String val = (ipaddr[0] & 0xff)+"."+(ipaddr[1] & 0xff)+"."+(ipaddr[2] & 0xff)+"."+(ipaddr[3] & 0xff); 
						ipInfo.put(key,val);//storing allocated parameters in ipInfo Hash Map.
						
						ipPool.remove(val);//removing ip from free ip pool.
						System.out.println("key: "+key + "\t" + "Value: " + ipInfo.get(key));
						socket.send(p);
						System.out.println("DHCP Ack: " + Arrays.toString(p.getData()));
						
					}
					else{
						/*DHCPNACK sent to client if no free ip is available*/
						p.getData()[0] = (byte)(p.getData()[0]+1);
						p.getData()[36] = (byte)6;
						socket.send(p);
						System.out.println("DHCP Nack: " + Arrays.toString(p.getData()));
						
					}
				}
				
			}
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	
	/**
	* offerMsg() : Offer message sent by server.
	* @param op,s_ip: opcode and server ip address 
	* @exception Unknown Host Exception
	* @see UnknownHostException
	*/
	public static void offerMsg(byte op,String s_ip) throws UnknownHostException
	{
		String ip;
		
		
		//Set siaddr field of DHCPOFFER message
		byte[] s_ip_b;
		try {
			s_ip_b = InetAddress.getByName(s_ip).getAddress();
			for (int i=0; i < 4; i++) 
				p.getData()[20+i]= s_ip_b[i];
			
		} catch (UnknownHostException e1) {
			
			e1.printStackTrace();
		}
		
		//Set yiaddr field of DHCPOFFER message
		ip = ipPool.get(0);
		ipPool.remove(ip);
		ipPool.addLast(ip);
		
		
		System.out.println("\nIP offered:"+ip+"\n");
		byte[] ip_b;
		try {
			ip_b = InetAddress.getByName(ip).getAddress();
			for (int i=0; i < 4; i++) 
				p.getData()[16+i]= ip_b[i];
		
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		}
		
		if(op == 13)//Client has requested for both ip and subnet mask.
		{
			byte[] subnet = new byte[4];
			subnet = getSubnetMask(p.getData()[19]);
			

			 System.out.println("Subnet Mask=" + (subnet[0]& 0xFF)+"."+(subnet[1]& 0xFF)+"."+(subnet[2]& 0xFF)+"."+(subnet[3]& 0xFF)+"\n");
			 
			 for (int i =0;i<4;i++) p.getData()[39+i] = subnet[i];
		}
		
		if(op == 13 | op ==9)//Subnet request
		{
			p.getData()[43] = (byte) 51;
			p.getData()[44] = (byte) 4;
		
			for(int i=0;i<4;i++)p.getData()[45+i] = (byte) 0xff;
		
			p.getData()[49] = (byte) 0xff;
		}
		else
		{
			p.getData()[37] = (byte) 51;
			p.getData()[38] = (byte) 4;
		
			for(int i=0;i<4;i++)p.getData()[39+i] = (byte) 0xff;
		
			p.getData()[43] = (byte) 0xff;
		}
		
		p.getData()[0]= (byte) (op+1);//change op to BOOTPREPLY
		p.getData()[36]= (byte) (2);//change message type to DHCPOFFER
	}
	
	
	/**
	* getSubnetMask method returns the subnet of the client ip address.
	* @param ciaddr last octet of client ip address.
	* @return subnet mask in byte array form.
	*/
	public static byte[] getSubnetMask(byte ciaddr) throws UnknownHostException
	{
		System.out.println("\n"+subnet1 + "\t"+subnet2+"\n");
		
		String[] parts1 = subnet1.split("/");
	    String ip1 = parts1[0];
	    int prefix1;
	    if (parts1.length < 2)  prefix1 = 0;
	    else prefix1 = Integer.parseInt(parts1[1]);
	    
	    int mask1 = 0xffffffff << (32 - prefix1);
	    int value1 = mask1;
	    byte[] bytes1 = new byte[]{ 
	            (byte)(value1 >>> 24), (byte)(value1 >> 16 & 0xff), (byte)(value1 >> 8 & 0xff), (byte)(value1 & 0xff) };

	   
	    
	    
	    String[] parts2 = subnet2.split("/");
	    String ip2 = parts2[0];
	    int prefix2;
	    if (parts2.length < 2)  prefix2 = 0;
	    else prefix2 = Integer.parseInt(parts2[1]);
	   int mask2 = 0xffffffff << (32 - prefix2);
	    int value2 = mask2;
	    byte[] bytes2 = new byte[]{ 
	            (byte)(value2 >>> 24), (byte)(value2 >> 16 & 0xff), (byte)(value2 >> 8 & 0xff), (byte)(value2 & 0xff) };
		
	    
	    InetAddress netAddr1 = InetAddress.getByAddress(bytes1);
		System.out.println("Subnet Mask1=" + netAddr1.getHostAddress()+"\n");
		 
		 InetAddress netAddr2 = InetAddress.getByAddress(bytes2);
		 System.out.println("Subnet Mask2=" + netAddr2.getHostAddress()+"\n");
	   
	    byte lastOct1 = InetAddress.getByName(ip1).getAddress()[3];
	    byte lastOct2 = InetAddress.getByName(ip2).getAddress()[3];
	    
	    String s1 = toBinary(new byte[]{lastOct1});
	    String s2 = toBinary(new byte[]{lastOct2});
	   
	    String p1 = s1.substring(0,prefix1-24);
	    String p2 = s2.substring(0,prefix2-24);
	    
	    String c = toBinary(new byte[]{ciaddr});
	    
	   
	    if(c.startsWith(p1))
	       return (bytes1);
	    else if(c.startsWith(p2))
	    	return (bytes2);
	    else 
	    	return (new byte[]{0,0,0,0});
	}    
	    
	/*Converts byte to binary string*/	
	public static String toBinary( byte[] bytes )
	{
	    StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
	    for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
	        sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
	    return sb.toString();
	}

	public static void main(String[] args) 
	{
		new DHCPServer();
	
	}
}
