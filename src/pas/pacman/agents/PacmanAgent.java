package src.pas.pacman.agents;

// SYSTEM IMPORTS
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.interfaces.ThriftyPelletEater;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;
import edu.bu.pas.pacman.game.DefaultBoard;
import edu.bu.pas.pacman.game.entity.Entity;



public class PacmanAgent
    extends SearchAgent
    implements ThriftyPelletEater
{
    //
    // Fields
    //
    private final Random random;
    private Map<Pair<Coordinate, Coordinate>, Float> distanceCache; // Used to cache distances between coords.
    // final Stack<Coordinate> --> Plan (from SearchAgent)
    // final Coordinate --> tgt Coordinate (from SearchAgent)

    //
    // Constructors
    //

    public PacmanAgent(int myUnitId,
                       int pacmanId,
                       int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();
        this.distanceCache = new HashMap<>();
    }

    //
    // Getter/Setters
    //

    public final Random getRandom() { return this.random; }

    //
    // Methods
    //

    // Gets all possible vertices of pellets eaten from an inital vertex source. DOESN'T check the path's feasibility.
    @Override
    public Set<PelletVertex> getOutoingNeighbors(final PelletVertex vertex,
                                                 final GameView game)
    {
        //
        // RETURN VAR
        //
        Set<PelletVertex> neighbors = new HashSet<>();
    
        Set<Coordinate> remainingPellets = vertex.getRemainingPelletCoordinates();
        
        // For each pellet, we make a new vertex representing that we ate it.
        // Only add reachable pellets
        for (Coordinate pellet : remainingPellets) {
            if (graphSearch(vertex.getPacmanCoordinate(), pellet, game) != null) {
                neighbors.add(vertex.removePellet(pellet));
            }
        }
        
        return neighbors;
    }

    // Calculates the cost of moving from src --> dst vertices
    // We query distanceCache that should've been made up in the A* search before.
    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst)
    {       
        // tgt pellet is simply dst's pacman location
        Coordinate tgtCoord = dst.getPacmanCoordinate();
        
        // SHOULDN'T HAPPEN, but only calls if dst is not valid.
        if (tgtCoord == null) {
            System.out.println("dst coord doesn't exist?");
            return Float.MAX_VALUE;
        }
        
        // Ret the cached distance. If it doesn't exist. Make a new key and set it to MAX
        Pair<Coordinate, Coordinate> key = new Pair<>(src.getPacmanCoordinate(), tgtCoord);
        Float weight = distanceCache.getOrDefault(key, Float.MAX_VALUE);
        return weight;  
    }

    // So basically this method tries to calculate the BEST CASE cost of traversing the maze such that all pellets are eaten
    // For now we simply return the # of pellets (the best-case senario, as # of pellets could be = # of moves needed)
    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game)
    {
        return src.getRemainingPelletCoordinates().size();
    }

    // Makes a path to go through the entire maze to eat every single pellet in the quickest way possible.
    // Uses the A* search
    @Override
    public Path<PelletVertex> findPathToEatAllPelletsTheFastest(final GameView game)
    {
        // Pre compute all distance b4 A* search.
        precomputeDistances(game);

        // Get the init state
        PelletVertex start = new PelletVertex(game);
        
        List<PelletVertex> pathSequence = new ArrayList<>();
        pathSequence.add(start);
        PelletVertex current = start;

        // Always gonna go to the nearest pellet initally too        
        // Keep eating pellets until none left
        while (!current.getRemainingPelletCoordinates().isEmpty()) {
            Coordinate currentPos = current.getPacmanCoordinate();
            Set<Coordinate> remainingPellets = current.getRemainingPelletCoordinates();
            
            // Find the closest reachable pellet
            Coordinate closestPellet = null;
            float shortestDistance = Float.MAX_VALUE;
            
            for (Coordinate pellet : remainingPellets) {
                Path<Coordinate> path = graphSearch(currentPos, pellet, game);
                if (path != null) {
                    float distance = path.getTrueCost();
                    if (distance < shortestDistance) {
                        shortestDistance = distance;
                        closestPellet = pellet;
                    }
                }
            }
            
            // SHOULDN'T HAPPEN, but this is what happens if we can't find a path.
            if (closestPellet == null) {
                System.out.println("No possible path from " + currentPos);
                break;
            }
            
            // Move to the next selected pellet to eat it
            current = current.removePellet(closestPellet);
            pathSequence.add(current);
            
            System.out.println("Pellet @ " + closestPellet + " eaten, " + current.getRemainingPelletCoordinates().size() + " pellets remaining");
        }
        
        // Convert into a path from src --> end
        Path<PelletVertex> resultPath = null;
        for (int i = pathSequence.size() - 1; i >= 0; i--) {
            PelletVertex vertex = pathSequence.get(i);
            if (resultPath == null) {
                resultPath = new Path<>(vertex);
            } else {
                resultPath = new Path<>(vertex, 1f, resultPath);
            }
        }
        
        System.out.println("Took" + (pathSequence.size() - 1) + " moves to eat all pellets");
        
        return resultPath;
    }

    // Helper method to precompute all distances between positions
    // Called by findPathToEatAllPelletsTheFastest, but primarily queried within getEdgeWeights
    private void precomputeDistances(GameView game) {
        distanceCache.clear();
        
        // Get all pellet location including pacman location
        PelletVertex start = new PelletVertex(game);
        Set<Coordinate> allPellets = start.getRemainingPelletCoordinates();
        Coordinate pacmanStart = start.getPacmanCoordinate();
        
        allPellets.add(pacmanStart);
        
        // Convert to list for easier iteration
        List<Coordinate> positionList = new ArrayList<>(allPellets);
        
        // Compute distances between all pairs of positions
        for (int i = 0; i < positionList.size(); i++) {
            for (int j = i; j < positionList.size(); j++) {
                Coordinate pos1 = positionList.get(i);
                Coordinate pos2 = positionList.get(j);
                
                // Skip if its just itself
                if (pos1.equals(pos2)) {
                    continue;
                }
                
                Path<Coordinate> path = graphSearch(pos1, pos2, game);
                if (path != null) {
                    float distance = path.getTrueCost();
                    distanceCache.put(new Pair<>(pos1, pos2), distance);
                    distanceCache.put(new Pair<>(pos2, pos1), distance); // for symmetry
                }
            }
        }
        
        System.out.println("Cached " + distanceCache.size() + " distances between " + allPellets.size() + " positions");
    }
    // Gets all possible neighboring moves from a src coord.
    @Override
    public Set<Coordinate> getOutgoingNeighbors(final Coordinate src,
                                                final GameView game)
    {
        Set<Coordinate> validMoves = new HashSet<Coordinate>();

        // List out all possible actions.
        Action[] actions = {Action.EAST, Action.WEST, Action.NORTH, Action.SOUTH};

        for (Action action : actions) {
            // If we can move a certain way, add it to the set.
            if (game.isLegalPacmanMove(src, action)) {
                int x = src.getXCoordinate();
                int y = src.getYCoordinate();
                
                // Check which direction to add.
                if (game.isLegalPacmanMove(src, Action.EAST)) {
                    validMoves.add(new Coordinate(x + 1, y));
                }
                if (game.isLegalPacmanMove(src, Action.WEST)) {
                    validMoves.add(new Coordinate(x - 1, y));
                }
                if (game.isLegalPacmanMove(src, Action.NORTH)) {
                    validMoves.add(new Coordinate(x, y - 1));
                }
                if (game.isLegalPacmanMove(src, Action.SOUTH)) {
                    validMoves.add(new Coordinate(x, y + 1));
                }
            }
        }

        return validMoves;
    }

    // Does a BFS search to the tgt coordinate.
    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        Queue<Coordinate> bfsQueue = new LinkedList<>(); // queue for bfs queue
        Set<Coordinate> visited = new HashSet<>(); // contains visited nodes
        Map<Coordinate, Coordinate> parents = new HashMap<>(); // Will help to backtrace the path to output path in correct order

        // Init the BFS search
        bfsQueue.add(src);
        visited.add(src);
        parents.put(src, null);


        while(!bfsQueue.isEmpty()){
            // Get latest vertex from the queue.
            Coordinate currentVertex = bfsQueue.poll();
            //System.out.println("Current vertex is " + currentVertex);

            // If we hit tgt, return the path (src --> tgt, it is reversed)
            if (currentVertex.equals(tgt)) {
                List<Coordinate> pathCoordinates = new ArrayList<>();
                Coordinate v = currentVertex;
                
                // Collect coords from tgt --> src
                while (v != null) {
                    pathCoordinates.add(v);
                    v = parents.get(v);
                }
                
                // Build Path from src --> tgt
                Path<Coordinate> path = null;
                for (int i = pathCoordinates.size() - 1; i >= 0; i--) {
                    Coordinate coord = pathCoordinates.get(i);
                    if (path == null) {
                        path = new Path<>(coord);
                    } else {
                        path = new Path<>(coord, 1f, path);
                    }
                }
                
                //
                // IDEAL RETURN PATH
                //
                return path;
            } 

            // Else, go through each potential path and add UNVISITED neighbors to queue.
            Set<Coordinate> neighbors = getOutgoingNeighbors(currentVertex, game);
            for (Coordinate currentNeighbor : neighbors) {
                if (!visited.contains(currentNeighbor)) {
                    visited.add(currentNeighbor);
                    bfsQueue.add(currentNeighbor);
                    parents.put(currentNeighbor, currentVertex);
                }
            }           
        }

        // SHOULDN'T EVER REACH THIS POINT.
        System.out.println("If reached this point no bfs path found, returning null");
        return null;
    }

    // Calculate's the plan the agent needs to do from their own coordinate to the tgt coordinate.
    // Assumes that this agent has fields for the following:
    // - tgt coordinate
    // - plan (Stack<Coordinate>)
    @Override
    public void makePlan(final GameView game)
    {
        // Firstly, call the search algo & init new plan.
        Path<Coordinate> pathToTgt = graphSearch(game.getEntity(this.getPacmanId()).getCurrentCoordinate(),
                                                this.getTargetCoordinate(),
                                                game);
        Stack<Coordinate> newPlan = new Stack<Coordinate>();

        // Have a list temporarily store the output of Path (src --> tgt)
        List<Coordinate> tempPath = new ArrayList<>();
        while (pathToTgt != null && pathToTgt.getDestination() != null) {
            tempPath.add(pathToTgt.getDestination());
            pathToTgt = pathToTgt.getParentPath();
        }

        // Push to stack by going through the list in reverse (STACK: tgt (bottom) --> 2nd to src (top))
        // We skip 1st item since it is the src (moving src --> src is redundant).
        for (int i = 0; i <= tempPath.size() - 2; i++) {
            Coordinate coord = tempPath.get(i);
            //System.out.println("Added " + coord.getXCoordinate() + ", " + coord.getYCoordinate() + " to plan");
            newPlan.push(coord);
        }

        // Set the new plan in the class.
        this.setPlanToGetToTarget(newPlan);
    }

    // Calls all the method we made previously to traverse maze.
    @Override
    public Action makeMove(final GameView game)
    {
        Coordinate currentPos = game.getEntity(this.getPacmanId()).getCurrentCoordinate();
        Stack<Coordinate> plan = this.getPlanToGetToTarget();
        
        // No plan, use A*
        if (plan == null || plan.isEmpty()) {
            System.out.println("Plan's empty, using A*");
            
            // Calculate path of all vertices
            Path<PelletVertex> optimalPath = findPathToEatAllPelletsTheFastest(game);
            
            if (optimalPath != null) {
                // Path<PelletVertex> --> Path<Coordinate>
                Stack<Coordinate> newPlan = convertPelletPathToCoordinatePlan(optimalPath, game);
                this.setPlanToGetToTarget(newPlan);
                plan = this.getPlanToGetToTarget();

                System.out.println("Total moves should be " + (newPlan.size()));
            } else {
                // no path to tgt
                Coordinate targetPellet = findFirstPellet(game);

                if (targetPellet != null) {
                    this.setTargetCoordinate(targetPellet);
                    this.makePlan(game);
                    plan = this.getPlanToGetToTarget();
                } else {
                    // no tgt
                }
            }
        }
        
        // POP coord from plan
        Coordinate nextCoord = plan.pop();
        
        // Go to next coordinate
        try {
            System.out.println("Going to " + nextCoord.getXCoordinate() + ", " + nextCoord.getYCoordinate());

            return Action.inferFromCoordinates(currentPos, nextCoord);
        } catch (Exception e) {
            System.out.println("Can't infer coordinates properly " + e.getMessage());
            
            return Action.values()[this.getRandom().nextInt(Action.values().length)];
        }
    }

    // Helper method to convert PelletVertex path to Coordinate plan
    private Stack<Coordinate> convertPelletPathToCoordinatePlan(Path<PelletVertex> pelletPath, GameView game) {
        Stack<Coordinate> coordinatePlan = new Stack<>();
        
        // Get the seq of pellet vertices
        List<PelletVertex> vertexSequence = new ArrayList<>();
        Path<PelletVertex> current = pelletPath;
        while (current != null) {
            vertexSequence.add(current.getDestination());
            current = current.getParentPath();
        }
        
        // FOR EACH pair of pellet vertices, find path between pos.
        for (int i = 0; i < vertexSequence.size() - 1; i++) {
            PelletVertex currentVertex = vertexSequence.get(i);
            PelletVertex nextVertex = vertexSequence.get(i + 1);
            
            Coordinate startCoord = currentVertex.getPacmanCoordinate();
            Coordinate endCoord = nextVertex.getPacmanCoordinate();
            
            // Find path between the 2 coords
            Path<Coordinate> pathSegment = graphSearch(startCoord, endCoord, game);
            
            if (pathSegment != null) {
                // Turn path to coordinates
                List<Coordinate> segmentCoords = new ArrayList<>();
                Path<Coordinate> segmentCurrent = pathSegment;
                while (segmentCurrent != null) {
                    segmentCoords.add(segmentCurrent.getDestination());
                    segmentCurrent = segmentCurrent.getParentPath();
                }
                
                // Reverse & add to plan
                for (int j = segmentCoords.size() - 2; j >= 0; j--) {
                    coordinatePlan.push(segmentCoords.get(j));
                }
            }
        }
        
        return coordinatePlan;
    }

    // Helper test method to find a pellet in map (goes u --> d first, then l --> r on the map)
    private Coordinate findFirstPellet(GameView game) {
        for (int x = 0; x < game.getXBoardDimension(); x++) {
            for (int y = 0; y < game.getYBoardDimension(); y++) {
                Coordinate coord = new Coordinate(x, y);
                if (game.getCell(coord).getCellState() == DefaultBoard.CellState.PELLET) {
                    return coord;
                }
            }
        }

        // SHOULDN'T HAPPEN as long as there's pellets in game.
        return null;
    }

    @Override
    public void afterGameEnds(final GameView game) { }
}