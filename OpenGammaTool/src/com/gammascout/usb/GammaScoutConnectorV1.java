package com.gammascout.usb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Implements the old version (Version 1) of the Gammascout protocol.
 * 
 * @author Erik Berglund
 *
 */
public class GammaScoutConnectorV1 extends GammaScoutConnectorBase
{

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyy");
	static
	{
		DATE_FORMAT.setTimeZone(Tools.UTC_TIMEZONE);
	}
	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmm");
	static
	{
		TIME_FORMAT.setTimeZone(Tools.UTC_TIMEZONE);
	}

	/**
	 * @param portname
	 * @throws SerialPortException
	 */
	public GammaScoutConnectorV1(String portname) throws SerialPortException
	{
		super(portname);
		serial.setParams(2400, 7, 1, SerialPort.PARITY_EVEN);
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
		updateInfo();
		connected = true;
	}

	@Override
	public List<Reading> getLog() throws Exception
	{
		// clear the reading data
		lineBuffer.clear();
		totalBytesRead = 0;
		temporaryBuffer = "";
		readings = new ArrayList<>();
		serial.writeString("b");
		waitForString("\r\n");
		waitForString(" GAMMA-SCOUT Protokoll \r\n\r\n");
		int intervalSeconds = 0;
		long currentLogTime = 0;

		// Read serial number
		waitForBuffer();
		synchronized (buffer)
		{
			String line = buffer.remove(0);
			totalBytesRead+=line.length()/2;
			System.out.println("Read: \"" + line+"\"");
			String serialString = line.substring(4,6)+line.substring(2,4)+line.substring(0, 2);
			this.serialNumber = Integer.parseInt(serialString);
		}
		//skip a line
		waitForBuffer();
		String line = buffer.remove(0);
		System.out.println("Read: \"" + line+"\"");
		totalBytesRead+=line.length()/2;
		waitForBuffer();
		// Read the end address
		line = buffer.remove(0);
		System.out.println("Read: \"" + line+"\"");
		totalBytesRead+=line.length()/2;
		String addressString = line.substring(2,4)+line.substring(0, 2);
		bytesUsed = Integer.parseInt(addressString);
		// Skip to address 0x100
		while(totalBytesRead < 0x100)
		{
			waitForBuffer();
			line = buffer.remove(0);
			System.out.println("Read: \"" + line+"\"");
			totalBytesRead+=line.length()/2;			
		}

		// read data lines
		while (true)
		{
			waitForBuffer();
			readAvailableDataString();
			while (bytesAvailable())
			{
				String next = pop();
				switch (next)
				{
					case "fe":
						// set date
						String mm = pop();
						String HH = pop();
						String dd = pop();
						String MM = pop();
						String yy = pop();
						DateFormat logDateFormat = new SimpleDateFormat("mmHHddMMyy");
						logDateFormat.setTimeZone(Tools.UTC_TIMEZONE);
						Date logDate = logDateFormat.parse(mm + HH + dd + MM + yy);
						currentLogTime = logDate.getTime();
						break;
					case "ff":
					{
						String g1 = pop();
						String g2 = pop();
						// Give the number of seconds elapsed
						int gap = Integer.parseInt(g2 + g1, 16) * 60;
						long count = decodeCount(pop(), pop());
						currentLogTime += gap * 1000;
						Reading r = new Reading(gap, count, currentLogTime);
						if (gap > 0)
						{
							readings.add(r);
							announceReading(r);
						}
						break;
					}
					case "f4":
						// one minute
						intervalSeconds = 60;
						break;
					case "f3":
						// 10 minutes
						intervalSeconds = 10 * 60;
						break;
					case "f2":
						// one hour
						intervalSeconds = 60 * 60;
						break;
					case "f1":
						// one day
						intervalSeconds = 24 * 60 * 60;
						break;
					case "f0":
						// Seven days
						intervalSeconds = 7 * 24 * 60 * 60;
						break;
					default:
						if (next.startsWith("f"))
						{
							System.out.println("Got unknown command: \"" + next + "\"");
						}
						else
						{
							// decode impulse count
							long count = decodeCount(next, pop());
							// update time
							currentLogTime += intervalSeconds * 1000;
							Reading r = new Reading(intervalSeconds, count, currentLogTime);
							readings.add(r);
							announceReading(r);
						}
						break;
				}

			}
			if (totalBytesRead >= this.bytesUsed)
			{
				break;
			}
		}
		return readings;
	}

	@Override
	protected void put(String sub) throws Exception
	{
		totalBytesRead++;
		lineBuffer.add(sub);
	}

	@Override
	public void setClock(Date time) throws SerialPortException
	{
		// set the time first
		String timeString = "u" + TIME_FORMAT.format(time);
		serial.writeString(timeString);
		waitForString("\r\n Zeit gestellt \r\n");
		// then set the date

		String dateString = "d" + DATE_FORMAT.format(time);
		serial.writeString(dateString);
		waitForString("\r\n Datum gestellt \r\n");
	}

	@Override
	public void clearLog() throws SerialPortException
	{
		serial.writeString("z");
		waitForString("\r\n");
		waitForString(" Protokollspeicher wieder frei \r\n");
	}

}
