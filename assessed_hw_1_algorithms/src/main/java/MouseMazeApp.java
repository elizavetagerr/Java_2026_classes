import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class MouseMazeApp extends JFrame {
    private final JTextArea schemeArea = new JTextArea(12, 28);
    private final MazePanel mazePanel = new MazePanel();
    private final JLabel statusLabel = new JLabel("Загрузите лабиринт и нажмите 'Шаг мыши'.");
    private final JSpinner rowsSpinner = new JSpinner(new SpinnerNumberModel(8, 3, 30, 1));
    private final JSpinner colsSpinner = new JSpinner(new SpinnerNumberModel(10, 3, 30, 1));
    private final JSpinner cheeseRewardSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
    private final JSpinner waterRewardSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
    private final JSpinner shockPenaltySpinner = new JSpinner(new SpinnerNumberModel(30, 1, 1000, 1));
    private final javax.swing.Timer autoTimer;
    private Maze maze;
    private MouseAgent agent;

    public MouseMazeApp() {
        super("Мышь в лабиринте");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildControlPanel(), BorderLayout.WEST);
        add(mazePanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        autoTimer = new javax.swing.Timer(250, this::runOneStep);
        loadExampleScheme();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(360, 560));

        JPanel settings = new JPanel(new GridLayout(0, 2, 6, 6));
        settings.add(new JLabel("Строки"));
        settings.add(rowsSpinner);
        settings.add(new JLabel("Столбцы"));
        settings.add(colsSpinner);
        settings.add(new JLabel("Сыр +Z"));
        settings.add(cheeseRewardSpinner);
        settings.add(new JLabel("Вода +x"));
        settings.add(waterRewardSpinner);
        settings.add(new JLabel("Ток -y"));
        settings.add(shockPenaltySpinner);

        schemeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        schemeArea.setLineWrap(false);

        JButton generateButton = new JButton("Сгенерировать пустой");
        generateButton.addActionListener(e -> generateEmptyScheme());

        JButton loadButton = new JButton("Загрузить схему");
        loadButton.addActionListener(e -> loadSchemeFromText());

        JButton stepButton = new JButton("Шаг мыши");
        stepButton.addActionListener(this::runOneStep);

        JButton autoButton = new JButton("Авто");
        autoButton.addActionListener(e -> {
            if (autoTimer.isRunning()) {
                autoTimer.stop();
                autoButton.setText("Авто");
            } else {
                autoTimer.start();
                autoButton.setText("Стоп");
            }
        });

        JButton resetButton = new JButton("Сброс");
        resetButton.addActionListener(e -> loadSchemeFromText());

        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
        buttons.add(generateButton);
        buttons.add(loadButton);
        buttons.add(stepButton);
        buttons.add(autoButton);
        buttons.add(resetButton);

        JTextArea legend = new JTextArea(
                "Обозначения:\n" +
                        "S - старт мыши\n" +
                        "C - большой сыр\n" +
                        "W - вода\n" +
                        "E - электротравма\n" +
                        "# - стена\n" +
                        ". - свободная клетка");
        legend.setEditable(false);
        legend.setOpaque(false);

        panel.add(settings, BorderLayout.NORTH);
        panel.add(new JScrollPane(schemeArea), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.add(legend, BorderLayout.EAST);
        return panel;
    }

    private void loadExampleScheme() {
        schemeArea.setText(
                "S..#.....W\n" +
                        ".#.#.###..\n" +
                        ".#...#....\n" +
                        ".###.#.##.\n" +
                        "...W...#..\n" +
                        "##.###.#E.\n" +
                        "...#...#..\n" +
                        ".E...#...C");
        rowsSpinner.setValue(8);
        colsSpinner.setValue(10);
        loadSchemeFromText();
    }

    private void generateEmptyScheme() {
        int rows = (Integer) rowsSpinner.getValue();
        int cols = (Integer) colsSpinner.getValue();
        StringBuilder builder = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char value = '.';
                if (r == 0 && c == 0) value = 'S';
                if (r == rows - 1 && c == cols - 1) value = 'C';
                builder.append(value);
            }
            if (r < rows - 1) builder.append('\n');
        }
        schemeArea.setText(builder.toString());
        loadSchemeFromText();
    }

    private void loadSchemeFromText() {
        try {
            int cheeseReward = (Integer) cheeseRewardSpinner.getValue();
            int waterReward = (Integer) waterRewardSpinner.getValue();
            int shockPenalty = (Integer) shockPenaltySpinner.getValue();
            maze = Maze.fromText(schemeArea.getText(), cheeseReward, waterReward, shockPenalty);
            agent = new MouseAgent(maze);
            mazePanel.setMazeAndAgent(maze, agent);
            statusLabel.setText("Лабиринт загружен. Суммарное подкрепление: 0");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка схемы", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runOneStep(ActionEvent event) {
        if (maze == null || agent == null) {
            return;
        }
        StepResult result = agent.makeStep();
        mazePanel.repaint();
        statusLabel.setText(result.message);
        if (result.finished && autoTimer.isRunning()) {
            autoTimer.stop();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MouseMazeApp().setVisible(true));
    }
}

