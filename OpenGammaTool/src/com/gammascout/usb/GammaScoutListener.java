package com.gammascout.usb;


/**
 * @author Erik Berglund
 *
 */
public interface GammaScoutListener
{
	/**
	 * A new reading has become available.
	 * 
	 * @param r
	 */
	public void receiveReading(Reading r);
}
