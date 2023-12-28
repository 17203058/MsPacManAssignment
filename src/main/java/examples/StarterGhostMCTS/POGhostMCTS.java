package examples.StarterGhostMCTS;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;

import java.util.Random;

import static pacman.game.Constants.DELAY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import pacman.Executor;
import pacman.controllers.Controller;
import pacman.controllers.examples.RandomPacMan;
import examples.StarterPacMan.SecondCustomAI;
import examples.StarterPacMan.FirstCustomAI;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.internal.Maze;
import pacman.game.internal.Node;
import pacman.game.Game;



/**
 * Created by piers on 16/02/17.
 */
public class POGhostMCTS extends IndividualGhostController {
    
    private final static int PILL_PROXIMITY = 15;        //if Ms Pac-Man is this close to a power pill, back away
    Random rnd = new Random();

    
	public static final int GHOST_LOST_LIFE_VALUE = -5;
	public static final int GHOST_RETAIN_LIFE_VALUE = 10;
	public static final int PAC_LOST_LIFE_VALUE = 500;
	public static final int CLOSE_TO_POWER = -10;
	private static final int SIM_STEPS = 200; //simulations steps
	private static final int TREE_TIME_LIMIT = 55;


	private static int totalNumOfGhostEaten = 0;
	
	// Hoeffding ineqality
	float C = (float) (1f / Math.sqrt(2));
	Controller<EnumMap<GHOST,MOVE>> ghosts =  new Legacy();
	
	public static Set<Integer> junctions;
	int lastLevel = 1;
	Maze maze3;
	boolean useScript = false;
	MOVE scriptMove = MOVE.LEFT;

	

    public POGhostMCTS(Constants.GHOST ghost) {
        super(ghost);
		
    }

    public Constants.MOVE getMove(Game game, long timeDue) {
        int level = game.getCurrentLevel();

		if (game.getNumGhostsEaten() > 0){
                totalNumOfGhostEaten+= game.getNumGhostsEaten();
                
            }

        if (game.doesGhostRequireAction(ghost)){
            
            
            
            if (junctions == null || lastLevel != level){
                junctions = getJunctions(game);
            }
            
            lastLevel = level;
            
                   

            
            return MctsSearch(game, 38);
        }
		
        
        return null;

    }

    private MOVE MctsSearch(Game game, long ms) {
		
		long start = new Date().getTime();
		MctsNode v0 = new MctsNode(new MctsState(true, game), null, game.getGhostLastMoveMade(ghost), 0,ghost);
		
		while(new Date().getTime() < start + ms){
			
			MctsNode v1 = treePolicy(v0);
			
			if (v1 == null)
				return MOVE.DOWN;
			
			int score = defaultPolicy(v1, v0);
			
			backup(v1, score);
			
		}
		
		MctsNode bestNode = bestChild(v0, 0);
		MOVE move = MOVE.UP;
		if (bestNode != null)
			move = bestNode.getMove();
	
		
		return move;
		
	}

	private MctsNode treePolicy(MctsNode node) {
		if (node != null){
			
			if (node.isExpandable()){
				if (node.getTime() <= TREE_TIME_LIMIT)
					return node.expand();
				else
					return node;
			}
			
			if (node.getState().isAlive())
				return treePolicy(bestChild(node, C));
			else
				return node;
		}
		return node;	
	}
	
	private MctsNode bestChild(MctsNode v, float c) {
		
		float bestValue = -99999999;
		MctsNode urgent = null;
		
		for(MctsNode node : v.children){
			float value = UCT(node, c);
			
			if (!node.getState().isAlive())
				value = -99999;
			
			if (value > bestValue){
				if (c != 0 || dieTest(v, node)){
					urgent = node;
					bestValue = value;
				}
			}
		}
		
		return urgent;
	}

	private boolean dieTest(MctsNode v, MctsNode node) {
		
		Controller<MOVE> pacManController = new FirstCustomAI(false);
		Controller<EnumMap<GHOST,MOVE>> ghostController = ghosts;
    	
		Game game = v.getState().getGame().copy();
			
		
		EnumMap<GHOST, MOVE> myMoves = ghostController.getMove(game.copy(),System.currentTimeMillis());
		myMoves.remove(ghost);
		myMoves.put(ghost, node.getMove());

		game.advanceGame(pacManController.getMove(game.copy(),System.currentTimeMillis()),
	        	myMoves);
	        
	    
		if (game.wasGhostEaten(ghost))
			return false;
		
		return true;
		
	}

