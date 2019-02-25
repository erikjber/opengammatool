/*******************************************************************************
 *  
 * Copyright (c) 2009, 2019 Erik Berglund.
 *    
 *       This file is part of Conserve.
 *   
 *       Conserve is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Affero General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *   
 *       Conserve is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Affero General Public License for more details.
 *   
 *       You should have received a copy of the GNU Affero General Public License
 *       along with Conserve.  If not, see <https://www.gnu.org/licenses/agpl.html>.
 *       
 *******************************************************************************/
package com.gammascout.usb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.gammascout.MainWindow;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Handles the connection to the GammaScout, encoding commands and decoding log
 * data. GammaScoutListeners can be connected to this object to receive log data
 * when it is decoded.
 * 
 * @author Erik Berglund
 * 
 */
public abstract class GammaScoutConnectorBase implements Runnable
{
	private static final int DEFAULT_TIMEOUT = 2000;

	private List<GammaScoutListener> listeners = new ArrayList<>();
	protected SerialPort serial;
	private boolean running = true;
	protected List<String> buffer = new ArrayList<>();
	private Date deviceTime;
	protected Integer bytesUsed;
	protected Integer serialNumber;
	private String version;
	private long deviceTimeUpdatedAt;
	protected List<String> lineBuffer = new ArrayList<>();
	protected List<Reading> readings;
	protected int totalBytesRead;
	protected String temporaryBuffer;
	protected boolean connected = false;

	/**
	 * Create a new connector to the named serial port
	 * 
	 * @param portname
	 *            the name of the serial port, e.g. /dev/ttyUSB0 or COM1
	 * @throws SerialPortException
	 */
	public GammaScoutConnectorBase(String portname) throws SerialPortException
	{
		serial = new SerialPort(portname);
		serial.openPort();
		System.out.println("Connecting to \"" + portname + "\"");
	}

