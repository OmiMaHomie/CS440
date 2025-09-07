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

    private Set<Integer> myUnitIds;         // Friendly unit IDs
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
        // discover friendly units
        Set<Integer> myUnitIds = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(this.getPlayerNumber())) {
            myUnitIds.add(unitID);
        }

        // check that we have at least one unit
        if(myUnitIds.size() < 1) {
            System.err.println("[ERROR] ClosestUnitAgent.initialStep: Should control at least 1 unit");
            System.exit(-1);
        }

        // check that all units are footmen
        for(Integer unitID : myUnitIds) {
            if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman")) {
                System.err.println("[ERROR] ClosestUnitAgent.initialStep: Should control only footman units");
                System.exit(-1);
            }
        }

        // check that there is another player and get their player ID
        Integer[] playerNumbers = state.getPlayerNumbers();
        if(playerNumbers.length != 2) {
            System.err.println("ERROR: Should only be two players in the game");
            System.exit(1);
        }
        Integer enemyPlayerNumber = (playerNumbers[0] != this.getPlayerNumber()) ? playerNumbers[0] : playerNumbers[1];

        // get the enemy units
        Set<Integer> enemyUnitIds = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(enemyPlayerNumber)) {
            enemyUnitIds.add(unitID);
        }

        // enemy should have exactly 1 unit
        if(enemyUnitIds.size() != 1) {
            System.err.println("[ERROR] ClosestUnitAgent.initialStep: Enemy should control exactly 1 unit");
            System.exit(-1);
        }

        // check enemy unit type
        for(Integer unitID : enemyUnitIds) {
            if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman")) {
                System.err.println("[ERROR] ClosestUnitAgent.initialStep: Enemy should only control footman units");
                System.exit(-1);
            }
        }

        // ID gold node
        Integer goldResourceNodeId = null;
        for (Integer resourceId : state.getAllResourceIds()) {
            ResourceView resource = state.getResourceNode(resourceId);
            if (resource.getType().equals(ResourceType.GOLD)) {
                goldResourceNodeId = resourceId;
                break;
            }
        }

        // set fields
        this.setMyUnitIds(myUnitIds);
        this.setEnemyUnitId(enemyUnitIds.iterator().next());
        this.setGoldResourceNodeId(goldResourceNodeId);

        // Find and set the closest unit to the enemy
        Integer closestUnitId = this.findClosestUnitToEnemy(state);
        this.setSelectedUnitId(closestUnitId);

        return this.middleStep(state, history);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // Check if enemy is alive
        UnitView enemyUnit = state.getUnit(this.getEnemyUnitId());
        if (enemyUnit == null) {
            return actions; // Enemy is dead, no actions needed
        }

        // Recheck closest unit in case units have moved or died
        Integer selectedUnitId = this.findClosestUnitToEnemy(state);
        this.setSelectedUnitId(selectedUnitId);
        
        if (selectedUnitId == null) {
            return actions; // No friendly units available
        }

        UnitView selectedUnit = state.getUnit(selectedUnitId);
        if (selectedUnit == null) {
            return actions; // Selected unit died
        }

        int enemyX = enemyUnit.getXPosition();
        int enemyY = enemyUnit.getYPosition();
        int unitX = selectedUnit.getXPosition();
        int unitY = selectedUnit.getYPosition();

        // Check if unit is adjacent to enemy
        int dx = Math.abs(unitX - enemyX);
        int dy = Math.abs(unitY - enemyY);
        
        if (dx <= 1 && dy <= 1) {
            // Attack if adjacent
            actions.put(selectedUnitId, Action.createPrimitiveAttack(selectedUnitId, this.getEnemyUnitId()));
            return actions;
        }

        // Move toward enemy: x-axis first, then y-axis
        Direction moveDirection = null;
        
        if (unitX != enemyX) {
            moveDirection = getMovementDirection(unitX, unitY, enemyX, unitY);
        } else {
            moveDirection = getMovementDirection(unitX, unitY, unitX, enemyY);
        }

        if (moveDirection != null) {
            actions.put(selectedUnitId, Action.createPrimitiveMove(selectedUnitId, moveDirection));
        }

        // Other units do nothing (only the closest unit moves/attacks)
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

