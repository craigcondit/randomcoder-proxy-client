package org.randomcoder.proxy.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.randomcoder.proxy.client.config.ProxyConfigurationListener;

/**
 * Thread which handles communication between a local socket and a remote HTTP
 * proxy.
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
public class SocketListenerThread extends Thread {
	private static final Logger logger = Logger.getLogger(SocketListenerThread.class);

	private final Socket socket;
	private final String name;
	private final HttpHost httpHost;
	private final CloseableHttpClient client;
	private final HttpClientContext localContext;
	private final CredentialsProvider credsProvider;
	private final Authenticator auth;
	private final String proxyUrl;
	private final String username;
	private final String host;
	private final int port;
	private final ProxyConfigurationListener listener;

	private volatile boolean shutdown = false;

	/**
	 * Creates a new socket listener thread.
	 * 
	 * @param socket
	 *            socket to process
	 * @param name
	 *            name of proxy instance
	 * @param httpHost
	 *            HTTP host to connect to
	 * @param client
	 *            HTTP client
	 * @param localContext
	 *            local HTTP context
	 * @param credsProvider
	 *            credentials provider
	 * @param proxyUrl
	 *            base URL of remote proxy
	 * @param username
	 *            saved username for proxy
	 * @param host
	 *            remote host to connect to
	 * @param port
	 *            remote port to connect to
	 * @param auth
	 *            authenticator used to retrieve credentials
	 * @param listener
	 *            proxy configuration listener
	 */
	public SocketListenerThread(Socket socket, String name, HttpHost httpHost, CloseableHttpClient client,
			HttpClientContext localContext, CredentialsProvider credsProvider, Authenticator auth, String proxyUrl,
			String username, String host, int port, ProxyConfigurationListener listener) {
		logger.debug("Socket listener created");
		logger.debug("  Proxy URL: " + proxyUrl);
		logger.debug("  Remote host: " + host);
		logger.debug("  Remote port: " + port);

		this.socket = socket;
		this.name = name;
		this.proxyUrl = proxyUrl;
		this.username = username;
		this.host = host;
		this.port = port;
		this.listener = listener;
		this.httpHost = httpHost;
		this.client = client;
		this.localContext = localContext;
		this.credsProvider = credsProvider;
		this.auth = auth;
	}

	@Override
	@SuppressWarnings("resource")
	public void run() {
		logger.debug("Socket listener running");

		InputStream socketInput = null;
		OutputStream socketOutput = null;
		ProxyInputStream proxyInput = null;
		ProxyOutputStream proxyOutput = null;
		String connectionId = null;

		try {
			if (listener != null)
				listener.connectionOpened(null);

			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();

			boolean force = false;

			do {
				Credentials creds = auth.getCredentials(name, httpHost.getSchemeName() + "://" + httpHost.getHostName() + proxyUrl, username, force);

				if (creds == null) {
					throw new IOException("No credentials supplied");
				}

				credsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()),
						new UsernamePasswordCredentials(creds.getUserPrincipal().getName(), creds.getPassword()));
				localContext.getAuthCache().clear();
				localContext.getAuthCache().put(httpHost, new BasicScheme());

				force = true;
			} while (!authenticate());

			logger.debug("Connecting to remote socket...");
			connectionId = connect();
			logger.debug("Connection complete.");

			proxyOutput = new ProxyOutputStream(client, httpHost, localContext, proxyUrl, connectionId, listener);
			proxyInput = new ProxyInputStream(client, httpHost, localContext, proxyUrl, connectionId, listener);

			logger.debug("Proxy streams created");

			StreamCopyThread proxyToSocket = new StreamCopyThread(proxyInput, socketOutput);
			StreamCopyThread socketToProxy = new StreamCopyThread(socketInput, proxyOutput);

			proxyToSocket.start();
			socketToProxy.start();

			while (!shutdown) {
				try {
					proxyToSocket.join(1000);
				} catch (InterruptedException ignored) {
				}
				if (!proxyToSocket.isAlive()) {
					if (proxyToSocket.isSuccess()) {
						logger.debug(
								"Proxy to socket thread terminated. Bytes copied = " + socketToProxy.getBytesCopied());
					} else {
						IOException error = proxyToSocket.getException();
						if (error == null) {
							logger.error("Proxy to socket thread terminated with unknown error");
						} else {
							logger.error("Proxy to socket thread terminated with error", error);
						}
					}
					break;
				}

				try {
					socketToProxy.join(1000);
				} catch (InterruptedException ignored) {
				}
				if (!socketToProxy.isAlive()) {
					if (socketToProxy.isSuccess()) {
						logger.debug(
								"Socket to proxy thread terminated. Bytes copied = " + socketToProxy.getBytesCopied());
					} else {
						IOException error = socketToProxy.getException();
						if (error == null) {
							logger.error("Socket to proxy thread terminated with unknown error");
						} else {
							logger.error("Socket to proxy thread terminated with error", error);
						}
					}
					break;
				}
			}
		} catch (Throwable t) {
			// log uncaught exceptions
			logger.error("Error during socket listener setup", t);
		} finally {
			logger.debug("Shutting down streams...");

			logger.debug("  Socket input...");
			try {
				if (socketInput != null)
					socketInput.close();
			} catch (Throwable ignored) {
			}

			logger.debug("  Socket output...");

			try {
				if (socketOutput != null)
					socketOutput.close();
			} catch (Throwable ignored) {
			}

			logger.debug("  Proxy input...");
			try {
				if (proxyInput != null)
					proxyInput.close();
			} catch (Throwable ignored) {
			}

			logger.debug("  Proxy output...");
			try {
				if (proxyOutput != null)
					proxyOutput.close();
			} catch (Throwable ignored) {
			}

			logger.debug("  Socket...");
			try {
				if (socket != null)
					socket.close();
			} catch (Throwable ignored) {
			}

			logger.debug("  Calling disconnect...");
			try {
				if (connectionId != null)
					disconnect(connectionId);
			} catch (Throwable ignored) {
			}

			logger.debug("Closing all connections...");
			try {
				client.close();
			} catch (Throwable ignored) {
			}

			logger.debug("Socket listener terminated");

			if (listener != null)
				listener.connectionClosed(null);
		}
	}

	/**
	 * Shuts the socket listener down.
	 */
	public void shutdown() {
		shutdown = true;
	}

	private boolean authenticate() throws IOException {
		HttpGet get = null;
		try {
			get = new HttpGet(proxyUrl + "/auth");
			get.setConfig(RequestConfig.custom().setAuthenticationEnabled(true).setRedirectsEnabled(false).build());
			get.setProtocolVersion(new ProtocolVersion("http", 1, 1));
			get.setHeader("User-Agent", "Randomcoder-Proxy 1.0-SNAPSHOT");

			HttpResponse httpResponse = client.execute(httpHost, get, localContext);
			int status = httpResponse.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {

				System.err.println(IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8));
				return true;
			}

			if (status == HttpStatus.SC_UNAUTHORIZED) {
				// bad
				System.err.println(IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8));
				return false;
			}

			// ugly
			throw new IOException("Unknown status received from remote proxy: " + status);
		} finally {
			try {
				if (get != null)
					get.releaseConnection();
			} catch (Throwable ignored) {
			}
		}
	}

	private String connect() throws IOException {
		HttpGet get = null;
		try {
			DecimalFormat df = new DecimalFormat("##########");
			get = new HttpGet(
					proxyUrl + "/connect?host=" + URLEncoder.encode(host, "UTF-8") + "&port=" + df.format(port));
			get.setConfig(RequestConfig.custom().setAuthenticationEnabled(true).setRedirectsEnabled(false)
					.setSocketTimeout(0).build());
			get.setProtocolVersion(new ProtocolVersion("http", 1, 1));
			get.setHeader("User-Agent", "Randomcoder-Proxy 1.0-SNAPSHOT");

			HttpResponse httpResponse = client.execute(httpHost, get, localContext);
			int status = httpResponse.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				// good
				String response = IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8).trim();
				String[] parts = response.split(" ", 3);
				if (parts.length != 2 || !("OPEN".equals(parts[0])))
					throw new IOException("Garbled response from remote proxy: " + response);

				return parts[1]; // connection id
			}

			if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				// bad
				String response = IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8);
				throw new IOException(response);
			}

			// ugly
			throw new IOException("Unknown status received from remote proxy: " + status);
		} finally {
			try {
				if (get != null)
					get.releaseConnection();
			} catch (Throwable ignored) {
			}
		}
	}

	private void disconnect(String connectionId) throws IOException {
		HttpGet get = null;
		try {
			get = new HttpGet(proxyUrl + "/disconnect?id=" + URLEncoder.encode(connectionId, "UTF-8"));
			get.setConfig(RequestConfig.custom().setAuthenticationEnabled(true).setRedirectsEnabled(false)
					.setSocketTimeout(30000).build());
			get.setProtocolVersion(new ProtocolVersion("http", 1, 1));
			get.setHeader("User-Agent", "Randomcoder-Proxy 1.0-SNAPSHOT");

			HttpResponse httpResponse = client.execute(httpHost, get, localContext);
			int status = httpResponse.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				// good
				String response = IOUtils.toString(httpResponse.getEntity().getContent(), Charsets.UTF_8).trim();
				if (!("CLOSED".equals(response)))
					throw new IOException("Garbled response from remote proxy: " + response);
			}

			// ugly
			throw new IOException("Unknown status received from remote proxy: " + status);
		} finally {
			try {
				if (get != null)
					get.releaseConnection();
			} catch (Throwable ignored) {
			}
		}
	}
}
