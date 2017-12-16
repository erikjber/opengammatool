package com.gammascout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SpringLayout;
import javax.swing.ToolTipManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import com.gammascout.fileio.ImageTool;
import com.gammascout.fileio.TextTool;
import com.gammascout.usb.GammaScoutConnectorBase;
import com.gammascout.usb.GammaScoutConnectorV1;
import com.gammascout.usb.GammaScoutConnectorV2;
import com.gammascout.usb.GammaScoutListener;
import com.gammascout.usb.ProtocolVersionDetector;
import com.gammascout.usb.ProtocolVersionDetector.ProtocolVersion;
import com.gammascout.usb.Reading;
import com.gammascout.usb.Tools;

import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * The main application window, and application entry point.
 * 
 * @author Erik Berglund
 * 
 */
public class MainWindow implements ActionListener, GammaScoutListener, Runnable
{

	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static
	{
		DATE_FORMAT.setTimeZone(Tools.UTC_TIMEZONE);
	}
	private JFrame frmOpenGammaTool;
	private JRadioButtonMenuItem rdbtnmntmCountsPerMinute;
	private JRadioButtonMenuItem rdbtnmntmMicroSievertsPer;
	private JCheckBoxMenuItem chckbxmntmHideOverflowReadings;
	private List<Reading> readings = new ArrayList<>();
	private GammaScoutConnectorBase gsc;
	private JLabel infoLabel;

	private boolean clearLog;
	private boolean setTime;
	private boolean loadData;
	private JButton btnLoadData;
	private JButton btnSetTime;
	private JButton btnClearLog;
	private DefaultXYDataset dataset;
	private JFreeChart chart;
	private ChartPanel chartPanel;
	private boolean updateGraph;
	private JRadioButtonMenuItem rdbtnmntmLinear;
	private JRadioButtonMenuItem rdbtnmntmLogarithmic;

	/**
	 * Launch the application.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		final MainWindow window = new MainWindow();
		window.frmOpenGammaTool.setVisible(true);
		Thread t = new Thread(window);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Create the application.
	 */
	public MainWindow()
	{
		initialize();
		updateLabels();
	}

