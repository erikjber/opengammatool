package com.gammascout.usb;

import java.util.TimeZone;

/**
 * Tools, utilities, and global constants.
 * 
 * @author Erik Berglund
 *
 */
public class Tools
{
	public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			//do nothing
		}
	}
}
