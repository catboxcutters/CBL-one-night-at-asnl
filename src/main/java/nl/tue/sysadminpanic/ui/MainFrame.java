package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.scenario.DemoState;
import nl.tue.sysadminpanic.scenario.Scenario;

/**
 * The main window: the network map in the center and the inspector on the right.
 *
 * <p>The frame coordinates <em>selection</em>, which involves both panels:
 * clicking a map node updates the map highlight and the inspector together.
 */
public class MainFrame extends JFrame {

    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 720;
    private static final int MIN_WIDTH = 900;
    private static final int MIN_HEIGHT = 560;
    private static final int SIDE_PANEL_WIDTH = 380;

    private final NetworkMapPanel mapPanel;
    private final InspectorPanel inspectorPanel;

    /**
     * Creates the window. It is not visible until {@code setVisible(true)} is called.
     *
     * @param scenario the scenario whose network is shown
     */
    public MainFrame(Scenario scenario) {
        super("One Night at ASNL");
        Network network = scenario.createNetwork();
        DemoState.apply(network);
        mapPanel = new NetworkMapPanel(network, this::selectDevice);
        inspectorPanel = new InspectorPanel(DemoState.SNAPSHOT_TICK, mapPanel::refresh);
        inspectorPanel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));

        JPanel view = new JPanel(new BorderLayout());
        view.add(mapPanel, BorderLayout.CENTER);
        view.add(inspectorPanel, BorderLayout.EAST);
        setContentPane(view);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
    }

    private void selectDevice(Device device) {
        mapPanel.setSelectedDevice(device);
        inspectorPanel.setDevice(device);
    }
}
