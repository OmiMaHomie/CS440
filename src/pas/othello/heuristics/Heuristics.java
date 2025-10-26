package src.pas.othello.heuristics;


import java.util.List;
import java.util.Set;

import edu.bu.pas.othello.utils.Coordinate;
import edu.bu.pas.othello.game.Direction;
import edu.bu.pas.othello.game.Game;
import edu.bu.pas.othello.game.PlayerType;
// SYSTEM IMPORTS
import edu.bu.pas.othello.traversal.Node;
import edu.bu.pas.pacman.game.Game.GameView;


// JAVA PROJECT IMPORTS



public class Heuristics
    extends Object
{

    public static double calculateHeuristicValue(Node node)
    {
        // TODO: complete me!
        //this heuristic approach uses the wouldSandwichOppositePlayerInDirection which estimates the strength of a move with its flips
        PlayerType mytype = node.getCurrentPlayerType();
        Game newGame = new Game(node.getGameView());
        int flips = 0;
        Set<Coordinate> legalMoves = newGame.getView().getFrontier(node.getCurrentPlayerType()); 
        for (Coordinate move : legalMoves) { //iterating through frontier of legal moves of the game state
            int flipsForMove = 0;
            for (Direction d : Direction.values()) { //each direction of potential flips
                if (newGame.getBoard().wouldSandwichOppositePlayerInDirection(mytype, move, d)) {
                    flipsForMove++;
                }
            }
        flips += flipsForMove;
        }
        return flips;
    }
}
