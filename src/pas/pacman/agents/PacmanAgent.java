package src.pas.pacman.agents;

// SYSTEM IMPORTS
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

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

    @Override
    public Set<Coordinate> getOutgoingNeighbors(final Coordinate src,
                                                final GameView game)
    {
        int xCord = src.getXCoordinate();
        int yCord = src.getYCoordinate();
        Set<Coordinate> validMoves = new HashSet<Coordinate>();
        if (game.isLegalPacmanMove(src, Action.EAST)) {
            validMoves.add(new Coordinate(xCord - 1, yCord));
        }
        if (game.isLegalPacmanMove(src, Action.WEST)) {
            validMoves.add(new Coordinate(xCord + 1, yCord));
        }
        if (game.isLegalPacmanMove(src, Action.NORTH)) {
            validMoves.add(new Coordinate(xCord, yCord + 1));
        }
        if (game.isLegalPacmanMove(src, Action.SOUTH)) {
            validMoves.add(new Coordinate(xCord, yCord - 1));
        }
        return null;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        return null;
    }

    @Override
    public void makePlan(final GameView game)
    {

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