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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Activity reading from the GammaScout log
 * 
 * @author Erik Berglund
 * 
 */
public class Reading
{
	//number of counts per minute per microSievert per hour. Divide CPM by this number to get uSv/h
	private static final double CONVERSION_FACTOR = 236;
	private long time;
	private long count;
	private long intervalSeconds;
	private boolean saturated;

	public Reading()
	{
	}

	/**
	 * 
	 * @param gap time interval in seconds.
	 * @param value count indicated during the time interval.
	 * @param timeInMillis
	 */
	public Reading(int gap, long value, long timeInMillis)
	{
		intervalSeconds = gap;
		count = value;
		time = timeInMillis;
	}

	/**
	 * Return true if the input in the measuring interval was sufficient to
	 * saturate the GM tube (over 1mSv/h).
	 * 
	 * @return
	 */
	public boolean isSaturated()
	{
		return saturated;
	}

	public void setSaturated(boolean saturated)
	{
		this.saturated = saturated;
	}

	/**
	 * Get the time the measuring interval ended, in milliseconds since the
	 * epoch, UTC.
	 * 
	 * @return
	 */
	public long getTime()
	{
		return time;
	}

	public void setTime(long time)
	{
		this.time = time;
	}

	/**
	 * Get the number of counts that occured during this measurement interval.
	 * 
	 * @return
	 */
	public long getCount()
	{
		return count;
	}

	public void setCount(long count)
	{
		this.count = count;
	}

	/**
	 * Get the length of the measurement interval, in seconds.
	 * 
	 * @return
	 */
	public long getInterval()
	{
		return intervalSeconds;
	}

	/**
	 * Set the length of the measurement interval, in seconds.
	 * 
	 * @return
	 */
	public void setInterval(long interval)
	{
		this.intervalSeconds = interval;
	}
	
	public double getCountsPerMinute()
	{
		return count*60.0/(double)intervalSeconds;
	}

	/**
	 * Calculate the microsievert per hour value
	 * @return
	 */
	public double getMicroSievertsPerHour()
	{
		return getCountsPerMinute()/CONVERSION_FACTOR;
	}

	/**
	 * Get the date when the measuring started.
	 * @return
	 */
	public Date getFromDate()
	{
		Calendar c = new GregorianCalendar(Tools.UTC_TIMEZONE);
		c.setTimeInMillis(getTime());
		//move backwards
		c.add(Calendar.SECOND, (int)-getInterval());
		return c.getTime();
	}
	
	/**
	 * Get the date when the measuring ended.
	 * @return
	 */
	public Date getToDate()
	{
		Calendar c = new GregorianCalendar(Tools.UTC_TIMEZONE);
		c.setTimeInMillis(getTime());
		return c.getTime();		
	}

}
