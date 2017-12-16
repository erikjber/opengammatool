package com.gammascout.usb;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

/**
 * Opens a port and determines the version. Currently two versions are
 * supported: Version 1 and version 2.
 * 
 * @author Erik Berglund
 *
 */
public class ProtocolVersionDetector
{
	private String portName;

	public enum ProtocolVersion
	{
		VERSION1, VERSION2;
	}

	/**
	 * @throws SerialPortException
	 * 
	 */
	public ProtocolVersionDetector(String portname)
	{
		this.portName = portname;
	}

	public ProtocolVersion getVersion() throws SerialPortException
	{
		ProtocolVersion result = null;
		//try version 1
		SerialPort serial = new SerialPort(portName);
		serial.openPort();
		serial.setParams(2400, 7, 1, SerialPort.PARITY_EVEN);
		serial.writeString("v");
		result = innerTest(serial);
		if(result == null)
		{
			//Didn't identify, try again with Version 2 settings.
			serial = new SerialPort(portName);
			serial.openPort();
			serial.setParams(9600, 7, 1, SerialPort.PARITY_EVEN);
			serial.writeString("v");
			result = innerTest(serial);
		}
		
		return result;
	}
	
	/**
	 * Read the response from the remote end, see if we can identify it.
	 * Return null if we can't 
	 */
	private ProtocolVersion innerTest(SerialPort serial) throws SerialPortException
	{
		ProtocolVersion result = null;
		try
		{
			String prelude = serial.readString(10, 1000);
			System.out.println("Read \""+prelude+"\"");
			if(prelude.startsWith("\r\n Vers"))
			{
				result = ProtocolVersion.VERSION1;
			}
			else if(prelude.startsWith("\r\nVers") || prelude.startsWith("\r\nStand"))
			{
				//no space between newline and "Version" indicates version 2.
				result = ProtocolVersion.VERSION2;
			}
		}
		catch (SerialPortTimeoutException e)
		{
			System.out.println("Timed out waiting for response.");
			e.printStackTrace();
		}
		finally
		{
			String remainder = serial.readString();
			System.out.println("Read \""+remainder+"\"");
			serial.closePort();
		}
		return result;
	}

}
