import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

/**
 * This class represents a C-DHCP client. 
 * @author Tanushree & Mrunal
 * @version 1.0
 */
public class DHCPClient 
{
	private static final int MAX_BUFFER_SIZE = 1024; 
	private int listenPort =  1068;
	private String serverIP = "169.254.173.255";
	private int serverPort =  1067;
	private DHCPMessage msgTest;
	private byte[] msg;
	private static String CMD;
	private static byte[] clientMAC = new byte[4];
	
	/*Constructor*/
	public DHCPClient() throws IOException 
	{
		System.out.println("Connecting to DHCPServer at " + serverIP + " on port " + serverPort + "...");
		DatagramSocket socket = null;
		clientMAC = getMacAddress();
		
		try {
			
			socket = new DatagramSocket(listenPort);  
			
			msg = new byte[MAX_BUFFER_SIZE];
			msgTest = new DHCPMessage();
			
			getUserInput();
			
			System.out.println(msgTest);
			int length = msg.length;
			
			//Sending DHCP discover message from client side
			DatagramPacket p = new DatagramPacket(msg, length, InetAddress.getByName(serverIP), serverPort);
			socket.send(p); 
			System.out.println("Connection Established Successfully!");
			System.out.println("DHCP discover: " + Arrays.toString(p.getData()));
			
			
			//DHCP offer received from server
			byte[] data = new byte[64];
			DatagramPacket offer = new DatagramPacket(data, length);
			
			while(true)
			{
				socket.receive(offer);
			if(compareMac(offer.getData()) == 1)//Check if the offer packet recieved is meant for the client by comparing hardware address of client and message packet
			{
				if(offer.getData()[0] == (byte)2)//DHCP ack recieved in response to DHCPRELEASE
				{
					System.out.println("DHCP ack: " + Arrays.toString(offer.getData()));
					CMD = "netsh int ip set address name = \"Wireless Network Connection\" source = static addr = 1.0.0.0 mask = 255.255.255.0";
					try {
					Runtime.getRuntime().exec(CMD);
					} catch (IOException e2) {
					e2.printStackTrace();
					}
				
					File file = new File("parameters.txt");//removing the file storing parameters after the allocated parameters are released.
					file.delete();
					System.exit(0);
				}
			
				if( offer.getData()[36] == 6)//DHCPNACK is received from C-DHCP Sever.
				{
					System.out.println("DHCP Nack: " + Arrays.toString(offer.getData()));
					System.exit(0);
				}
			
			
				System.out.println("DHCP offer: " + Arrays.toString(offer.getData()));
			
			
				//Sending DHCP request from client side
				byte[] serverIP = new byte[4];
				for(int i = 0;i<4;i++)serverIP[i] = offer.getData()[20+i];
			
				InetAddress s_address = InetAddress.getByAddress(serverIP);
				offer.setAddress(s_address);//DHCPREQUEST message is unicasted to the C-DHCP server using the address in the siaddr field of DHCPOFFER.
				
				offer.getData()[0] = (byte) (offer.getData()[0] -1);//BOOTREPLY
				offer.getData()[10] = 0;//Broadcast bit set to 0;
				offer.getData()[36]= (byte) (3);//DHCP message type set to DHCPREQUEST.
				
				socket.send(offer);
				System.out.println("DHCP request: " + Arrays.toString(offer.getData()));
				break;
			}
				
		}
			while(true)
			{
				socket.receive(offer);
			
			
				if(compareMac(offer.getData())==1)//Check if the ack packet recieved is meant for the client by comparing hardware address of client and message packet
				{
					System.out.println("DHCP Ack: " + Arrays.toString(offer.getData()));
				
					/*Write allocated parameters to a file*/
					FileWriter fw = new FileWriter("parameters.txt"); 
					fw.write((offer.getData()[16]& 0xFF)+"."+(offer.getData()[17]& 0xFF)+"."+(offer.getData()[18]& 0xFF)+"."+(offer.getData()[19]& 0xFF)+"\n");
					fw.write((offer.getData()[20]& 0xFF)+"."+(offer.getData()[21]& 0xFF)+"."+(offer.getData()[22]& 0xFF)+"."+(offer.getData()[23]& 0xFF));
					fw.close();
			
			
					BufferedReader br = new BufferedReader(new FileReader("parameters.txt"));
					String c_ip = br.readLine();
					String subnet = (offer.getData()[39]& 0xFF)+"."+(offer.getData()[40]& 0xFF)+"."+(offer.getData()[41]& 0xFF)+"."+(offer.getData()[42]& 0xFF);
					br.close();
			
					if(offer.getData()[0] ==6)//client requested only for IP.
						CMD = "netsh int ip set address name = \"Wireless Network Connection\" source = static addr = "+c_ip+" mask = 255.255.255.0";
					else
						CMD = "netsh int ip set address name = \"Wireless Network Connection\" source = static addr = "+c_ip+" mask = "+subnet;
				
					Runtime.getRuntime().exec(CMD);
				}
				break;
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

	public static void main(String[] args) throws IOException
	{
		
		 new DHCPClient();
		

	}

	/**
	* getMacAddress method retrieves the hardware address of the wireless network interface.
	* @exception IOException On input error.
	* @see IOException
	*/
	public static byte[] getMacAddress() throws IOException
	{
			byte[] mac = null;
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			for(NetworkInterface ni: Collections.list(networkInterfaces))
			{
				String s1=ni.getDisplayName().toUpperCase();
				try{
					if(s1.contains("WIRELESS"))
					{
						mac =ni.getHardwareAddress();
						break;
					}
				}
				catch(NullPointerException e){}
			}
		   return(mac);
	}
	 
	/**
	* printMacAddress method retrieves the hardware address of the wireless network interface.
	*/	
	public static void printMacAddress() 
	{
		
			byte[] mac;
			try {
				mac = getMacAddress();
				for (int i = 0; i < mac.length; i++) 
				{
					System.out.format("%02X%s", mac[i], (i < mac.length - 1) ? "-"	: "");
				}
			} catch (IOException e) {
					e.printStackTrace();
			}
		}
	
	

	/**
	* getUserInput(): get the parameters needed by the client as console input
	* @exception IOException On input error.
	* @see IOException
	*/
	private void getUserInput() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter the type of message to be sent: \n A for DHCPDISCOVER \n B for DHCPRELEASE ");
		String clientOp = br.readLine();
		
		if(clientOp.compareToIgnoreCase("A") == 0)
		{
			System.out.println("1 for IP Request \n 2 for Subnet Request \n 3 for IP and Subnet Request");
			msg =msgTest.discoverMsg(getMacAddress(),br.readLine());
		}
		else if(clientOp.compareToIgnoreCase("B") == 0)
		{
			msg = msgTest.releaseMsg(getMacAddress());
		}
		else
		{
			System.out.println("Wrong option given");
			System.exit(0);
		}
		br.close();
	}
	
	/**
	* compareMac(): compares hardware address of client and chaddr field of DHCP message recieved by the client.
	* @param p : chaddr in DHCP message
	* @return 1 if addresses match, 0 otherwise
	*/
	private static int compareMac(byte[] p)
	{
		for(int i =0;i<6;i++)
		{
			if(clientMAC[i] != p[24+i])
			{
				return 0;
			}
			
		}
		return 1;
	}

}
