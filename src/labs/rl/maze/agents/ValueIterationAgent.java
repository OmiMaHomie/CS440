package src.labs.rl.maze.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.agents.StochasticAgent;
import edu.bu.labs.rl.maze.agents.StochasticAgent.RewardFunction;
import edu.bu.labs.rl.maze.agents.StochasticAgent.TransitionModel;
import edu.bu.labs.rl.maze.utilities.Coordinate;
import edu.bu.labs.rl.maze.utilities.Pair;



public class ValueIterationAgent
    extends StochasticAgent
{

    public static final double GAMMA = 0.9; // feel free to change this around!
    public static final double EPSILON = 1e-6; // don't change this though

    private Map<Coordinate, Double> utilities;

	public ValueIterationAgent(int playerNum)
	{
		super(playerNum);
        this.utilities = null;
	}

    public Map<Coordinate, Double> getUtilities() { return this.utilities; }
    private void setUtilities(Map<Coordinate, Double> u) { this.utilities = u; }

    public boolean isTerminalState(Coordinate c)
    {
        return c.equals(StochasticAgent.POSITIVE_TERMINAL_STATE)
            || c.equals(StochasticAgent.NEGATIVE_TERMINAL_STATE);
    }

    public Map<Coordinate, Double> getZeroMap(StateView state)
    {
        Map<Coordinate, Double> m = new HashMap<Coordinate, Double>();
        for(int x = 0; x < state.getXExtent(); ++x)
        {
            for(int y = 0; y < state.getYExtent(); ++y)
            {
                if(!state.isResourceAt(x, y))
                {
                    // we can go here
                    m.put(new Coordinate(x, y), 0.0);
                }
            }
        }
        return m;
    }

    public void valueIteration(StateView state)
    {
        // init utils to 0
        Map<Coordinate, Double> U = getZeroMap(state);
        Map<Coordinate, Double> U_prime = new HashMap<>(U);
        
        // hit terminal state? then get the reward
        U.put(POSITIVE_TERMINAL_STATE, RewardFunction.getReward(POSITIVE_TERMINAL_STATE));
        U.put(NEGATIVE_TERMINAL_STATE, RewardFunction.getReward(NEGATIVE_TERMINAL_STATE));
        U_prime.putAll(U);
        
        double e = EPSILON;
        double g = GAMMA;
        double threshold = e * (1 - g) / g;
        double delta = Double.POSITIVE_INFINITY;

        // System.out.println("epsilon = " + e);
        // System.out.println("gamma = " + g);
        // System.out.println("convergenceThreshold = " + threshold);

        int iteration = 0;
        
        while (delta > threshold) {
        delta = 0.0;
        
        for (Coordinate s : U.keySet()) {
            if (isTerminalState(s)) continue;
            
            double maxExpectedUtility = Double.NEGATIVE_INFINITY;
            
            for (Direction action : TransitionModel.CARDINAL_DIRECTIONS) {
                double expectedUtility = 0.0;
                Set<Pair<Coordinate, Double>> transitions = 
                    TransitionModel.getTransitionProbs(state, s, action);
                
                for (Pair<Coordinate, Double> transition : transitions) {
                    expectedUtility += transition.getSecond() * U.get(transition.getFirst());
                }
                
                maxExpectedUtility = Math.max(maxExpectedUtility, expectedUtility);
            }
            
            double newUtility = RewardFunction.getReward(s) + g * maxExpectedUtility;
            U_prime.put(s, newUtility);
            delta = Math.max(delta, Math.abs(newUtility - U.get(s)));
        }
        
            // Swap U for U prime
            Map<Coordinate, Double> temp = U;
            U = U_prime;
            U_prime = temp;
        }
        
        // After loop, U has the converged utils
        setUtilities(U);
    }

    @Override
    public void computePolicy(StateView state,
                              HistoryView history)
    {
        this.valueIteration(state);

        // compute the policy from the utilities
        Map<Coordinate, Direction> policy = new HashMap<Coordinate, Direction>();

        for(Coordinate c : this.getUtilities().keySet())
        {
            // figure out what to do when in this state
            double maxActionUtility = Double.NEGATIVE_INFINITY;
            Direction bestDirection = null;

            for(Direction d : TransitionModel.CARDINAL_DIRECTIONS)
            {
                double thisActionUtility = 0.0;
                for(Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d))
                {
                    thisActionUtility += transition.getSecond() * this.getUtilities().get(transition.getFirst());
                }

                if(thisActionUtility > maxActionUtility)
                {
                    maxActionUtility = thisActionUtility;
                    bestDirection = d;
                }
            }

            policy.put(c, bestDirection);

        }

        this.setPolicy(policy);
    }

}
