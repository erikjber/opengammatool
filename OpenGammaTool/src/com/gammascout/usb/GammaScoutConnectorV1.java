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

	protected boolean bytesAvailable() {
		String first = peek();
		if (first == null) { //no new byte available return directely
			return false;
		}
		if (first.equals("fe") && lineBuffer.size() < 6) {
			return false;//note enough data for date change command (6 bytes)
		} else if (first.equals("ff") && lineBuffer.size() < 5){
			return false;//note enough data for time change command (5 bytes)
		} else if (first.equals("f4") || first.equals("f3") ||first.equals("f2") || first.equals("f1")|| first.equals("f0")){
			return true;//one byte command do not care if there is none left in the buffer
		}else{//actual value incoming, we need at least two bytes
			if(lineBuffer.size()>=2){
				return true;
			}else {
				return false;
			}
		}
	}

	@Override
    protected void readAvailableDataString() throws Exception
    {
        synchronized (buffer)
        {
            while (!buffer.isEmpty())
            {
                String line = buffer.get(0);
                buffer.remove(0);
                temporaryBuffer = line; //changed from += to =

                for (int x = 6; x < temporaryBuffer.length() - 1; x += 3) //moved x into loop
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
				if (totalBytesRead >= bytesUsed)
				{
					break;
				}
            }
        }
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
		waitForString(" GAMMA-SCOUT Protokoll \r\n");
		waitForString("\r\n");
		int intervalSeconds = 0;
		long currentLogTime = 0;

		// Read serial number
		waitForBuffer();
		synchronized (buffer)
		{
			String line = buffer.remove(0);
			totalBytesRead+=(line.length()-7)/3; //subtract 6 for address (4) and /n/r (2) and divide by 3 because of spaces to get number of 2 byte packs
			System.out.println("Read: \"" + line+"\"");
			String serialString = line.substring(12,14)+line.substring(9,11)+line.substring(6, 8);
			this.serialNumber = Integer.parseInt(serialString);
		}
		//skip a line
		waitForBuffer();
		String line = buffer.remove(0);
		System.out.println("Read: \"" + line+"\"");
		totalBytesRead+=(line.length()-7)/3;
		waitForBuffer();
		// Read the end address
		line = buffer.remove(0);
		System.out.println("Read: \"" + line+"\"");
		totalBytesRead+=(line.length()-7)/3;
		String addressString = line.substring(9,11)+line.substring(6, 8);
		bytesUsed = Integer.parseInt(addressString,16);
		System.out.println("Expecting "+bytesUsed.toString()+" bytes to arrive or in hex address read till: "+addressString);
		// Skip to address 0x100
		while(totalBytesRead < 0x100)
		{
			waitForBuffer();
			line = buffer.remove(0);
			System.out.println("Read: \"" + line+"\"");
			totalBytesRead+=(line.length()-7)/3;
		}

		// read data lines
		while (true)
		{
			waitForBuffer();
			readAvailableDataString();
			System.out.println("one line received");
			while (bytesAvailable())
			{
				String next = pop();
				switch (next)
				{
					case "fe":
						System.out.println("changing DateFormat");
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
						System.out.println("Got new interval");
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
							System.out.println("Got new impulse with:"+ Long.toString(count));
						}
						break;
				}

			}
			if (totalBytesRead >= this.bytesUsed)
			{
				System.out.println("finished reading actual data, wait for protocol to finish...");
				break;
			}
		}

		waitForMatchString("07f0",60000); //time it actually takes are 53 seconds from line 100
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
        for(int pos=0;pos<timeString.length();pos++) {
            long start=System.currentTimeMillis();
            while ((System.currentTimeMillis()) - start<500);
            System.out.println("set time delay");
            serial.writeByte(timeString.getBytes()[pos]);
        }
		//serial.writeString(timeString);
		waitForString(" Zeit gestellt \r\n");
		// then set the date

		String dateString = "d" + DATE_FORMAT.format(time);
		for(int pos=0;pos<dateString.length();pos++) {
		    long start=System.currentTimeMillis();
            while ((System.currentTimeMillis()) - start<500);
            System.out.println("set date delay");
            serial.writeByte(dateString.getBytes()[pos]);
        }
		//serial.writeString(dateString);
		waitForString(" Datum gestellt \r\n");
	}

	@Override
	public void clearLog() throws SerialPortException
	{
		serial.writeString("z");
		waitForString("\r\n");
		waitForString(" Protokollspeicher wieder frei \r\n");
		System.out.println("sucessfully erased storage");
	}

}
