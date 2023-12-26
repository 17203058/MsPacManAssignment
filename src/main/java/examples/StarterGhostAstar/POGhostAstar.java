package examples.StarterGhostAstar;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import examples.StarterPacMan.TreeSearchPacMan;
import pacman.game.Constants.GHOST;

import pacman.game.GameView;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.awt.Color;

import java.util.Collections;
import java.util.Comparator;

/**
 * Created by piers on 16/02/17.
 */
public class POGhostAstar extends IndividualGhostController {
    private final static float CONSISTENCY = 0.9f; // attack Ms Pac-Man with this probability
    private final static int PILL_PROXIMITY = 15; // if Ms Pac-Man is this close to a power pill, back away
    Random rnd = new Random();

    private int TICK_THRESHOLD;
    private int tickSeen = -1;

    public POGhostAstar(Constants.GHOST ghost) {
        super(ghost);
    }

    private static final Random RANDOM = new Random();
    private Game game;
    private int ghostCurrentNodeIndex;
    MOVE ghostLastMoveMade;
    private int lastPacmanIndex = -1;

    int pathLengthBase = 70; // 70, 70, 100 // Make it longer when no pills around
    int minPacmanDistanceBase = 100; // 80, 100, 100
    private List<Path> paths = new ArrayList<>();
    private static int totalNumberOfGhostEaten = 0;

    private int getRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    // This helper function checks if Ms Pac-Man is close to an available power pill
    private boolean closeToPower(Game game) {
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();

            if (powerPillStillAvailable == null || pacmanNodeIndex == -1) {
                return false;
            }
            if (powerPillStillAvailable
                    && game.getShortestPathDistance(powerPills[i], pacmanNodeIndex) < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        this.game = game;
        if (lastPacmanIndex == -1) {
            lastPacmanIndex = game.getPacManInitialNodeIndex();

        }

        if (game.doesGhostRequireAction(ghost)) // if ghost requires an action
        {

            int currentTick = game.getCurrentLevelTime();
            if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
                lastPacmanIndex = -1;
                tickSeen = -1;
            }

            // Can we see PacMan? If so tell people and update our info
            int pacmanIndex = game.getPacmanCurrentNodeIndex();
            int currentIndex = game.getGhostCurrentNodeIndex(ghost);
            Messenger messenger = game.getMessenger();
            if (pacmanIndex != -1) {
                lastPacmanIndex = pacmanIndex;
                tickSeen = game.getCurrentLevelTime();
                if (messenger != null) {
                    messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN,
                            pacmanIndex, game.getCurrentLevelTime()));
                }
            }

