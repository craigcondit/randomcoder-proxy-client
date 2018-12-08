package org.randomcoder.proxy.client;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.randomcoder.proxy.client.config.ProxyConfigurationListener;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * <code>InputStream</code> implementation which wraps a remote proxied
 * connection.
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
public class ProxyInputStream extends InputStream {
  private static final Logger logger = Logger.getLogger(ProxyInputStream.class);

  private final HttpResponse connection;
  private final HttpGet get;
  private final DataInputStream inputStream;
  private final ProxyConfigurationListener listener;
  private final CloseableHttpClient client;
  private final HttpHost httpHost;
  private final HttpClientContext localContext;
  private byte[] buffer;
  private int offset = 0;
  private int remaining = 0;

  /**
   * Creates a new input stream connected to a remote HTTP proxy.
   *
   * @param client       HTTP client to use for connections
   * @param httpHost     HTTP host to connect to
   * @param localContext local HTTP context
   * @param proxyUrl     proxy URL
   * @param connectionId connection id
   * @param listener     proxy configuration listener
   * @throws IOException if an error occurs while establishing communications
   */
  public ProxyInputStream(CloseableHttpClient client, HttpHost httpHost,
      HttpClientContext localContext, String proxyUrl, String connectionId,
      ProxyConfigurationListener listener) throws IOException {
    logger.debug("Creating proxy input stream");

    this.client = client;
    this.listener = listener;
    this.httpHost = httpHost;
    this.localContext = localContext;

    buffer = new byte[32768];

    this.get = new HttpGet(
        proxyUrl + "/receive?id=" + URLEncoder.encode(connectionId, "UTF-8"));
    get.setProtocolVersion(new ProtocolVersion("http", 1, 1));
    get.setConfig(
        RequestConfig.custom().setSocketTimeout(0).setRedirectsEnabled(false)
            .setAuthenticationEnabled(true).build());
    get.setHeader("User-Agent", "Randomcoder-Proxy 1.0-SNAPSHOT");

    this.connection = openConnection(get);

    logger.debug("Getting response as stream");

    logger.debug("Chunked: " + connection.getEntity().isChunked());
    logger.debug("Streaming: " + connection.getEntity().isStreaming());

    inputStream = new DataInputStream(connection.getEntity().getContent());
    logger.debug("Got response stream");

    logger.debug("Removing first line");

    while (inputStream.read() != '\n') {
    }

    logger.debug("Proxy input stream initialized");
  }

  @Override public void close() throws IOException {
    logger.debug("close()");

    logger.debug("Aborting connection...");
    try {
      get.abort();
    } catch (Throwable ignored) {
    }

    logger.debug("Closing input stream...");
    try {
      inputStream.close();
    } catch (Throwable ignored) {
    }

    logger.debug("Releasing connection...");
    try {
      get.releaseConnection();
    } catch (Throwable ignored) {
    }

    logger.debug("close() complete");
  }

  private void fillBuffer() throws IOException {
    int size = 0;

    while (size == 0) {
      try {
        size = inputStream.readInt();
      } catch (EOFException e) {
        offset = 0;
        remaining = -1;
        return;
      }

      if (size < 0 || size > buffer.length) {
        throw new IOException("Protocol error: Got invalid length " + size);
      }

      try {
        inputStream.readFully(buffer, 0, size);
      } catch (EOFException e) {
        throw new IOException("Protocol error: Input stream ended prematurely");
      }

      offset = 0;
      remaining = size;

      logger.debug("Received " + size + " bytes");
      listener.dataReceived(null, size);
    }
  }

  @Override public int read() throws IOException {
    if (remaining == 0) {
      fillBuffer();
    }

    if (remaining < 1) {
      // EOF
      return -1;
    }

    remaining--;
    return buffer[offset++];
  }

  @Override public int read(byte[] b, int off, int len) throws IOException {
    if (remaining == 0) {
      fillBuffer();
    }

    if (remaining < 0) {
      return -1;
    }

    if (remaining == 0) {
      return 0;
    }

    if (remaining >= len) {
      System.arraycopy(buffer, offset, b, off, len);
      remaining -= len;
      offset += len;
      return len;
    }

    System.arraycopy(buffer, offset, b, off, remaining);
    offset += remaining;
    int result = remaining;
    remaining = 0;

    return result;
  }

  @Override public int available() throws IOException {
    return remaining;
  }

  private HttpResponse openConnection(HttpGet get) throws IOException {
    logger.debug("Opening connection");

    logger.debug("Executing method");
    HttpResponse httpResponse = client.execute(httpHost, get, localContext);

    int status = httpResponse.getStatusLine().getStatusCode();

    logger.debug("Receive executed");

    if (status == HttpStatus.SC_OK)
      return httpResponse;

    try {
      if (status == HttpStatus.SC_NOT_FOUND) {
        String response = IOUtils
            .toString(httpResponse.getEntity().getContent(), Charsets.UTF_8);

        // not found. means connection is unavailable
        throw new IOException(response);
      }

      throw new IOException("Got unknown status from proxy server: " + status);
    } finally {
      try {
        get.releaseConnection();
      } catch (Throwable ignored) {
      }
    }
  }
}
