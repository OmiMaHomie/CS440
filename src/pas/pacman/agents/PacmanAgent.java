package src.pas.pacman.agents;

// SYSTEM IMPORTS
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.LinkedList;
import java.util.Map;

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
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.pas.pacman.game.DefaultBoard;



public class PacmanAgent
    extends SearchAgent
    implements ThriftyPelletEater
{
    //
    // Fields
    //

    private final Random random;

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

    @Override
    public Set<PelletVertex> getOutoingNeighbors(final PelletVertex vertex,
                                                 final GameView game)
    {
        return null;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst)
    {
        return 1f;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game)
    {
        return 1f;
    }

    @Override
    public Path<PelletVertex> findPathToEatAllPelletsTheFastest(final GameView game)
    {
        return null;
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
    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        Stack<Coordinate> dfsStack = new Stack<>(); // stack for dfs queue
        Set<Coordinate> visited = new HashSet<>(); // contains visited nodes
        Map<Coordinate, Coordinate> parents = new HashMap<>(); // Will help to backtrace the path to output path in correct order

        // Init the DFS search
        dfsStack.add(src);
        visited.add(src);
        parents.put(src, null);


        while(!dfsStack.isEmpty()){
            // Get latest vertex from stack.
            Coordinate currentVertex = dfsStack.pop();
            System.out.println("Current vertex is " + currentVertex);

            // If we hit tgt, return the path (tgt --> src, it is reversed)
            if (currentVertex.equals(tgt)) {
                Path<Coordinate> path = null;
                Coordinate v = currentVertex;
                while (v != null) { //while have parent
                    if (path == null) {
                        path = new Path<>(v);
                    } else {
                        path = new Path<>(v, 1f, path);
                    }
                    v = parents.get(v); //getting parent node from the child
                }

                //
                // This is the ideal RETURN PATH.
                //
                return path;
            } 

            // Else, go through each potential path and add UNVISITED neighbors to stack.
            Set<Coordinate> neighbors = getOutgoingNeighbors(currentVertex, game);
            for (Coordinate currentNeighbor : neighbors) {
                if (!visited.contains(currentNeighbor)) {
                    visited.add(currentNeighbor);
                    dfsStack.add(currentNeighbor);
                    parents.put(currentNeighbor, currentVertex);
                }
            }           
        }

        // SHOULDN'T EVER REACH THIS POINT.
        System.out.println("If reached this point no dfs path found, I will return new path with just src vertex");
        return new Path<Coordinate>(src);
    }

    // TODO
    @Override
    public void makePlan(final GameView game)
    {
        return;
    }

    @Override
    public Action makeMove(final GameView game)
    {
        return Action.values()[this.getRandom().nextInt(Action.values().length)];
    }

    @Override
    public void afterGameEnds(final GameView game)
    {
    }
}