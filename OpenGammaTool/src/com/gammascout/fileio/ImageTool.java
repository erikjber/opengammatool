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
package com.gammascout.fileio;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.DOMImplementation;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Tools for converting graphs to various image formats.
 * 
 * @author Erik Berglund
 * 
 */
public class ImageTool
{
	/**
	 * @param frame
	 * @param chart
	 * @param width
	 * @param height
	 */
	public static void savePdf(JFrame frame, JFreeChart chart, int width,
			int height) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Portable Document Format");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"PDF Images", "pdf");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION
				&& fileChooser.getSelectedFile() != null
				&& !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if (f.exists())
			{
				// confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()
						+ " already exists. Overwrite?", "File exists",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				// file does not exists
				write = true;
			}
			if (write)
			{
				PdfWriter writer = null;

				com.itextpdf.text.Document document = new com.itextpdf.text.Document();
				document.setPageSize(new Rectangle(width, height));
				try
				{
					writer = PdfWriter.getInstance(document,
							new FileOutputStream(f));
					document.open();
					PdfContentByte contentByte = writer.getDirectContent();
					PdfTemplate template = contentByte.createTemplate(width,
							height);
					Graphics2D graphics2d = new PdfGraphics2D(template, width,
							height, new DefaultFontMapper());
					Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0,
							width, height);

					chart.draw(graphics2d, rectangle2d);

					graphics2d.dispose();
					contentByte.addTemplate(template, 0, 0);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				document.close();
			}
		}
	}

	/**
	 * @param frame
	 * @param chart
	 * @param width
	 * @param height
	 */
	public static void saveSvg(JFrame frame, JFreeChart chart, int width,
			int height) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Scalable Vector Graphics");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"SVG Images", "svg");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION
				&& fileChooser.getSelectedFile() != null
				&& !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if (f.exists())
			{
				// confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()
						+ " already exists. Overwrite?", "File exists",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				// file does not exists
				write = true;
			}
			if (write)
			{
			    // Get a DOMImplementation.
			    DOMImplementation domImpl =
			      GenericDOMImplementation.getDOMImplementation();

			    // Create an instance of org.w3c.dom.Document.
			    String svgNS = "http://www.w3.org/2000/svg";
			    org.w3c.dom.Document document = domImpl.createDocument(svgNS, "svg", null);

			    // Create an instance of the SVG Generator.
			    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

			    // render the chart to the generator
			    chart.draw(svgGenerator, new Rectangle2D.Double(0,0,width,height));

			    // Finally, stream out SVG to the standard output using
			    // UTF-8 encoding.
			    boolean useCSS = true; // we want to use CSS style attributes
			    Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
			    svgGenerator.stream(out, useCSS);
			}
		}
	}

	/**
	 * @param frame
	 * @param chart
	 * @param width
	 * @param height
	 */
	public static void saveEps(JFrame frame, JFreeChart chart, int width,
			int height) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Encapsulated PostScript");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"EPS Images", "eps");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION
				&& fileChooser.getSelectedFile() != null
				&& !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if (f.exists())
			{
				// confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()
						+ " already exists. Overwrite?", "File exists",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				// file does not exists
				write = true;
			}
			if (write)
			{
				EPSDocumentGraphics2D g2d = new EPSDocumentGraphics2D(false);
				g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

				// Set up the document size
				g2d.setupDocument(new FileOutputStream(f), width, height);
				Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width,
						height);
				chart.draw(g2d, rectangle2d);
				g2d.finish(); // Wrap up and finalize the EPS file
			}
		}
	}

	/**
	 * @param frame
	 * @param chart
	 * @param width
	 * @param height
	 */
	public static void savePng(JFrame frame, JFreeChart chart, int width,
			int height) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Portable Network Graphics");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"PNG Images", "png");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION
				&& fileChooser.getSelectedFile() != null
				&& !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if (f.exists())
			{
				// confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()
						+ " already exists. Overwrite?", "File exists",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				// file does not exists
				write = true;
			}
			if (write)
			{
				// write the image
				ChartUtilities.saveChartAsPNG(f, chart, width, height);
			}
		}
	}

	/**
	 * @param frame
	 * @param chart
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public static void saveJpg(JFrame frame, JFreeChart chart, int width,
			int height) throws IOException
	{
		boolean write = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save as Portable Network Graphics");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"JPG Images", "jpg", "jpeg");
		fileChooser.setFileFilter(filter);
		int option = fileChooser.showSaveDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION
				&& fileChooser.getSelectedFile() != null
				&& !fileChooser.getSelectedFile().isDirectory())
		{
			File f = fileChooser.getSelectedFile();
			if (f.exists())
			{
				// confirm over-write
				int result = JOptionPane.showConfirmDialog(frame, f.getName()
						+ " already exists. Overwrite?", "File exists",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION)
				{
					write = true;
				}
			}
			else
			{
				// file does not exists
				write = true;
			}
			if (write)
			{
				// write the image
				ChartUtilities.saveChartAsJPEG(f, chart, width, height);
			}
		}
	}

}
