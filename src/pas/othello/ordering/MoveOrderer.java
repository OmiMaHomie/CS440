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
        int maxDepthSort;
        if (children.size() < 2) {
            maxDepthSort = children.size();
        }
        else {
            maxDepthSort = 2;
        }
        for (int i = 0; i < maxDepthSort - 1; i++) {
            if (Heuristics.calculateHeuristicValue(children.get(i)) > Heuristics.calculateHeuristicValue(children.get(i + 1))) {
                    Node temp = children.get(i);
                    children.set(i, children.get(i+1));
                    children.set(i+1, temp);   
            }
        }
        for (int i = 0; i < children.size(); i++) {
            System.out.println("end " + children.get(i));
        }
        return children;
        
    }

}
