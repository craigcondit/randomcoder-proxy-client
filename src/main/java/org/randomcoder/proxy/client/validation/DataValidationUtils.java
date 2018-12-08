package org.randomcoder.proxy.client.validation;

import java.util.Locale;

/**
 * Convenience methods to validate common data types.
 *
 * <pre>
 * Copyright (c) 2006, Craig Condit. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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
public final class DataValidationUtils {
  private DataValidationUtils() {
  }

  /**
   * Returns the canonical representation of a domain name.
   *
   * @param domain domain nam
   * @return domain name with
   */
  public static String canonicalizeDomainName(String domain) {
    if (domain == null)
      return null;
    domain = domain.toLowerCase(Locale.US).trim();
    if (domain.length() == 0)
      return null;
    return domain;
  }

  /**
   * Determines whether a domain name is valid or not.
   *
   * <p>
   * <strong>NOTE:</strong> This method does not handle internationalized
   * domain names.
   * </p>
   *
   * @param domain domain name
   * @return true if valid, false otherwise
   */
  public static boolean isValidHostName(String domain) {
    domain = canonicalizeDomainName(domain);
    if (domain == null)
      return false;
    if (domain.length() > 255)
      return false;

    String dom = "([a-z0-9]+|([a-z0-9]+[a-z0-9\\-]*[a-z0-9]+))";
    if (!domain.matches("^(" + dom + "\\.)*" + dom + "+$"))
      return false;
    String[] parts = domain.split("\\.");
    for (String part : parts)
      if (part.length() > 67)
        return false;
    return true;
  }

  /**
   * Determines if a specified IP address is valid.
   *
   * <p>
   * <strong>NOTE:</strong> This is IPv4 only.
   * </p>
   *
   * @param ipAddress ip address to test
   * @return true if valid, false otherwise
   */
  public static boolean isValidIpAddress(String ipAddress) {
    if (ipAddress == null)
      return false;
    ipAddress = ipAddress.trim();
    if (!ipAddress.matches("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$"))
      return false;
    String[] parts = ipAddress.split("\\.");
    for (int i = 0; i < 4; i++) {
      int value = Integer.parseInt(parts[i]);
      if (value < 0 || value > 255)
        return false;
    }
    return true;
  }
}