	private float UCT(MctsNode node, float c) {
		
		float reward = node.getValue() / node.getVisited();
		reward = normalize(reward);
		
		float n = 0;
		if (node.getParent() != null)
			n = node.getParent().getVisited();
		
		float nj = node.getVisited();
		
		float uct = (float) (reward + 2 * c * Math.sqrt((2 * Math.log(n)) / nj));
		
		return uct;
		
	}

	private float normalize(float x) {	
		
		float min = -5; 
		float max = 2000; 
		float range = max - min;
		float inZeroRange = (x - min);
		float norm = inZeroRange / range;
		
		return norm;
	}

	private int defaultPolicy(MctsNode node, MctsNode root) {
		
		// Terminal
		if (!node.getState().isAlive() || 
				node.getState().getGame().wasGhostEaten(ghost))
			return GHOST_LOST_LIFE_VALUE;
		
		int result = runExperimentWithAvgScoreLimit(node, SIM_STEPS);
		
		return result; 

	}

	private void backup(MctsNode v, int score) {
		
		v.setVisited(v.getVisited() + 1);
		v.setValue(v.getValue() + score);
		v.getSimulations().add(score);
		if (v.getParent() != null)
			backup(v.getParent(), score);
		
	}
	

	public static Set<Integer> getJunctions(Game game){
		Set<Integer> junctions = new HashSet<Integer>();
		
		int[] juncArr = game.getJunctionIndices();
		for(Integer i : juncArr)
			junctions.add(i);
		
		junctions.addAll(getTurns(game));
		
		return junctions;
		
	}
	
	private static Collection<? extends Integer> getTurns(Game game) {
		
		List<Integer> turns = new ArrayList<Integer>();
		
		for(Node n : game.getCurrentMaze().graph){
			
			int down = game.getNeighbour(n.nodeIndex, MOVE.DOWN);
			int up = game.getNeighbour(n.nodeIndex, MOVE.UP);
			int left = game.getNeighbour(n.nodeIndex, MOVE.LEFT);
			int right = game.getNeighbour(n.nodeIndex, MOVE.RIGHT);
			
			if (((down != -1) != (up != -1)) || ((left != -1) != (right != -1))){
				turns.add(n.nodeIndex);
			} else if (down != -1 && up != -1 && left != -1 && right != -1){
				turns.add(n.nodeIndex);
			}
			
		}
		
		return turns;
	}
	
	public int runExperimentWithAvgScoreLimit(MctsNode node, int steps) {
		
		Controller<MOVE> pacManController = new FirstCustomAI(false);
		Controller<EnumMap<GHOST,MOVE>> ghostController = ghosts;
    	
		
		Game game = node.getState().getGame().copy();
			
		int livesBefore = game.getPacmanNumberOfLivesRemaining();
		 

		int s = 0;
		int score = 0;
		boolean ghostWasEaten = false;
				
		while(!game.gameOver())
		{
			if (s >= steps && game.getNeighbouringNodes(game.getGhostCurrentNodeIndex(ghost)).length > 2)
				break;
			
			EnumMap<GHOST, MOVE> myMoves = ghostController.getMove(game.copy(),System.currentTimeMillis());
			myMoves.remove(ghost);
			myMoves.put(ghost, node.getMove());

	        game.advanceGame(pacManController.getMove(game.copy(),System.currentTimeMillis()),
	        		myMoves);
	        s++;
	        
			if(closeToPower(game)){
				score += CLOSE_TO_POWER;
			}	

	        int livesAfter = game.getPacmanNumberOfLivesRemaining();
			
			if (livesAfter < livesBefore){
				score += PAC_LOST_LIFE_VALUE;
				livesBefore = livesAfter;
				
			}

			if (game.wasGhostEaten(ghost)){
				ghostWasEaten = true;
				break;
			}else{
				score += GHOST_RETAIN_LIFE_VALUE;
			}
			
			
		}
		
		
		
		if (ghostWasEaten == true){
			score += GHOST_LOST_LIFE_VALUE;
		} 
		
		return score;
	}

	
    //This helper function checks if Ms Pac-Man is close to an available power pill
    private boolean closeToPower(Game game) {
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();

            if (powerPillStillAvailable == null || pacmanNodeIndex == -1) {
                return false;
            }
            if (powerPillStillAvailable && game.getShortestPathDistance(powerPills[i], pacmanNodeIndex) < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }
}