enum CellType {
    EMPTY, WALL, START, CHEESE, WATER, SHOCK
}

enum Direction {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

    final int dr;
    final int dc;

    Direction(int dr, int dc) {
        this.dr = dr;
        this.dc = dc;
    }
}

class Position {
    final int row;
    final int col;

    Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    Position move(Direction direction) {
        return new Position(row + direction.dr, col + direction.dc);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Position other)) return false;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}

class Maze {
    final CellType[][] cells;
    final int rows;
    final int cols;
    final Position start;
    final int cheeseReward;
    final int waterReward;
    final int shockPenalty;

    private Maze(CellType[][] cells, Position start, int cheeseReward, int waterReward, int shockPenalty) {
        this.cells = cells;
        this.rows = cells.length;
        this.cols = cells[0].length;
        this.start = start;
        this.cheeseReward = cheeseReward;
        this.waterReward = waterReward;
        this.shockPenalty = shockPenalty;
    }

    static Maze fromText(String text, int cheeseReward, int waterReward, int shockPenalty) {
        String[] lines = text.strip().split("\\R");
        if (lines.length == 0) throw new IllegalArgumentException("Схема лабиринта пустая.");
        int cols = lines[0].length();
        CellType[][] cells = new CellType[lines.length][cols];
        Position start = null;
        boolean hasCheese = false;

        for (int r = 0; r < lines.length; r++) {
            if (lines[r].length() != cols) {
                throw new IllegalArgumentException("Все строки схемы должны иметь одинаковую длину.");
            }
            for (int c = 0; c < cols; c++) {
                char ch = lines[r].charAt(c);
                cells[r][c] = switch (ch) {
                    case 'S' -> {
                        if (start != null) throw new IllegalArgumentException("Старт S должен быть один.");
                        start = new Position(r, c);
                        yield CellType.START;
                    }
                    case 'C' -> {
                        hasCheese = true;
                        yield CellType.CHEESE;
                    }
                    case 'W' -> CellType.WATER;
                    case 'E' -> CellType.SHOCK;
                    case '#' -> CellType.WALL;
                    case '.' -> CellType.EMPTY;
                    default -> throw new IllegalArgumentException("Недопустимый символ: " + ch);
                };
            }
        }
        if (start == null) throw new IllegalArgumentException("В схеме должен быть старт S.");
        if (!hasCheese) throw new IllegalArgumentException("В схеме должен быть сыр C.");
        return new Maze(cells, start, cheeseReward, waterReward, shockPenalty);
    }

    boolean canMove(Position position) {
        return position.row >= 0 && position.row < rows
                && position.col >= 0 && position.col < cols
                && cells[position.row][position.col] != CellType.WALL;
    }

    int reward(Position position) {
        CellType type = cells[position.row][position.col];
        return switch (type) {
            case CHEESE -> cheeseReward;
            case WATER -> waterReward;
            case SHOCK -> -shockPenalty;
            default -> -1;
        };
    }

    void collectWater(Position position) {
        if (cells[position.row][position.col] == CellType.WATER) {
            cells[position.row][position.col] = CellType.EMPTY;
        }
    }
}

class MouseAgent {
    private final Maze maze;
    private final Map<Position, EnumMap<Direction, Double>> q = new HashMap<>();
    private final Random random = new Random();
    private Position position;
    private int totalReward = 0;
    private int steps = 0;
    private boolean finished = false;
    private final double alpha = 0.40;
    private final double gamma = 0.80;
    private final double epsilon = 0.20;

    MouseAgent(Maze maze) {
        this.maze = maze;
        this.position = maze.start;
    }

    Position getPosition() {
        return position;
    }

    int getTotalReward() {
        return totalReward;
    }

