package com.gammascout.fileio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.gammascout.usb.Reading;
import com.gammascout.usb.Tools;

/**
 * Tools for reading and writing GammaScout logs in CSV (comma separated value) format.
 * 
 * @author Erik Berglund
 *
 */
public class TextTool
{
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static
	{
		DATE_FORMAT.setTimeZone(Tools.UTC_TIMEZONE);
	}
	/**
	 * Open a "save as" dialog and ask the user for a file name.
	 * Write the data as Comma Separated Values (CSV) to the desired file.
	 * 
	 * @param frame
	 * @param data
	 * @throws IOException 
	 */
	public static void saveCSV(JFrame frame, List<Reading>data) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Comma Separated Values");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "CSV files", "csv");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if(option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null && !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if(f.exists())
			{
				//confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()+" already exists. Overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
				if(result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				//file does not exists
				write = true;
			}
			if(write)
			{
				//write the data
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				//write header
				bw.write("From,To,Counts,Seconds,CPM,CPS,microSievertsPerHour,saturated\n");
				//write data
				for(Reading r:data)
				{
					bw.write(DATE_FORMAT.format(r.getFromDate()));
					bw.write(",");
					bw.write(DATE_FORMAT.format(r.getToDate()));
					bw.write(","+r.getCount());
					bw.write(","+r.getInterval());
					bw.write(","+r.getCountsPerMinute());
					bw.write(","+r.getCountsPerMinute()/60.0);
					bw.write(","+r.getMicroSievertsPerHour());
					bw.write(","+r.isSaturated());
					bw.write("\n");
				}
				bw.close();				
			}
		}
	}
	
	public static void loadCSV(JFrame frame, List<Reading>data) throws IOException, ParseException
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Load Comma Separated Values");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "CSV files", "csv");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showOpenDialog(frame);
		if(option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null && !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if(f.exists() )
			{
				//clear old values
				data.clear();
				//read line-by-line
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = br.readLine();
				//discard first line
				line = br.readLine();
				while(line != null)
				{
					Reading r = new Reading();
					//parse the line
					String [] parts = line.split(",");
					long time = DATE_FORMAT.parse(parts[1]).getTime();
					r.setTime(time);
					long gap = Long.parseLong(parts[3]);
					r.setInterval(gap);
					long count = Long.parseLong(parts[2]);
					r.setCount(count);
					boolean saturated = Boolean.parseBoolean(parts[7]);
					r.setSaturated(saturated);
					data.add(r);
					line = br.readLine();
				}
				br.close();
			}
		}
	}
}
