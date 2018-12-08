package org.randomcoder.proxy.client.config;

import java.util.List;

/**
 * Interface for objects which need to be notified of configuration changes.
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
public interface ProxyConfigurationListener {
  /**
   * Method called when a proxy configuration is saved.
   *
   * @param config list of new proxy configuration items
   */
  public void configSaved(List<ProxyConfiguration> config);

  /**
   * Event fired when a proxy is setup.
   *
   * @param config configuration which was modified
   */
  public void connectionSetup(ProxyConfigurationStatistics config);

  /**
   * Event fired when a connection is terminated.
   *
   * @param config configuration which was modified
   */
  public void connectionTeardown(ProxyConfigurationStatistics config);

  /**
   * Event fired when a proxy begins to setup.
   *
   * @param config configuration which was modified
   */
  public void connectionSetupStarting(ProxyConfigurationStatistics config);

  /**
   * Event fired when a proxy begins to terminate.
   *
   * @param config configuration which was modified
   */
  public void connectionTeardownStarting(ProxyConfigurationStatistics config);

  /**
   * Event fired when a connection is opened.
   *
   * @param config configuration which was modified
   */
  public void connectionOpened(ProxyConfigurationStatistics config);

  /**
   * Event fired when a connection is closed.
   *
   * @param config configuration which was modified
   */
  public void connectionClosed(ProxyConfigurationStatistics config);

  /**
   * Event fired when data is sent.
   *
   * @param config configuration which was modified
   * @param bytes  number of bytes sent
   */
  public void dataSent(ProxyConfigurationStatistics config, long bytes);

  /**
   * Event fired when data is received.
   *
   * @param config configuration which was modified
   * @param bytes  number of bytes received
   */
  public void dataReceived(ProxyConfigurationStatistics config, long bytes);
}
