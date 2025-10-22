package src.pas.othello.agents;


// SYSTEM IMPORTS
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.bu.labs.rttt.game.Constants.Rendering.Player;
// JAVA PROJECT IMPORTS
import edu.bu.pas.othello.agents.Agent;
import edu.bu.pas.othello.agents.TimedTreeSearchAgent;
import edu.bu.pas.othello.game.Game.GameView;
import edu.bu.pas.othello.game.Game;
import edu.bu.pas.othello.game.PlayerType;
import edu.bu.pas.othello.traversal.Node;
import edu.bu.pas.othello.utils.Coordinate;


public class OthelloAgent
    extends TimedTreeSearchAgent
{

    public static class OthelloNode
        extends Node
    {
        public OthelloNode(final PlayerType maxPlayerType,  // who is MAX (me)
                           final GameView gameView,         // current state of the game
                           final int depth)                 // the depth of this node
        {
            super(maxPlayerType, gameView, depth);
        }
        @Override
        public double getTerminalUtility()
        {
            System.out.println("--run get terminal utility---");
            double cValue = 100.00d; //defining c value symmetric
            int whiteCellCount = 0;
            int blackCellCount = 0;
            PlayerType[][] cells = getGameView().getCells();
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < cells[i].length; j++) {
                    PlayerType cell = cells[i][j];
                    if (cell == PlayerType.BLACK) {
                        blackCellCount++;
                    } else if (cell == PlayerType.WHITE) {
                        whiteCellCount++;
                    }
                }
            }
            //now to determine whether we are white or black
            int multiplier = 1; //1 for white if black becomes -1
            if (getGameView().getCurrentPlayerType().equals(PlayerType.BLACK)) {
                multiplier *= - 1;
            }
            if (whiteCellCount > blackCellCount) {
                    return cValue * multiplier;
                }   
                else if (blackCellCount > whiteCellCount) {
                    return cValue * multiplier;
                }   
                else {
                    return 0d;
                }
        }

        @Override
        public List<Node> getChildren()
        {
            //if no nodes available to play (game over terminal) return empty list
            List<Node> children = new ArrayList<>();
            if (getGameView().isGameOver()) {
                return children;
            }
            //use frontier to get the available legal moves
            Set<Coordinate> legalMoves = getGameView().getFrontier(getCurrentPlayerType()); //getting frontier for my current player
            if (!legalMoves.isEmpty()) { //case that we have legal moves to make
                for (Coordinate move : legalMoves) {
                    Game newGame = new Game(getGameView()); //creating new game object for this new move
                    newGame.applyMove(move); //applied the move in the new game
                    OthelloNode child = new OthelloNode(getCurrentPlayerType(), newGame.getView(), getDepth() + 1);
                    child.setLastMove(move); //adding last move
                    children.add(child);
                }
            }
            else { //legal moves empty no moves
                Game newGame = new Game(getGameView());
                newGame.playTurn(); //it seems like playTurn would be skipping turn based off the name of the method
                OthelloNode child = new OthelloNode(getCurrentPlayerType(), newGame.getView(), getDepth() + 1);
                children.add(child);
            }
            return children;
        }
    }

    private final Random random;

    public OthelloAgent(final PlayerType myPlayerType,
                        final long maxMoveThinkingTimeInMS)
    {
        super(myPlayerType,
              maxMoveThinkingTimeInMS);
        this.random = new Random();
    }

    public final Random getRandom() { return this.random; }

    @Override
    public OthelloNode makeRootNode(final GameView game)
    {
        // if you change OthelloNode's constructor, you will want to change this!
        // Note: I am starting the initial depth at 0 (because I like to count up)
        //       change this if you want to count depth differently

        //testing terminal utility works
        OthelloNode n = new OthelloNode(getMyPlayerType(), game, 0);
        System.out.println(n.getTerminalUtility());


        return new OthelloNode(this.getMyPlayerType(), game, 0);
    }

    @Override
    public Node treeSearch(Node n)
    {
        // TODO: complete me!
        return null;
    }

    @Override
    public Coordinate chooseCoordinateToPlaceTile(final GameView game)
    {
        // TODO: this move will be called once per turn
        //       you may want to use this method to add to data structures and whatnot
        //       that your algorithm finds useful

        // make the root node
        Node node = this.makeRootNode(game);

        // call tree search
        Node moveNode = this.treeSearch(node);

        // return the move inside that node
        //return moveNode.getLastMove();
        return null;

    }

    @Override
    public void afterGameEnds(final GameView game) {}
}
