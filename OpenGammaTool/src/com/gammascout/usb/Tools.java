package com.gammascout.usb;

/**
 * @author Erik Berglund
 *
 */
public class Tools
{
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