    StepResult makeStep() {
        if (finished) {
            return new StepResult(true, "Мышь уже дошла до сыра. Итоговое подкрепление: " + totalReward);
        }

        Direction direction = chooseDirection(position);
        Position next = position.move(direction);
        if (!maze.canMove(next)) {
            next = position;
        }

        int reward = maze.reward(next);
        updateQ(position, direction, reward, next);
        maze.collectWater(next);
        position = next;
        totalReward += reward;
        steps++;

        if (maze.cells[position.row][position.col] == CellType.CHEESE) {
            finished = true;
            return new StepResult(true, "Сыр найден за " + steps + " шагов. Итоговое подкрепление: " + totalReward);
        }
        return new StepResult(false, "Шаг " + steps + ": " + direction + ", награда " + reward
                + ", сумма " + totalReward);
    }

    private Direction chooseDirection(Position current) {
        List<Direction> allowed = Arrays.stream(Direction.values())
                .filter(direction -> maze.canMove(current.move(direction)))
                .toList();
        if (allowed.isEmpty()) {
            return Direction.UP;
        }
        if (random.nextDouble() < epsilon) {
            return allowed.get(random.nextInt(allowed.size()));
        }
        EnumMap<Direction, Double> values = q.computeIfAbsent(current, p -> new EnumMap<>(Direction.class));
        return allowed.stream()
                .max(Comparator.comparingDouble(direction -> values.getOrDefault(direction, 0.0)))
                .orElse(allowed.get(0));
    }

    private void updateQ(Position current, Direction direction, int reward, Position next) {
        EnumMap<Direction, Double> currentValues = q.computeIfAbsent(current, p -> new EnumMap<>(Direction.class));
        EnumMap<Direction, Double> nextValues = q.computeIfAbsent(next, p -> new EnumMap<>(Direction.class));
        double oldValue = currentValues.getOrDefault(direction, 0.0);
        double bestNext = Arrays.stream(Direction.values())
                .mapToDouble(d -> nextValues.getOrDefault(d, 0.0))
                .max()
                .orElse(0.0);
        double newValue = oldValue + alpha * (reward + gamma * bestNext - oldValue);
        currentValues.put(direction, newValue);
    }
}

class StepResult {
    final boolean finished;
    final String message;

    StepResult(boolean finished, String message) {
        this.finished = finished;
        this.message = message;
    }
}

class MazePanel extends JPanel {
    private Maze maze;
    private MouseAgent agent;

    MazePanel() {
        setPreferredSize(new Dimension(640, 560));
        setBackground(Color.WHITE);
    }

    void setMazeAndAgent(Maze maze, MouseAgent agent) {
        this.maze = maze;
        this.agent = agent;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (maze == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cell = Math.min(getWidth() / maze.cols, getHeight() / maze.rows);
        int x0 = (getWidth() - cell * maze.cols) / 2;
        int y0 = (getHeight() - cell * maze.rows) / 2;

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                int x = x0 + c * cell;
                int y = y0 + r * cell;
                drawCell(g, maze.cells[r][c], x, y, cell);
            }
        }

        Position mouse = agent.getPosition();
        int mx = x0 + mouse.col * cell;
        int my = y0 + mouse.row * cell;
        g.setColor(new Color(80, 80, 80));
        g.fillOval(mx + cell / 5, my + cell / 5, cell * 3 / 5, cell * 3 / 5);
        g.setColor(Color.WHITE);
        g.drawString("M", mx + cell / 2 - 4, my + cell / 2 + 5);
        g.dispose();
    }

    private void drawCell(Graphics2D g, CellType type, int x, int y, int size) {
        Color fill = switch (type) {
            case WALL -> new Color(35, 40, 50);
            case CHEESE -> new Color(255, 220, 80);
            case WATER -> new Color(125, 205, 255);
            case SHOCK -> new Color(255, 120, 120);
            case START -> new Color(210, 245, 210);
            default -> new Color(245, 247, 250);
        };
        g.setColor(fill);
        g.fillRect(x, y, size, size);
        g.setColor(new Color(200, 205, 215));
        g.drawRect(x, y, size, size);

        String label = switch (type) {
            case CHEESE -> "C";
            case WATER -> "W";
            case SHOCK -> "E";
            case START -> "S";
            default -> "";
        };
        if (!label.isEmpty()) {
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, Math.max(12f, size * 0.35f)));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(label, x + (size - metrics.stringWidth(label)) / 2,
                    y + (size + metrics.getAscent()) / 2 - 4);
        }
    }
}
