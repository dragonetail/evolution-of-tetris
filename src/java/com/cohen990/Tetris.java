package com.cohen990;

import com.cohen990.ArtificialIntelligence.IntelligentStrategy;
import com.cohen990.ArtificialIntelligence.Strategy;
import com.cohen990.Commands.*;
import com.cohen990.Reproduction.ChildPair;
import com.cohen990.Reproduction.Crossover;
import com.cohen990.Reproduction.Mutator;
import com.cohen990.Tetraminos.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFrame;
import javax.swing.JPanel;

// stolen from https://gist.github.com/DataWraith/5236083
public class Tetris extends JPanel {
    public final Tetramino[] Tetraminos = Tetramino.initializeTetraminos();

    public final Color[] tetraminoColors = {
            Color.cyan, Color.blue, Color.orange, Color.yellow, Color.green, Color.pink, Color.red
    };

    public final int gameHeight = 24;
    public final int gameWidth = 12;

    private final Painter painter = new Painter(this);
    private static int GAME_CLOCK = 2;
    private static int AI_CLOCK = GAME_CLOCK;
    public final int playerIndex;

    public Point pieceOrigin;
    public int currentPiece;
    public int rotation;
    private ArrayList<Integer> nextPieces = new ArrayList<Integer>();

    public long score;
    public Color[][] well;

    private static JFrame GameFrame;
    public boolean finished = false;
    private boolean shouldDraw = false;

    public Tetris(int playerIndex) {
        this.playerIndex = playerIndex;
    }

    // Creates a border around the well and initializes the dropping piece
    private void init() {
        well = new Color[gameWidth][gameHeight];
        for (int i = 0; i < gameWidth; i++) {
            for (int j = 0; j < gameHeight-1; j++) {
                if (i == 0 || i == gameWidth - 1 || j == gameHeight - 2) {
                    well[i][j] = Color.GRAY;
                } else {
                    well[i][j] = Color.BLACK;
                }
            }
        }
        addNewPiece();
        finished = false;
        shouldDraw = false;
    }

    // Put a new, random piece into the dropping position
    public void addNewPiece() {
        pieceOrigin = new Point(5, 2);
        rotation = 0;
        if (nextPieces.isEmpty()) {
            Collections.addAll(nextPieces, 0, 1, 2, 3, 4, 5, 6);
            Collections.shuffle(nextPieces);
        }
        currentPiece = nextPieces.get(0);

        if(collidesAt(pieceOrigin.x, pieceOrigin.y, rotation))
        {
            gameOver();
        }
        nextPieces.remove(0);
    }

    private void gameOver() {
        finished = true;
    }

    // Collision test for the dropping piece
    public boolean collidesAt(int x, int y, int rotation) {
        for (Point p : currentTetraminoPoints()[rotation]) {
            if (well[p.x + x][p.y + y] != Color.BLACK) {
                return true;
            }
        }
        return false;
    }

    public Point[][] currentTetraminoPoints() {
        return Tetraminos[currentPiece].points;
    }

    // Make the dropping piece part of the well, so it is available for
    // collision detection.
    public void fixToWell() {
        for (Point p : currentTetraminoPoints()[rotation]) {
            well[pieceOrigin.x + p.x][pieceOrigin.y + p.y] = tetraminoColors[currentPiece];
        }
        clearRows();
        addNewPiece();
    }

    public void deleteRow(int row) {
        for (int j = row-1; j > 0; j--) {
            for (int i = 1; i < gameWidth - 1; i++) {
                well[i][j+1] = well[i][j];
            }
        }
    }

    // Clear completed rows from the field and award score according to
    // the number of simultaneously cleared rows.
    public void clearRows() {
        boolean gap;
        int numClears = 0;

        for (int j = gameHeight - 3; j > 0; j--) {
            gap = false;
            for (int i = 1; i < gameWidth - 1; i++) {
                if (well[i][j] == Color.BLACK) {
                    gap = true;
                    break;
                }
            }
            if (!gap) {
                deleteRow(j);
                j += 1;
                numClears += 1;
            }
        }

        switch (numClears) {
            case 1:
                score += 10000;
                break;
            case 2:
                score += 30000;
                break;
            case 3:
                score += 50000;
                break;
            case 4:
                score += 80000;
                break;
        }
    }

