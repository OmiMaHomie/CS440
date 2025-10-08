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
    private Set<Coordinate> allReachableCoords = null; // All valid moves from the init state.
    private Map<Pair<Coordinate, Coordinate>, Float> distanceCache = new HashMap<>(); // Used to cache distances between coords.
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
        return distanceCache.getOrDefault(key, Float.MAX_VALUE);
    }

    // So basically this method tries to calculate the BEST CASE cost of traversing the maze such that all pellets are eaten
    // Uses a MST to calculate the best-case path to traverse through all pellets
    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game)
    {
        Set<Coordinate> remainingPellets = src.getRemainingPelletCoordinates();
    
        // If no pellets left, ret 0
        if (remainingPellets.isEmpty()) {
            return 0f;
        }
        
        // If only one pellet left, ret the distance to it
        if (remainingPellets.size() == 1) {
            Coordinate lastPellet = remainingPellets.iterator().next();
            Path<Coordinate> path = graphSearch(src.getPacmanCoordinate(), lastPellet, game);
            return path != null ? path.getTrueCost() : Float.MAX_VALUE;
        }
        
        // Calculate MST cost of all remaining pellets
        // The heuristic is just the distance to reach the MST
        float mstCost = calculateMST(src.getPacmanCoordinate(), remainingPellets, game);
        return mstCost;
    }

    // Helper method for getHeuristics
    // Calculate the MST cost of traversing through every pellet.
    private float calculateMST(Coordinate pacmanPos, Set<Coordinate> pellets, GameView game) {
        // Ret 0 if no pellets
        if (pellets.size() <= 1) {
            return 0f;
        }
        
        List<Coordinate> pelletList = new ArrayList<>(pellets);
        pelletList.add(pacmanPos);
        int n = pelletList.size();
        
        // Vars used for the MST
        boolean[] inMST = new boolean[n];
        float[] minEdge = new float[n];
        Arrays.fill(minEdge, Float.MAX_VALUE);
        minEdge[0] = 0f;
        float totalCost = 0f;
        
        for (int i = 0; i < n; i++) {
            // Find a vertex with min. cost thats NOT in MST currently
            int u = -1;
            for (int j = 0; j < n; j++) {
                if (!inMST[j] && (u == -1 || minEdge[j] < minEdge[u])) {
                    u = j;
                }
            }
            
            inMST[u] = true;
            totalCost += minEdge[u];
            
            // Update minEdge for adj vertices
            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    float distance = getDistanceBetweenPellets(pelletList.get(u), 
                                                            pelletList.get(v), game);
                    if (distance < minEdge[v]) {
                        minEdge[v] = distance;
                    }
                }
            }
        }
        
        return totalCost;
    }

    // Helper method for calculateMST
    // Simply calls graphSearch from pellet1 --> pellet2 and returns its path cost
    private float getDistanceBetweenPellets(Coordinate pellet1, Coordinate pellet2, GameView game) {
        Path<Coordinate> path = graphSearch(pellet1, pellet2, game);
        return path != null ? path.getTrueCost() : Float.MAX_VALUE;
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
        
        // Get all possible pellet & pacman locations
        PelletVertex start = new PelletVertex(game);
        Set<Coordinate> allPellets = start.getRemainingPelletCoordinates();
        Coordinate pacmanStart = start.getPacmanCoordinate();
        
        // From prev locations, compute their path
        for (Coordinate pellet : allPellets) {
            Path<Coordinate> path = graphSearch(pacmanStart, pellet, game);
            if (path != null) {
                distanceCache.put(new Pair<>(pacmanStart, pellet), path.getTrueCost());
            }
        }
        
        // Now from prev paths, compute their distance
        List<Coordinate> pelletList = new ArrayList<>(allPellets);
        for (int i = 0; i < pelletList.size(); i++) {
            for (int j = i + 1; j < pelletList.size(); j++) {
                Coordinate pellet1 = pelletList.get(i);
                Coordinate pellet2 = pelletList.get(j);
                
                Path<Coordinate> path = graphSearch(pellet1, pellet2, game);
                if (path != null) {
                    distanceCache.put(new Pair<>(pellet1, pellet2), path.getTrueCost());
                    distanceCache.put(new Pair<>(pellet2, pellet1), path.getTrueCost()); // for symmetry
                }
            }
        }
        
        System.out.println("Cached a total of " + distanceCache.size() + " distances");
    }

    // Gets ALL POSSIBLE, VALID coords we can reach from a src coord.
    // We essentially do 1 big BFS search. Should only occur once in the game tho, every other time it should return cached location.
    // Caches all valid moves from src INCLUDING src.
    @Override
    public Set<Coordinate> getOutgoingNeighbors(final Coordinate src, final GameView game)
    {
        // If we already have the global cache, just return it (excluding src)
        if (allReachableCoords != null) {
            Set<Coordinate> result = new HashSet<>(allReachableCoords);
            result.remove(src);
            return result;
        }

        // Do the BFS search.
        Set<Coordinate> valid = new HashSet<>();
        Queue<Coordinate> moveNext = new LinkedList<>();
        Set<Coordinate> visited = new HashSet<>();
        Action[] actions = {Action.EAST, Action.WEST, Action.NORTH, Action.SOUTH};

        moveNext.add(src);
        visited.add(src);
        valid.add(src); // src included in the search, will later be removed on return

        while (!moveNext.isEmpty()) {
            Coordinate currentCoord = moveNext.poll();
            
            for (Action action : actions) {
                if (game.isLegalPacmanMove(currentCoord, action)) {
                    Coordinate neighbor = getNeighborCoordinate(currentCoord, action);
                    
                    // Don't add node alr visited.
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        valid.add(neighbor);
                        moveNext.add(neighbor);
                    }
                }
            }
        }

        // Cache the global set
        allReachableCoords = new HashSet<>(valid);
        
        // Return the resultant set EXCLUDING src
        Set<Coordinate> result = new HashSet<>(valid);
        result.remove(src);
        return result;
    }

    // Helper method for getOutgoingNeighbors(Coord)
    // Gets the resulting coord after moving.
    private Coordinate getNeighborCoordinate(Coordinate current, Action action) {
        int x = current.getXCoordinate();
        int y = current.getYCoordinate();
        
        switch (action) {
            case EAST:
                return new Coordinate(x + 1, y);
            case WEST:
                return new Coordinate(x - 1, y);
            case NORTH:
                return new Coordinate(x, y - 1);
            case SOUTH:
                return new Coordinate(x, y + 1);
            default:
                return current; // SHOULDN'T EVER HAPPEN
        }
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

    @Override
    public Action makeMove(final GameView game)
    {
        return Action.values()[this.getRandom().nextInt(Action.values().length)];
    }

    @Override
    public void afterGameEnds(final GameView game) { }
}