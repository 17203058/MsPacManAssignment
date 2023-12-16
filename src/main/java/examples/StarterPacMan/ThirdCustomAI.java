package examples.StarterPacMan;

import pacman.Executor;
import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import examples.StarterNNPacMan.NeuralNet;
import examples.StarterNNPacMan.examples.NNLocPacMan;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class ThirdCustomAI extends PacmanController {
    private static final Random RANDOM = new Random();
    private Game game;
    private int pacmanCurrentNodeIndex;
    MOVE pacmanLastMoveMade;

    private MOVE getRandomMove() {
        MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

        return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        this.game = game;
        pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
        pacmanLastMoveMade = game.getPacmanLastMoveMade();
        try {
            NNLocPacMan neuralNet = new NNLocPacMan(NeuralNet.readFromFile("results/model.txt"));
            
        } catch (IOException e) {
            // Handle the exception, e.g., print an error message or log it
            e.printStackTrace();
        }
        MOVE bestPathMove = getRandomMove();

        return bestPathMove;
    }

}