            // Has anybody else seen PacMan if we haven't?
            if (pacmanIndex == -1 && game.getMessenger() != null) {
                for (Message message : messenger.getMessages(ghost)) {
                    if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                        if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer
                                                                                               // information
                            lastPacmanIndex = message.getData();
                            tickSeen = message.getTick();
                        }
                    }
                }
            }
            if (pacmanIndex == -1) {
                pacmanIndex = lastPacmanIndex;
            }

            if (pacmanIndex != -1) {
                lastPacmanIndex = game.getPacmanCurrentNodeIndex();

            }

            // System.out.println(lastPacmanIndex + ":" + pacmanIndex);

            ghostCurrentNodeIndex = game.getGhostCurrentNodeIndex(ghost);
            ghostLastMoveMade = game.getGhostLastMoveMade(ghost);

            // Random path length and minPacmanDistance
            int pathLength = pathLengthBase /* + getRandomInt(-50, 10) */;

            // Get possible paths
            paths = getPaths(pathLength);

            // Sort the path with highest value DESC
            Collections.sort(paths, new PathValueComparator());

            for (Path path : paths) {
                path.summary(game);
            }

            Path bestPath = paths.get(0);
            for (Segment segment : bestPath.segments)
                GameView.addLines(game, Color.CYAN, segment.start, segment.end);
            MOVE bestPathMove = game.getMoveToMakeToReachDirectNeighbour(ghostCurrentNodeIndex, bestPath.start);

            if (bestPath.value == 0 && game.isJunction(ghostCurrentNodeIndex)) {
                // Get only safe paths from paths
                List<MOVE> safeMoves = new ArrayList<>();
                for (Path path : paths) {
                    if (path.safe) {
                        MOVE safeMove = game.getMoveToMakeToReachDirectNeighbour(ghostCurrentNodeIndex, path.start);
                        safeMoves.add(safeMove);
                    }
                }

                // Random safe path
                while (true) {
                    MOVE randomMove = getRandomMove();
                    if (safeMoves.contains(randomMove)) {
                        bestPathMove = randomMove;
                        break;
                    }
                }
            }

            // No safe paths
            else if (bestPath.value < 0) {
                bestPathMove = ghostLastMoveMade;
            }

            // if the current best move is no better than previous move, then we maintain
            // previous move, this is to avoid pacman flickering movement
            else if (bestPathMove != ghostLastMoveMade) {
                for (Path path : paths) {
                    MOVE move = game.getMoveToMakeToReachDirectNeighbour(ghostCurrentNodeIndex, path.start);

                    if (move == ghostLastMoveMade && path.value == bestPath.value) {
                        bestPathMove = move;
                        break;
                    }
                }

                System.out.println("time : " + game.getTotalTime());

                if (game.getNumGhostsEaten() > 0) {
                    totalNumberOfGhostEaten += game.getNumGhostsEaten();

                }
                System.out.println("number of ghost eaten : " + totalNumberOfGhostEaten);
                // return bestPathMove;

            }
            return bestPathMove;

        } 
        return null;
        // else {
        //     Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost),
        //             game.getGhostLastMoveMade(ghost));
        //     return possibleMoves[rnd.nextInt(possibleMoves.length)];
        // }

    }

    private MOVE getRandomMove() {
        MOVE[] possibleMoves = game.getPossibleMoves(ghostCurrentNodeIndex, ghostLastMoveMade);

        return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
    }

    public class PathValueComparator implements Comparator<Path> {
        @Override
        public int compare(Path path1, Path path2) {
            // Compare based on the value first
            int valueComparison = Integer.compare(path2.value, path1.value);

            // If values are equal, then compare based on the total cost
            if (valueComparison == 0) {
                return Double.compare(path1.totalCost, path2.totalCost);
            } else {
                return valueComparison;
            }
        }
    }

    public class Path implements Comparable<Path> {
        public int start;
        public int end;
        public List<GHOST> ghosts = new ArrayList<GHOST>();
        public int powerPillsCount = 0;
        public int pillsCount = 0;
        public List<Segment> segments = new ArrayList<Segment>();
        public int length;
        public String description = "";
        public boolean safe = true;
        public int value = 0;
        public double totalCost;

        // Important: Segments must be in sequence
        Path(List<Segment> segments) {
            this.segments = segments;
        }

        public void render(Game game) {
            for (Segment segment : segments)
                GameView.addLines(game, segment.color, segment.start, segment.end);
        }

        // Calculate heuristic value using Euclidean distance
        private double calculateHeuristic(int targetNode) {
            return game.getShortestPathDistance(end, targetNode);
        }

        // Calculate total cost
        public void calculateTotalCost(int targetNode) {

            double heuristic = calculateHeuristic(targetNode);
            int actualCost=100;
            if (!game.isGhostEdible(ghost)) {
                totalCost = heuristic-actualCost;
            }if (game.isGhostEdible(ghost) || closeToPower(game)) {
                totalCost = heuristic+actualCost;
            }

        }

        // Implement compareTo method for Comparable interface
        @Override
        public int compareTo(Path other) {
            return Double.compare(this.totalCost, other.totalCost);
        }

        public void summary(Game game) {
            String ghostsName = "";
            for (GHOST ghost : ghosts)
                ghostsName += ghost.name() + " ";

            // String text = description + "::" + " value:" + value + " cost:" + totalCost +
            // ", safe:"
            // + (safe ? "safe" : "unsafe") + ", pills:"
            // + pillsCount + ", power pills:" + powerPillsCount + ", ghost:" + ghostsName;

            // if (!safe)
            // System.err.println(text);
            // else
            // System.out.println(text);

            // render(game);
        }

        public void process() {
            // TODO: calculate path value

            int segmentsCount = segments.size();

            if (segmentsCount > 0) {
                Segment firstSegment = segments.get(0);
                Segment lastSegment = segments.get(segmentsCount - 1);
                start = firstSegment.start;
                end = lastSegment.end;
                length = lastSegment.lengthSoFar;
                value = 0;
                calculateTotalCost(lastPacmanIndex);

                int unsafeSegmentsCount = 0;

                for (Segment segment : segments) {

                    value += segment.value;
                    if (segment.parent != null && !segment.parent.safe)
                        segment.safe = segment.parent.safe;

                    if (!segment.safe) {
                        unsafeSegmentsCount++;
                        value -= 100;
                        segment.color = Color.RED;
                    }

                    description += segment.direction.toString() + " ";
                }

                if (unsafeSegmentsCount > 0)
                    safe = false;
            }
        }

    }

    /**
     * InnerFirstCustomAI
     */

    public class Segment {
        public int start;
        public int end;
        public int value;
        public int lengthSoFar;
        public MOVE direction;
        public Segment parent;
        public Color color = Color.GREEN;
        public boolean safe = true;
    }

    public List<Path> getPaths(int maxPathLength) {
        MOVE[] startingPossibleMoves = game.getPossibleMoves(ghostCurrentNodeIndex);
        List<Path> paths = new ArrayList<>();
        int minPacmanDistance = minPacmanDistanceBase /* + getRandomInt(10, 30) */;

        // Start searching from the possible moves at the current pacman location
        for (MOVE startingPossibleMove : startingPossibleMoves) {
            List<Segment> pendingSegments = new ArrayList<Segment>();

            // Step into next node
            int currentNode = game.getNeighbour(ghostCurrentNodeIndex, startingPossibleMove);

            // Create new segment starting from the node next to pacman
            Segment currentSegment = new Segment();
            currentSegment.start = currentNode;
            currentSegment.parent = null;
            currentSegment.direction = startingPossibleMove;
            currentSegment.value = 0;
            currentSegment.lengthSoFar++;

            do {
                // System.out.println(lastPacmanIndex + " " + " " + currentNode);
                if (lastPacmanIndex == currentNode) {

                    if (game.isGhostEdible(ghost) && game.getEuclideanDistance(lastPacmanIndex,
                            currentNode) <= minPacmanDistance || game.getGhostEdibleTime(ghost) > 0
                            || closeToPower(game)) {
                        currentSegment.safe = false;
                        if (currentSegment.parent != null)
                            currentSegment.parent.safe = false;
                    }

                    if (!game.isGhostEdible(ghost) || game.getEuclideanDistance(lastPacmanIndex,
                            currentNode) <= minPacmanDistance) {
                        currentSegment.value += 10;

                    }
                }

                // Check if length is max
                if (currentSegment.lengthSoFar >= maxPathLength) {
                    currentSegment.end = currentNode;
                    // System.out.println("Current segment " + "start:" + currentSegment.start + ",
                    // end:" + currentSegment.end + ", direction:" + currentSegment.direction + ",
                    // ended");

                    // Create a new path and insert segments that make up the path
                    List<Segment> pathSegments = new ArrayList<>();
                    do {
                        pathSegments.add(currentSegment);
                        currentSegment = currentSegment.parent;
                    } while (currentSegment != null);

                    Collections.reverse(pathSegments);
                    Path path = new Path(pathSegments);

                    paths.add(path);

                    if (!pendingSegments.isEmpty()) {
                        currentSegment = pendingSegments.remove(pendingSegments.size() - 1);
                        currentNode = currentSegment.start;
                        currentSegment.lengthSoFar++;

                        continue;
                    } else
                        break;
                }

                MOVE[] possibleMoves = game.getPossibleMoves(currentNode, currentSegment.direction);

                // If neighbor is a junction or a corner, end the current segment and create a
                // new segment
                if (possibleMoves.length > 1
                        || (possibleMoves.length == 1 && possibleMoves[0] != currentSegment.direction)) {
                    currentSegment.end = currentNode;
                    Segment parentSegment = currentSegment;

                    for (int i = 0; i < possibleMoves.length; i++) {
                        MOVE possibleMove = possibleMoves[i];
                        int neighborNode = game.getNeighbour(currentNode, possibleMove);

                        // Create new segment for each neighbor node
                        Segment segment = new Segment();
                        segment.start = neighborNode;
                        segment.direction = possibleMove;
                        segment.parent = parentSegment;
                        segment.value = parentSegment.value;
                        segment.lengthSoFar = currentSegment.lengthSoFar;
                        segment.safe = parentSegment.safe;

                        if (i == 0)
                            currentSegment = segment;
                        else
                            pendingSegments.add(segment);
                    }
                }

                // Step into next node
                currentNode = game.getNeighbour(currentNode, currentSegment.direction);
                currentSegment.lengthSoFar++;

            } while (!pendingSegments.isEmpty() || currentSegment.lengthSoFar <= maxPathLength);
        }

        // Required to calculate the required data in each path
        for (Path path : paths)
            path.process();
        System.out.println(ghost.name());
        System.out.println("Path search complete found " + paths.size() + " path");
        return paths;
    }
}