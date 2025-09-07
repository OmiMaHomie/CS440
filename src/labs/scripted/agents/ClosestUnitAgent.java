package src.labs.scripted.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;                                        // how we tell sepia what each unit will do
import edu.cwru.sepia.agent.Agent;                                          // base class for an Agent in sepia
import edu.cwru.sepia.environment.model.history.History.HistoryView;        // history of the game so far
import edu.cwru.sepia.environment.model.state.ResourceNode;                 // tree or gold
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;    // the "state" of that resource
import edu.cwru.sepia.environment.model.state.ResourceType;                 // what kind of resource units are carrying
import edu.cwru.sepia.environment.model.state.State.StateView;              // current state of the game
import edu.cwru.sepia.environment.model.state.Unit.UnitView;                // current state of a unit
import edu.cwru.sepia.util.Direction;                                       // directions for moving in the map


import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS



public class ClosestUnitAgent
    extends Agent
{

    private Set<Integer> myUnitIds;         // THIS unit IDs
    private Integer enemyUnitId;            // Enmy unit ID
    private Integer goldResourceNodeId;     // Gold node ID
    private Integer selectedUnitId;         // Selected unit ID


    /**
     * The constructor for this type. The arguments (including the player number: id of the team we are controlling)
     * are contained within the game's xml file that we are running. We can also add extra arguments to the game's xml
     * config for this agent and those will be included in args.
     */
	public ClosestUnitAgent(int playerNum, String[] args)
	{
		super(playerNum); // make sure to call parent type (Agent)'s constructor!

        // initialize your fields here!
        this.myUnitIds = new HashSet<Integer>();
        this.enemyUnitId = null;
        this.goldResourceNodeId = null;
        this.selectedUnitId = null;

        // helpful printout just to help debug
		System.out.println("Constructed ClosestUnitAgent");
	}

    /////////////////////////////// GETTERS AND SETTERS (this is Java after all) ///////////////////////////////
    public final Set<Integer> getMyUnitIds() { return this.myUnitIds; }
    public final Integer getEnemyUnitId() { return this.enemyUnitId; }
    public final Integer getGoldResourceNodeId() { return this.goldResourceNodeId; }
    public final Integer getSelectedUnitId() { return this.selectedUnitId; }

    private void setMyUnitIds(Set<Integer> ids) { this.myUnitIds = ids; }
    private void setEnemyUnitId(Integer id) { this.enemyUnitId = id; }
    private void setGoldResourceNodeId(Integer id) { this.goldResourceNodeId = id; }
    private void setSelectedUnitId(Integer id) { this.selectedUnitId = id; }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Gets the distance between players x and y
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    // Determins the closest friendly player
    private Integer findClosestUnitToEnemy(StateView state) {
        UnitView enemyUnit = state.getUnit(this.getEnemyUnitId());
        if (enemyUnit == null) {
            return null;
        }

        int enemyX = enemyUnit.getXPosition();
        int enemyY = enemyUnit.getYPosition();
        
        Integer closestUnitId = null;
        double minDistance = Double.MAX_VALUE;

        for (Integer unitId : this.getMyUnitIds()) {
            UnitView friendlyUnit = state.getUnit(unitId);
            if (friendlyUnit != null) {
                double distance = calculateDistance(
                    friendlyUnit.getXPosition(), friendlyUnit.getYPosition(),
                    enemyX, enemyY
                );
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closestUnitId = unitId;
                }
            }
        }

        return closestUnitId;
    }

    // Determine the way to move
    private Direction getMovementDirection(int currentX, int currentY, int targetX, int targetY) {
        if (currentX < targetX) {
            return Direction.EAST;
        } else if (currentX > targetX) {
            return Direction.WEST;
        } else if (currentY < targetY) {
            return Direction.SOUTH;
        } else if (currentY > targetY) {
            return Direction.NORTH;
        }
        return null; // Already at target position
    }

	@Override
	public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
	{
        // TODO: identify units, set fields, and then decide what to do
		return null;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // TODO: your code to give your unit actions for this turn goes here!

        return actions;
	}

    @Override
	public void terminalStep(StateView state,
                             HistoryView history)
    {
        // don't need to do anything
    }

    /**
     * The following two methods aren't really used by us much in this class. These methods are used to load/save
     * the Agent (for instance if our Agent "learned" during the game we might want to save the model, etc.). Until the
     * very end of this class we will ignore these two methods.
     */
    @Override
	public void loadPlayerData(InputStream is) {}

	@Override
	public void savePlayerData(OutputStream os) {}

}

