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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Implements the new version (Version 2) of the gammascout protocol.
 * 
 * @author Erik Berglund
 *
 */
public class GammaScoutConnectorV2 extends GammaScoutConnectorBase
{
	protected int byteSum;
	protected int bytesRead;

	/**
	 * @param portname
	 * @throws SerialPortException
	 */
	public GammaScoutConnectorV2(String portname) throws SerialPortException
	{
		super(portname);
		serial.setParams(9600, 7, 1, SerialPort.PARITY_EVEN);
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
		setPcMode(true);
		updateInfo();
		setPcMode(false);
		connected = true;
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
		}
		waitForString("\r\n");
		waitForString("Datum und Zeit gestellt\r\n");
		updateInfo();
		setPcMode(false);
	}

	/**
	 * Get the contents of the log. This method will also fire events to any
	 * registered listeners.
	 * 
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
		this.temporaryBuffer = "";
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
							DateFormat logDateFormat = new SimpleDateFormat("mmHHddMMyy");
							logDateFormat.setTimeZone(Tools.UTC_TIMEZONE);
							Date logDate = logDateFormat.parse(mm + HH + dd + MM + yy);
							currentLogTime = logDate.getTime();
							break;
						case "ee":
							// log gap
							String g1 = pop();
							String g2 = pop();
							// find the number of seconds that have passed
							int gap = Integer.parseInt(g2 + g1, 16) * 10;
							long count = decodeCount(pop(), pop());
							currentLogTime += gap * 1000;
							Reading r = new Reading(gap, count, currentLogTime);
							if (gap > 0)
							{
								if (overFlow)
								{
									r.setSaturated(true);
									overFlow = false;
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
							System.out.println("Unknown log command: f5" + command);
							break;
					}
				}
				else if (next.equals("fa"))
				{
					// the next pulse count overflowed
					overFlow = true;
				}
				else
				{
					// decode impulse count
					long count = decodeCount(next, pop());
					// update time
					currentLogTime += intervalSeconds * 1000;
					Reading r = new Reading(intervalSeconds, count, currentLogTime);
					if (overFlow)
					{
						r.setSaturated(true);
						overFlow = false;
					}
					readings.add(r);
					announceReading(r);
				}
			}
			if (totalBytesRead >= this.bytesUsed)
			{
				break;
			}
		}

		setPcMode(false);
		return readings;
	}

	/**
	 * Add a line to the buffer, calculate checksum if appropriate.
	 */
	protected void put(String sub) throws Exception
	{
		if (bytesRead > 0 && bytesRead % 32 == 0)
		{
			int checksum = Integer.parseInt(sub, 16);
			if (byteSum % 256 != checksum)
			{
				throw new Exception("Checksum error: " + (byteSum % 256) + " != " + checksum);
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
}
