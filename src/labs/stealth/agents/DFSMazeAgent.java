package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS
import java.util.HashMap;
import java.util.Map;

public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        // Vars to help with DFS and backtracing.
        Stack<Vertex> toExplore = new Stack<>();
        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, Vertex> parents = new HashMap<>(); 
        boolean isPathfindingDone = false;

        // Init the DFS search
        toExplore.add(src);
        visited.add(src);
        parents.put(src, null);

        // Runs through each potential branch, and adds UNVISITED nodes into the Stack
        while(!toExplore.isEmpty() || !isPathfindingDone) {
            Vertex current = toExplore.pop();

            // Once a node is the goal, we terminate
            if (current.equals(goal)) {
                isPathfindingDone = true;
                break;
            }
            
            for (Vertex neighbor : getNeighbors(current, state)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    toExplore.add(neighbor);
                    parents.put(neighbor, current);
                }
            }
        }

        if (isPathfindingDone) {
            // Get best path length
            int pathLength = 1;
            Vertex current = goal;
            while (parents.get(current) != null) {
                pathLength++;
                current = parents.get(current);
            }
            
            // Make an array from src-->goal
            Vertex[] pathVertices = new Vertex[pathLength];
            current = goal;
            for (int i = pathLength - 1; i >= 0; i--) {
                pathVertices[i] = current;
                current = parents.get(current);
            }
            
            // Make the path linked list
            Path path = new Path(pathVertices[0]);
            for (int i = 1; i < pathLength; i++) {
                path = new Path(pathVertices[i], 1.0f, path);
            }
            
            System.out.println(path.toString());
            
            return path;
        } else {
            return null; // Shouldn't happen
        }
    }

    // Finds all traversable neighbors of a given vertex.
    private Vertex[] getNeighbors (Vertex v, StateView state) {
        Vertex[] neighbors = new Vertex[8];
        int index = 0;

        // Check all 8 possible directions
        for (Direction dir : Direction.values()) {
            int newX = v.getXCoordinate();
            int newY = v.getYCoordinate();

            switch (dir) {
                case NORTH:
                    newY -= 1;
                    break;
                case NORTHEAST:
                    newX += 1;
                    newY -= 1;
                    break;
                case EAST:
                    newX += 1;
                    break;
                case SOUTHEAST:
                    newX += 1;
                    newY += 1;
                    break;
                case SOUTH:
                    newY += 1;
                    break;
                case SOUTHWEST:
                    newX -= 1;
                    newY += 1;
                    break;
                case WEST:
                    newX -= 1;
                    break;
                case NORTHWEST:
                    newX -= 1;
                    newY -= 1;
                    break;
            }

            // Adds neighbors that are in bounds and not occupied by resources
            if (state.inBounds(newX, newY) && !state.isResourceAt(newX, newY)) {
                neighbors[index++] = new Vertex(newX, newY);
            }
        }

        // Trim the array to the actual number of neighbors found
        Vertex[] result = new Vertex[index];
        System.arraycopy(neighbors, 0, result, 0, index);
        return result;
    }
}
