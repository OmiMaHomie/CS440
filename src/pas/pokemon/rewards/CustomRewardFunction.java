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
import edu.bu.pas.pokemon.core.enums.Type;
import edu.bu.pas.pokemon.core.enums.Height;

public class CustomRewardFunction
    extends RewardFunction
{

    public CustomRewardFunction()
    {
        super(RewardType.STATE_ACTION_STATE); // doing R(s, a, s')
    }

    public double getLowerBound()
    {
        return -1000.0; // Worst-case
    }

    public double getUpperBound()
    {
        return 1000.0; // Best-case
    }

    // R(s), not used
    public double getStateReward(final BattleView state)
    {
        return 0d;
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

        // This function is essentially the culmination of a bunch of various checks and rewards.

        // Win or lose?
        reward += calculateGameOutcomeReward(state, nextState);
        
        // KO modifier
        reward += calculateKOReward(state, nextState);
        
        // DMG exchange
        reward += calculateDamageExchangeReward(state, nextState);
        
        // Status effect dynamics
        reward += calculateStatusEffectReward(state, nextState);
        
        // Stat change (dis)advantage
        reward += calculateStatChangeReward(state, nextState);
        
        // Type effectiveness, move strat
        reward += calculateStrategicMoveReward(state, action, nextState);
        
        // Team & resource maangement
        reward += calculateResourceManagementReward(state, nextState);
        
        // Positionality, battlefield dynamics
        reward += calculateBattlefieldControlReward(state, nextState);
        
        // Calculates progressive (dis)advantage
        reward += calculateMomentumReward(state, nextState);
        
        return Math.max(getLowerBound(), Math.min(getUpperBound(), reward));
    }

    // based of if we won/lost the game
    private double calculateGameOutcomeReward(final BattleView state, final BattleView nextState) {
        if (!nextState.isOver()) return 0.0;
        
        TeamView myTeam = state.getTeam1View();
        TeamView theirTeam = state.getTeam2View();
        
        boolean weWon = true;
        for (int i = 0; i < theirTeam.size(); i++) {
            if (!theirTeam.getPokemonView(i).hasFainted()) {
                weWon = false;
                break;
            }
        }
        
        if (weWon) {
            return 100.0; // Big win reward
        } else {
            return -100.0; // Big loss penalty
        }
    }

    // based on the # of allies/opponents KOed in current state
    private double calculateKOReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        // Reward for KOing opponent
        if (theirCurrent != null && theirNext != null &&
            !theirCurrent.hasFainted() && theirNext.hasFainted()) {
            reward += 40.0; // Significant reward for KO
            
            // If we KOed without taking much dmg, award even more
            PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
            PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
            if (myCurrent != null && myNext != null) {
                double healthRemaining = (double) myNext.getCurrentStat(Stat.HP) / myCurrent.getInitialStat(Stat.HP);
                if (healthRemaining > 0.7) {
                    reward += 15.0; // Clean KO
                }
            }
        }
        
        // Penalty for our KO
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null &&
            !myCurrent.hasFainted() && myNext.hasFainted()) {
            reward -= 35.0; // Significant penalty for our KO
        }
        
        return reward;
    }

    // based on the ratio of dmg dealth to dmg taken
    private double calculateDamageExchangeReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null && theirCurrent != null && theirNext != null) {
            // dmg dealth
            int damageDealt = theirCurrent.getCurrentStat(Stat.HP) - theirNext.getCurrentStat(Stat.HP);
            double damageDealtRatio = (double) damageDealt / theirCurrent.getInitialStat(Stat.HP);
            
            // dmg taken
            int damageTaken = myCurrent.getCurrentStat(Stat.HP) - myNext.getCurrentStat(Stat.HP);
            double damageTakenRatio = (double) damageTaken / myCurrent.getInitialStat(Stat.HP);
            
            // dmg efficency
            double netDamageEfficiency = damageDealtRatio - damageTakenRatio;
            reward += netDamageEfficiency * 25.0;
            
            // give a bonus for big dmg dealers WITH low dmg taken
            if (damageDealt > 0 && damageDealtRatio > 0.3) { 
                reward += 5.0;
            }
        }
        
        return reward;
    }

    // based on the culminated status changes of enemies/allies
    private double calculateStatusEffectReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        // enemy status changes
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (theirCurrent != null && theirNext != null) {
            reward += evaluateStatusChange(theirCurrent, theirNext, true);
        }
        
        // our status changes
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null) {
            reward += evaluateStatusChange(myCurrent, myNext, false);
        }
        
        return reward;
    }

    // Helper method for calculateStatusEffectReward
    // Does the actual calculations of status changes in pokemons.
    private double evaluateStatusChange(PokemonView current, PokemonView next, boolean isOpponent) {
        double reward = 0.0;
        double multiplier = isOpponent ? 1.0 : -1.0; // + for opponent, - for us
        
        // Non-volatile status changes
        NonVolatileStatus currentStatus = current.getNonVolatileStatus();
        NonVolatileStatus nextStatus = next.getNonVolatileStatus();
        
        // Non-volatile status changes
        // Justifications for the weights below
        if (currentStatus != nextStatus) {
            switch (nextStatus) {
                case SLEEP:
                case FREEZE:
                    reward += 12.0 * multiplier; // prevents enemy moves
                    break;
                case PARALYSIS:
                    reward += 8.0 * multiplier; // SPD reduction, turn loss CHANCE
                    break;
                case TOXIC:
                    reward += 10.0 * multiplier; // dmg over time (is EXPONENTIAL)
                    break;
                case POISON:
                case BURN:
                    reward += 6.0 * multiplier; // const dmg over time
                    break;
                case NONE:
                    // This will depend on if, on the previous state, the pokemon was effected with something
                    // + if it was on opponent, - if on us
                    if (currentStatus != NonVolatileStatus.NONE) {
                        reward += -8.0 * multiplier;
                    }
                    break;
            }
        }
        
        // Volatile status changes
        // Justifications for the weights below
        for (Flag flag : Flag.values()) {
            boolean currentFlag = current.getFlag(flag);
            boolean nextFlag = next.getFlag(flag);
            
            if (currentFlag != nextFlag) {
                switch (flag) {
                    case CONFUSED:
                        reward += 4.0 * multiplier; // chance to deal dmg to self
                        break;
                    case TRAPPED:
                        reward += 5.0 * multiplier; // can't retreat
                        break;
                    case SEEDED:
                        reward += 6.0 * multiplier; // takes HP, gives to opponent
                        break;
                    case FLINCHED:
                        if (nextFlag) reward += 3.0 * multiplier; // chance to miss turn
                        break;
                    case FOCUS_ENERGY:
                        if (nextFlag) reward += -5.0 * multiplier; // better chance to do crit
                        break;
                }
            }
        }
        
        return reward;
    }

    // based on changes in stats.
    private double calculateStatChangeReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        // Our stat changes
        PokemonView myCurrent = state.getTeam1View().getActivePokemonView();
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        
        if (myCurrent != null && myNext != null) {
            reward += evaluateStatChanges(myCurrent, myNext, false);
        }
        
        // Opponent stat changes
        PokemonView theirCurrent = state.getTeam2View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (theirCurrent != null && theirNext != null) {
            reward += evaluateStatChanges(theirCurrent, theirNext, true);
        }
        
        return reward;
    }

    // Helper method for calculateStatChangeReward
    // Does the calculation of stat changes between states.
    private double evaluateStatChanges(PokemonView current, PokemonView next, boolean isOpponent) {
        double reward = 0.0;
        double multiplier = isOpponent ? -1.0 : 1.0; // + when opponent stats decrease OR when our stats increase
        
        Stat[] combatStats = {Stat.ATK, Stat.DEF, Stat.SPD, Stat.SPATK, Stat.SPDEF};
        
        for (Stat stat : combatStats) {
            int currentStage = current.getStatMultiplier(stat);
            int nextStage = next.getStatMultiplier(stat);
            int change = nextStage - currentStage;
            
            if (change != 0) {
                double statReward = change * 3.0 * multiplier;
                
                // Weighing OFF stats higher
                if (stat == Stat.ATK || stat == Stat.SPATK) {
                    statReward *= 1.2;
                }
                // SPD is good, weighing higher
                else if (stat == Stat.SPD) {
                    statReward *= 1.5;
                }
                
                reward += statReward;
            }
        }
        
        return reward;
    }

    // Based on move evaluation
    private double calculateStrategicMoveReward(final BattleView state, final MoveView action, final BattleView nextState) {
        double reward = 0.0;
        
        if (action == null) return 0.0;
        
        PokemonView opponent = state.getTeam2View().getActivePokemonView();
        PokemonView myActive = state.getTeam1View().getActivePokemonView();
        
        if (opponent != null && myActive != null) {
            // This is doing type effectiveness
            if (action.getPower() != null && action.getPower() > 0) {
                double effectiveness = calculateTypeEffectiveness(action.getType(), 
                    opponent.getCurrentType1(), opponent.getCurrentType2());
                
                if (effectiveness >= 4.0) {
                    reward += 15.0; // 4x super effective
                } else if (effectiveness >= 2.0) {
                    reward += 8.0; // 2x super effective
                } else if (effectiveness <= 0.5 && effectiveness > 0.0) {
                    reward -= 4.0; // Not very effective
                } else if (effectiveness == 0.0) {
                    reward -= 10.0; // No effect
                }
            }
            
            // Same Type Attack Bonus (STAB) reward
            if (action.getPower() != null && action.getPower() > 0) {
                Type moveType = action.getType();
                if (moveType == myActive.getCurrentType1() || moveType == myActive.getCurrentType2()) {
                    reward += 3.0; // STAB bonus
                }
            }
            
            // Priority move strategy
            if (action.getPriority() > 0) {
                // Weight higher when using priority moves when we're slower or low health
                int mySpeed = myActive.getCurrentStat(Stat.SPD);
                int theirSpeed = opponent.getCurrentStat(Stat.SPD);
                double myHealthRatio = (double) myActive.getCurrentStat(Stat.HP) / 
                                     myActive.getInitialStat(Stat.HP);
                
                if (mySpeed < theirSpeed || myHealthRatio < 0.3) {
                    reward += 4.0;
                }
            }
        }
        
        return reward;
    }

    // based on resource management (the pokemons within the team)
    private double calculateResourceManagementReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        TeamView myTeamNext = nextState.getTeam1View();
        TeamView theirTeamNext = nextState.getTeam2View();
        
        // Alive pokemon differential
        int myAlive = countAlivePokemon(myTeamNext);
        int theirAlive = countAlivePokemon(theirTeamNext);
        reward += (myAlive - theirAlive) * 8.0;
        
        // Team health advantage
        reward += calculateTeamHealthDifferential(myTeamNext, theirTeamNext);
        
        return reward;
    }

    // based on battlefield control 
    private double calculateBattlefieldControlReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        // Height adv.
        PokemonView myNext = nextState.getTeam1View().getActivePokemonView();
        PokemonView theirNext = nextState.getTeam2View().getActivePokemonView();
        
        if (myNext != null && theirNext != null) {
            // Weight better if a team is taking advantage of height limitation that retricts the opponent
            Height myHeight = myNext.getHeight();
            Height theirHeight = theirNext.getHeight();
            
            if ((myHeight == Height.IN_AIR || myHeight == Height.UNDERGROUND) && 
                theirHeight == Height.NONE) {
                reward += 2.0; // We have the height adv.
            }
        }
        
        // Screen effects (Reflect/Light Screen)
        TeamView myTeamNext = nextState.getTeam1View();
        TeamView theirTeamNext = nextState.getTeam2View();
        
        if (myTeamNext.getNumReflectTurnsRemaining() > 0) {
            reward += 1.0; // Reflect
        }
        if (myTeamNext.getNumLightScreenTurnsRemaining() > 0) {
            reward += 1.0; // Light Screen
        }
        
        return reward;
    }

    // based on maintaining the initative/momentum
    private double calculateMomentumReward(final BattleView state, final BattleView nextState) {
        double reward = 0.0;
        
        // This stat will grow higher OR lower progressive over time AS LONG AS:
        // You maintain a better outlook of the game over time (+)
        // The enemy maintains a better outlook of the game over time (-)
        double currentAdvantage = evaluateBattleState(state);
        double nextAdvantage = evaluateBattleState(nextState);
        double momentum = nextAdvantage - currentAdvantage;
        
        reward += momentum * 2.0; // Scaling
        
        return reward;
    }

    // Helper method for type effectiveness
    private double calculateTypeEffectiveness(Type moveType, Type defenderType1, Type defenderType2) {
        double effectiveness = 1.0;
        effectiveness *= Type.getEffectivenessModifier(moveType, defenderType1);
        if (defenderType2 != null) {
            effectiveness *= Type.getEffectivenessModifier(moveType, defenderType2);
        }
        return effectiveness;
    }

    // Helper method, calc. difference in team's total HP
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
        
        return (myHealthRatio - theirHealthRatio) * 20.0;
    }

    // Helper method, calc. # of alive pokemons in a team
    private int countAlivePokemon(TeamView team) {
        int count = 0;
        for (int i = 0; i < team.size(); i++) {
            if (!team.getPokemonView(i).hasFainted()) {
                count++;
            }
        }
        return count;
    }

    // Helper method, calc. the general outlook of the game (which side appears to be winning?)
    private double evaluateBattleState(final BattleView state) {
        TeamView myTeam = state.getTeam1View();
        TeamView theirTeam = state.getTeam2View();
        
        double score = 0.0;
        score += calculateTeamHealthDifferential(myTeam, theirTeam) / 2.0;
        score += (countAlivePokemon(myTeam) - countAlivePokemon(theirTeam)) * 5.0;
        
        return score;
    }
}