    @Override
    public void paintComponent(Graphics g)
    {
        if(shouldDraw) {
            painter.paintComponent(g);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int populationSize = 1000;

        List<TetrisPlayer> players = new ArrayList<>();

        for(int i = 0; i < populationSize; i++){
            players.add(new TetrisPlayer());
        }

        boolean experimentIsNotComplete = true;

        Integer generation = 1;

        while(experimentIsNotComplete) {
            for (int i = 0; i < populationSize; i++) {
//                System.out.printf("player %d\n", i);
                final Tetris game = new Tetris(i);
                game.init();

                if(i%10 == 0){
                    game.shouldDraw = true;
                    initialiseGameFrame(game);
                }

                Strategy aiStrategy = new IntelligentStrategy(game, players.get(i));

                while (!game.finished) {
                    Command command = aiStrategy.pickMove(game);
                    command.execute();
                    new DropByOne(game).execute();
                }

                players.get(i).evaluateFitness(game.score, game.well);

//                System.out.println("finished");
            }

            String directory = String.format("experiment5\\generation%d\\", generation);

//            for(int i = 0; i < players.size(); i++){
//                writeResultToFile(directory, players.getWeights(i));
//            }

            writeSummary(directory, players);

            generation = generation + 1;

            players = reproduce(players);
        }
    }

    private static List<TetrisPlayer> reproduce(List<TetrisPlayer> players) {
        Stream<TetrisPlayer> playersStream = players.stream();

        playersStream = playersStream.sorted(Comparator.comparingLong(o -> o.fitness));

        List<TetrisPlayer> parents = playersStream.skip(players.size() / 2).collect(Collectors.toList());

        Collections.shuffle(parents);

        List<TetrisPlayer> children = new ArrayList<>();

        Crossover crossover = new Crossover();

        for(int i = 0; i < parents.size(); i+=2){
            ChildPair siblings;
            if(i == parents.size() - 1){
                siblings = crossover.getChildren(parents.get(i), parents.get(i));
            } else {
                siblings = crossover.getChildren(parents.get(i), parents.get(i+1));
            }

            children.add(siblings.first);
            children.add(siblings.second);
        }

        parents.addAll(children);

        return parents;
    }

    private static void writeSummary(String pathName, List<TetrisPlayer> players) throws IOException {
        File directory = new File(pathName);

        makeDirectoryIfNotExists(directory);

        String fileName = pathName + "summary.txt";
        FileWriter writer = new FileWriter(fileName);

        int maxHash = 0;
        long max = Long.MIN_VALUE;
        int minHash = 0;
        long min = Long.MAX_VALUE;
        long total = 0;

        for(int i = 0; i < players.size(); i++){
            TetrisPlayer player = players.get(i);
            long fitness = player.fitness;
            if(fitness > max){
                max = fitness;
                maxHash = player.hashCode();
            }
            if(fitness < min){
                min = fitness;
                minHash = player.hashCode();
            }
            total += fitness;
        }

        float average = (float) total/ players.size();

        writer.write(String.format("Max: %d - player: %d\n", max, maxHash));
        writer.write(String.format("Min: %d - player: %d\n", min, minHash));
        writer.write(String.format("Average: %f", average));

        writer.close();
    }

    private static void writeResultToFile(String pathName, TetrisPlayer element) throws IOException {
        File directory = new File(pathName);

        makeDirectoryIfNotExists(directory);

        System.out.println(element);
        String fileName = String.format("%s%d.txt", pathName, element.hashCode());
        System.out.printf("using %s\n", fileName);
        FileWriter writer = new FileWriter(fileName);
        writer.write(element.toLongString());
        writer.close();
    }

    private static void makeDirectoryIfNotExists(File directory) {
        if (!directory.exists()) {
            System.out.println("creating directory: " + directory.getName());
            boolean result = false;

            try{
                directory.mkdirs();
                result = true;
            }
            catch(SecurityException se){
                //handle it
            }
            if(result) {
                System.out.println("DIR created");
            }
        }
    }

    private static void initialiseGameFrame(Tetris game) {
        if(GameFrame != null) {
            GameFrame.dispose();
        }
        GameFrame = new JFrame("Tetris");
        GameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GameFrame.setSize(12*26+10, 26*23+25);
        GameFrame.setVisible(true);
        GameFrame.add(game);
    }
}