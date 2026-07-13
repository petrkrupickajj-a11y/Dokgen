package cz.petrk.dokgen.service;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Minimalni okno "Spouštím Dokgen..." zobrazene pri startu appky pres .exe,
 * dokud Spring Boot nedobehne (muze trvat i desitky sekund) - appka nema
 * viditelnou konzoli (viz sestavit-exe.bat), takze by jinak uzivatel nemel
 * zadnou zpetnou vazbu, ze se neco deje. Zavira ho StartovaciProhlizecListener,
 * jakmile appka nabehne.
 */
public final class SplashOkno {

    private static JWindow okno;

    private SplashOkno() {
    }

    public static void zobraz() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JWindow noveOkno = new JWindow();
            JPanel panel = new JPanel(new BorderLayout(12, 12));
            panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
            panel.setBackground(new Color(0xF4, 0xF1, 0xEA));

            JLabel label = new JLabel("Spouštím Dokgen…", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 15f));

            JProgressBar pruh = new JProgressBar();
            pruh.setIndeterminate(true);

            panel.add(label, BorderLayout.NORTH);
            panel.add(pruh, BorderLayout.CENTER);

            noveOkno.getContentPane().add(panel);
            noveOkno.pack();
            noveOkno.setSize(Math.max(280, noveOkno.getWidth()), noveOkno.getHeight());
            noveOkno.setLocationRelativeTo(null);
            noveOkno.setAlwaysOnTop(true);
            noveOkno.setVisible(true);
            okno = noveOkno;
        });
    }

    public static void zavri() {
        SwingUtilities.invokeLater(() -> {
            if (okno != null) {
                okno.dispose();
                okno = null;
            }
        });
    }
}
