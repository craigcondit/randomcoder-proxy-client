package org.randomcoder.proxy.client.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.log4j.Logger;

import org.randomcoder.apple.eawt.*;
import org.randomcoder.proxy.client.config.*;

/**
 * Main window for HTTP proxy.
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
@SuppressWarnings("synthetic-access")
public class MainWindow extends JFrame implements ProxyConfigurationListener
{
	private static final long serialVersionUID = -1041159986250381985L;

	private static final Logger logger = Logger.getLogger(MainWindow.class);
	
	private final ConnectionListModel listModel;
	private final JList connectionList;
	private final JButton connectButton;
	private final JButton disconnectButton;
	private final SwingAuthenticator auth = new SwingAuthenticator();
	private final boolean mac;
	
	private TrayMenu trayMenu;
	
	/**
	 * Creates a new status window.
	 * 
	 * @param aboutWindow
	 *            about window
	 * @param prefsWindow
	 *            preferences window
	 */
	public MainWindow(final AboutWindow aboutWindow, final PreferencesWindow prefsWindow)
	{
		super("HTTP Proxy Status");
		mac = Application.isSupported();
		
		// TODO Make updates actually refresh the connection list view on Mac OS X.
		//      Seems to happen for connect / disconnect / send / receive; nothing
		//      gets updated until the list selection changes. Could be an issue
		//      with the Mac L&F, or more likely a threading issue.
		
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (mac || (trayMenu != null && trayMenu.isSupported()))
				{
					setVisible(false);
				}
				else
				{
					if (handleExit())
						System.exit(0);
				}
			}			
		});

		JMenuBar menuBar = new JMenuBar();
		
		if (mac)
		{
			Application.getApplication().setEnabledPreferencesMenu(true);
			Application.getApplication().setEnabledAboutMenu(true);
			
			Application.getApplication().addApplicationListener(new ApplicationAdapter()
			{
				@Override
				public void handleOpenApplication(ApplicationEvent event)
				{
					setVisible(true);
				}

				@Override
				public void handleQuit(ApplicationEvent event)
				{
					event.setHandled(handleExit());
				}

				@Override
				public void handleReOpenApplication(ApplicationEvent event)
				{
					event.setHandled(true);
					setVisible(true);
					requestFocus();
				}

				@Override
				public void handleAbout(ApplicationEvent event)
				{
					event.setHandled(true);
					if (aboutWindow != null)
					{
						aboutWindow.setVisible(true);
						aboutWindow.requestFocus();
					}
				}

				@Override
				public void handlePreferences(ApplicationEvent event)
				{
					event.setHandled(true);
					if (prefsWindow != null)
					{
						prefsWindow.setVisible(true);
						prefsWindow.requestFocus();
					}
				}
			});
			
			JMenu windowMenu = new JMenu("Window");
			
			JMenuItem statusItem = new JMenuItem("Status");
			statusItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setVisible(true);
				}
			});
			windowMenu.add(statusItem);
			
			menuBar.add(windowMenu);
		}
		else
		{
			JMenu fileMenu = new JMenu("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			
			JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
			exitItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (handleExit())
						System.exit(0);
				}
			});
			exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
			fileMenu.add(exitItem);
			
			menuBar.add(fileMenu);
			
			JMenu editMenu = new JMenu("Edit");
			editMenu.setMnemonic(KeyEvent.VK_E);
			
			JMenuItem prefsItem = new JMenuItem("Preferences", KeyEvent.VK_P);
			prefsItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (prefsWindow != null)
					{
						prefsWindow.setVisible(true);
						prefsWindow.requestFocus();
					}
				}
			});			
			editMenu.add(prefsItem);
			
			menuBar.add(editMenu);
			
			JMenu helpMenu = new JMenu("Help");
			helpMenu.setMnemonic(KeyEvent.VK_H);
			
			JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
			aboutItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (aboutWindow != null)
					{
						aboutWindow.setVisible(true);
						aboutWindow.requestFocus();
					}
				}
			});
			helpMenu.add(aboutItem);
			
			menuBar.add(helpMenu);			
		}
		
		setJMenuBar(menuBar);
				
		// set the window's icon
		setIconImage(new ImageIcon(getClass().getResource("/icon-512x512.png")).getImage());
		
		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		
		// set up a list of connections		
		listModel = new ConnectionListModel();

		connectionList = new JList(listModel);
		connectionList.setCellRenderer(new ConnectionListCellRenderer());
		connectionList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;
				
				updateButtonState();
			}
		});
		
		JScrollPane conScroll = new JScrollPane(connectionList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); 
		conScroll.setMinimumSize(new Dimension(400, 300));
		conScroll.setPreferredSize(conScroll.getMinimumSize());
		
		content.add(conScroll, new GridBagConstraints(
			0, 0, 1, 1, 1.0, 1.0,
			GridBagConstraints.NORTH, GridBagConstraints.BOTH,
			new Insets(12, 12, 11, 11), 0, 0));
		
		JPanel buttons = new JPanel(new GridBagLayout());
		
		connectButton = new JButton("Start");
		connectButton.setEnabled(false);
		connectButton.setFocusable(false);
		
		connectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				handleConnect();
			}
		});
		
		buttons.add(connectButton, new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 11), 0, 0));

		disconnectButton = new JButton("Stop");
		disconnectButton.setEnabled(false);
		disconnectButton.setFocusable(false);

		disconnectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				handleDisconnect();
			}
		});
		
		buttons.add(disconnectButton, new GridBagConstraints(
				1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
		
		content.add(buttons, new GridBagConstraints(
				0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(6, 12, 17, 11), 0, 0));

		pack();
		//setResizable(false);
		setLocationRelativeTo(null);
		
		load();
		launchAll();
		
		if (prefsWindow != null)
			prefsWindow.addProxyConfigurationListener(this);
	}
	
	/**
	 * Sets the tray menu to associated with this application.
	 * 
	 * @param trayMenu
	 *            tray menu
	 */
	public void setTrayMenu(TrayMenu trayMenu)
	{
		this.trayMenu = trayMenu;
		updateTray();
	}
	
	public void configSaved(List<ProxyConfiguration> config)
	{
		logger.debug("Config saved. Merging...");
		
		// hash connections
		HashMap<String, ProxyConfiguration> configMap = new HashMap<String, ProxyConfiguration>();
		for (ProxyConfiguration entry : config)
			configMap.put(entry.getName(), entry);
		
		HashMap<String, ProxyConfigurationStatistics> statMap = new HashMap<String, ProxyConfigurationStatistics>();
		
		for (int i = 0; i < listModel.getSize(); i++)
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			if (item == null)
				continue;
			
			if (configMap.containsKey(item.getName()))
			{
				// track
				statMap.put(item.getName(), item);
			}
			else
			{
				// disconnect
				configMap.remove(item.getName());
				item.disconnect();				
			}
		}
		
		List<ProxyConfigurationStatistics> updated = new ArrayList<ProxyConfigurationStatistics>();
		
		// create new connections for new items
		for (Map.Entry<String, ProxyConfiguration> entry : configMap.entrySet())
		{
			String key = entry.getKey();
			ProxyConfiguration value = entry.getValue();
			
			if (statMap.containsKey(key))
			{
				ProxyConfigurationStatistics stat = new ProxyConfigurationStatistics(statMap.get(key), value);
				
				// add either old or new entry to list, depending on extent of
				// changes
				updated.add(stat.isModified() ? stat : statMap.get(key));
				
				// disconnect if any key items have changed
				if (stat.isModified() && stat.isConnected())
					stat.disconnect();
			}
			else
			{
				// create
				updated.add(new ProxyConfigurationStatistics(value));
			}
		}
		
		Collections.sort(updated);
		listModel.setData(updated);
		launchAll();
		updateTray();
	}
	
	public void connectionClosed(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
		updateTray();
	}

	public void connectionOpened(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
		updateTray();
	}

	public void connectionSetup(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
		updateButtonState();
		updateTray();
	}

	public void connectionTeardown(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
		updateButtonState();
		updateTray();
	}

	public void connectionSetupStarting(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
	}

	public void connectionTeardownStarting(ProxyConfigurationStatistics config)
	{
		listModel.update(config);
	}

	public void dataReceived(ProxyConfigurationStatistics config, long bytes)
	{
		listModel.update(config);
	}

	public void dataSent(ProxyConfigurationStatistics config, long bytes)
	{
		listModel.update(config);
	}
	
	/**
	 * Loads the existing configuration.
	 */
	public void load()
	{
		try
		{
			List<ProxyConfiguration> configs = ProxyConfiguration.load();
			
			List<ProxyConfigurationStatistics> stats = new ArrayList<ProxyConfigurationStatistics>(configs.size());
			for (ProxyConfiguration config : configs)
			{
				ProxyConfigurationStatistics stat = new ProxyConfigurationStatistics(config);
				stat.addProxyConfigurationListener(this);
				stats.add(stat);
			}
			
			listModel.setData(stats);
		}
		catch (BackingStoreException e)
		{
			logger.error("Unable to load preferences", e);
		}		
	}

	private void updateTray()
	{
		if (trayMenu == null)
			return;
		
		int proxyCount = 0;
		int connCount = 0;
		
		for (int i = 0; i < listModel.getSize(); i++)
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			
			if (item.isConnected())
			{
				proxyCount++;
				connCount += item.getActiveCount();
			}
		}
		
		trayMenu.updateStatus(proxyCount, connCount);
	}
	
	/**
	 * Handles requests to exit the program, optionally showing confirmation.
	 * 
	 * @return true if shutdown should proceed
	 */
	public boolean handleExit()
	{
		boolean active = false;
		
		for (int i = 0; i < listModel.getSize(); i++)
			if (listModel.getElementAt(i).getActiveCount() > 0)
				active = true;
		
		if (active)
		{
			Object[] options = new Object[] { "Quit", "Continue" };
			
			if (JOptionPane.YES_OPTION != JOptionPane.showOptionDialog(this, "There are active connections. Close anyway?", "Active connections", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]))
				return false;
		}
		
		// disconnect all connections
		for (int i = 0; i < listModel.getSize(); i++)
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			if (item.isConnected())
				item.disconnect();
		}
		
		return true;
	}
		
	private void launchAll()
	{
		for (int i = 0; i < listModel.getSize(); i++)
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			if (!item.isConnected())
				item.connect(auth);
		}
		updateTray();
	}
	
	/**
	 * Handles connect event processing.
	 */
	protected void handleConnect()
	{
		for (int i : connectionList.getSelectedIndices())
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			if (!item.isConnected())
				item.connect(auth);
		}
		updateTray();
	}
	
	/**
	 * Handles disconnect event processing.
	 */
	protected void handleDisconnect()
	{
		for (int i : connectionList.getSelectedIndices())
		{
			ProxyConfigurationStatistics item = listModel.getElementAt(i);
			if (item.isConnected())
				item.disconnect();
		}
		updateTray();
	}
	
	/**
	 * Updates connect / disconnect button states
	 */
	protected void updateButtonState()
	{
		int[] selected = connectionList.getSelectedIndices();
		
		boolean allowConnect = false;
		
		boolean allowDisconnect = false;
		
		for (int i : selected)
		{
			if (listModel.getElementAt(i).isConnected())
				allowDisconnect = true;
			else
				allowConnect = true;
		}
		connectButton.setEnabled(allowConnect);
		disconnectButton.setEnabled(allowDisconnect);
	}
	
	private static class ConnectionListModel extends AbstractListModel
	{
		private static final long serialVersionUID = 7645396010970634961L;

		private List<ProxyConfigurationStatistics> data = new ArrayList<ProxyConfigurationStatistics>();

		/**
		 * Gets the proxy element at the given index
		 * 
		 * @param index
		 *            index to lookup
		 */
		public ProxyConfigurationStatistics getElementAt(int index)
		{
			if (index < 0 || index >= data.size())
				return null;
			
			return data.get(index);
		}

		/**
		 * Gets the number of elements in the proxy list.
		 * 
		 * @return size
		 */
		public int getSize()		
		{
			return data.size();
		}		
		
		/**
		 * Updates the given configuration item.
		 * 
		 * @param config
		 *            configuration to update
		 */
		public void update(ProxyConfigurationStatistics config)
		{
			// update the requested entry
			for (int i = 0; i < data.size(); i++)
				if (config == data.get(i))
					super.fireContentsChanged(this, i, i);
		}
				
		/**
		 * Replaces all data with new items.
		 * 
		 * @param data
		 *            updated data
		 */
		public void setData(List<ProxyConfigurationStatistics> data)
		{
			super.fireIntervalRemoved(this, 0, this.data.size());
			this.data = data;
			super.fireIntervalAdded(this, 0, this.data.size());
		}
	}
	
	private static class ConnectionListCellRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = 612103334213131138L;
		
		// cache some resources so we can redraw quickly
		
		private static final Font CONNECTED_HEADING_FONT;
		private static final Font DISCONNECTED_HEADING_FONT;
		private static final Font CONNECTED_FONT;
		private static final Font DISCONNECTED_FONT;
		
		private static final GridBagConstraints NAME_CONSTRAINTS = new GridBagConstraints(
			0, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(6, 6, 0, 6), 0, 0);

		private static final GridBagConstraints CONNECTED_CONSTRAINTS = new GridBagConstraints(
				0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 6, 6, 6), 0, 0);

		private static final GridBagConstraints STATS_CONSTRAINTS = new GridBagConstraints(
				1, 0, 1, 2, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 6), 0, 0);

		private static final GridBagConstraints LINE1_CONSTRAINTS = new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0);
		
		private static final GridBagConstraints LINE2_CONSTRAINTS = new GridBagConstraints(
				0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0);
		
		static
		{
			JLabel temp = new JLabel("text");
			DISCONNECTED_HEADING_FONT = temp.getFont().deriveFont(14.0f).deriveFont(Font.PLAIN);
			CONNECTED_HEADING_FONT = temp.getFont().deriveFont(14.0f).deriveFont(Font.BOLD);
			
			DISCONNECTED_FONT = temp.getFont().deriveFont(11.0f).deriveFont(Font.PLAIN).deriveFont(Font.ITALIC);
			CONNECTED_FONT = temp.getFont().deriveFont(11.0f).deriveFont(Font.PLAIN);
		}
		
		/**
		 * Gets the custom list cell component.
		 * 
		 * @param list
		 *            list to query
		 * @param value
		 *            current element
		 * @param index
		 *            current index
		 * @param isSelected
		 *            <code>true</code> if selected
		 * @param cellHasFocus
		 *            <code>true</code> if cell has focus
		 * @return graphical representation of list item
		 */
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			ProxyConfigurationStatistics stats = (ProxyConfigurationStatistics) value;
			
			JComponent parent = (JComponent)
				super.getListCellRendererComponent(list, stats.getName(), index, isSelected, cellHasFocus);
			
			JPanel component = new JPanel(new GridBagLayout());
			
			component.setBorder(parent.getBorder());
			component.setBackground(parent.getBackground());
			component.setForeground(parent.getForeground());
			
			boolean isConnected = stats.isConnected();
			boolean isStarting = stats.isStarting();
			boolean isStopping = stats.isStopping();
			
			JLabel text = new JLabel(stats.getName());
			text.setFont(isConnected ? CONNECTED_HEADING_FONT : DISCONNECTED_HEADING_FONT);
			text.setForeground(parent.getForeground());
			component.add(text, NAME_CONSTRAINTS);

			String connectedText = "Stopped";
			if (isConnected)
				connectedText = "Running (" + Integer.toString(stats.getActiveCount()) + " active connections)";
			else if (isStarting)
				connectedText = "Starting...";
			else if (isStopping)
				connectedText = "Stopping...";
			
			JLabel connected = new JLabel(connectedText);
			connected.setFont(isConnected ? CONNECTED_FONT : DISCONNECTED_FONT);
			connected.setForeground(parent.getForeground());
			
			if (!isConnected)
			{
				Color prev = parent.getForeground();
				Color c = new Color(prev.getRed(), prev.getGreen(), prev.getBlue(), 128);
				connected.setForeground(c);
			}
			
			component.add(connected, CONNECTED_CONSTRAINTS);
			
			JPanel statsPanel = new JPanel(new GridBagLayout());
			statsPanel.setBackground(parent.getBackground());

			if (isConnected)
			{
				long sent = stats.getBytesSent();
	
				DecimalFormat df = new DecimalFormat("###,###");
				DecimalFormat df2 = new DecimalFormat("###,###,###.#"); 
				
				String text1 = null;
				if (sent < 1024L)
				{
					text1 = df.format(sent) + " bytes sent";
				}
				else if (sent < 1048576L)
				{
					text1 = df2.format(sent / 1024D) + " KiB sent";
				}
				else if (sent < 107374182L)
				{
					text1 = df2.format(sent / 1048576D) + " MiB sent";
				}
				else
				{
					text1 = df2.format(sent / 107374182D) + " GiB sent";
				}
				
				JLabel line1 = new JLabel(text1);
				line1.setFont(CONNECTED_FONT);
				line1.setForeground(parent.getForeground());
				
				statsPanel.add(line1, LINE1_CONSTRAINTS);
	
				long received = stats.getBytesReceived();
				
				String text2 = null;
				if (received < 1024L)
				{
					text2 = df.format(received) + " bytes received";
				}
				else if (received < 1048576L)
				{
					text2 = df2.format(received / 1024D) + " KiB received";
				}
				else if (received < 107374182L)
				{
					text2 = df2.format(received / 1048576D) + " MiB received";
				}
				else
				{
					text2 = df2.format(received / 107374182D) + " GiB received";
				}
				
				JLabel line2 = new JLabel(text2);
				line2.setFont(CONNECTED_FONT);
				line2.setForeground(parent.getForeground());
				
				statsPanel.add(line2, LINE2_CONSTRAINTS);
				
				component.add(statsPanel, STATS_CONSTRAINTS);
			}
			
			return component;
		}

	}
	
	/**
	 * Application entry point for GUI.
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args)
	{
		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			AboutWindow about = new AboutWindow();
			PreferencesWindow prefs = new PreferencesWindow();
			
			MainWindow window = new MainWindow(about, prefs);
			
			TrayMenu tray = new TrayMenu(window, about, prefs);
			tray.setVisible(true);
			
			window.setTrayMenu(tray);
			window.setVisible(!tray.isSupported());			
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

}
