
import examples.QLearning.QLearner;
import examples.QLearning.QTable;
import examples.StarterGhost.Blinky;
import examples.StarterGhost.Inky;
import examples.StarterGhost.POGhostRandom;
import examples.StarterGhost.Pinky;
import examples.StarterGhost.Sue;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import examples.StarterNNPacMan.NNTrainer;
import examples.StarterNNPacMan.NeuralNet;
import examples.StarterNNPacMan.NeuralPacMan;
import examples.StarterNNPacMan.examples.NNLocPacMan;
import examples.StarterNNPacMan.examples.NNPacMan;
import examples.StarterNNPacMan.examples.SimpleNNLocPacMan;
import examples.StarterPacMan.*;
import examples.demo.DemoPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.controllers.examples.po.POGhosts;
import pacman.game.Constants.*;
import pacman.game.internal.POType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {

        int sightRadius = 10; // 5000 is maximum

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(false)
                .setTickLimit(30000)
                .setScaleFactor(2.5) // Increase game visual size
                .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide fashion instead of straight
                                          // line sights
                .setSightLimit(sightRadius) // The sight radius limit, set to maximum
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        int speed = 1; // smaller number will run faster


        MASController ghosts = new examples.StarterGhostAstar.POGhostsAstar();
        // MASController ghosts = new POCommGhosts();




        executor.runGame(new TreeSearchPacMan(), ghosts, speed);
        // executor.runGame(new MyPacMan(), ghosts, speed);
        // executor.runGame(new InformationSetMCTSPacMan(), ghosts, speed);
        // executor.runGame(new SecondCustomAI(), ghosts, speed);
        // executor.runGame(new ThirdCustomAI(), ghosts, speed);

     

    }
}