	protected void updateInfo() throws SerialPortException
	{
		serial.writeString("v");
		waitForString("\r\n");
		waitForBuffer();
		synchronized (buffer)
		{
			if (!buffer.isEmpty())
			{
				String lin = buffer.get(0).trim();
				buffer.remove(0);
				if (lin.startsWith("Version"))
				{
					String[] parts = lin.split(" ");
					// parse the version
					version = parts[1];

					// if the protocol version is 2, there will be more info.
					if (parts.length >= 6)
					{
						// parse the serial number
						serialNumber = Integer.parseInt(parts[2]);
						// parse the amount of used space
						bytesUsed = Integer.parseInt(parts[3], 16);
						// parse the date and time
						String dateTime = parts[4] + " " + parts[5];
						DateFormat getDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
						getDateFormat.setTimeZone(Tools.UTC_TIMEZONE);
						try
						{
							deviceTime = getDateFormat.parse(dateTime);
							deviceTimeUpdatedAt = System.currentTimeMillis();
						}
						catch (ParseException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Get the current time and date on the device.
	 * 
	 * @return
	 */
	public String getDeviceDateTime()
	{
		if(deviceTime == null)
		{
			return "N/A";
		}
		else
		{
			Date res = new Date(deviceTime.getTime() + System.currentTimeMillis() - deviceTimeUpdatedAt);
			return MainWindow.DATE_FORMAT.format(res);
		}
	}

	/**
	 * Get the number of bytes of memory used.
	 * 
	 * @return
	 */
	public Integer getBytesUsed()
	{
		return bytesUsed;
	}

	/**
	 * Get the amount of memory used. 0 means the memory is empty, 1 means the
	 * memory is full. Multiply by 100 to get percentage.
	 * 
	 * @return
	 */
	public double getMemoryUsed()
	{
		return getBytesUsed() / 65280.0;
	}

	/**
	 * Get the device serial number.
	 * 
	 * @return
	 */
	public String getSerialNumber()
	{
		if(serialNumber == null)
		{
			return "N/A";
		}
		else
		{
			return Integer.toString(serialNumber);
		}
	}

	/**
	 * Get the device firmware version.
	 * 
	 * @return
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * Wait until the given string appears or the timeout happens.
	 * 
	 * @param string
	 */
	protected void waitForString(String string)
	{
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < DEFAULT_TIMEOUT)
		{
			synchronized (buffer)
			{
				while (!buffer.isEmpty())
				{
					String line = buffer.get(0);
					buffer.remove(0);
					if (line.equals(string))
					{
						return;
					}
				}
			}
			Tools.sleep(50);
		}
		Exception e = new Exception("Timed out waiting for string \"" + string + "\".");
		e.printStackTrace();
	}

	/**
	 * Wait for any data to appear in the input queue, or the timeout.
	 * 
	 */
	protected void waitForBuffer()
	{

		long start = System.currentTimeMillis();
		while (buffer.isEmpty())
		{
			Tools.sleep(50);
			if (System.currentTimeMillis() - start > DEFAULT_TIMEOUT)
			{
				Exception e = new Exception("Timed out waiting for buffer.");
				e.printStackTrace();
				return;
			}
		}
	}

	public void addListener(GammaScoutListener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(GammaScoutListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Get the contents of the log. This method will also fire events to any
	 * registered listeners.
	 */
	public abstract List<Reading> getLog() throws Exception;

	/**
	 * @param r
	 */
	protected void announceReading(Reading r)
	{
		for (GammaScoutListener l : listeners)
		{
			l.receiveReading(r);
		}
	}

	/**
	 * Decode an impulse count
	 * 
	 * @param first
	 *            first byte of impulse count
	 * @param second
	 *            second byte of impulse count
	 * @return
	 */
	protected long decodeCount(String first, String second)
	{
		int a = Integer.parseInt(first, 16);
		int b = Integer.parseInt(second, 16);
		int value = (a << 8) | b;
		int exponent = (value & 0xfc00) >> 10;
		int mantissa = (value & 0x03ff);
		exponent = (exponent + 1) / 2;
		if (exponent == 0)
		{
			return mantissa;
		}
		else
		{
			return Math.round((mantissa + 1024) * (Math.pow(2, exponent - 1)));
		}
	}

	/**
	 * Check if the required number of bytes are available. The required number
	 * depends on the first byte.
	 * 
	 * @return
	 */
	protected boolean bytesAvailable()
	{
		String first = peek();
		if (first == null)
		{
			return false;
		}
		if (first.equals("f5"))
		{
			// special command
			if (lineBuffer.size() < 2)
			{
				// there are no commands shorter than two bytes
				return false;
			}
			else
			{
				// check the command
				String command = lineBuffer.get(1);
				if (command.equals("ef") && lineBuffer.size() < 7)
				{
					// date/time, but not enough bytes available
					return false;
				}
				else if (command.equals("ee") && lineBuffer.size() < 6)
				{
					// gap, but not enough bytes available
					return false;
				}
				else
				{
					// all other commands are just two bytes
					return true;
				}
			}
		}
		else
		{
			// just a count, only two bytes needed
			return lineBuffer.size() > 1;
		}
	}

	/**
	 * Read the available data and put it in the buffer.
	 * 
	 * @throws Exception
	 * 
	 */
	protected void readAvailableDataString() throws Exception
	{
		synchronized (buffer)
		{
			while (!buffer.isEmpty())
			{
				String line = buffer.get(0);
				buffer.remove(0);
				temporaryBuffer += line;
				int x = 0;
				for (; x < temporaryBuffer.length() - 1; x += 2)
				{
					String sub = temporaryBuffer.substring(x, x + 2);
					if (!sub.equals("\r\n"))
					{
						put(sub);
						if (totalBytesRead >= bytesUsed)
						{
							break;
						}
					}
				}
				temporaryBuffer = temporaryBuffer.substring(x);
			}
		}
	}

	/**
	 * Add a line to the buffer.
	 */
	protected abstract void put(String sub) throws Exception;

	/**
	 * Get the first two characters in the line buffer
	 * 
	 * @return
	 */
	protected String peek()
	{
		// get the first two characters in the line buffer
		if (!lineBuffer.isEmpty())
		{
			return lineBuffer.get(0);
		}
		return null;
	}

	/**
	 * Get the first two characters in the line buffer, and remove them from the
	 * buffer.
	 * 
	 * @return
	 */
	protected String pop()
	{
		String res = null;
		if (!lineBuffer.isEmpty())
		{
			res = lineBuffer.get(0);
			lineBuffer.remove(0);
		}
		return res;
	}

	/**
	 * Set the desired time.
	 * 
	 * @param time
	 * @throws SerialPortException
	 */
	public abstract void setClock(Date time) throws SerialPortException;

	/**
	 * Reset the log.
	 * 
	 * @throws SerialPortException
	 */
	public abstract void clearLog() throws SerialPortException;

	public void close()
	{
		connected = false;
		running = false;
		if (serial != null)
		{
			try
			{
				serial.closePort();
			}
			catch (SerialPortException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		String tmpData = "";
		while (running)
		{
			try
			{
				String data = serial.readString();
				if (data != null)
				{
					tmpData += data;
					int breakIndex = tmpData.indexOf("\n");
					while (breakIndex >= 0)
					{
						String line = tmpData.substring(0, breakIndex + 1);
						tmpData = tmpData.substring(breakIndex + 1);
						// we've reached the end of a line, store it
						synchronized (buffer)
						{
							buffer.add(line);
						}
						breakIndex = tmpData.indexOf("\n");
					}
				}
				else
				{
					Tools.sleep(50);
				}
			}
			catch (SerialPortException e)
			{
				running = false;
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public boolean isConnected()
	{
		return connected;
	}
}
