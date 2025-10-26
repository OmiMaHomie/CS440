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
        //overall deciding to combine multiple different values to make one heuristic.
        PlayerType mytype = node.getCurrentPlayerType();
        PlayerType opponentType = node.getOtherPlayerType();
        Game newGame = new Game(node.getGameView());
        int numRows = newGame. getBoard().getNumRows();
        int numCols = newGame.getBoard().getNumCols();
        PlayerType[][] board = newGame.getView().getCells();
        //flips
        //this  uses the wouldSandwichOppositePlayerInDirection which estimates the strength of a move with its flips

        int flips = 0;
        Set<Coordinate> legalMoves = newGame.getView().getFrontier(mytype); 
        for (Coordinate move : legalMoves) { //iterating through frontier of legal moves of the game state

            for (Direction d : Direction.values()) { //each direction of potential flips
                if (newGame.getBoard().wouldSandwichOppositePlayerInDirection(mytype, move, d)) {
                    flips++;
                }
            }
        }
        //num pieces on the board of me vs opponent
        int myPieceCount = 0;
        int opponentPieceCount = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (board[i][j] == mytype) {
                    myPieceCount += 1;
                } 
                else if (board[i][j] == opponentType) {
                    opponentPieceCount += 1;
                } 
            }
        }
        int pieceDifference = myPieceCount - opponentPieceCount;


        //num moves, just comparing the size of my frontier and opponents then translating to a var
        int myNumMoves = legalMoves.size();
        int opponentNumMoves = newGame.getView().getFrontier(opponentType).size(); 
        int frontierDifference = myNumMoves - opponentNumMoves;

        int overallHueristicScore = frontierDifference * 3 + flips * 2 + pieceDifference;
        return overallHueristicScore;
    }
}
