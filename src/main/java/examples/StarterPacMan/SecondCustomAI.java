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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class SecondCustomAI extends PacmanController {
    private static final Random RANDOM = new Random();
    private Game game;
    private int pacmanCurrentNodeIndex;
    private List<Path> paths = new ArrayList<>();

    MOVE pacmanLastMoveMade;
    int pathLengthBase = 70; // 70, 70, 100 // Make it longer when no pills around
    int minGhostDistanceBase = 100; // 80, 100, 100

    public class PathValueComparator implements Comparator<Path> {
        @Override
        public int compare(Path path1, Path path2) {
            return Double.compare(path1.value, path2.value);
        }
    }
    @Override
    public MOVE getMove(Game game, long timeDue) {
        this.game = game;

        int pathLength = pathLengthBase /* + getRandomInt(-50, 10) */;

        // get pacman current location
        pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
        // get packman last move
        pacmanLastMoveMade = game.getPacmanLastMoveMade();
        // get allpossible move
        MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

        // get active pills
        int[] pills = game.getActivePillsIndices();
        // calculate distant to each pills and get the closes one
        double[][] euclideanDistanceOfPills = new double[pills.length][2];
        double[] targetPill = new double[2];
        targetPill[0] = pills[0];
        targetPill[1] = calculateHeuristic(pacmanCurrentNodeIndex, pills[0]);
        for (int i = 0; i < pills.length; i++) {
            int pillIndex = pills[i];
            int pillNodeIndex = game.getPillIndex(pillIndex);
            double distance = calculateHeuristic(pacmanCurrentNodeIndex, pillNodeIndex);
            euclideanDistanceOfPills[i][0] = pillNodeIndex;
            euclideanDistanceOfPills[i][1] = distance;

            if (distance <= targetPill[1]) {
                targetPill[0] = pillNodeIndex;
                targetPill[1] = distance;
            }
        }
        System.out.println(targetPill[1]);

        // Get possible paths
        // Get possible paths
        paths = getPaths(pathLength,(int)targetPill[0]);

        // Sort the path with highest value DESC
        Collections.sort(paths, new PathValueComparator());

        for (Path path : paths) {
            path.summary(game);
        }

        Path bestPath = paths.get(0);
        MOVE bestPathMove = game.getMoveToMakeToReachDirectNeighbour(pacmanCurrentNodeIndex,
                bestPath.start);

        // No pills around while at junction but has safe paths, choose random safe
        if (bestPath.value == 0 && game.isJunction(pacmanCurrentNodeIndex)) {
            // Get only safe paths from paths
            List<MOVE> safeMoves = new ArrayList<>();
            for (Path path : paths) {
                if (path.safe) {
                    MOVE safeMove = game.getMoveToMakeToReachDirectNeighbour(pacmanCurrentNodeIndex, path.start);
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
            bestPathMove = pacmanLastMoveMade;
        }

        // if the current best move is no better than previous move, then we maintain
        // previous move, this is to avoid pacman flickering movement
        else if (bestPathMove != pacmanLastMoveMade) {
            for (Path path : paths) {
                MOVE move = game.getMoveToMakeToReachDirectNeighbour(pacmanCurrentNodeIndex,
                        path.start);

                if (move == pacmanLastMoveMade && path.value == bestPath.value) {
                    bestPathMove = move;
                    break;
                }
            }
        }

        return bestPathMove;
        // // random move algo
        // MOVE move = possibleMoves[RANDOM.nextInt(possibleMoves.length)];
        // System.out.println(pacmanCurrentNodeIndex);
        // System.out.println(Arrays.toString(possibleMoves));
        // System.out.println(move.toString());
        // System.out.println(Arrays.toString(targetPill));

        // // Get path by initiating segement to the node by finding all posible move at
        // // node,calculate the segement end value distant from the target node

        // // check if the segement closer to the target extend if not keep in stack

        // // if it has value higer then the previous item in stack back track

        // // once the segement get to the target node .return the path to get list of
        // move

        // return move;
    }
    private MOVE getRandomMove() {
        MOVE[] possibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex, pacmanLastMoveMade);

        return possibleMoves[RANDOM.nextInt(possibleMoves.length)];
    }


    private double calculateHeuristic(int node, int goalNode) {
        return game.getEuclideanDistance(node, goalNode);
    }

    public class Segment {
        public double distantFromTarget;
        public double distantFromTargetSoFar;
        public int start;
        public int end;
        public int pillsCount = 0;
        public int powerPillsCount = 0;
        public int lengthSoFar;
        public MOVE direction;
        public Segment parent;
        public List<GHOST> ghosts = new ArrayList<>();
        public Color color = Color.GREEN;
        public boolean safe = true;
    }

    public class Path {
        public int start;
        public int end;
        public List<GHOST> ghosts = new ArrayList<GHOST>();
        public int powerPillsCount = 0;
        public int pillsCount = 0;
        public List<Segment> segments = new ArrayList<Segment>();
        public int length;
        public String description = "";
        public boolean safe = true;
        public double value = 0;

        // Important: Segments must be in sequence
        Path(List<Segment> segments) {
            this.segments = segments;
        }

        public void render(Game game) {
            for (Segment segment : segments)
                GameView.addLines(game, segment.color, segment.start, segment.end);
        }

        public void summary(Game game) {
            String ghostsName = "";
            for (GHOST ghost : ghosts)
                ghostsName += ghost.name() + " ";

            String text = description + "::" + " value:" + value + ", safe:" + (safe ? "safe" : "unsafe") + ", pills:"
                    + pillsCount + ", power pills:" + powerPillsCount + ", ghost:" + ghostsName;

            if (!safe)
                System.err.println(text);
            else
                System.out.println(text);

            render(game);
        }

        public void process() {

            int segmentsCount = segments.size();

            if (segmentsCount > 0) {
                Segment firstSegment = segments.get(0);
                Segment lastSegment = segments.get(segmentsCount - 1);
                start = firstSegment.start;
                end = lastSegment.end;
                length = lastSegment.lengthSoFar;
                value = lastSegment.distantFromTargetSoFar;
                int unsafeSegmentsCount = 0;

                for (Segment segment : segments) {
                    if (!segment.ghosts.isEmpty()) {
                        ghosts.addAll(segment.ghosts);
                        for (GHOST ghost : ghosts)
                            if (game.isGhostEdible(ghost)) {
                                int distance = game.getShortestPathDistance(pacmanCurrentNodeIndex,
                                        game.getGhostCurrentNodeIndex(ghost));
                                if (distance < 10)
                                    value += 15;// 15;
                                else
                                    value += 10;// 10;
                            }
                    }

                    if (segment.parent != null && !segment.parent.safe)
                        segment.safe = segment.parent.safe;

                    if (!segment.safe) {
                        unsafeSegmentsCount++;
                        value -= 10;
                        segment.color = Color.RED;
                    }

                    value += segment.powerPillsCount * 1;

                    description += segment.direction.toString() + " ";
                }

                if (unsafeSegmentsCount > 0)
                    safe = false;
            }
        }
    }

    public List<Path> getPaths(int maxPathLength, int targetPillIndex) {
        MOVE[] startingPossibleMoves = game.getPossibleMoves(pacmanCurrentNodeIndex);
        List<Path> paths = new ArrayList<>();
        int minGhostDistance = minGhostDistanceBase /* + getRandomInt(10, 30) */;

        // Start searching from the possible moves at the current pacman location
        for (MOVE startingPossibleMove : startingPossibleMoves) {
            List<Segment> pendingSegments = new ArrayList<Segment>();
            int currentNode = game.getNeighbour(pacmanCurrentNodeIndex, startingPossibleMove);

            Segment currentSegment = new Segment();
            currentSegment.start = currentNode;
            currentSegment.parent = null;
            currentSegment.direction = startingPossibleMove;
            currentSegment.lengthSoFar++;
            currentSegment.distantFromTarget = calculateHeuristic(currentNode, targetPillIndex);

            // Get all ghosts node index in a list
            List<Integer> ghostNodeIndices = new ArrayList<>();
            GHOST[] ghosts = GHOST.values();
            for (GHOST ghost : ghosts)
                ghostNodeIndices.add(game.getGhostCurrentNodeIndex(ghost));
            // Loop each step
            do {

                currentSegment.distantFromTargetSoFar += currentSegment.distantFromTarget;
              

                // Check if length is max
                if (currentSegment.lengthSoFar >= maxPathLength) {
					currentSegment.end = currentNode;
                    // Create a new path and insert segments that make up the path
                    List<Segment> pathSegments = new ArrayList<>();
                    do {
                        pathSegments.add(currentSegment);
                        currentSegment = currentSegment.parent;
                    } while (currentSegment != null);

                    Collections.reverse(pathSegments);
                    Path path = new Path(pathSegments);
                    paths.add(path);

                    // Pop out the latest pending segment and set it as current segment
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
                        segment.distantFromTarget = calculateHeuristic(currentNode, targetPillIndex);
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
                currentSegment.distantFromTargetSoFar += currentSegment.distantFromTarget;

            } while (!pendingSegments.isEmpty() || currentSegment.end != targetPillIndex);
        }
        // Required to calculate the required data in each path
        for (Path path : paths)
            path.process();

        System.out.println("\nPath search complete found " + paths.size() + " path");
        return paths;
    }

}
