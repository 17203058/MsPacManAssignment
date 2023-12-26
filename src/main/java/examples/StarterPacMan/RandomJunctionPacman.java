package examples.StarterPacMan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.internal.Node;


public class RandomJunctionPacman extends Controller<MOVE>
{
	
	public MOVE getMove(Game game, long timeDue) {
		
		if (SecondCustomAI.junctions == null)
			SecondCustomAI.junctions = SecondCustomAI.getJunctions(game);
			
		
		MOVE lastMove = game.getPacmanLastMoveMade();
		
		if (inJunction(game))
			return randomAction(lastMove);
		else
			return lastMove;
		
	}
	
	private boolean inJunction(Game game) {
		
		if (SecondCustomAI.junctions.contains(game.getPacmanCurrentNodeIndex()))
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
