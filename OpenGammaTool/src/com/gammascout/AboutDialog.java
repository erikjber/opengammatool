package com.gammascout;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * About dialog. Displays basic information and credits for the application.
 * 
 * @author Erik Berglund
 * 
 */
public class AboutDialog extends JDialog implements HyperlinkListener
{
	private static final long serialVersionUID = 7420857955560785995L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public AboutDialog()
	{
		setTitle("About");
		setResizable(false);
		setModal(true);
		setBounds(100, 100, 450, 266);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		JEditorPane dtrpnOpenGammaTool = new JEditorPane();
		dtrpnOpenGammaTool.setContentType("text/html");
		dtrpnOpenGammaTool.setEditable(false);
		dtrpnOpenGammaTool.setBackground(UIManager
				.getColor("Button.background"));
		dtrpnOpenGammaTool
				.setText("<html><body>\nOpen Gamma Tool<br/>\nVersion 1.1<br/>\n&#169; 2014-2017 Erik Berglund<br/>\n<br/>\nSpecial thanks to <a href=\"http://johannes-bauer.com/linux/gammascout/\">Johannes Bauer</a><br/>\nfor reverse engineering and documenting <br/>\nthe GammaScout&#8482; protocol<br/>\n</body></html>");
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(gl_contentPanel.createParallelGroup(
				Alignment.TRAILING).addGroup(
				gl_contentPanel
						.createSequentialGroup()
						.addGap(51)
						.addComponent(dtrpnOpenGammaTool,
								GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
						.addGap(41)));
		gl_contentPanel.setVerticalGroup(gl_contentPanel.createParallelGroup(
				Alignment.LEADING).addGroup(
				gl_contentPanel
						.createSequentialGroup()
						.addContainerGap()
						.addComponent(dtrpnOpenGammaTool,
								GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
						.addContainerGap()));
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
		dtrpnOpenGammaTool.addHyperlinkListener(this);
	}

	/**
	 * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent arg)
	{
		if (arg.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
		{
			if (Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().browse(
							new URI(arg.getURL().toExternalForm()));
				}
				catch (IOException | URISyntaxException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
