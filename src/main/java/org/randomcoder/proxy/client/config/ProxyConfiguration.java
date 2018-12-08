package org.randomcoder.proxy.client.config;

import org.apache.commons.lang3.StringUtils;
import org.randomcoder.proxy.client.ProxyClient;
import org.randomcoder.proxy.client.validation.DataValidationUtils;
import org.randomcoder.proxy.client.validation.ValidationResult;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * HTTP proxy configuration bean.
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
public class ProxyConfiguration
    implements Serializable, Comparable<ProxyConfiguration>, Cloneable {
  private static final long serialVersionUID = 8946339597124804174L;

  protected String name;
  protected String proxyUrl;
  protected String username;
  protected String remoteHost;
  protected Integer remotePort;
  protected Integer localPort;

  /**
   * Default constructor.
   */
  public ProxyConfiguration() {
  }

  /**
   * Copy constructor.
   *
   * @param source source object to clone
   */
  public ProxyConfiguration(ProxyConfiguration source) {
    name = source.name;
    proxyUrl = source.proxyUrl;
    username = source.username;
    remoteHost = source.remoteHost;
    remotePort = source.remotePort;
    localPort = source.localPort;
  }

  /**
   * Gets the name of this configuration item.
   *
   * @return configuration name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this configuration item.
   *
   * @param name configuration name
   */
  public void setName(String name) {
    this.name = StringUtils.trimToNull(name);
  }

  /**
   * Gets the URL of the remote proxy.
   *
   * @return proxy url
   */
  public String getProxyUrl() {
    return proxyUrl;
  }

  /**
   * Sets the URL of the remote proxy.
   *
   * @param proxyUrl proxy url
   */
  public void setProxyUrl(String proxyUrl) {
    this.proxyUrl = StringUtils.trimToNull(proxyUrl);
  }

  /**
   * Gets the username associated with this proxy.
   *
   * @return username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username associated with this proxy.
   *
   * @param username username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the remote host to tunnel to.
   *
   * @return remote host
   */
  public String getRemoteHost() {
    return remoteHost;
  }

  /**
   * Sets the remote host to tunnel to.
   *
   * @param remoteHost remote host
   */
  public void setRemoteHost(String remoteHost) {
    this.remoteHost = StringUtils.trimToNull(remoteHost);
  }

  /**
   * Gets the remote port to tunnel to.
   *
   * @return remote port
   */
  public Integer getRemotePort() {
    return remotePort;
  }

  /**
   * Sets the remote port to tunnel to.
   *
   * @param remotePort remote port
   */
  public void setRemotePort(Integer remotePort) {
    if (remotePort != null && (remotePort < 1 || remotePort > 65535))
      remotePort = null;

    this.remotePort = remotePort;
  }

  /**
   * Gets the local port to listen on.
   *
   * @return local port
   */
  public Integer getLocalPort() {
    return localPort;
  }

  /**
   * Sets the local port to listen on.
   *
   * @param localPort local port
   */
  public void setLocalPort(Integer localPort) {
    if (localPort != null && (localPort < 1 || localPort > 65535))
      localPort = null;

    this.localPort = localPort;
  }

  /**
   * Compares this configuration to another configuration.
   *
   * @param obj object to compare to
   * @return -1, 0, or 1 if this object is less than, equal, or greater than
   * the specified object
   */
  public int compareTo(ProxyConfiguration obj) {
    String s1 = (name == null) ? "" : name.trim();
    String s2 = (obj.name == null) ? "" : obj.name.trim();

    return s1.compareToIgnoreCase(s2);
  }

  /**
   * Validates this object.
   *
   * @param peers list of other objects which are also loaded
   * @return list of validation results
   */
  public List<ValidationResult> validate(List<ProxyConfiguration> peers) {
    List<ValidationResult> results = new ArrayList<ValidationResult>();

    if (name == null)
      results.add(new ValidationResult("name", "Connection name is required."));
    else {
      for (ProxyConfiguration peer : peers) {
        if ((peer != this) && name.equals(peer.getName())) {
          results.add(new ValidationResult("name",
              "Another connection with this name already exists."));
          break;
        }
      }
    }

    if (proxyUrl == null)
      results.add(new ValidationResult("proxyUrl", "Proxy URL is required."));
    else {
      URL url = null;
      try {
        url = new URL(proxyUrl);
        if (!("http".equals(url.getProtocol())) && !("https"
            .equals(url.getProtocol())))
          url = null;
      } catch (MalformedURLException e) {
        url = null;
      }

      if (url == null)
        results.add(new ValidationResult("proxyUrl", "Proxy URL is invalid."));
    }

    if (remoteHost == null)
      results
          .add(new ValidationResult("remoteHost", "Remote host is required."));
    else {
      if (!DataValidationUtils.isValidHostName(remoteHost)
          && !DataValidationUtils.isValidIpAddress(remoteHost)) {
        results
            .add(new ValidationResult("remoteHost", "Remote host is invalid."));
      }
    }

    if (remotePort == null)
      results
          .add(new ValidationResult("remotePort", "Remote port is required."));

    if (localPort == null)
      results.add(new ValidationResult("localPort", "Local port is required."));

    return results;
  }

  /**
   * Clones this object into a new one.
   *
   * @return deep copy of this object
   */
  @Override public ProxyConfiguration clone() {
    return new ProxyConfiguration(this);
  }

  /**
   * Loads the list of proxy configurations.
   *
   * @return list of proxy configs
   * @throws BackingStoreException if preferences are unavailable
   */
  public static List<ProxyConfiguration> load() throws BackingStoreException {
    List<ProxyConfiguration> configs = new ArrayList<ProxyConfiguration>();

    Preferences prefs = Preferences.userNodeForPackage(ProxyClient.class);

    // iterate children to get items
    for (String child : prefs.childrenNames()) {
      Preferences sub = prefs.node(child);

      ProxyConfiguration config = new ProxyConfiguration();

      config.setName(child);
      config.setProxyUrl(sub.get("ProxyUrl", null));
      config.setUsername(sub.get("Username", null));
      config.setRemoteHost(sub.get("RemoteHost", null));
      config.setRemotePort(sub.getInt("RemotePort", -1));
      config.setLocalPort(sub.getInt("LocalPort", -1));

      configs.add(config);
    }
    Collections.sort(configs);
    return configs;
  }

  /**
   * Saves the list of proxy configurations.
   *
   * @param config list of proxy configs
   * @throws BackingStoreException if preferences are unavailable
   */
  public static void save(List<ProxyConfiguration> config)
      throws BackingStoreException {
    Preferences prefs = Preferences.userNodeForPackage(ProxyClient.class);

    // remove existing preferences
    for (String child : prefs.childrenNames())
      prefs.node(child).removeNode();

    for (int i = 0; i < config.size(); i++) {
      ProxyConfiguration cfg = config.get(i);

      String name = cfg.getName();
      if (name == null)
        continue;

      Preferences child = prefs.node(name);

      String url = cfg.getProxyUrl();
      if (url != null)
        child.put("ProxyUrl", url);

      String user = cfg.getUsername();
      if (user != null)
        child.put("Username", user);

      String rhost = cfg.getRemoteHost();
      if (rhost != null)
        child.put("RemoteHost", rhost);

      Integer rport = cfg.getRemotePort();
      if (rport != null)
        child.putInt("RemotePort", rport);

      Integer lport = cfg.getLocalPort();
      if (lport != null)
        child.putInt("LocalPort", lport);

      child.flush();
    }
    prefs.flush();
  }
}
