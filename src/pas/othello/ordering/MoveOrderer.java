package src.pas.othello.ordering;


// SYSTEM IMPORTS
import edu.bu.pas.othello.traversal.Node;
import java.util.List;
import java.util.Collections;


// JAVA PROJECT IMPORTS



public class MoveOrderer
    extends Object
{


    public static List<Node> orderChildren(List<Node> children)
    {
        /*
        for (int i = 0; i < children.size(); i++) {
            System.out.println("start " + children.get(i));
        }

        for (int i = 0; i < children.size() - 1; i++) {
            Boolean swapped = false;
            for (int j = 0; j < i - i - 1; j++) {
                if (Heuristics.calculateHeuristicValue(children.get(j)) < Heuristics.calculateHeuristicValue(children.get(j + 1))) {
                    Node temp = children.get(j);
                    children.set(j, children.get(j+1));
                    children.set(j+1, temp);
                    swapped = true;
                }
            }
            if (swapped == false) {
                break;
            }
        }
       
        for (int i = 0; i < children.size(); i++) {
            System.out.println("end " + children.get(i));
        }
        */
        Collections.shuffle(children); //testing shuffle because seems to be sorted getting same error
        return children;
        
    }

}
