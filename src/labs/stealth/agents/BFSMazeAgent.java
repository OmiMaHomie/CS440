package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;


import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;           // will need for bfs


// JAVA PROJECT IMPORTS
import edu.cwru.sepia.util.Direction;
import java.util.HashMap;
import java.util.Map;


public class BFSMazeAgent
    extends MazeAgent
{
    // Constructor(s)
    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    // Performing a BFS search of the envrionment in order to traverse to the goal.
    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        // Vars to help with BFS and backtracing.
        Queue<Vertex> toExplore = new LinkedList<>();
        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, Vertex> parents = new HashMap<>(); 
        boolean isPathfindingDone = false;

        // Init the BFS search
        toExplore.add(src);
        visited.add(src);
        parents.put(src, null);

        // Runs through each branch, and adds UNVISITED nods into the Queue
        while(!toExplore.isEmpty() || !isPathfindingDone) {
            Vertex current = toExplore.poll();
            
            for (Vertex neighbor : getNeighbors(current, state)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    toExplore.add(neighbor);
                    parents.put(neighbor, current);
                }
            
                // Once a node is the goal, we terminate
                if (neighbor.equals(goal)) {
                    isPathfindingDone = true;
                    break;
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