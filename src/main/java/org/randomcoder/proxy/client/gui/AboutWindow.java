package org.randomcoder.proxy.client.gui;

import javax.swing.*;
import java.awt.*;

/**
 * About window for HTTP proxy.
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
public class AboutWindow extends JFrame {
  private static final long serialVersionUID = -7783337611538845870L;

  /**
   * Creates a new About window.
   */
  public AboutWindow() {
    super("About HTTP Proxy");

    setIconImage(
        new ImageIcon(getClass().getResource("/icon-512x512.png")).getImage());

    ImageIcon icon = new ImageIcon(getClass().getResource("/icon-128x128.png"));

    JLabel iconLabel = new JLabel(icon);

    Container content = getContentPane();
    content.setLayout(new GridBagLayout());

    content.add(iconLabel, new GridBagConstraints(0, 0, 1, 4, 0.0, 0.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(12, 12, 12, 12), 0, 0));
    setIconImage(icon.getImage());

    JLabel titleLabel = new JLabel("HTTP Proxy");
    titleLabel
        .setFont(titleLabel.getFont().deriveFont(24.0f).deriveFont(Font.BOLD));
    content.add(titleLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(12, 0, 0, 12), 0, 0));

    JLabel versionLabel = new JLabel("1.0.0-SNAPSHOT");
    versionLabel.setFont(
        versionLabel.getFont().deriveFont(12.0f).deriveFont(Font.BOLD));
    content.add(versionLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, 12, 12), 0, 0));

    JLabel copyLabel1 = new JLabel(((char) 0xA9) + " 2007 Craig Condit.");
    copyLabel1
        .setFont(copyLabel1.getFont().deriveFont(12.0f).deriveFont(Font.PLAIN));
    content.add(copyLabel1, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 12), 0, 0));

    JLabel copyLabel2 = new JLabel("All rights reserved.");
    copyLabel2
        .setFont(copyLabel2.getFont().deriveFont(12.0f).deriveFont(Font.PLAIN));
    content.add(copyLabel2, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, 12, 12), 0, 0));

    pack();
    setResizable(false);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }
}
