package src.pas.othello.ordering;


// SYSTEM IMPORTS
import edu.bu.pas.othello.traversal.Node;
import src.pas.othello.heuristics.Heuristics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;


// JAVA PROJECT IMPORTS



public class MoveOrderer
    extends Object
{


    public static List<Node> orderChildren(List<Node> children)
    {
        // TODO: complete me!
        if (children == null || children.isEmpty()) { //base case empty ull
            return children;
        }
        List<Node> childrenOrdered = new ArrayList<>(children);
        Collections.sort(childrenOrdered, new Comparator<Node>() { //sorting the children with collections sort
            @Override
            public int compare(Node childA, Node childB) {
                double childAHueristicVal = Heuristics.calculateHeuristicValue(childA); //calculate heuristic now using hashmap caching
                double childBHueristicVal = Heuristics.calculateHeuristicValue(childB);
                return Double.compare(childBHueristicVal, childAHueristicVal); // sorted with highest hueristic val first
            }
        });
        return childrenOrdered;
    }

}
