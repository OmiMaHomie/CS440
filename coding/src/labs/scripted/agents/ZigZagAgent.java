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



public class ZigZagAgent
    extends Agent
{
    private Integer myUnitId;               // ID of THIS unit
    private Integer enemyUnitId;            // ID of enemy unit
    private Integer goldResourceNodeId;     // ID of the gold node
    private boolean lastMoveWasEast;        // Tracks the last move from THIS unit


    /**
     * The constructor for this type. The arguments (including the player number: id of the team we are controlling)
     * are contained within the game's xml file that we are running. We can also add extra arguments to the game's xml
     * config for this agent and those will be included in args.
     */
	public ZigZagAgent(int playerNum, String[] args)
	{
		super(playerNum); // make sure to call parent type (Agent)'s constructor!

        this.myUnitId = null;
        this.enemyUnitId = null;
        this.goldResourceNodeId = null;
        this.lastMoveWasEast = false; // Start with east move
	}

    /////////////////////////////// GETTERS AND SETTERS (this is Java after all) ///////////////////////////////
    public final Integer getMyUnitId() { return this.myUnitId; }
    public final Integer getEnemyUnitId() { return this.enemyUnitId; }
    public final Integer getGoldResourceNodeId() { return this.goldResourceNodeId; }
    public final boolean getLastMoveWasEast() { return this.lastMoveWasEast; }

    private void setMyUnitId(Integer i) { this.myUnitId = i; }
    private void setEnemyUnitId(Integer i) { this.enemyUnitId = i; }
    private void setGoldResourceNodeId(Integer i) { this.goldResourceNodeId = i; }
    private void setLastMoveWasEast(boolean b) { this.lastMoveWasEast = b; }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
	{
		// Find the friendly units
        Set<Integer> myUnitIds = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(this.getPlayerNumber())) // for each unit on my team
        {
            myUnitIds.add(unitID);
        }

        // Make sure we only controlling 1 unit
        if(myUnitIds.size() != 1)
        {
            System.err.println("[ERROR] ZigZagAgent.initialStep: Should control only 1 unit");
            System.exit(-1);
        }

        // Checking that friendly units are of right type (footman or melee)
        for(Integer unitID : myUnitIds)
        {
            if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman"))
            {
                System.err.println("[ERROR] ZigZagAgent.initialStep: Should control only footman units");
                System.exit(-1);
            }
        }

        // Checking other players and saving their IDs
        Integer[] playerNumbers = state.getPlayerNumbers();
        if(playerNumbers.length != 2)
        {
            System.err.println("ERROR: Should only be two players in the game");
            System.exit(1);
        }
        Integer enemyPlayerNumber = null;
        if(playerNumbers[0] != this.getPlayerNumber())
        {
            enemyPlayerNumber = playerNumbers[0];
        } else
        {
            enemyPlayerNumber = playerNumbers[1];
        }

        // Same as last batch but now for enemy IDs
        Set<Integer> enemyUnitIds = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(enemyPlayerNumber))
        {
            enemyUnitIds.add(unitID);
        }

        // Making sure only 1 enemy
        if(enemyUnitIds.size() != 1)
        {
            System.err.println("[ERROR] ZigZagAgent.initialStep: Enemy should control only 1 unit");
            System.exit(-1);
        }

        // Checking enemy type
        for(Integer unitID : enemyUnitIds)
        {
            if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman"))
            {
                System.err.println("[ERROR] ZigZagAgent.initialStep: Enemy should only control footman units");
                System.exit(-1);
            }
        }

        // Locate and ID the gold
        Integer goldResourceNodeId = null;
        for (Integer resourceId : state.getAllResourceIds()) {
            ResourceView resource = state.getResourceNode(resourceId);
            if (resource.getType().equals(ResourceType.GOLD)) {
                goldResourceNodeId = resourceId;
                break;
            }
        }

        // Setting fields
        this.setMyUnitId(myUnitIds.iterator().next());
        this.setEnemyUnitId(enemyUnitIds.iterator().next());
        this.setGoldResourceNodeId(goldResourceNodeId);

        // Call to middlestep
        return this.middleStep(state, history);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // Check if enemy is alive. If dead, do nothing.
        UnitView enemyUnit = state.getUnit(this.getEnemyUnitId());
        if (enemyUnit == null) {
            return actions;
        }

        UnitView myUnit = state.getUnit(this.getMyUnitId());

        // Check if enemy is adjacent - if so, attack
        int dx = Math.abs(myUnit.getXPosition() - enemyUnit.getXPosition());
        int dy = Math.abs(myUnit.getYPosition() - enemyUnit.getYPosition());
        
        if (dx <= 1 && dy <= 1) {
            actions.put(this.getMyUnitId(), Action.createPrimitiveAttack(this.getMyUnitId(), this.getEnemyUnitId()));
            return actions;
        }

        // Zig-zag to enemy
        if (this.getLastMoveWasEast()) {
            actions.put(this.getMyUnitId(), Action.createPrimitiveMove(this.getMyUnitId(), Direction.NORTH));
            this.setLastMoveWasEast(false);
        } else {
            actions.put(this.getMyUnitId(), Action.createPrimitiveMove(this.getMyUnitId(), Direction.EAST));
            this.setLastMoveWasEast(true);
        }

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

