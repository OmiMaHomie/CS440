package src.labs.rttt.agents;


// SYSTEM IMPORTS
import edu.bu.labs.rttt.agents.Agent;
import edu.bu.labs.rttt.game.CellType;
import edu.bu.labs.rttt.game.PlayerType;
import edu.bu.labs.rttt.game.RecursiveTicTacToeGame;
import edu.bu.labs.rttt.game.RecursiveTicTacToeGame.RecursiveTicTacToeGameView;
import edu.bu.labs.rttt.traversal.Node;
import edu.bu.labs.rttt.utils.Coordinate;
import edu.bu.labs.rttt.utils.Pair;

import java.util.List;
import java.util.Map;

// JAVA PROJECT IMPORTS
import src.labs.rttt.heuristics.Heuristics;
import src.labs.rttt.ordering.MoveOrderer;

public class DepthThresholdedAlphaBetaAgent
    extends Agent
{
    //
    // Fields
    //
    public static final int DEFAULT_MAX_DEPTH = 3;
    private int maxDepth;

    //
    // Constructor(s)
    //
    public DepthThresholdedAlphaBetaAgent(PlayerType myPlayerType)
    {
        super(myPlayerType);
        this.maxDepth = DEFAULT_MAX_DEPTH;
    }

    //
    // Getter/Setters
    //
    public final int getMaxDepth() { return this.maxDepth; }
    public void setMaxDepth(int i) { this.maxDepth = i; }
    public String getTabs(Node node)
    {
        StringBuilder b = new StringBuilder();
        for(int idx = 0; idx < node.getDepth(); ++idx)
        {
            b.append("\t");
        }
        return b.toString();
    }

    //
    // Methods
    //

    // Runs the function to determine and play the best possible moves.
    public Node alphaBeta(Node node,
                          double alpha,
                          double beta)
    {
        // // uncomment if you want to see the tree being made
        // System.out.println(this.getTabs(node) + "Node(currentPlayer=" + node.getCurrentPlayerType() +
        //      " isTerminal=" + node.isTerminal() + " lastMove=" + node.getLastMove() + ")");

        // @ Terminal node, end
        if (node.isTerminal()) {
            node.setUtilityValue(node.getTerminalUtility());
            return node;
        }
        
        // @ Depth limit, ret best node so far
        if (node.getDepth() >= this.getMaxDepth()) {
            double heuristicValue = Heuristics.calculateHeuristicValue(node);
            node.setUtilityValue(heuristicValue);
            return node;
        }

        // Get all possible moves & ORDER
        List<Node> children = MoveOrderer.orderChildren(node.getChildren());; 
        
        // Empty check (SHOULDN'T HAPPEN?)
        if (children.isEmpty()) {
            double heuristicValue = Heuristics.calculateHeuristicValue(node);
            node.setUtilityValue(heuristicValue);
            return node;
        }

        Node bestChild = null;
        double bestUtility;

        // Setting bestUtility
        if (node.getCurrentPlayerType() == node.getMyPlayerType()) {
            bestUtility = Double.NEGATIVE_INFINITY; // MAX
        } else {
            bestUtility = Double.POSITIVE_INFINITY; // MIN
        }

        // A-B pruning main loop
        for (Node child : children) {
            Node resultNode = alphaBeta(child, alpha, beta);
            double childUtility = resultNode.getUtilityValue();
            
            // MAX
            if (node.getCurrentPlayerType() == node.getMyPlayerType()) {
                if (childUtility > bestUtility) {
                    bestUtility = childUtility;
                    bestChild = child;
                }
                
                // Alpha & pruning update
                alpha = Math.max(alpha, bestUtility);
                if (beta <= alpha) {
                    break; // @ beta, so cutoff and check other siblings
                }
                
            } else { // MIN
                if (childUtility < bestUtility) {
                    bestUtility = childUtility;
                    bestChild = child;
                }
                
                // Beta & pruning update
                beta = Math.min(beta, bestUtility);
                if (beta <= alpha) {
                    break; // @ aplha, so cutoff and check other siblings
                }
            }
        }
            // Set nod to the one with best util, and ret the best-move child.
            node.setUtilityValue(bestUtility);
            return bestChild;
        }

    @Override
    public Pair<Coordinate, Coordinate> makeFirstMove(final RecursiveTicTacToeGameView game)
    {
        // the first move has two choices we need to make:
        //      (1) which small board do we want to play on?
        //      (2) what square in the small board to we want to mark?
        // we'll solve this by iterating over all options for decision (1) and using minimax over all options for (2).
        // we'll pick the answer to (1) which leads to the best utility amongst all options for (1)
        // and choose the move which optimizes the choice for (1) to decide (2)
        Coordinate bestOuterBoardChoice = null;
        Double bestOuterUtility = null;
        Coordinate bestInnerBoardChoice = null;
        for(Coordinate potentialOuterBoardChoice : game.getAvailableFirstMoves().keySet())
        {
            
            // now that we have a choice for (1) we need to convey that to the game
            // so we'll make a RecursiveTicTacToeGame object which is mutable and set
            // the current game to the potentialOuterBoardChoice
            // then we can search like normal
            RecursiveTicTacToeGame gameToSetCurrentGame = new RecursiveTicTacToeGame(game);
            gameToSetCurrentGame.setCurrentGameCoord(potentialOuterBoardChoice);

            Node innerChoiceNode = this.alphaBeta(new Node(gameToSetCurrentGame.getView(), this.getMyPlayerType(), 0),
                                                  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

            if(bestOuterUtility == null || (innerChoiceNode.getUtilityValue() > bestOuterUtility))
            {
                bestOuterBoardChoice = potentialOuterBoardChoice;
                bestOuterUtility = innerChoiceNode.getUtilityValue();
                bestInnerBoardChoice = innerChoiceNode.getLastMove();   // get the move that lead to this node
            }
        }

        return new Pair<Coordinate, Coordinate>(bestOuterBoardChoice, bestInnerBoardChoice);
    }

    @Override
    public Coordinate makeOtherMove(final RecursiveTicTacToeGameView game)
    {
        Node bestInnerChoiceNode = this.alphaBeta(new Node(game, this.getMyPlayerType(), 0),
                                                  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        return bestInnerChoiceNode.getLastMove();
    }

    @Override
    public void afterGameEnds(final RecursiveTicTacToeGameView game) { }
}