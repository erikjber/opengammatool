package com.gammascout.usb;


/**
 * Implement this interface to listen for log data from a GammaScoutConnector.
 * 
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
