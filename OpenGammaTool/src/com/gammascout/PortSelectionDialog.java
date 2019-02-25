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
package com.gammascout;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A dialog for selecting the port to use.
 * 
 * @author Erik Berglund
 * 
 */
public class PortSelectionDialog extends JDialog
{
	private static final long serialVersionUID = 4302819916435039520L;
	private final JPanel contentPanel = new JPanel();
	private String selectedPort;
	private JList<String> list;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		try
		{
			PortSelectionDialog dialog = new PortSelectionDialog("foo", "bar");
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public String getSelectedPort()
	{
		return this.selectedPort;
	}

	/**
	 * Create the dialog.
	 */
	public PortSelectionDialog(String... ports)
	{
		setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		setModal(true);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Select serial port");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout());
		{
			{
				list = new JList<>(ports);
				list.setAutoscrolls(false);
				list.addListSelectionListener(new ListSelectionListener()
				{
					public void valueChanged(ListSelectionEvent arg0)
					{
						selectedPort = list.getSelectedValue();
					}
				});
				list.addMouseListener(new MouseListener(){

					@Override
					public void mouseClicked(MouseEvent e)
					{
						if(e.getClickCount()==2)
						{
							//handle double-click
							dispose();
						}
					}
					@Override
					public void mouseEntered(MouseEvent e)
					{						
					}
					@Override
					public void mouseExited(MouseEvent e)
					{						
					}
					@Override
					public void mousePressed(MouseEvent e)
					{
					}
					@Override
					public void mouseReleased(MouseEvent e)
					{
					}
					
				});
				list.setVisibleRowCount(15);
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.setSelectedIndex(0);
			}
			contentPanel.add(new JScrollPane(list), BorderLayout.CENTER);
		}
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
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						selectedPort = null;
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

}