	/**
	 * Initialise the contents of the frame.
	 */
	private void initialize()
	{
		frmOpenGammaTool = new JFrame();

		ClassLoader cldr = MainWindow.class.getClassLoader();
		URL url = cldr.getResource("res/icon.png");

		if (url == null)
			JOptionPane.showMessageDialog(null, "Could not load icon.");

		try
		{
			BufferedImage img = ImageIO.read(url);
			frmOpenGammaTool.setIconImage(img);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		frmOpenGammaTool.setTitle("Open Gamma Tool");
		frmOpenGammaTool.setBounds(100, 100, 1023, 603);
		frmOpenGammaTool.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frmOpenGammaTool.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenu mnSaveGraphAs = new JMenu("Save graph as");
		mnFile.add(mnSaveGraphAs);

		JMenuItem mntmPdf = new JMenuItem("PDF");
		mntmPdf.setActionCommand("savepdf");
		mntmPdf.addActionListener(this);
		mnSaveGraphAs.add(mntmPdf);

		JMenuItem mntmSvg = new JMenuItem("SVG");
		mntmSvg.setActionCommand("savesvg");
		mntmSvg.addActionListener(this);
		mnSaveGraphAs.add(mntmSvg);

		JMenuItem mntmEps = new JMenuItem("EPS");
		mntmEps.setActionCommand("saveeps");
		mntmEps.addActionListener(this);
		mnSaveGraphAs.add(mntmEps);

		JMenuItem mntmPng = new JMenuItem("PNG");
		mntmPng.setActionCommand("savepng");
		mntmPng.addActionListener(this);
		mnSaveGraphAs.add(mntmPng);

		JMenuItem mntmJpg = new JMenuItem("JPG");
		mntmJpg.setActionCommand("savejpg");
		mntmJpg.addActionListener(this);
		mnSaveGraphAs.add(mntmJpg);

		JMenu mnSaveDataAs = new JMenu("Save data as");
		mnFile.add(mnSaveDataAs);

		JMenuItem mntmCsv = new JMenuItem("CSV");
		mntmCsv.setActionCommand("savecsv");
		mntmCsv.addActionListener(this);
		mnSaveDataAs.add(mntmCsv);

		JMenuItem mntmLoadData = new JMenuItem("Load CSV");
		mntmLoadData.setActionCommand("loadcsv");
		mntmLoadData.addActionListener(this);
		mnFile.add(mntmLoadData);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setActionCommand("exit");
		mntmExit.addActionListener(this);
		mnFile.add(mntmExit);

		JMenu mnNewMenu = new JMenu("View");
		menuBar.add(mnNewMenu);

		ButtonGroup buttonGroup = new ButtonGroup();

		rdbtnmntmCountsPerMinute = new JRadioButtonMenuItem("counts per minute");
		rdbtnmntmCountsPerMinute.setActionCommand("counts");
		rdbtnmntmCountsPerMinute.setSelected(false);
		rdbtnmntmCountsPerMinute.addActionListener(this);
		mnNewMenu.add(rdbtnmntmCountsPerMinute);

		rdbtnmntmMicroSievertsPer = new JRadioButtonMenuItem("micro Sieverts per hour");
		rdbtnmntmMicroSievertsPer.setActionCommand("microsieverts");
		rdbtnmntmMicroSievertsPer.setSelected(true);
		rdbtnmntmMicroSievertsPer.addActionListener(this);
		mnNewMenu.add(rdbtnmntmMicroSievertsPer);

		// add both buttons to same group
		buttonGroup.add(rdbtnmntmCountsPerMinute);
		buttonGroup.add(rdbtnmntmMicroSievertsPer);

		chckbxmntmHideOverflowReadings = new JCheckBoxMenuItem("Hide overflow readings");
		chckbxmntmHideOverflowReadings.setSelected(true);
		chckbxmntmHideOverflowReadings.setActionCommand("refresh");
		chckbxmntmHideOverflowReadings.addActionListener(this);
		mnNewMenu.add(chckbxmntmHideOverflowReadings);

		// create menu that lets the user choose log/lin scale
		JMenu mnScale = new JMenu("Scale");
		menuBar.add(mnScale);

		rdbtnmntmLinear = new JRadioButtonMenuItem("Linear");
		rdbtnmntmLinear.setActionCommand("linear");
		rdbtnmntmLinear.addActionListener(this);
		rdbtnmntmLinear.setSelected(true);
		mnScale.add(rdbtnmntmLinear);

		rdbtnmntmLogarithmic = new JRadioButtonMenuItem("Logarithmic");
		rdbtnmntmLogarithmic.setActionCommand("logarithmic");
		rdbtnmntmLogarithmic.addActionListener(this);
		rdbtnmntmLogarithmic.setSelected(false);
		mnScale.add(rdbtnmntmLogarithmic);

		// group the log/lin radio buttons together
		ButtonGroup scaleButtonGroup = new ButtonGroup();
		scaleButtonGroup.add(rdbtnmntmLinear);
		scaleButtonGroup.add(rdbtnmntmLogarithmic);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		mntmAbout.setActionCommand("about");
		mntmAbout.addActionListener(this);

		SpringLayout springLayout = new SpringLayout();
		frmOpenGammaTool.getContentPane().setLayout(springLayout);

		dataset = new DefaultXYDataset();
		// create the chart
		chart = ChartFactory.createTimeSeriesChart("Gamma Scout measured data", "time", "value", dataset, false, true, false);
		chart.getXYPlot().setRangePannable(true);
		chart.getXYPlot().setDomainPannable(true);
		DateAxis dateAxis = (DateAxis) chart.getXYPlot().getDomainAxis();
		dateAxis.setTimeZone(Tools.UTC_TIMEZONE);
		chartPanel = new ChartPanel(chart);
		chartPanel.setInitialDelay(0);
		chartPanel.setDisplayToolTips(true);
		springLayout.putConstraint(SpringLayout.NORTH, chartPanel, 6, SpringLayout.NORTH, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, chartPanel, 6, SpringLayout.WEST, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, chartPanel, 6, SpringLayout.EAST, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, chartPanel, 6, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		chartPanel.setMaximumDrawWidth(7000);
		chartPanel.setMaximumDrawHeight(4000);
		chartPanel.setForeground(Color.BLACK);
		chartPanel.setMouseWheelEnabled(true);

		frmOpenGammaTool.getContentPane().add(chartPanel);

		btnLoadData = new JButton("Load data");
		btnLoadData.setToolTipText("Read all data from connected device");
		springLayout.putConstraint(SpringLayout.WEST, chartPanel, 0, SpringLayout.WEST, btnLoadData);
		springLayout.putConstraint(SpringLayout.SOUTH, chartPanel, -6, SpringLayout.NORTH, btnLoadData);
		btnLoadData.setEnabled(false);
		springLayout.putConstraint(SpringLayout.NORTH, btnLoadData, -31, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, btnLoadData, -6, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		btnLoadData.addActionListener(this);
		btnLoadData.setActionCommand("loaddata");
		btnLoadData.setBackground(Color.GREEN);
		btnLoadData.setForeground(Color.BLACK);
		springLayout.putConstraint(SpringLayout.WEST, btnLoadData, 6, SpringLayout.WEST, frmOpenGammaTool.getContentPane());
		frmOpenGammaTool.getContentPane().add(btnLoadData);

		btnSetTime = new JButton("Set time");
		btnSetTime.setToolTipText("Set device time to current system time");
		btnSetTime.setEnabled(false);
		springLayout.putConstraint(SpringLayout.NORTH, btnSetTime, -31, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, btnSetTime, -6, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		btnSetTime.addActionListener(this);
		btnSetTime.setActionCommand("settime");
		btnSetTime.setBackground(Color.ORANGE);
		btnSetTime.setForeground(Color.BLACK);
		springLayout.putConstraint(SpringLayout.WEST, btnSetTime, 118, SpringLayout.WEST, frmOpenGammaTool.getContentPane());
		frmOpenGammaTool.getContentPane().add(btnSetTime);

		btnClearLog = new JButton("Clear log");
		btnClearLog.setToolTipText("Erase all log data from device");
		btnClearLog.setEnabled(false);
		springLayout.putConstraint(SpringLayout.NORTH, btnClearLog, -31, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, btnClearLog, -6, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		btnClearLog.addActionListener(this);
		btnClearLog.setActionCommand("clearlog");
		btnClearLog.setBackground(Color.RED);
		btnClearLog.setForeground(Color.BLACK);
		springLayout.putConstraint(SpringLayout.WEST, btnClearLog, 217, SpringLayout.WEST, frmOpenGammaTool.getContentPane());
		frmOpenGammaTool.getContentPane().add(btnClearLog);

		infoLabel = new JLabel("Connecting...");
		springLayout.putConstraint(SpringLayout.EAST, chartPanel, 0, SpringLayout.EAST, infoLabel);
		springLayout.putConstraint(SpringLayout.WEST, infoLabel, 6, SpringLayout.EAST, btnClearLog);
		springLayout.putConstraint(SpringLayout.EAST, infoLabel, -6, SpringLayout.EAST, frmOpenGammaTool.getContentPane());
		infoLabel.setForeground(Color.BLACK);
		springLayout.putConstraint(SpringLayout.NORTH, infoLabel, -26, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, infoLabel, -11, SpringLayout.SOUTH, frmOpenGammaTool.getContentPane());
		frmOpenGammaTool.getContentPane().add(infoLabel);

		ToolTipManager.sharedInstance().setEnabled(true);
		ToolTipManager.sharedInstance().registerComponent(btnLoadData);
		ToolTipManager.sharedInstance().setInitialDelay(0);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg)
	{
		try
		{
			switch (arg.getActionCommand())
			{
				case "about":
					showAboutDialog();
					break;
				case "exit":
					frmOpenGammaTool.dispose();
					break;
				case "savepng":
					ImageTool.savePng(frmOpenGammaTool, chart, chartPanel.getWidth(), chartPanel.getHeight());
					break;
				case "savejpg":
					ImageTool.saveJpg(frmOpenGammaTool, chart, chartPanel.getWidth(), chartPanel.getHeight());
					break;
				case "savepdf":
					ImageTool.savePdf(frmOpenGammaTool, chart, chartPanel.getWidth(), chartPanel.getHeight());
					break;
				case "saveeps":
					ImageTool.saveEps(frmOpenGammaTool, chart, chartPanel.getWidth(), chartPanel.getHeight());
					break;
				case "savesvg":
					ImageTool.saveSvg(frmOpenGammaTool, chart, chartPanel.getWidth(), chartPanel.getHeight());
					break;
				case "linear":
				case "logarithmic":
				case "counts":
				case "microsieverts":
					updateLabels();
					// fall through
				case "refresh":
					updateGraph();
					break;
				case "savecsv":
					TextTool.saveCSV(frmOpenGammaTool, readings);
					break;
				case "loaddata":
					loadData = true;
					break;
				case "settime":
					setTime = true;
					break;
				case "loadcsv":
					TextTool.loadCSV(frmOpenGammaTool, readings);
					updateGraph = true;
					break;
				case "clearlog":
					clearLog = true;
					break;
				default:
					System.out.println("Unhandled command: " + arg.getActionCommand());
					break;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Repaint the graph from existing values.
	 * 
	 */
	private void updateGraph()
	{
		// get the selected state
		boolean ignoreSaturated = this.chckbxmntmHideOverflowReadings.isSelected();
		boolean countsPerMinute = this.rdbtnmntmCountsPerMinute.isSelected();
		double[][] matrix = null;
		synchronized (readings)
		{
			int count = readings.size();
			if (count > 0 && ignoreSaturated)
			{
				// count the number of un-saturated samples
				count = 0;
				for (Reading r : readings)
				{
					if (!r.isSaturated())
					{
						count++;
					}
				}
			}
			// create data matrix
			matrix = new double[2][count];
			int index = 0;
			for (Reading r : readings)
			{
				if (!ignoreSaturated || !r.isSaturated())
				{
					matrix[0][index] = r.getTime();
					if (countsPerMinute)
					{
						matrix[1][index] = r.getCountsPerMinute();
					}
					else
					{
						matrix[1][index] = r.getMicroSievertsPerHour();
					}
					index++;
				}
			}
			dataset.addSeries("values", matrix);
		}

	}

	/**
	 * Set the axis labels and tooltip format appropriately.
	 */
	private void updateLabels()
	{
		boolean isLinear = this.rdbtnmntmLinear.isSelected();
		XYPlot p = chart.getXYPlot();
		if (isLinear)
		{
			p.setRangeAxis(new NumberAxis());
		}
		else
		{
			p.setRangeAxis(new LogAxis());
		}
		if (this.rdbtnmntmCountsPerMinute.isSelected())
		{
			// set up for counts per minute
			if (isLinear)
			{
				p.getRangeAxis().setLabel("counts per minute");
			}
			else
			{
				p.getRangeAxis().setLabel("counts per minute (log)");
			}
			XYItemRenderer renderer = p.getRenderer();
			renderer.setBaseToolTipGenerator(
					new StandardXYToolTipGenerator("<html><body>{1}:<br>{2} c/m</body></html>", DATE_FORMAT, NumberFormat.getInstance()));
		}
		else
		{
			// set up for microsieverts per hour
			if (isLinear)
			{
				p.getRangeAxis().setLabel("micro Sieverts per hour");
			}
			else
			{
				p.getRangeAxis().setLabel("micro Sieverts per hour (log)");
			}
			XYItemRenderer renderer = p.getRenderer();
			renderer.setBaseToolTipGenerator(
					new StandardXYToolTipGenerator("<html><body>{1}:<br>{2} &micro;Sv/h</body></html>", DATE_FORMAT, NumberFormat.getInstance()));
		}
		p.getRangeAxis().setAutoRange(true);
		p.getRangeAxis().setMinorTickMarksVisible(true);
		chartPanel.setDisplayToolTips(true);
	}

	/**
	 * Load the data from the connected device
	 */
	private void refreshData()
	{
		setGuiEnabled(false);
		readings.clear();
		if (gsc != null && gsc.isConnected())
		{
			try
			{
				gsc.getLog();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		setGuiEnabled(true);

	}

	private void clearLog()
	{
		setGuiEnabled(false);
		// ask user for confirmation
		int result = JOptionPane.showConfirmDialog(frmOpenGammaTool, "Do you really want to clear the internal Gamma Scout log?", "Confirm clear log",
				JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION)
		{
			try
			{
				gsc.clearLog();
				readings.clear();
				updateGraph = true;
			}
			catch (SerialPortException e)
			{
				e.printStackTrace();
			}
		}
		setGuiEnabled(true);
	}

	private void setTime()
	{
		setGuiEnabled(false);
		try
		{
			// set the time to the current time
			gsc.setClock(new Date());
		}
		catch (SerialPortException e)
		{
			e.printStackTrace();
		}
		setGuiEnabled(true);

	}

	/**
	 * Disable or enable GUI elements related to the connected device.
	 * 
	 * @param b
	 */
	private void setGuiEnabled(boolean b)
	{
		if (gsc != null)
		{
			this.btnClearLog.setEnabled(b);
			this.btnLoadData.setEnabled(b);
			this.btnSetTime.setEnabled(b);
		}
	}

	/**
	 * 
	 */
	private void showAboutDialog()
	{
		AboutDialog ad = new AboutDialog();
		ad.setVisible(true);
	}

	@Override
	public void run()
	{
		try
		{
			String portName = null;
			String[] portNames = SerialPortList.getPortNames();
			if (portNames.length == 0)
			{
				// pop up warning
				JOptionPane.showMessageDialog(frmOpenGammaTool, "Could not find serial port for connecting to Gamma Scout",
						"Warning - device not found", JOptionPane.WARNING_MESSAGE);
			}
			else if (portNames.length == 1)
			{
				// one serial port, use it
				portName = portNames[0];
			}
			else
			{
				// multiple serial ports, select one
				PortSelectionDialog dialog = new PortSelectionDialog(portNames);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
				portName = dialog.getSelectedPort();
			}
			if (portName != null)
			{
				ProtocolVersion version = new ProtocolVersionDetector(portName).getVersion();
				switch (version)
				{
					case VERSION1:
						gsc = new GammaScoutConnectorV1(portName);
						break;
					case VERSION2:
						gsc = new GammaScoutConnectorV2(portName);
						break;
					default:
						System.out.println("Couldn't determine protocol version.");
						break;
				}
				if(gsc!=null)
				{
					gsc.addListener(this);
				}
			}
			Thread t = new Thread(new Runnable()
			{
				/**
				 * A thread that updates GUI elements like the info bar and the
				 * chart
				 * 
				 * @see java.lang.Runnable#run()
				 */
				@Override
				public void run()
				{
					NumberFormat memoryPercentFormat = NumberFormat.getPercentInstance();
					memoryPercentFormat.setMaximumFractionDigits(1);
					while (true)
					{
						// sleep for a short while
						Tools.sleep(200);
						// repaint the info string
						if (gsc != null)
						{
							String infoString = "Firmware vers.: " + gsc.getVersion();
							infoString += ", serial: " + gsc.getSerialNumber();
							if(gsc.getBytesUsed() != null)
							{
								infoString += ", " + memoryPercentFormat.format(gsc.getMemoryUsed()) + " memory used";
							}
							if(gsc instanceof GammaScoutConnectorV2)
							{
								infoString += ", device time: " + gsc.getDeviceDateTime();
							}
							infoLabel.setText(infoString);
						}
						else
						{
							infoLabel.setText("Not connected.");
						}
						if (updateGraph)
						{
							updateGraph();
							updateGraph = false;
						}
					}
				}
			});
			t.setDaemon(true);
			t.start();
			// we've connected, allow user interaction
			setGuiEnabled(true);
			// loop forever while waiting for the user to push buttons
			while (true)
			{
				if (loadData)
				{
					refreshData();
					loadData = false;
				}
				else if (setTime)
				{
					setTime();
					setTime = false;
				}
				else if (clearLog)
				{
					clearLog();
					clearLog = false;
				}
				else
				{
					// sleep a little
					Tools.sleep(100);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @see com.gammascout.usb.GammaScoutListener#receiveReading(com.gammascout.usb.Reading)
	 */
	@Override
	public void receiveReading(Reading r)
	{
		synchronized (readings)
		{
			readings.add(r);
		}
		updateGraph = true;
	}
}
