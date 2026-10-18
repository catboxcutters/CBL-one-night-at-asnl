package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.scenario.Scenario;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;
import nl.tue.sysadminpanic.sim.Simulation;

/**
 * The main window. It switches between the start screen and the game view, and
 * wires the panels together.
 *
 * <p>Every panel observes the simulation on its own. The frame only coordinates
 * <em>selection</em>, which involves several panels at once: clicking a map node
 * or an alert updates the map highlight and the inspector together.
 */
public class MainFrame extends JFrame {

    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 800;
    private static final int MIN_WIDTH = 1000;
    private static final int MIN_HEIGHT = 640;
    private static final int SIDE_PANEL_WIDTH = 420;

    private final Scenario scenario;
    private Simulation simulation;
    private GameClock clock;
    private NetworkMapPanel mapPanel;
    private InspectorPanel inspectorPanel;

    /**
     * Creates the window showing the start screen. It is not visible until
     * {@code setVisible(true)} is called.
     *
     * @param scenario the scenario to play
     */
    public MainFrame(Scenario scenario) {
        super("One Night at ASNL");
        this.scenario = scenario;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
        setContentPane(new StartScreen(this::startGame));
    }

    /** Starts a fresh game; also used by "Play again". */
    private void startGame() {
        simulation = Simulation.forScenario(scenario);
        clock = new GameClock(simulation::tick);
        setContentPane(buildGameView());
        revalidate();
        repaint();
        clock.start();
    }

    private JPanel buildGameView() {
        StatusBar statusBar = new StatusBar(scenario.getName(), simulation);
        mapPanel = new NetworkMapPanel(simulation.getNetwork(), this::selectDevice);
        AlertPanel alertPanel = new AlertPanel(this::selectAlert);
        inspectorPanel = new InspectorPanel(simulation);
        ControlBar controlBar = new ControlBar(clock);
        for (GameEventListener panel : List.of(statusBar, mapPanel, alertPanel,
                inspectorPanel, controlBar)) {
            simulation.addListener(panel);
        }
        // Added last, so every panel shows the final state before the dialog opens.
        simulation.addListener(this::onGameEvent);

        JSplitPane side = new JSplitPane(JSplitPane.VERTICAL_SPLIT, alertPanel, inspectorPanel);
        side.setResizeWeight(0.35);
        // Metal paints a light, dotted grip on the divider; the basic UI paints a
        // plain strip in the background color, which fits the terminal look.
        side.setUI(new BasicSplitPaneUI());
        side.setDividerSize(4);
        side.setBorder(null);
        side.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));

        JPanel view = new JPanel(new BorderLayout());
        view.add(statusBar, BorderLayout.NORTH);
        view.add(mapPanel, BorderLayout.CENTER);
        view.add(side, BorderLayout.EAST);
        view.add(controlBar, BorderLayout.SOUTH);
        return view;
    }

    private void selectDevice(Device device) {
        mapPanel.setSelectedDevice(device);
        inspectorPanel.setDevice(device);
    }

    private void selectAlert(Alert alert) {
        inspectorPanel.setAlert(alert);
        selectDevice(simulation.getNetwork().getDevice(alert.targetDeviceId()));
    }

    private void onGameEvent(GameEvent event) {
        if (event instanceof GameEvent.GameOver) {
            clock.pause();
            // Open the dialog after the current tick has finished: a modal dialog
            // opened here would block inside Simulation.tick().
            SwingUtilities.invokeLater(() ->
                    new EndDialog(this, simulation.getStats(), this::startGame).setVisible(true));
        }
    }
}
