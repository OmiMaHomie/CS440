package src.pas.othello.heuristics;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.bu.pas.othello.utils.Coordinate;
import edu.bu.pas.othello.game.Direction;
import edu.bu.pas.othello.game.Game;
import edu.bu.pas.othello.game.PlayerType;
// SYSTEM IMPORTS
import edu.bu.pas.othello.traversal.Node;


// JAVA PROJECT IMPORTS



public class Heuristics
    extends Object
{
    private static Map<Long, Double> cachedHeuristics = new HashMap<>(); // Used for heuristic lookup
    public static double calculateHeuristicValue(Node node) {
        // Check cache first
        long key = node.hashCode(); //node has function to get hash value
        if (cachedHeuristics.containsKey(key)) { //if contiains key just grabbing and returning stored val
            //System.out.println("got cache for " + key);
            return cachedHeuristics.get(key);
        } else {
            //System.out.println("calculating heuristic not in cache");
            double value = getHeuristicVal(node); // running get heuristic then storing
            cachedHeuristics.put(key, value);
            return value;
        }
    }
    public static double getHeuristicVal(Node node)
    {
        // TODO: complete me!

        //commenting out for test of correctness
        

        //overall deciding to combine multiple different values to make one heuristic.
        PlayerType mytype = node.getCurrentPlayerType();
        PlayerType opponentType = node.getOtherPlayerType();
        Game newGame = new Game(node.getGameView());
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
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
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


        //graph that has weights for tiles being there
        //corners strongest, middle sort of strong, adjacent corners weak
        int[][] weightedGraph = {
            {100, -30,  15,   10,   10,  15, -30, 100},
            {-30, -60,  -5,  -5,  -5,  -5, -60, -30},
            { 15,  -5,  15,   3,   3,  15,  -5,  15},
            {  10,  -5,   3,   3,   3,   3,  -5,   10},
            {  10,  -5,   3,   3,   3,   3,  -5,   10},
            { 15,  -5,  15,   3,   3,  15,  -5,  15},
            {-30, -60,  -5,  -5,  -5,  -5, -60, -30},
            {100, -30,  15,   10,   10,  15, -30, 100}
        };
        int positionValue = 0;
        for (int i = 0; i < weightedGraph.length; i++) {
            for (int j = 0; j < weightedGraph[i].length; j++) {
                if (board[i][j] == mytype) {
                    positionValue += weightedGraph[i][j]; //adding i have control
                }
                if (board[i][j] == opponentType) {
                    positionValue -= weightedGraph[i][j]; //subtraction because opponent controls
                }
            }
        }   

        //corners
        PlayerType[] cornerStates = {board[0][0], board[0][7], board[7][7], board[7][0]};
        int myCorners = 0;
        int opponentCorners = 0;
        for (int i = 0; i < cornerStates.length; i++) {
            if (cornerStates[i] == mytype) {
                myCorners += 1;
            }
            if (cornerStates[i] == opponentType) {
                opponentCorners += 1;
            }
        }
        int cornerDifference = myCorners - opponentCorners;

        //calculating stability pieces that cannot be flipped

        //changing heuristic value depending on how far into the game
        //total 64 squares
        int overallHueristicScore = 0;
        int totalPieceCount = myPieceCount + opponentPieceCount;
        //early game
        if (totalPieceCount < 20) {
            overallHueristicScore = 
            frontierDifference * 6 + 
            flips * 1 + 
            pieceDifference * -4 + 
            cornerDifference * 4 + 
            positionValue;
            //want to be able to make many moves
            //want to have fewer pieces
        }
        //middle game
        if (totalPieceCount >= 20 && totalPieceCount <= 50) {
            overallHueristicScore = 
            frontierDifference * 4 + 
            flips * 2 + 
            pieceDifference * 2 +
            cornerDifference * 4 + 
            positionValue;
            //want to control the corners and walls (not the x off corner)
            //control the corners very strong
        }
        //end game
        if (totalPieceCount > 50) {
            overallHueristicScore = 
            frontierDifference * 2 + 
            flips * 5 +
            pieceDifference * 8 + 
            cornerDifference * 4 + 
            positionValue;
            //end game want to have a higher piece count
        }
        
        return overallHueristicScore;
        /* 

        //return 1; //testing purposes
        PlayerType mytype = node.getCurrentPlayerType();
        PlayerType opponentType = node.getOtherPlayerType();
        PlayerType[][] board = node.getGameView().getCells();
        //flips
        //this  uses the wouldSandwichOppositePlayerInDirection which estimates the strength of a move with its flip 
        int[][] weightedGraph = {
            {100, -30,  15,   10,   10,  15, -30, 100},
            {-30, -60,  -5,  -5,  -5,  -5, -60, -30},
            { 15,  -5,  15,   3,   3,  15,  -5,  15},
            {  10,  -5,   3,   3,   3,   3,  -5,   10},
            {  10,  -5,   3,   3,   3,   3,  -5,   10},
            { 15,  -5,  15,   3,   3,  15,  -5,  15},
            {-30, -60,  -5,  -5,  -5,  -5, -60, -30},
            {100, -30,  15,   10,   10,  15, -30, 100}
        };
        int positionValue = 0;
        for (int i = 0; i < weightedGraph.length; i++) {
            for (int j = 0; j < weightedGraph[i].length; j++) {
                if (board[i][j] == mytype) {
                    positionValue += weightedGraph[i][j]; //adding i have control
                }
                if (board[i][j] == opponentType) {
                    positionValue -= weightedGraph[i][j]; //subtraction because opponent controls
                }
            }
        }   
        return positionValue;
        */

    }
}
