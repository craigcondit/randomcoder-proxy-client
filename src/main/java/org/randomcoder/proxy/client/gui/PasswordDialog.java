package org.randomcoder.proxy.client.gui;

import static java.awt.GridBagConstraints.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Password prompt dialog.
 * 
 * <pre>
 * Copyright (c) 2007, Craig Condit. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot;
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * </pre>
 */
public class PasswordDialog extends JDialog
{
	private static final long serialVersionUID = -917090059644356701L;
	
	protected String username = null;
	protected String password = null;
	
	/**
	 * Creates a new password dialog.
	 * 
	 * @param parent
	 *            parent frame or <code>null</code> if none
	 * @param name
	 *            name of proxy to display
	 * @param proxyUrl
	 *            URL of remote proxy to display
	 * @param user
	 *            default username
	 */
	public PasswordDialog(JFrame parent, String name, String proxyUrl, String user)
	{
		super(parent, true);
		
		Container cp = getContentPane();
		
		cp.setLayout(new GridBagLayout());

		if (name == null)
		{
			JLabel proxy = new JLabel(proxyUrl);
			proxy.setFont(proxy.getFont().deriveFont(Font.PLAIN));
			
			cp.add(new JLabel("Proxy URL:"), new GridBagConstraints(0, 0, 1, 1, 1, 1, EAST, NONE, new Insets(10,10,0,0), 0, 0));
			cp.add(proxy, new GridBagConstraints(1, 0, 2, 1, 1, 1, WEST, HORIZONTAL, new Insets(10,10,0,10), 0, 0));
		}
		else
		{
			// name specified
			JLabel proxy = new JLabel(name);
			proxy.setFont(proxy.getFont().deriveFont(Font.PLAIN));
			
			cp.add(new JLabel("Proxy:"), new GridBagConstraints(0, 0, 1, 1, 1, 1, EAST, NONE, new Insets(10,10,0,0), 0, 0));
			cp.add(proxy, new GridBagConstraints(1, 0, 2, 1, 1, 1, WEST, HORIZONTAL, new Insets(10,10,0,10), 0, 0));
		}
		
		final JTextField userField = new JTextField();
		userField.setMaximumSize(new Dimension(100, (int) userField.getPreferredSize().getHeight()));
		userField.setPreferredSize(userField.getMaximumSize());
		userField.setMinimumSize(userField.getMaximumSize());
		
		if (user == null)
		{
			userField.requestFocusInWindow();
		}
		else
		{
			userField.setText(user);
			userField.setEnabled(false);
		}
		
		final JPasswordField passField = new JPasswordField();
		passField.setMaximumSize(new Dimension(100, (int) passField.getPreferredSize().getHeight()));
		passField.setPreferredSize(passField.getMaximumSize());
		passField.setMinimumSize(passField.getMaximumSize());

		if (user != null)
		{
			passField.requestFocusInWindow();
		}
		
		cp.add(new JLabel("User name:"), new GridBagConstraints(0, 1, 1, 1, 1, 1, EAST, NONE, new Insets(10,10,0,0), 0, 0));
		cp.add(userField, new GridBagConstraints(1, 1, 2, 1, 1, 1, WEST, HORIZONTAL, new Insets(10,10,0,10), 0, 0));
		cp.add(new JLabel("Password:"), new GridBagConstraints(0, 2, 1, 1, 1, 1, EAST, NONE, new Insets(10,10,0,0), 0, 0));
		cp.add(passField, new GridBagConstraints(1, 2, 2, 1, 1, 1, WEST, HORIZONTAL, new Insets(10,10,0,10), 0, 0));
			
		JButton login = new JButton("Login");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getRootPane().setDefaultButton(login);
		
		getRootPane().registerKeyboardAction(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		login.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				username = userField.getText();
				password = new String(passField.getPassword());
				dispose();
			}
		});
		cp.add(login, new GridBagConstraints(1, 3, 1, 1, 0, 0, WEST, NONE, new Insets(10,10,10,10), 0, 0));

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}		
		});
		cp.add(cancel, new GridBagConstraints(2, 3, 1, 1, 0, 0, WEST, NONE, new Insets(10,0,10,10), 0, 0));
		
		setTitle("Login required");
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
	}

	/**
	 * Gets the username selected by the user.
	 * 
	 * @return username, or <code>null</code> if dialog was canceled.
	 */
	public String getUsername()
	{
		return username;
	}
	
	/**
	 * Gets the password selected by the user.
	 * 
	 * @return password, or <code>null</code> if dialog was canceled.
	 */
	public String getPassword()
	{
		return password;
	}
}
