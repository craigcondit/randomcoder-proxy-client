package org.randomcoder.proxy.client.gui;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.randomcoder.proxy.client.Authenticator;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Authenticator which displays a Swing password dialog. Credentials are cached
 * in memory per proxy URL.
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
public class SwingAuthenticator implements Authenticator {
  private final ConcurrentHashMap<String, Credentials> credMap =
      new ConcurrentHashMap<String, Credentials>();

  public Credentials getCredentials(String name, String proxyUrl,
      String username, boolean force) {
    String key = proxyUrl + "," + ((username == null) ? "" : username);

    Credentials creds = credMap.get(key);

    if (creds == null || force) {
      PasswordDialog prompt =
          new PasswordDialog(null, name, proxyUrl, username);

      prompt.setVisible(true);
      prompt.requestFocus();

      String user = prompt.getUsername();
      String password = prompt.getPassword();

      if (user == null || password == null)
        return null;

      creds = new UsernamePasswordCredentials(user, password);

      key = proxyUrl + "," + ((user == null) ? "" : user);

      credMap.put(key, creds);
    }

    return creds;
  }
}
