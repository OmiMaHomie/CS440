package src.pas.pacman.agents;

// SYSTEM IMPORTS
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.LinkedList;
import java.util.Map;

import edu.bu.labs.stealth.graph.Vertex;
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
        System.out.println("hi 1");
    }

    //
    // Getter/Setters
    //

    public final Random getRandom() { System.out.println("hi 2"); return this.random; }

    //
    // Methods
    //

    @Override
    public Set<PelletVertex> getOutoingNeighbors(final PelletVertex vertex,
                                                 final GameView game)
    {
        System.out.println("hi 3");
        return null;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst)
    {
        System.out.println("hi 4");
        return 1f;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game)
    {
        System.out.println("hi 5");
        return 1f;
    }

    @Override
    public Path<PelletVertex> findPathToEatAllPelletsTheFastest(final GameView game)
    {
        System.out.println("hi 6");
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
                } else if (game.isLegalPacmanMove(src, Action.WEST)) {
                    validMoves.add(new Coordinate(x - 1, y));
                } else if (game.isLegalPacmanMove(src, Action.NORTH)) {
                    validMoves.add(new Coordinate(x, y - 1));
                } else if (game.isLegalPacmanMove(src, Action.SOUTH)) {
                    validMoves.add(new Coordinate(x, y + 1));
                } else {
                    System.out.println("There's some error in the logic of checking valid neighboring tiles."); // Shouldn't happen.
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
<<<<<<< HEAD

            // If we hit tgt, return the path (tgt --> src, it is reversed)
            if (currentVertex.equals(tgt)) {
=======
            if (currentVertex.equals(tgt)) { //if enter this then i have found the opponent
>>>>>>> de7a7d1da4d61fd0d6114ae89888a2d4947ca01c
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
                System.out.println(path);
                return path;
            }
            //if got to this point not the goal vertex so finding neighbors
            Set<Coordinate> neighbors = getOutgoingNeighbors(currentVertex, game);
            for (Coordinate currentNeighbor : neighbors) { //going through the possible directions
                if (visited.contains(currentNeighbor) == false) { //don't go to visited vertex and also be in bounds
                    visited.add(currentNeighbor);
                    dfsStack.add(currentNeighbor);
                    parents.put(currentNeighbor, currentVertex);
                    }
                }
            }
        }

        // SHOULDN'T EVER REACH THIS POINT.
        System.out.println("If reached this point no dfs path found, I will return new path with just src vertex");
        return new Path<Coordinate>(src);
    }

    @Override
    public void makePlan(final GameView game)
    {
        System.out.println("hi 7");
    }

    @Override
    public Action makeMove(final GameView game)
    {
        System.out.println("hi 8");
        return Action.values()[this.getRandom().nextInt(Action.values().length)];
    }

    @Override
    public void afterGameEnds(final GameView game)
    {
        System.out.println("hi 9");
    }
}