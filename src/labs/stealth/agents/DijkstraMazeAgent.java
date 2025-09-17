package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;                           // Directions in Sepia


import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue; // heap in java
import java.util.Set;


// JAVA PROJECT IMPORTS


public class DijkstraMazeAgent
    extends MazeAgent
{

    public DijkstraMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        // Setup the min heap and vars for the search
        PriorityQueue<VertexCost> toExplore = new PriorityQueue<>(
            Comparator.comparingDouble(vc -> vc.cost)
        );
        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, Vertex> parents = new HashMap<>();
        Map<Vertex, Float> costs = new HashMap<>(); // Min costs to each V
        
        // Init
        toExplore.add(new VertexCost(src, 0f));
        costs.put(src, 0f);
        parents.put(src, null);
        
        boolean isPathfindingDone = false;
        Vertex goalVertex = null;

        while (!toExplore.isEmpty() && !isPathfindingDone) {
            VertexCost currentVC = toExplore.poll();
            Vertex current = currentVC.vertex;
            
            // Skip if there's a cheaper one
            if (visited.contains(current)) {
                continue;
            }
            
            visited.add(current);
            
            // Reached goal?
            if (current.equals(goal)) {
                isPathfindingDone = true;
                goalVertex = current;
                break;
            }
            
            // Visits neighbors, calculating cost and logging cheaper finds
            for (Vertex neighbor : getNeighbors(current, state)) {
                if (!visited.contains(neighbor)) {
                    float edgeCost = getEdgeCost(current, neighbor);
                    float newCost = costs.get(current) + edgeCost;
                    
                    if (!costs.containsKey(neighbor) || newCost < costs.get(neighbor)) {
                        costs.put(neighbor, newCost);
                        parents.put(neighbor, current);
                        toExplore.add(new VertexCost(neighbor, newCost));
                    }
                }
            }
        }

        if (isPathfindingDone && goalVertex != null) {
            return backtracePath(goalVertex, parents, costs, state);
        } else {
            return null; // Shouldn't happen.
        }
    }

    // Helps to store vertices with their cul. cost
    private class VertexCost {
        Vertex vertex;
        float cost;
        
        VertexCost(Vertex vertex, float cost) {
            this.vertex = vertex;
            this.cost = cost;
        }
    }

    // Calculate edge cost based on direction and the given constraints
    private float getEdgeCost(Vertex from, Vertex to) {
        int dx = to.getXCoordinate() - from.getXCoordinate();
        int dy = to.getYCoordinate() - from.getYCoordinate();
        
        if (dx == 0 && dy == -1) { // N
            return 10f;
        } else if (dx == 0 && dy == 1) { // S
            return 1f;
        } else if (dx == 1 && dy == 0) { // E
            return 5f;
        } else if (dx == -1 && dy == 0) { // W
            return 5f;
        } else if (dx == 1 && dy == -1) { // NE
            return (float) Math.sqrt(Math.pow(10f, 2) + Math.pow(5f, 2));
        } else if (dx == 1 && dy == 1) { // SE
            return (float) Math.sqrt(Math.pow(1f, 2) + Math.pow(5f, 2)); 
        } else if (dx == -1 && dy == 1) { // SW
            return (float) Math.sqrt(Math.pow(1f, 2) + Math.pow(5f, 2));
        } else if (dx == -1 && dy == -1) { // NW
            return (float) Math.sqrt(Math.pow(10f, 2) + Math.pow(5f, 2));
        }
        
        return Float.MAX_VALUE; // Shouldn't happen
    }

    // Backtrace the path.
    private Path backtracePath(Vertex goal, Map<Vertex, Vertex> parents, 
                                Map<Vertex, Float> costs, StateView state) {
        // Build the reverse path
        java.util.List<Vertex> pathVertices = new java.util.ArrayList<>();
        Vertex current = goal;
        
        while (current != null) {
            pathVertices.add(0, current);
            current = parents.get(current);
        }
        
        // Build the Path
        Path path = new Path(pathVertices.get(0));
        
        for (int i = 1; i < pathVertices.size(); i++) {
            Vertex currentVertex = pathVertices.get(i);
            Vertex prevVertex = pathVertices.get(i-1);
            float edgeCost = getEdgeCost(prevVertex, currentVertex);
            path = new Path(currentVertex, edgeCost, path);
        }

        System.out.println(path.toString());
        
        return path;
    }

    // Finds all traversable neighbors of a given vertex
    private Vertex[] getNeighbors(Vertex v, StateView state) {
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