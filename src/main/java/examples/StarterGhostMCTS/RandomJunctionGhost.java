package examples.StarterGhostMCTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import examples.StarterPacMan.SecondCustomAI;
import pacman.controllers.Controller;
import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.internal.Node;


public class RandomJunctionGhost extends IndividualGhostController
{
	private Constants.GHOST ghost;

	public RandomJunctionGhost(GHOST ghost){
		super(ghost);
	}
	
	public MOVE getMove(Game game, long timeDue) {
		
		if (POGhostMCTS.junctions == null)
		POGhostMCTS.junctions = POGhostMCTS.getJunctions(game);
			
		
		MOVE lastMove = game.getGhostLastMoveMade(ghost);
		
		if (inJunction(game))
			return randomAction(lastMove);
		else
			return lastMove;
		
	}
	
	private boolean inJunction(Game game) {
		
		if (POGhostMCTS.junctions.contains(game.getGhostCurrentNodeIndex(ghost)))
			return true;
		
		return false;
	}

	private MOVE randomAction(MOVE except) {
		MOVE move = null;
		
		while(move == null){
			int random = (int) (Math.random() * 4);
			
			switch(random){
			case 0: move = MOVE.UP; break;
			case 1: move = MOVE.RIGHT; break;
			case 2: move = MOVE.DOWN; break;
			case 3: move = MOVE.LEFT; break;
			}
			
			if (move == except)
				move = null;
			
		}
		
		return move;
	}

	
	
	

}
