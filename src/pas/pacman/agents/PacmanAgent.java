package src.pas.pacman.agents;

// SYSTEM IMPORTS
import java.util.*;

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
    private Map<String, Float> cachedDistances = new HashMap<>(); // Used to lookup edge weights between states.
    // final Stack<Coordinate> --> Plan (from SearchAgent)
    // final Coordinate --> tgt Coordinate (from SearchAgent)

    //
    // Constructors
    //

    public PacmanAgent(int myUnitId, int pacmanId, int ghostChaseRadius) {
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

    // Gets all possible PelletVertices from a vertex source.
    @Override
    public Set<PelletVertex> getOutoingNeighbors(final PelletVertex vertex, final GameView game) {
        Set<PelletVertex> neighboringVertices = new HashSet<PelletVertex>();

        // Add each coord from each possible children of vertex (getRemainingPelletCoordinates())
        for (Coordinate coord : vertex.getRemainingPelletCoordinates()) {
            neighboringVertices.add(vertex.removePellet(coord));
        }

        return neighboringVertices;
    }

    // Calculates the cost of moving from src --> dst vertices
    // Will attempt to query cache for distance. If it doesn't exist, ret MAX_VAL to search for distance (should be querable later).
    @Override
    public float getEdgeWeight(final PelletVertex src, final PelletVertex dst) {       
        String cacheKey = getCacheKey(src.getPacmanCoordinate(), dst.getPacmanCoordinate());
    
        // Check cache first
        if (cachedDistances.containsKey(cacheKey)) {
            return cachedDistances.get(cacheKey);
        } else {
            // If we reach this point, the cache doesn't contain the key, return MAX_VAL
            return Float.MAX_VALUE;
        }
    }

    // Helper method to for getEdgeWeight
    // Converts a pair of Coords into a string that can be compared.
    private String getCacheKey(Coordinate a, Coordinate b) {
        int x1 = a.getXCoordinate();
        int y1 = a.getYCoordinate();
        int x2 = b.getXCoordinate();
        int y2 = b.getYCoordinate();
        
        // Always store with smaller coordinates first
        // Ensures symmtery so keys don't need to be added twice
        if (x1 < x2 || (x1 == x2 && y1 < y2)) {
            return x1 + "," + y1 + ":" + x2 + "," + y2;
        } else {
            return x2 + "," + y2 + ":" + x1 + "," + y1;
        }
    }

    // So basically this method tries to calculate the BEST CASE cost of traversing the maze such that all pellets are eaten
    // For now we simply return the # of pellets (the best-case senario, as # of pellets could be = # of moves needed)
    @Override
    public float getHeuristic(final PelletVertex src, final GameView game) {
        return src.getRemainingPelletCoordinates().size();
    }

    // A helper class for findPathToEatAllPelletsTheFastest
    // Represents a vertex and its given values in the A* search.
    // Implement Comparable so that it can be put in a Priority Queue
    class Node implements Comparable<Node> {
        //
        // Fields
        //
        public PelletVertex vertex;
        public Node parent = null;
        public Float g = 0f;
        public Float h = 0f;
        // f is just g + h

        //
        // Constructor(s)
        //
        public Node(PelletVertex vertex) {
            this.vertex = vertex;
        }

        //
        // Methods
        //
        public Float f() {
            return g + h;
        }

        @Override
        public boolean equals(Object obj) {
            // obj is literally this obj
            if (this == obj) {
                return true;
            }

            // obj is not of type Node
            if (obj == null || getClass() != obj.getClass()) { 
                return false;
            }
            
            // Explicitly turn obj to a node, now that we know its a node, and check its the vertices are the same.
            Node node = (Node) obj;
            return this.vertex.equals(node.vertex);
        }

        @Override
        public int hashCode() {
            return vertex.hashCode();
        }

        @Override
        public int compareTo(Node other) {
            return Float.compare(this.f(), other.f());
        }
    }

    // Makes a path to go through the entire maze to eat every single pellet in the quickest way possible.
    // Uses a greedy first-pellet approach
    // Will implement A* later.
    @Override
    public Path<PelletVertex> findPathToEatAllPelletsTheFastest(final GameView game)
    {
        // Init the vars for the search
        PriorityQueue<Node> open = new PriorityQueue<>();
        List<Node> closed = new ArrayList<>();
        Node start = new Node(new PelletVertex(game));

        start.g = 0f;
        start.h = getHeuristic(start.vertex, game);
        // start.f() is implicilty set
        // start.parent should be null

        open.add(start);

        while (open.size() != 0) {
            // Find node within open w/ LOWEST f
            Node current = open.poll();
            if (current == null) {
                //System.out.println("In A* search, no valid node w/ valid f found.");
                return null;
            }

            // Reached goal state, begin reconstructing path.
            if (current.vertex.getRemainingPelletCoordinates().isEmpty()) {
                //
                // IDEAL RETURN PATH
                //
                return reconstructPath(current);
            }

            // Moving current from open --> closed
            // open.remove(current);
            closed.add(current);

            // Now adding all vertices adj to current.vertex
            Set<PelletVertex> adjPellets = getOutoingNeighbors(current.vertex, game);
            List<Node> neighbors = new ArrayList<>();
            for (PelletVertex neighbor : adjPellets) {
                Node adjNode = new Node(neighbor);
                neighbors.add(adjNode);
            }
            for (Node neighbor : neighbors) {
                // neighbor alr eval'd
                if (closed.contains(neighbor)) {
                    continue;
                }

                // Calc tentative g from current --> neighbor
                // Try to get cached result. If cached result is inconclusive, then we search the graph and cache that result.
                // Don't need to add 2 keys since the way the key gets extracted ensures symmetry.
                Float distance = getEdgeWeight(current.vertex, neighbor.vertex);
                if (distance == Float.MAX_VALUE) {
                    distance = graphSearch(current.vertex.getPacmanCoordinate(), neighbor.vertex.getPacmanCoordinate(), game).getTrueCost();

                    String cacheKey = getCacheKey(current.vertex.getPacmanCoordinate(), neighbor.vertex.getPacmanCoordinate());

                    cachedDistances.put(cacheKey, distance);
                    //System.out.println("Inputted key " + cacheKey + " w/ distance " + distance);
                } else {
                    //System.out.println("Grabbed distance of " + distance + "from cache");
                }
                Float tentative_g = current.g + distance;

                // Add neighbor to be eval'd
                // If @ this point, this path is better
                if (!open.contains(neighbor)) {
                    neighbor.parent = current;
                    neighbor.g = tentative_g;
                    neighbor.h = getHeuristic(neighbor.vertex, game);
                    // neighbor.f implicitly set

                    open.add(neighbor);
                } else if (tentative_g >= neighbor.g) { // This path isn't better
                    continue;
                }                
            }
        }

        // If we reached this point, no path was found
        //System.out.println("The A* search didn't return a viable path.");
        return null;
    }

    // A helper method for findPathToEatAllPelletsTheFastest
    // Reconstruct the path.
    private Path<PelletVertex> reconstructPath(Node goal) {
        Path<PelletVertex> path = null;
        List<Node> pathNodes = new ArrayList<>();

        // Build list of PelletVertices goal --> init
        while (goal != null) {
            pathNodes.add(goal);
            goal = goal.parent;
        }

        // Builds the path of PelletVertices from init --> goal
        for (int i = pathNodes.size() - 1; i >= 0; i--) {
            if (path == null) {
                path = new Path<>(pathNodes.get(i).vertex);
            } else {
                path = new Path<>(pathNodes.get(i).vertex, pathNodes.get(i).g, path);
            }
        }

        if (path == null) {
            //System.out.println("The path created by the A* search is empty or didn't traverse at all");
        }    
        return path;
    }

    // Gets all possible neighboring moves from a src coord.
    @Override
    public Set<Coordinate> getOutgoingNeighbors(final Coordinate src, final GameView game) {
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

    // Helper method for graphSearch
    // Helps store coords & their cul. cost
    class PathCost {
        //
        // FIELDS
        //
        public Coordinate coord;
        public Float cost;

        //
        // CONSTRUCTOR(S)
        //
        public PathCost(Coordinate coord, Float cost) {
            this.coord = coord;
            this.cost = cost;
        }
    }

    // Does Dijstrak's algorithm search to the tgt coordinate.
    @Override
    public Path<Coordinate> graphSearch(final Coordinate src, final Coordinate tgt, final GameView game) {
        PriorityQueue<PathCost> toExplore = new PriorityQueue<>(
            Comparator.comparing(pc -> pc.cost)
        );
        Set<Coordinate> visited = new HashSet<>();
        Map<Coordinate, Coordinate> parents = new HashMap<>();
        Map<Coordinate, Float> costs = new HashMap<>(); // min costs to each coord

        toExplore.add(new PathCost(src, 0f));
        costs.put(src, 0f);
        parents.put(src, null);

        Boolean isPathfindingDone = false;
        // goalVertex is tgt

        while (!toExplore.isEmpty() && !isPathfindingDone) {
            PathCost currentPC = toExplore.poll();
            Coordinate current = currentPC.coord;

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            if (current.equals(tgt)) {
                isPathfindingDone = true;
                break;
            }

            for (Coordinate neighbor : getOutgoingNeighbors(current, game)) {
                if (!visited.contains(neighbor)) {
                    // Every edge is 1f for now.
                    Float newCost = costs.get(current) + 1f;

                    if (!costs.containsKey(neighbor) || newCost < costs.get(neighbor)) {
                        costs.put(neighbor, newCost);
                        parents.put(neighbor, current);
                        toExplore.add(new PathCost(neighbor, newCost));
                    }
                }
            }
        }

        if (isPathfindingDone) {
            List<Coordinate> pathCoordinates = new ArrayList<>();
            Coordinate v = tgt;

            // Collect coords from tgt --> src
            while (v != null) {
                pathCoordinates.add(v);
                v = parents.get(v);
            }

            // Build path from src --> tgt
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

        } else { // If reached this point, the Dijstrka search failed.
            System.out.println("Dijstrka search failed.");
            return null;
        }
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
        
        // No plan exists, run search
        if (plan == null || plan.isEmpty()) {
            //System.out.println("No plan found, doing A* search...");
            
            Path<PelletVertex> optimalPelletPath = findPathToEatAllPelletsTheFastest(game);
            
            if (optimalPelletPath != null) {
                // Convert the PelletVertex path to a executable Coordinate plan
                Stack<Coordinate> newPlan = convertPelletPathToCoordinatePlan(optimalPelletPath, game);
                
                if (newPlan != null && !newPlan.isEmpty()) {
                    this.setPlanToGetToTarget(newPlan);
                    plan = this.getPlanToGetToTarget();
                    //System.out.println("New plan contains " + newPlan.size() + " moves");
                } else {
                    //System.out.println("Couldn't convert Path<PelletVertex> --> Stack<Coordinate>");
                    return Action.values()[this.getRandom().nextInt(Action.values().length)];
                }
            } else {
                //System.out.println("A* Search failed");
                return Action.values()[this.getRandom().nextInt(Action.values().length)];
            }
        }
        
        // Execute the next step in the plan
        if (plan != null && !plan.isEmpty()) {
            Coordinate nextCoord = plan.pop();
            
            // Verify the move is still valid
            if (isValidMove(currentPos, nextCoord, game)) {
                try {
                    //System.out.println("Moving from " + currentPos + " to " + nextCoord);
                    return Action.inferFromCoordinates(currentPos, nextCoord);
                } catch (Exception e) {
                    //System.out.println("Couldn't infer action: " + e.getMessage());
                    return Action.values()[this.getRandom().nextInt(Action.values().length)];
                }
            } else {
                //System.out.println("Plan somehow got invalid");
                this.setPlanToGetToTarget(null);
                return Action.values()[this.getRandom().nextInt(Action.values().length)];
            }
        }
        
        // Fallback to rnd move if everything else fails
        return Action.values()[this.getRandom().nextInt(Action.values().length)];
    }

    // Helper method for makeMove
    // Convert the path of PelletVertex into a Coordinate Plan
    private Stack<Coordinate> convertPelletPathToCoordinatePlan(Path<PelletVertex> pelletPath, GameView game) {
        Stack<Coordinate> coordinatePlan = new Stack<>();
        
        if (pelletPath == null) {
            return coordinatePlan;
        }
        
        // Extract the sequence of pellet vertices in from goal --> init
        List<PelletVertex> vSeq = new ArrayList<>();
        Path<PelletVertex> currentPath = pelletPath;
        
        while (currentPath != null) {
            vSeq.add(currentPath.getDestination());
            currentPath = currentPath.getParentPath();
        }
        
        // For each consecutive pair of vertices, find the path between their pacman pos
        for (int i = 0; i < vSeq.size() - 1; i++) {
            PelletVertex currentVertex = vSeq.get(i);
            PelletVertex nextVertex = vSeq.get(i + 1);
            
            Coordinate startCoord = currentVertex.getPacmanCoordinate();
            Coordinate endCoord = nextVertex.getPacmanCoordinate();
            
            // Get actual path between coords
            Path<Coordinate> pathSeg = graphSearch(startCoord, endCoord, game);
            
            if (pathSeg != null) {
                // Turn path segment to coord seq
                List<Coordinate> segmentCoords = extractCoordinatesFromPath(pathSeg);
                
                // Add all intermediate coord to the plan (skip the first as it's the current position)
                for (int j = 1; j < segmentCoords.size(); j++) {
                    coordinatePlan.push(segmentCoords.get(j));
                }
            } else {
                //System.out.println("No path found from " + startCoord + " to " + endCoord);
            }
        }
        
        return coordinatePlan;
    }

    // Helper method for convertPelletPathToCoordinatePlan
    // Extract coordinates from a Path<Coordinate>
    private List<Coordinate> extractCoordinatesFromPath(Path<Coordinate> path) {
        List<Coordinate> coordinates = new ArrayList<>();
        Path<Coordinate> current = path;
        
        // Build list from end to start
        while (current != null) {
            coordinates.add(current.getDestination());
            current = current.getParentPath();
        }
        
        // // Reverse to get start to end order
        // Collections.reverse(coordinates);
        return coordinates;
    }

    // Helper method for makeMove
    // Just checks if move is valid
    private boolean isValidMove(Coordinate from, Coordinate to, GameView game) {
        if (from == null || to == null || !game.isInBounds(to)) {
            return false;
        }
        
        try {
            Action action = Action.inferFromCoordinates(from, to);
            return game.isLegalPacmanMove(from, action);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterGameEnds(final GameView game) { }
}