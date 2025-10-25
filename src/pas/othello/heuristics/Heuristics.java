package src.pas.othello.heuristics;


import java.util.List;

// SYSTEM IMPORTS
import edu.bu.pas.othello.traversal.Node;


// JAVA PROJECT IMPORTS



public class Heuristics
    extends Object
{

    public static double calculateHeuristicValue(Node node)
    {
        // TODO: complete me!
        //Hueristic should take in a node, get the children and return the utility value of each.
        List<Node> children = node.getChildren();
        double sum = 0;
        for (Node child : children) { //only want to iterate through children of node
            sum += child.getUtilityValue();
        }
        return sum;
    }

}
