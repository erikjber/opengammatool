package com.gammascout.usb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * @author Erik Berglund
 * 
 */
public class GammaScoutConnector implements Runnable
{
	private static final int DEFAULT_TIMEOUT = 2000;

	private List<GammaScoutListener> listeners = new ArrayList<>();
	private SerialPort serial;
	private boolean running = true;
	private List<String> buffer = new ArrayList<>();
	private Date deviceTime;
	private int bytesUsed;
	private int serialNumber;
	private String version;
	private long deviceTimeUpdatedAt;
	private List<String> lineBuffer = new ArrayList<>();
	private List<Reading> readings;
	private int byteSum;
	private int bytesRead;
	private int totalBytesRead;
	private String temp;
	private boolean connected = false;

	/**
	 * Create a new connector to the named serial port
	 * 
	 * @param portname
	 *            the name of the serial port, e.g. /dev/ttyUSB0 or COM1
	 * @throws SerialPortException
	 */
	public GammaScoutConnector(String portname) throws SerialPortException
	{
		serial = new SerialPort(portname);
		serial.openPort();
		serial.setParams(9600, 7, 1, SerialPort.PARITY_EVEN);
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
		setPcMode(true);
		updateInfo();
		setPcMode(false);
		connected = true;
	}

	private void updateInfo() throws SerialPortException
	{
		serial.writeString("v");
		waitForString("\r\n");
		waitForBuffer();
		if (!buffer.isEmpty())
		{
			String lin = buffer.get(0);
			buffer.remove(0);
			if (lin.startsWith("Version"))
			{
				String[] parts = lin.split(" ");
				// parse the version
				version = parts[1];
				// parse the serial number
				serialNumber = Integer.parseInt(parts[2]);
				// parse the amount of used space
				bytesUsed = Integer.parseInt(parts[3], 16);
				// parse the date and time
				String dateTime = parts[4] + " " + parts[5];
				DateFormat getDateFormat = new SimpleDateFormat(
						"dd.MM.yy HH:mm:ss");
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

	/**
	 * Get the current time and date on the device.
	 * 
	 * @return
	 */
	public Date getDeviceDateTime()
	{
		Date res = new Date(deviceTime.getTime() + System.currentTimeMillis()
				- deviceTimeUpdatedAt);
		return res;
	}

	/**
	 * Get the number of bytes of memory used.
	 * 
	 * @return
	 */
	public int getBytesUsed()
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
	public int getSerialNumber()
	{
		return serialNumber;
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

	private void setPcMode(boolean pcmode) throws SerialPortException
	{
		if (pcmode)
		{
			serial.writeString("P");
			waitForString("\r\n");
			waitForString("PC-Mode gestartet\r\n");
		}
		else
		{
			// normal mode
			serial.writeString("X");
			waitForString("\r\n");
			waitForString("PC-Mode beendet\r\n");
		}
	}

	/**
	 * Wait until the given string appears or the timeout happens.
	 * 
	 * @param string
	 */
	private void waitForString(String string)
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
	}

	/**
	 * Wait for any data to appear in the input queue, or the timeout.
	 * 
	 */
	private void waitForBuffer()
	{

		long start = System.currentTimeMillis();
		while (buffer.isEmpty())
		{
			Tools.sleep(50);
			if (System.currentTimeMillis() - start > DEFAULT_TIMEOUT)
			{
				break;
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
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<Reading> getLog() throws Exception
	{
		setPcMode(true);
		updateInfo();
		// clear the reading data
		lineBuffer.clear();
		byteSum = 0;
		bytesRead = 0;
		totalBytesRead = 0;
		this.temp = "";
		readings = new ArrayList<>();
		serial.writeString("b");
		waitForString("\r\n");
		waitForString("GAMMA-SCOUT Protokoll\r\n");
		int intervalSeconds = 0;
		long currentLogTime = 0;
		boolean overFlow = false;

		// read data lines
		while (true)
		{
			waitForBuffer();
			readAvailableDataString();
			while (bytesAvailable())
			{
				String next = pop();
				if (next.equals("f5"))
				{
					String command = pop();
					switch (command)
					{
						case "ef":
							// set date
							String mm = pop();
							String HH = pop();
							String dd = pop();
							String MM = pop();
							String yy = pop();
							DateFormat logDateFormat = new SimpleDateFormat(
									"mmHHddMMyy");
							logDateFormat.setTimeZone(Tools.UTC_TIMEZONE);
							Date logDate = logDateFormat.parse(mm + HH + dd
									+ MM + yy);
							currentLogTime = logDate.getTime();
							break;
						case "ee":
							// log gap
							String g1 = pop();
							String g2 = pop();
							int gap = Integer.parseInt(g2 + g1, 16);
							long count = decodeCount(pop(), pop());
							currentLogTime += gap * 1000;
							Reading r = new Reading(gap, count, currentLogTime);
							if(gap>0)
							{
								if(overFlow)
								{
									r.setSaturated(true);
									overFlow=false;
								}
								readings.add(r);
								announceReading(r);
							}
							break;
						case "0c":
							intervalSeconds = 10;
							break;
						case "0b":
							intervalSeconds = 30;
							break;
						case "0a":
							intervalSeconds = 60;
							break;
						case "09":
							intervalSeconds = 2 * 60;
							break;
						case "08":
							intervalSeconds = 5 * 60;
							break;
						case "07":
							intervalSeconds = 10 * 60;
							break;
						case "06":
							// 30 minutes
							intervalSeconds = 30 * 60;
							break;
						case "05":
							// one hour
							intervalSeconds = 60 * 60;
							break;
						case "04":
							// two hours
							intervalSeconds = 2 * 60 * 60;
							break;
						case "03":
							// twelve hours
							intervalSeconds = 12 * 60 * 60;
							break;
						case "02":
							// 24 hours
							intervalSeconds = 24 * 60 * 60;
							break;
						case "01":
							// three days
							intervalSeconds = 3 * 24 * 60 * 60;
							break;
						case "00":
							// one week
							intervalSeconds = 7 * 24 * 60 * 60;
							break;
						case "f3":
						case "f4":
							System.out.println("Unknown log command: f5"
									+ command);
							break;
					}
				}
				else if(next.equals("fa"))
				{
					//the next pulse count overflowed
					overFlow=true;
				}
				else
				{
					// decode impulse count
					long count = decodeCount(next, pop());
					// update time
					currentLogTime += intervalSeconds * 1000;
					Reading r = new Reading(intervalSeconds, count,
							currentLogTime);
					if(overFlow)
					{
						r.setSaturated(true);
						overFlow=false;
					}
					readings.add(r);
					announceReading(r);
				}
			}
			if(totalBytesRead>=this.bytesUsed)
			{
				break;
			}
		}

		setPcMode(false);
		return readings;
	}

	/**
	 * @param r
	 */
	private void announceReading(Reading r)
	{		
		for(GammaScoutListener l:listeners)
		{
			l.receiveReading(r);
		}		
	}

	/**
	 * @param command
	 * @param pop
	 * @return
	 */
	private long decodeCount(String command, String pop)
	{
		int a = Integer.parseInt(command, 16);
		int b = Integer.parseInt(pop, 16);
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
	private boolean bytesAvailable()
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
	 * @throws Exception
	 * 
	 */
	private void readAvailableDataString() throws Exception
	{
		synchronized (buffer)
		{
			while (!buffer.isEmpty())
			{
				String line = buffer.get(0);
				buffer.remove(0);
				temp += line;
				int x = 0;
				for (; x < temp.length() - 1; x += 2)
				{
					String sub = temp.substring(x, x + 2);
					if (!sub.equals("\r\n"))
					{
						put(sub);
						if(totalBytesRead>=bytesUsed)
						{
							break;
						}
					}
				}
				temp = temp.substring(x);
			}
		}
	}

	/**
	 * @param sub
	 * @throws Exception
	 */
	private void put(String sub) throws Exception
	{
		if (bytesRead > 0 && bytesRead % 32 == 0)
		{
			int checksum = Integer.parseInt(sub, 16);
			if (byteSum % 256 != checksum)
			{
				throw new Exception("Checksum error: " + (byteSum % 256)
						+ " != " + checksum);
			}
			bytesRead = 0;
			byteSum = 0;
		}
		else
		{
			byteSum += Integer.parseInt(sub, 16);
			bytesRead++;
			totalBytesRead++;
			lineBuffer.add(sub);
		}
	}

	/**
	 * Get the first two characters in the line buffer
	 * 
	 * @return
	 */
	private String peek()
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
	private String pop()
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
	public void setClock(Date time) throws SerialPortException
	{
		setPcMode(true);
		// format the time
		DateFormat setTimeFormat = new SimpleDateFormat("ddMMyyHHmmss");
		setTimeFormat.setTimeZone(Tools.UTC_TIMEZONE);
		String t = "t" + setTimeFormat.format(time);
		for (int x = 0; x < t.length(); x++)
		{
			serial.writeString(t.substring(x, x + 1));
			if (x < t.length() - 1)
			{
				Tools.sleep(500);
			}
			waitForString("\r\n");
			waitForString("Datum und Zeit gestellt\r\n");
		}
		updateInfo();
		setPcMode(false);
	}

	/**
	 * Reset the log.
	 * 
	 * @throws SerialPortException
	 */
	public void clearLog() throws SerialPortException
	{
		setPcMode(true);
		serial.writeString("z");
		waitForString("\r\n");
		waitForString("Protokollspeicher wieder frei\r\n");
		updateInfo();
		setPcMode(false);
	}

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
