package src.pas.pokemon.rewards;

// SYSTEM IMPORTS
import java.util.List;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pokemon.agents.rewards.RewardFunction;
import edu.bu.pas.pokemon.agents.rewards.RewardFunction.RewardType;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.enums.Stat;
import edu.bu.pas.pokemon.core.enums.NonVolatileStatus;
import edu.bu.pas.pokemon.core.enums.Flag;

public class CustomRewardFunction
    extends RewardFunction
{

    public CustomRewardFunction()
    {
        super(RewardType.STATE_ACTION_STATE); // doing R(s, a, s')
    }

    public double getLowerBound()
    {
        return -100.0; // Worst-case
    }

    public double getUpperBound()
    {
        return 100.0; // Best-case
    }

    // R(s), not used
    public double getStateReward(final BattleView state)
    {
        return evaluateBattleState(state);
    }

    // R(s, a), not used
    public double getStateActionReward(final BattleView state,
                                       final MoveView action)
    {
        return 0d;
    }

    // R(s, a, s'):
    // Evals the state we're in, the action we wanna take, and then the resulting state after that
    // Comes up with a util val from that eval.
    public double getStateActionStateReward(final BattleView state,
                                            final MoveView action,
                                            final BattleView nextState)
    {
        double reward = 0.0;
        
        // Did we win or lose?
        if (nextState.isOver()) {
            TeamView myTeam = state.getTeam1View(); // WE are team 1 (assumpution)
            TeamView theirTeam = state.getTeam2View();
            
            boolean weWon = true;
            for (int i = 0; i < theirTeam.size(); i++) {
                if (!theirTeam.getPokemonView(i).hasFainted()) {
                    weWon = false;
                    break;
                }
            }
            
            // Big modifiers for this since this determines if we've won or lost.
            if (weWon) {
                reward += 50.0; // we won
            } else {
                reward -= 50.0; // we lost
            }
        }
        
        // In terms of other factors we'll be checking, it'll be the following:
        // dmg to enemies
        // dmg taken
        // status effects (both + and -)
        // ko rewards
        // move effectiveness (killer moves included in this)
        // other strategic stuff
        reward += calculateDamageDealtReward(state, nextState);
        reward += calculateDamageTakenReward(state, nextState);
        reward += calculateStatusEffectReward(state, nextState);
        reward += calculateKOReward(state, nextState);
        reward += calculateMoveEffectivenessReward(state, action, nextState);
        reward += calculateStrategicReward(state, nextState);
        
        return reward;
    }

    private double calculateDamageDealtReward(final BattleView state, final BattleView nextState) { }
    
    private double calculateDamageTakenReward(final BattleView state, final BattleView nextState) { }
    
    private double calculateStatusEffectReward(final BattleView state, final BattleView nextState) { }
    
    private double calculateKOReward(final BattleView state, final BattleView nextState) { }
    
    private double calculateMoveEffectivenessReward(final BattleView state, final BattleView nextState) { }
    
    private double calculateStrategicReward(final BattleView state, final BattleView nextState) { }

}
