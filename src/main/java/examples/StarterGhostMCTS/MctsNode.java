package examples.StarterGhostMCTS;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import examples.StarterPacMan.FirstCustomAI;
import examples.StarterPacMan.SecondCustomAI; 
import pacman.controllers.Controller;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class MctsNode {

	MctsState state;
	MctsNode parent;
	MOVE move;
	int visited;
	float value;
	List<MctsNode> children;
	int directions;
	int time;
	List<Integer> simulations;
	
	private boolean up = false;
	private boolean right = false;
	private boolean down = false;
	private boolean left = false;

	private Constants.GHOST ghost;
	
	public MctsNode(MctsState state, MctsNode parent, MOVE move, int time, GHOST ghost) {
		super();
		this.state = state;
		this.parent = parent;
		this.move = move;
		this.visited = 0;
		this.directions = getDirections();
		this.value = 0;
		this.children = new ArrayList<MctsNode>();
		this.time = time;
		this.simulations = new ArrayList<Integer>();
		this.ghost = ghost;
	}
	

	public MctsNode expand() {
		
		int gho = state.getGame().getGhostCurrentNodeIndex(ghost);
		int junction = -1;
		MOVE nextMove = null;
		
		if (!state.isAlive()){
			return this;
		}
		
		// Closest junctions // the children will expanded in this order: up_child, right_child, down_child, left_child
		if (!up && state.getGame().getNeighbour(gho, MOVE.UP) != -1 && (parent == null || move.opposite() != MOVE.UP)){
			junction = closestJunction(MOVE.UP);
			nextMove = MOVE.UP;
		} else if (!right && state.getGame().getNeighbour(gho, MOVE.RIGHT) != -1 && (parent == null || move.opposite() != MOVE.RIGHT)){
			junction = closestJunction(MOVE.RIGHT);
			nextMove = MOVE.RIGHT;
		} else if (!down && state.getGame().getNeighbour(gho, MOVE.DOWN) != -1 && (parent == null || move.opposite() != MOVE.DOWN)){
			junction = closestJunction(MOVE.DOWN);
			nextMove = MOVE.DOWN;
		} else if (!left && state.getGame().getNeighbour(gho, MOVE.LEFT) != -1 && (parent == null || move.opposite() != MOVE.LEFT)){
			junction = closestJunction(MOVE.LEFT);
			nextMove = MOVE.LEFT;
		}
		
		if (junction == -1){
			return this;
		}
		
		if (junction != -1){
			updateDirection(nextMove);
			
			MctsState childState = runExperimentUntilJunction(new SecondCustomAI(), new AggressiveGhosts(), state.getGame(), junction, nextMove);
			if (childState == null || childState.getGame() == null){
				return this;
			}
			
			int to = childState.getGame().getGhostCurrentNodeIndex(ghost);
			int distance = (int) state.getGame().getDistance(gho, to, DM.PATH);
			
			MctsNode child = new MctsNode(childState, this, nextMove, time + distance, ghost);
			children.add(child);
			return child;
		}

		return this;
		
	}

	private MctsState runExperimentUntilJunction(Controller<MOVE> pacmanController, Controller<EnumMap<GHOST,MOVE>> ghostController, Game game, int junction, MOVE move) {
		
		Game clone = game.copy();
		/*
		clone.advanceGame(move,
        		ghostController.getMove(clone.copy(),System.currentTimeMillis()));
		*/
		
		int now = clone.getGhostCurrentNodeIndex(ghost);
		int start = now;
		while(now != junction){

			int last = now;

			EnumMap<GHOST, MOVE> myMoves = ghostController.getMove(game.copy(),System.currentTimeMillis());
			myMoves.remove(ghost);
			myMoves.put(ghost, move);
			
			clone.advanceGame(pacmanController.getMove(clone.copy(),System.currentTimeMillis()),
		    		myMoves);
		    
			now = clone.getGhostCurrentNodeIndex(ghost);
			
			
			if (clone.wasGhostEaten(ghost))
				return new MctsState(false, clone);

			if (now == last){
				//System.out.println("ERROR: Junction not found");
				break;
			}
			
		}
		
		return new MctsState(true, clone);
		
	}


	private void updateDirection(MOVE move) {
		switch(move) {
		case UP : up = true; break;
		case DOWN : down = true; break;
		case RIGHT : right = true; break;
		case LEFT : left = true; break;
		}
	}


	private int closestJunction(MOVE move) {
		
		int from = state.getGame().getGhostCurrentNodeIndex(ghost);
		int current = from;
		if (current == -1)
			return -1;
		
		while(!POGhostMCTS.junctions.contains(current) || current == from){
			
			int next = state.getGame().getNeighbour(current, move);
			
			if (next == from)
				return -1;
			
			current = next;
			if (current == -1)
				return -1;
			
		}
		
		return current;
		
	}
	
	private int getDirections() {
		if (!state.isAlive() || state.getGame() == null || ghost == null) {
			return 0;
		}
	
		int node = state.getGame().getGhostCurrentNodeIndex(ghost);
	
		if (node < 0) {
			// Handle the case where node is invalid or ghost is not on the map.
			return 0;
		}
	
		int[] neighbors = state.getGame().getNeighbouringNodes(node);
		int count = 0;
	
		for (Integer i : neighbors) {
			if (parent == null || state.getGame().getMoveToMakeToReachDirectNeighbour(node, i) != move.opposite()) {
				count++;
			}
		}
	
		return count;
	}
	
	
	public MctsNode getParent() {
		return parent;
	}

	public void setParent(MctsNode parent) {
		this.parent = parent;
	}

	public MctsState getState() {
		return state;
	}

	public void setState(MctsState state) {
		this.state = state;
	}

	public MOVE getMove() {
		return move;
	}

	public void setMove(MOVE move) {
		this.move = move;
	}

	public int getVisited() {
		return visited;
	}

	public void setVisited(int visited) {
		this.visited = visited;
	}

	public List<MctsNode> getChildren() {
		return children;
	}

	public void setChildren(List<MctsNode> children) {
		this.children = children;
	}

	public void setDirections(int directions) {
		this.directions = directions;
	}

	public boolean isExpandable() {
		return directions != children.size() && state.isAlive();
	}


	public float getValue() {
		return value;
	}


	public void setValue(float value) {
		this.value = value;
	}


	public String print(int level) {
		
		String out = "";
		for(int n = 0; n < level; n++){
			out += "\t";
		}
		out += "<node move="+move+" score=" + state.getGame().getScore() + " avg=(" + value + "/" + visited + ")"+value/visited+" visited="+visited + " time=" + time;
		/*
		for(Integer i : simulations)
			out += "(" + i + ")";
		*/
		if (children.isEmpty()){
			out += "/>\n";
		} else {
			out += ">\n";
		}
		
		int next = level+1;
		for(MctsNode child : children){
			
			out += child.print(next);
			
		}
		
		if (!children.isEmpty()){
			for(int n = 0; n < level; n++){
				out += "\t";
			}
			out += "</node>\n";
		}
		
		return out;
		
	}


	public int getTime() {
		return time;
	}


	public void setTime(int time) {
		this.time = time;
	}


	public List<Integer> getSimulations() {
		return simulations;
	}


	public void setSimulations(List<Integer> simulations) {
		this.simulations = simulations;
	}
	
}
