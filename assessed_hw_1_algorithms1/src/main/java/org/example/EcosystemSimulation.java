package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EcosystemSimulation extends JPanel {
    static final int FIELD_SIZE = 50;
    static final int CELL_SIZE = 15;
    static final int MAX_STEPS = 1000;

    static final int INITIAL_GRASS = 300;
    static final int INITIAL_RABBITS = 200;
    static final int INITIAL_WOLVES = 1;

    static final int GRASS_GENERATION_PERIOD = 3;
    static final int GRASS_PER_GENERATION = 40;
    static final int GRASS_MAX_AGE = 30;

    static final int RABBIT_INITIAL_ENERGY = 30;
    static final int RABBIT_MAX_ENERGY = 30;
    static final int RABBIT_MOVE_COST = 1;
    static final int RABBIT_GRASS_ENERGY = 15;
    static final int RABBIT_REPRODUCTION_ENERGY = 30;
    static final double RABBIT_REPRODUCTION_CHANCE = 0.6;

    static final int WOLF_INITIAL_ENERGY = 30;
    static final int WOLF_MAX_ENERGY = 30;
    static final int WOLF_MOVE_COST = 1;
    static final int WOLF_RABBIT_ENERGY = 15;
    static final int WOLF_REPRODUCTION_ENERGY = 30;
    static final double WOLF_REPRODUCTION_CHANCE = 0.6;

    static final int TIMER_DELAY = 1;

    static final List<Grass> grasses = new ArrayList<>();
    static final List<Rabbit> rabbits = new ArrayList<>();
    static final List<Wolf> wolves = new ArrayList<>();

    static final Random RANDOM = new Random();

    static final int[] grassHistory = new int[MAX_STEPS + 1];
    static final int[] rabbitHistory = new int[MAX_STEPS + 1];
    static final int[] wolfHistory = new int[MAX_STEPS + 1];

    static int currentStep = 0;
    static int grassEatenThisStep;
    static int rabbitsEatenThisStep;
    static int rabbitsBornThisStep;
    static int wolvesBornThisStep;
    static int rabbitsDiedThisStep;
    static int wolvesDiedThisStep;

    static Timer timer;

    static double getAverageRabbitEnergy() {
        if (rabbits.isEmpty()) {
            return 0;
        }

        int totalEnergy = 0;

        for (Rabbit rabbit : rabbits) {
            totalEnergy += rabbit.energy;
        }

        return (double) totalEnergy / rabbits.size();
    }

    static double getAverageWolfEnergy() {
        if (wolves.isEmpty()) {
            return 0;
        }

        int totalEnergy = 0;

        for (Wolf wolf : wolves) {
            totalEnergy += wolf.energy;
        }

        return (double) totalEnergy / wolves.size();
    }

    static void printStatistics() {
        System.out.printf(
                "Step %d | Grass: %d | Rabbits: %d | Wolves: %d%n",
                currentStep,
                grasses.size(),
                rabbits.size(),
                wolves.size()
        );

        System.out.printf(
                "Energy: rabbits = %.2f | wolves = %.2f%n",
                getAverageRabbitEnergy(),
                getAverageWolfEnergy()
        );

        System.out.printf(
                "Food: grass eaten = %d | rabbits eaten = %d%n",
                grassEatenThisStep,
                rabbitsEatenThisStep
        );

        System.out.printf(
                "Born: rabbits = %d | wolves = %d%n",
                rabbitsBornThisStep,
                wolvesBornThisStep
        );

        System.out.printf(
                "Died: rabbits = %d | wolves = %d%n",
                rabbitsDiedThisStep,
                wolvesDiedThisStep
        );

        System.out.println("----------------------------------------");
    }

    static class Grass {
        int x;
        int y;
        int age;

        Grass(int x, int y) {
            this.x = x;
            this.y = y;
            this.age = 0;
        }
    }

    static abstract class Animal {
        int x;
        int y;
        int energy;

        Animal(int x, int y, int energy) {
            this.x = x;
            this.y = y;
            this.energy = energy;
        }

        abstract void move();

        void loseEnergy(int amount) {
            energy -= amount;
        }

        void moveRandomly() {
            int direction = RANDOM.nextInt(4);

            switch (direction) {
                case 0:
                    if (x > 0) {
                        x--;
                    }
                    break;
                case 1:
                    if (x < FIELD_SIZE - 1) {
                        x++;
                    }
                    break;
                case 2:
                    if (y > 0) {
                        y--;
                    }
                    break;
                case 3:
                    if (y < FIELD_SIZE - 1) {
                        y++;
                    }
                    break;
            }
        }
    }

    static class Rabbit extends Animal {

        Rabbit(int x, int y, int energy) {
            super(x, y, energy);
        }

        @Override
        void move() {
            List<Point> grassCells = new ArrayList<>();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    int newX = x + dx;
                    int newY = y + dy;

                    if (hasGrass(newX, newY)) {
                        grassCells.add(new Point(newX, newY));
                    }
                }
            }

            if (!grassCells.isEmpty()) {
                Point target = grassCells.get(
                        RANDOM.nextInt(grassCells.size())
                );

                if (target.x != x) {
                    x += Integer.signum(target.x - x);
                } else if (target.y != y) {
                    y += Integer.signum(target.y - y);
                }

                return;
            }

            moveRandomly();
        }
    }

    static class Wolf extends Animal {

        Wolf(int x, int y, int energy) {
            super(x, y, energy);
        }

        @Override
        void move() {
            List<Point> rabbitCells = new ArrayList<>();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    int newX = x + dx;
                    int newY = y + dy;

                    if (hasRabbit(newX, newY)) {
                        rabbitCells.add(new Point(newX, newY));
                    }
                }
            }

            if (!rabbitCells.isEmpty()) {
                Point target = rabbitCells.get(
                        RANDOM.nextInt(rabbitCells.size())
                );

                if (target.x != x) {
                    x += Integer.signum(target.x - x);
                } else if (target.y != y) {
                    y += Integer.signum(target.y - y);
                }

                return;
            }

            moveRandomly();
        }
    }

    public EcosystemSimulation() {
        setPreferredSize(
                new Dimension(
                        FIELD_SIZE * CELL_SIZE,
                        FIELD_SIZE * CELL_SIZE + 60
                )
        );

        initialize();

        grassHistory[0] = grasses.size();
        rabbitHistory[0] = rabbits.size();
        wolfHistory[0] = wolves.size();

        timer = new Timer(TIMER_DELAY, e -> {
            simulationStep();
            repaint();
        });

        timer.start();
    }

    static void initialize() {
        grasses.clear();
        rabbits.clear();
        wolves.clear();
        currentStep = 0;

        for (int i = 0; i < INITIAL_GRASS; i++) {
            createGrass();
        }

        for (int i = 0; i < INITIAL_RABBITS; i++) {
            createRabbit();
        }

        for (int i = 0; i < INITIAL_WOLVES; i++) {
            createWolf();
        }
    }

    static void createGrass() {
        int x = RANDOM.nextInt(FIELD_SIZE);
        int y = RANDOM.nextInt(FIELD_SIZE);

        grasses.add(new Grass(x, y));
    }

    static void createRabbit() {
        int x = RANDOM.nextInt(FIELD_SIZE);
        int y = RANDOM.nextInt(FIELD_SIZE);

        rabbits.add(
                new Rabbit(
                        x,
                        y,
                        RABBIT_INITIAL_ENERGY
                )
        );
    }

    static void createWolf() {
        int x = RANDOM.nextInt(FIELD_SIZE);
        int y = RANDOM.nextInt(FIELD_SIZE);

        wolves.add(
                new Wolf(
                        x,
                        y,
                        WOLF_INITIAL_ENERGY
                )
        );
    }

    static void simulationStep() {
        currentStep++;

        grassEatenThisStep = 0;
        rabbitsEatenThisStep = 0;
        rabbitsBornThisStep = 0;
        wolvesBornThisStep = 0;
        rabbitsDiedThisStep = 0;
        wolvesDiedThisStep = 0;

        moveRabbits();
        moveWolves();

        rabbitsEatGrass();
        wolvesEatRabbits();

        reproduceRabbits();
        reproduceWolves();

        updateGrass();
        removeDeadAnimals();
        generateNewGrass();

        grassHistory[currentStep] = grasses.size();
        rabbitHistory[currentStep] = rabbits.size();
        wolfHistory[currentStep] = wolves.size();

        printStatistics();

        if (currentStep == MAX_STEPS) {
            timer.stop();
            SwingUtilities.invokeLater(EcosystemSimulation::showChart);
        }
    }

    static void moveRabbits() {
        for (Rabbit rabbit : rabbits) {
            rabbit.move();
            rabbit.loseEnergy(RABBIT_MOVE_COST);
        }
    }

    static void moveWolves() {
        for (Wolf wolf : wolves) {
            wolf.move();
            wolf.loseEnergy(WOLF_MOVE_COST);
        }
    }

    static boolean hasGrass(int x, int y) {
        if (!isInsideField(x, y)) {
            return false;
        }

        for (Grass grass : grasses) {
            if (grass.x == x && grass.y == y) {
                return true;
            }
        }

        return false;
    }

    static boolean hasRabbit(int x, int y) {
        if (!isInsideField(x, y)) {
            return false;
        }

        for (Rabbit rabbit : rabbits) {
            if (rabbit.x == x && rabbit.y == y) {
                return true;
            }
        }

        return false;
    }

    static boolean isInsideField(int x, int y) {
        return x >= 0 &&
                x < FIELD_SIZE &&
                y >= 0 &&
                y < FIELD_SIZE;
    }

    static void rabbitsEatGrass() {
        for (Rabbit rabbit : rabbits) {
            Iterator<Grass> iterator = grasses.iterator();

            while (iterator.hasNext()) {
                Grass grass = iterator.next();

                if (grass.x == rabbit.x &&
                        grass.y == rabbit.y) {

                    rabbit.energy += RABBIT_GRASS_ENERGY;

                    if (rabbit.energy > RABBIT_MAX_ENERGY) {
                        rabbit.energy = RABBIT_MAX_ENERGY;
                    }

                    iterator.remove();
                    grassEatenThisStep++;

                    break;
                }
            }
        }
    }

    static void wolvesEatRabbits() {
        Iterator<Rabbit> rabbitIterator = rabbits.iterator();

        while (rabbitIterator.hasNext()) {
            Rabbit rabbit = rabbitIterator.next();

            for (Wolf wolf : wolves) {
                if (wolf.x == rabbit.x &&
                        wolf.y == rabbit.y) {

                    wolf.energy += WOLF_RABBIT_ENERGY;

                    if (wolf.energy > WOLF_MAX_ENERGY) {
                        wolf.energy = WOLF_MAX_ENERGY;
                    }

                    rabbitIterator.remove();
                    rabbitsEatenThisStep++;

                    break;
                }
            }
        }
    }

    static void reproduceRabbits() {
        List<Rabbit> newborns = new ArrayList<>();

        for (Rabbit rabbit : rabbits) {
            if (rabbit.energy >= RABBIT_REPRODUCTION_ENERGY) {
                if (RANDOM.nextDouble() <
                        RABBIT_REPRODUCTION_CHANCE) {

                    int x = RANDOM.nextInt(FIELD_SIZE);
                    int y = RANDOM.nextInt(FIELD_SIZE);

                    newborns.add(
                            new Rabbit(
                                    x,
                                    y,
                                    RABBIT_INITIAL_ENERGY
                            )
                    );

                    rabbitsBornThisStep++;
                    rabbit.energy /= 2;
                }
            }
        }

        rabbits.addAll(newborns);
    }

    static void reproduceWolves() {
        List<Wolf> newborns = new ArrayList<>();

        for (Wolf wolf : wolves) {
            if (wolf.energy >= WOLF_REPRODUCTION_ENERGY) {
                if (RANDOM.nextDouble() <
                        WOLF_REPRODUCTION_CHANCE) {

                    int x = RANDOM.nextInt(FIELD_SIZE);
                    int y = RANDOM.nextInt(FIELD_SIZE);

                    newborns.add(
                            new Wolf(
                                    x,
                                    y,
                                    WOLF_INITIAL_ENERGY
                            )
                    );

                    wolvesBornThisStep++;
                    wolf.energy /= 2;
                }
            }
        }

        wolves.addAll(newborns);
    }

    static void updateGrass() {
        Iterator<Grass> iterator = grasses.iterator();

        while (iterator.hasNext()) {
            Grass grass = iterator.next();

            grass.age++;

            if (grass.age >= GRASS_MAX_AGE) {
                iterator.remove();
            }
        }
    }

    static void generateNewGrass() {
        if (currentStep % GRASS_GENERATION_PERIOD == 0) {
            for (int i = 0;
                 i < GRASS_PER_GENERATION;
                 i++) {

                createGrass();
            }
        }
    }

    static void removeDeadAnimals() {
        Iterator<Rabbit> rabbitIterator = rabbits.iterator();

        while (rabbitIterator.hasNext()) {
            Rabbit rabbit = rabbitIterator.next();

            if (rabbit.energy <= 0) {
                rabbitIterator.remove();
                rabbitsDiedThisStep++;
            }
        }

        Iterator<Wolf> wolfIterator = wolves.iterator();

        while (wolfIterator.hasNext()) {
            Wolf wolf = wolfIterator.next();

            if (wolf.energy <= 0) {
                wolfIterator.remove();
                wolvesDiedThisStep++;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(230, 245, 230));

        g.fillRect(
                0,
                0,
                FIELD_SIZE * CELL_SIZE,
                FIELD_SIZE * CELL_SIZE
        );

        g.setColor(new Color(210, 220, 210));

        for (int i = 0; i <= FIELD_SIZE; i++) {
            g.drawLine(
                    i * CELL_SIZE,
                    0,
                    i * CELL_SIZE,
                    FIELD_SIZE * CELL_SIZE
            );

            g.drawLine(
                    0,
                    i * CELL_SIZE,
                    FIELD_SIZE * CELL_SIZE,
                    i * CELL_SIZE
            );
        }

        for (Grass grass : grasses) {
            g.setColor(new Color(50, 170, 60));

            g.fillRect(
                    grass.x * CELL_SIZE + 4,
                    grass.y * CELL_SIZE + 4,
                    CELL_SIZE - 8,
                    CELL_SIZE - 8
            );
        }

        for (Rabbit rabbit : rabbits) {
            g.setColor(Color.WHITE);

            g.fillOval(
                    rabbit.x * CELL_SIZE + 2,
                    rabbit.y * CELL_SIZE + 2,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4
            );

            g.setColor(Color.BLACK);

            g.drawOval(
                    rabbit.x * CELL_SIZE + 2,
                    rabbit.y * CELL_SIZE + 2,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4
            );
        }

        for (Wolf wolf : wolves) {
            g.setColor(Color.DARK_GRAY);

            g.fillOval(
                    wolf.x * CELL_SIZE + 2,
                    wolf.y * CELL_SIZE + 2,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4
            );
        }

        g.setColor(Color.BLACK);

        int infoY = FIELD_SIZE * CELL_SIZE + 20;

        g.drawString(
                "Ход: " + currentStep,
                10,
                infoY
        );

        g.drawString(
                "Трава: " + grasses.size(),
                100,
                infoY
        );

        g.drawString(
                "Зайцы: " + rabbits.size(),
                200,
                infoY
        );

        g.drawString(
                "Волки: " + wolves.size(),
                300,
                infoY
        );
    }

    static void showChart() {
        JFrame chartFrame = new JFrame("Численность популяций за 5000 шагов");

        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chartFrame.add(
                new PopulationChart(
                        grassHistory,
                        rabbitHistory,
                        wolfHistory,
                        MAX_STEPS
                )
        );

        chartFrame.setSize(1200, 700);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }

    static class PopulationChart extends JPanel {

        final int[] grassHistory;
        final int[] rabbitHistory;
        final int[] wolfHistory;
        final int maxSteps;

        PopulationChart(
                int[] grassHistory,
                int[] rabbitHistory,
                int[] wolfHistory,
                int maxSteps
        ) {
            this.grassHistory = grassHistory;
            this.rabbitHistory = rabbitHistory;
            this.wolfHistory = wolfHistory;
            this.maxSteps = maxSteps;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            int width = getWidth();
            int height = getHeight();

            int left = 70;
            int right = 30;
            int top = 60;
            int bottom = 60;

            int graphWidth = width - left - right;
            int graphHeight = height - top - bottom;

            int maxValue = 0;

            for (int i = 0; i <= maxSteps; i++) {
                maxValue = Math.max(maxValue, grassHistory[i]);
                maxValue = Math.max(maxValue, rabbitHistory[i]);
                maxValue = Math.max(maxValue, wolfHistory[i]);
            }

            if (maxValue == 0) {
                maxValue = 1;
            }

            int yStep = Math.max(1, (int) Math.ceil(maxValue / 10.0));
            int displayedMax = yStep * 10;

            g2.setColor(Color.BLACK);
            g2.drawLine(left, top, left, top + graphHeight);
            g2.drawLine(
                    left,
                    top + graphHeight,
                    left + graphWidth,
                    top + graphHeight
            );

            for (int i = 0; i <= 10; i++) {
                int value = i * yStep;

                int y = top + graphHeight -
                        (int) ((double) value / displayedMax * graphHeight);

                g2.setColor(new Color(225, 225, 225));
                g2.drawLine(
                        left,
                        y,
                        left + graphWidth,
                        y
                );

                g2.setColor(Color.BLACK);
                g2.drawString(
                        String.valueOf(value),
                        20,
                        y + 5
                );
            }

            for (int step = 0; step <= maxSteps; step += 1000) {
                int x = left +
                        (int) ((double) step / maxSteps * graphWidth);

                g2.setColor(new Color(225, 225, 225));
                g2.drawLine(
                        x,
                        top,
                        x,
                        top + graphHeight
                );

                g2.setColor(Color.BLACK);
                g2.drawString(
                        String.valueOf(step),
                        x - 10,
                        top + graphHeight + 25
                );
            }

            drawLine(
                    g2,
                    grassHistory,
                    left,
                    top,
                    graphWidth,
                    graphHeight,
                    displayedMax,
                    new Color(50, 170, 60)
            );

            drawLine(
                    g2,
                    rabbitHistory,
                    left,
                    top,
                    graphWidth,
                    graphHeight,
                    displayedMax,
                    new Color(220, 140, 40)
            );

            drawLine(
                    g2,
                    wolfHistory,
                    left,
                    top,
                    graphWidth,
                    graphHeight,
                    displayedMax,
                    Color.DARK_GRAY
            );

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString(
                    "Численность травы, зайцев и волков",
                    width / 2 - 220,
                    30
            );

            int legendY = 45;

            g2.setColor(new Color(50, 170, 60));
            g2.fillRect(350, legendY - 10, 20, 4);
            g2.setColor(Color.BLACK);
            g2.drawString("Трава", 378, legendY);

            g2.setColor(new Color(220, 140, 40));
            g2.fillRect(490, legendY - 10, 20, 4);
            g2.setColor(Color.BLACK);
            g2.drawString("Зайцы", 518, legendY);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(630, legendY - 10, 20, 4);
            g2.setColor(Color.BLACK);
            g2.drawString("Волки", 658, legendY);

            g2.dispose();
        }

        private void drawLine(
                Graphics2D g2,
                int[] history,
                int left,
                int top,
                int graphWidth,
                int graphHeight,
                int maxValue,
                Color color
        ) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));

            for (int step = 1; step <= maxSteps; step++) {
                int x1 = left +
                        (int) ((double) (step - 1) / maxSteps * graphWidth);

                int y1 = top + graphHeight -
                        (int) ((double) history[step - 1] / maxValue * graphHeight);

                int x2 = left +
                        (int) ((double) step / maxSteps * graphWidth);

                int y2 = top + graphHeight -
                        (int) ((double) history[step] / maxValue * graphHeight);

                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame =
                new JFrame(
                        "Экосистема: трава — зайцы — волки"
                );

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        frame.setContentPane(
                new EcosystemSimulation()
        );

        frame.pack();

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}