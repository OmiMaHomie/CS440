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

    private double evaluateBattleState(final BattleView state) {
        double score = 0.0;
        
        TeamView myTeam = state.getTeam1View();
        TeamView theirTeam = state.getTeam2View();
        
        // team hp difference
        score += calculateTeamHealthDifferential(myTeam, theirTeam);
        
        // # of alive pokemons difference
        int myAlive = countAlivePokemon(myTeam);
        int theirAlive = countAlivePokemon(theirTeam);
        score += (myAlive - theirAlive) * 5.0;
        
        return score;
    }

    // + modifier to reward, based on how much DMG dealth
    private double calculateDamageDealtReward(final BattleView state, final BattleView nextState) { 
        double reward = 0.0;
        
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (myCurrent != null && theirCurrent != null && theirNext != null) {
            int currentTheirHP = theirCurrent.getCurrentStat(Stat.HP);
            int nextTheirHP = theirNext.getCurrentStat(Stat.HP);
            int damageDealt = currentTheirHP - nextTheirHP;
            
            if (damageDealt > 0) {
                // Reward proportional to dmg dealt, normalized by max HP
                double damageRatio = (double) damageDealt / theirCurrent.getInitialStat(Stat.HP);
                reward += damageRatio * 10.0;
            }
        }
        
        return reward;
    }
    
    // - modifier to reward, based on how much DMG taken
    private double calculateDamageTakenReward(final BattleView state, final BattleView nextState) { 
        double penalty = 0.0;
        
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null) {
            int currentMyHP = myCurrent.getCurrentStat(Stat.HP);
            int nextMyHP = myNext.getCurrentStat(Stat.HP);
            int damageTaken = currentMyHP - nextMyHP;
            
            if (damageTaken > 0) {
                // Penalty proportional to damage taken, normalized by max HP
                double damageRatio = (double) damageTaken / myCurrent.getInitialStat(Stat.HP);
                penalty -= damageRatio * 8.0;
            }
        }
        
        return penalty;
    }
    
    // Variable modifier on reward, based on the status effects of the various pokemons in the game.
    private double calculateStatusEffectReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        // Check + status effects on opponent
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (theirCurrent != null && theirNext != null) {
            // Reward for dealing - status on opponent
            if (theirCurrent.getNonVolatileStatus() == NonVolatileStatus.NONE && 
                theirNext.getNonVolatileStatus() != NonVolatileStatus.NONE) {
                reward += 5.0; // Reward for dealing any status
                
                // Extra rewards for more dmging statuses
                // Not too sure on how to weigh these, so I'll add my justifications below
                switch (theirNext.getNonVolatileStatus()) {
                    case SLEEP:
                    case FREEZE:
                        reward += 10.0; // prevents moves
                        break;
                    case PARALYSIS:
                        reward += 3.0; // lose turn chance AND SPD reduction
                        break;
                    case TOXIC:
                        reward += 8.0; // increasing dmg over time
                        break;
                    case POISON:
                    case BURN:
                        reward += 5.0; // constant dmg over time
                        break;
                }
            }
            
            // Penalty for - statuses on our pokemon
            PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
            PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
            
            if (myCurrent != null && myNext != null) {
                if (myCurrent.getNonVolatileStatus() == NonVolatileStatus.NONE && 
                    myNext.getNonVolatileStatus() != NonVolatileStatus.NONE) {
                    reward -= 4.0; // Penalty for getting dealth some status effect at all
                }
            }
        }
        
        return reward;
    }
    
    // Variable modifier on rewards, based on how many pokemons we/they KO from resulting action
    private double calculateKOReward(final BattleView state, final BattleView nextState) { 
        double reward = 0.0;
        
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        // Reward for KOing opponent's active pokemon
        if (theirCurrent != null && theirNext != null) {
            if (!theirCurrent.hasFainted() && theirNext.hasFainted()) {
                reward += 25.0; // Big reward for KOing
            }
        }
        
        // Penalty for our active pokemons getting KOed
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null) {
            if (!myCurrent.hasFainted() && myNext.hasFainted()) {
                reward -= 20.0; // Penalty for getting KOe
            }
        }
        
        return reward;
    }
    
    // Variable modifier, this is based on certain moves that are stragetically very good (based on the superEffective)
    private double calculateMoveEffectivenessReward(final BattleView state, final MoveView action, final BattleView nextState) { 
        double reward = 0.0;
        
        // TODO: Work on fully implementing this
        // Alex we'll need to calc the type effectiveness here
        // Req some sort of type matchup logic
        // From the javadocs, we could use the actual type effectiveness via Type.getEffectivenessModifier()
        
        // in prog heuristic for this function:
        // Rewards using damaging moves
        if (action.getPower() != null && action.getPower() > 0) {
            reward += 0.5;
        }
                
        return reward;
    }
    
    // Variable modifier, will calc other misc. factors AND the inital state factors (the s in R(s, a, s'))
    private double calculateStrategicReward(final BattleView state, final BattleView nextState) { 
        double reward = 0.0;
        
        TeamView myTeamNext = nextState.getTeam1View();
        TeamView theirTeamNext = nextState.getTeam2View();
        
        // Reward for having more alive pokemon
        int myAlive = countAlivePokemon(myTeamNext);
        int theirAlive = countAlivePokemon(theirTeamNext);
        reward += (myAlive - theirAlive) * 3.0;
        
        // Reward for having healthier team
        reward += calculateTeamHealthDifferential(myTeamNext, theirTeamNext);
        
        return reward;
    }

    // Calc. the difference of health in my team vs their team.
    private double calculateTeamHealthDifferential(TeamView myTeam, TeamView theirTeam) {
        double myTotalHealth = 0.0;
        double myMaxHealth = 0.0;
        double theirTotalHealth = 0.0;
        double theirMaxHealth = 0.0;
        
        for (int i = 0; i < myTeam.size(); i++) {
            PokemonView p = myTeam.getPokemonView(i);
            if (!p.hasFainted()) {
                myTotalHealth += p.getCurrentStat(Stat.HP);
                myMaxHealth += p.getInitialStat(Stat.HP);
            }
        }
        
        for (int i = 0; i < theirTeam.size(); i++) {
            PokemonView p = theirTeam.getPokemonView(i);
            if (!p.hasFainted()) {
                theirTotalHealth += p.getCurrentStat(Stat.HP);
                theirMaxHealth += p.getInitialStat(Stat.HP);
            }
        }
        
        double myHealthRatio = (myMaxHealth > 0) ? myTotalHealth / myMaxHealth : 0.0;
        double theirHealthRatio = (theirMaxHealth > 0) ? theirTotalHealth / theirMaxHealth : 0.0;
        
        return (myHealthRatio - theirHealthRatio) * 15.0;
    }

    // Calc. the # of alive pokemons in a team
    private int countAlivePokemon(TeamView team) {
        int count = 0;
        for (int i = 0; i < team.size(); i++) {
            if (!team.getPokemonView(i).hasFainted()) {
                count++;
            }
        }
        return count;
    }
}
