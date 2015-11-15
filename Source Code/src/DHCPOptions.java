import java.util.Hashtable;

/**
 * This class represents a hash table of options for a DHCP message. 
 * Its purpose is to make adding, removing and changing the options easily.
 * @author Tanushree & Mrunal
 * @version 1.0
 */


public class DHCPOptions 
{
	private byte[] magicCookie;//Holds the values 0x63,0x82,0x53,0x63 known as Magic Cookie.This the first 4 octets of C-DHCP options field.
	private Hashtable<Integer,byte[]> options;//Hash Table to store options
	private byte endOp;//Holds the value 0xff. Indicates the end of options field.
	
	/*Constructor*/
	public DHCPOptions() 
	{
		magicCookie = new byte[]{(byte)0x63,(byte)0x82,(byte)0x53,(byte)0x63};
		endOp = (byte)0xff;
		 options = new Hashtable<Integer, byte[]>();
	}
	
	/**
	* getOption method gives the data stores with a particular option ID.
	* @param optionID.
	* @return value corresponding to optionID
	*/
	public byte[] getOption(int optionID) 
	{
		return options.get(optionID);
	}
	
	
	/**
	* setOption method inserts a option into the options Hash Table using optionID as key and option as value.
	* @param optionID , option
	*/
	public void setOption(int optionID, byte[] option) 
	{
		options.put(optionID, option);
	}
	
	
	/**
	* getOptionData returns the option data excluding the option ID and option length in bytes.
	* @param optionID option identifier.
	* @return optionData
	*/
	public byte[] getOptionData(int optionID) 
	{
		byte[] option = options.get(optionID);
		byte[] optionData = new byte[option.length-2];
		for (int i=0; i < optionData.length; i++)  
			optionData[i] = option[2+i];
		
		return optionData;
	}
	
	/**
	* setOptionData sets the option data using optionID as a key.
	* @param optionID,optionData
	*/
	public void setOptionData(int optionID, byte[] optionData) 
	{
		byte[] option = new byte[2+optionData.length];
		option[0] = (byte) optionID;
		option[1] = (byte) optionData.length;
		for (int i=0; i < optionData.length; i++) 
		{
			option[2+i] = optionData[i];
		}
		options.put(optionID, option);
	}
	
	/**
	* printOption prints the option data corresponding to an option ID.
	* @param optionID
	*/
	public void printOption (int optionID) 
	{
		String output = new String("");
		if (options.get(optionID) != null) 
		{
			byte[] option = options.get(optionID);
			for (int i=0; i < option.length; i++) 
			{
				output += option[i] + (i == option.length-1 ? "" : ","); 
			}
		} 
		else 
		{
			output = "<Empty>";
		}
		System.out.println(output);
	}
	
	/**
	* printOptions() prints all the options stored in the hash table.
	*/
	public void printOptions () 
	{
		for (byte[] option : options.values()) 
		{
			printOption(option[0]);
		}
	}
	
	
	/**
	* externalize() converts an options object to a byte array.
	*/
	public byte[] externalize() 
	{
		
		//get size
		int totalBytes = 5;//Magic cookie + end option
		for (byte[] option : this.options.values()) 
		{
			totalBytes += option.length;
		}
		
		byte[] options = new byte[totalBytes];
		
		//copy bytes
		int bytes = 4;
		for(int i=0;i<4;i++) options[i] = magicCookie[i];
		for (byte[] option : this.options.values()) 
		{
			for (int i=0; i < option.length; i++) 
			{
				options[bytes+i] = option[i];
			}
			bytes += option.length;
		}
		options[bytes] = endOp;
		return options;
	}
	
	/**
	* toString() overrides the toString() function of Object class.
	*/
	public String toString() 
	{
		String str = new String();
		
		str += "Magic Cookie: " + DHCPMessage.byteArrayToHex(magicCookie)+ "\n";
		str += "Options: \n";
		for (byte[] option : options.values()) 
		{
			String output = new String("");
			if (option != null) 
			{
				
				for (int i=0; i < option.length; i++) 
				{
					output += option[i] + (i == option.length-1 ? "" : ","); 
				}
			} 
			else 
			{
				output += "<Empty>";
			}
			str += output + "\n";
		}
		str+= "End Option: " + DHCPMessage.byteArrayToHex(new byte[]{endOp})+"\n";
		
		return str;
	}
	
}
