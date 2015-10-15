package org.randomcoder.proxy.client.config;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.*;
import org.apache.log4j.Logger;

import org.randomcoder.proxy.client.*;

/**
 * Class to track statistics and status of a proxy connection.
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
public class ProxyConfigurationStatistics extends ProxyConfiguration implements ProxyConfigurationListener
{
	private static final Logger logger = Logger.getLogger(ProxyConfigurationStatistics.class);
	
	private boolean connected;
	private boolean starting;
	private boolean stopping;
	private int activeCount;
	private long bytesReceived;
	private long bytesSent;
	private ProxyClient proxyClient;
	private ListenThread listenThread;
	private final ConcurrentLinkedQueue<ProxyConfigurationListener> listeners;
	
	private boolean modified = false;
	
	private static final long serialVersionUID = 7675553419493897275L;

	/**
	 * Wraps the given proxy configuration in a stats tracker.
	 * 
	 * @param config
	 *            proxy configuration to wrap
	 */
	public ProxyConfigurationStatistics(ProxyConfiguration config)
	{
		super(config);
		listeners = new ConcurrentLinkedQueue<ProxyConfigurationListener>();
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param source
	 *            source object to clone
	 */
	public ProxyConfigurationStatistics(ProxyConfigurationStatistics source)
	{
		super(source);
		connected = source.connected;
		starting = source.starting;
		stopping = source.stopping;
		activeCount = source.activeCount;
		bytesReceived = source.bytesReceived;
		bytesSent = source.bytesSent;
		proxyClient = source.proxyClient;
		listenThread = source.listenThread;
		listeners = new ConcurrentLinkedQueue<ProxyConfigurationListener>(source.listeners);
	}

	/**
	 * Creates a new configuration item
	 * @param source source object
	 * @param config new configuration
	 */
	public ProxyConfigurationStatistics(ProxyConfigurationStatistics source, ProxyConfiguration config)
	{
		this(source);
		name = config.name;
		
		if (!StringUtils.equals(proxyUrl, config.proxyUrl))
			modified = true;
		
		if (!StringUtils.equals(username, config.username))
			modified = true;

		if (!StringUtils.equals(remoteHost, config.remoteHost))
			modified = true;

		if (!ObjectUtils.equals(remotePort, config.remotePort))
			modified = true;

		if (!ObjectUtils.equals(localPort, config.localPort))
			modified = true;
		
		proxyUrl = config.proxyUrl;
		username = config.username;
		remoteHost = config.remoteHost;
		remotePort = config.remotePort;
		localPort = config.localPort;
	}
	
	/**
	 * Clones this object.
	 * 
	 * @return clone
	 */
	@Override
	public ProxyConfigurationStatistics clone()
	{
		return new ProxyConfigurationStatistics(this);
	}
	
	/**
	 * Adds a new proxy configuration listener.
	 * 
	 * @param listener
	 *            listener to add
	 */
	public void addProxyConfigurationListener(ProxyConfigurationListener listener)
	{
		for (ProxyConfigurationListener existing : listeners)
			if (existing == listener)
				return;
		
		listeners.add(listener);
	}
	
	/**
	 * Removes a proxy configuration listener.
	 * 
	 * @param listener
	 *            listener to remove
	 */
	public void removeProxyConfigurationListener(ProxyConfigurationListener listener)
	{
		for (Iterator<ProxyConfigurationListener> it = listeners.iterator(); it.hasNext();)
			if (listener == it.next())
				it.remove();
	}
	
	/**
	 * Checks to see if this object was modified during construction.
	 * 
	 * @return <code>true</code> if this object was constructed from an
	 *         incompatible original
	 */
	public boolean isModified()
	{
		return modified;
	}
	
	/**
	 * Determines if this proxy is connected.
	 * 
	 * @return <code>true</code> if connected
	 */
	public boolean isConnected()
	{
		return connected;
	}
	
	/**
	 * Determines if this proxy is starting up.
	 * 
	 * @return <code>true</code> if starting up
	 */
	public boolean isStarting()
	{
		return starting;
	}
	
	/**
	 * Determines if this proxy is shutting down.
	 * 
	 * @return <code>true</code> if shutting down
	 */
	public boolean isStopping()
	{
		return stopping;
	}
	
	/**
	 * Gets the number of active connections.
	 * 
	 * @return connection count
	 */
	public int getActiveCount()
	{
		return activeCount;
	}
	
	/**
	 * Gets the number of bytes received since startup.
	 * 
	 * @return bytes received
	 */
	public long getBytesReceived()
	{
		return bytesReceived;
	}
	
	/**
	 * Gets the number of bytes sent since startup.
	 * 
	 * @return bytes sent
	 */
	public long getBytesSent()
	{
		return bytesSent;
	}
	
	/**
	 * Connects to the remote proxy.
	 * 
	 * @param auth
	 *            authenticator to use for password lookups
	 */
	public synchronized void connect(Authenticator auth)
	{
		if (listenThread != null)
			disconnect();
		
		connectionSetupStarting(this);
		
		proxyClient = new ProxyClient(name, proxyUrl, username, remoteHost, remotePort, localPort, auth, this);
		
		listenThread = new ListenThread(proxyClient);
		listenThread.start();
		
		connectionSetup(this);
	}
	
	/**
	 * Disconnects from the remote proxy.
	 */
	public synchronized void disconnect()
	{
		connectionTeardownStarting(this);
		
		if (listenThread != null)
		{
			listenThread.shutdown();
			try { listenThread.join(); } catch (Exception ignored) {}
			logger.debug("Stopped listen thread");
		}
		
		if (proxyClient != null)
			proxyClient = null;
		
		connectionTeardown(this);
	}
	
	/**
	 * Listener thread which manages a proxy connection. 
	 */
	protected static final class ListenThread extends Thread
	{
		private final ProxyClient client;
		
		/**
		 * Creates a new thread.
		 * 
		 * @param client
		 *            proxy client
		 */
		public ListenThread(ProxyClient client)
		{
			super("Proxy Thread");
			this.client = client;
		}
		
		@Override
		public void run()
		{
			try
			{
				client.listen();
			}
			catch (IOException e)
			{
				logger.error("Caught exception while listening", e);
			}
		}
		
		/**
		 * Shuts down this thread.
		 */
		public void shutdown()
		{
			client.shutdown();
		}
	}

	public void configSaved(List<ProxyConfiguration> config)
	{
		for (ProxyConfigurationListener listener : listeners)
			listener.configSaved(config);
	}

	public void connectionClosed(ProxyConfigurationStatistics config)
	{
		activeCount--;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionClosed(this);
	}

	public void connectionOpened(ProxyConfigurationStatistics config)
	{
		activeCount++;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionOpened(this);
	}

	public void connectionSetup(ProxyConfigurationStatistics config)
	{
		bytesSent = 0L;
		bytesReceived = 0L;
		activeCount = 0;
		connected = true;
		stopping = false;
		starting = false;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionSetup(this);
	}

	public void connectionTeardown(ProxyConfigurationStatistics config)
	{
		bytesSent = 0L;
		bytesReceived = 0L;
		activeCount = 0;
		connected = false;
		stopping = false;
		starting = false;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionTeardown(this);
	}

	public void connectionSetupStarting(ProxyConfigurationStatistics config)
	{
		starting = true;
		connected = false;
		stopping = false;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionSetupStarting(this);
	}

	public void connectionTeardownStarting(ProxyConfigurationStatistics config)
	{
		starting = false;
		connected = false;
		stopping = false;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.connectionTeardownStarting(this);
	}

	public void dataReceived(ProxyConfigurationStatistics config, long bytes)
	{
		bytesReceived += bytes;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.dataReceived(this, bytes);
	}

	public void dataSent(ProxyConfigurationStatistics config, long bytes)
	{
		bytesSent += bytes;
		
		for (ProxyConfigurationListener listener : listeners)
			listener.dataSent(this, bytes);
	}	
}
