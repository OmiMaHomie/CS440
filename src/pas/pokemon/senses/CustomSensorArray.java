package src.pas.pokemon.senses;

import java.security.Policy;
// SYSTEM IMPORTS
import java.util.ArrayList;
import java.util.List;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pokemon.agents.senses.SensorArray;
import edu.bu.pas.pokemon.core.Move;
import edu.bu.pas.pokemon.core.Pokemon;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.linalg.Matrix;
import src.pas.pokemon.agents.PolicyAgent;
import edu.bu.pas.pokemon.core.enums.Type;
import edu.bu.pas.pokemon.core.enums.Stat;
import edu.bu.pas.pokemon.core.enums.NonVolatileStatus;
import edu.bu.pas.pokemon.core.enums.Flag;
import edu.bu.pas.pokemon.core.enums.Height;
public class CustomSensorArray
    extends SensorArray
{
   
    //
    // FIELDS
    //
    PolicyAgent agent;
    int numFeatures;

    //
    // CONSTRUCTOR(S)
    //
     public CustomSensorArray(PolicyAgent agent) { //This fixed it! I needed to include the agent to know which team it is on. Because gradescope showing won first 25 but lost other 25 wierd.
        numFeatures = 0; 
        this.agent = agent; // the actual agent playing the battle
    }

    //
    // GET/SET
    //
    public int getNumFeatures() {
        return this.numFeatures;
    }
    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }

    //
    // METHODS
    //
    
        // TODO: Convert a BattleView and a MoveView into a row-vector containing measurements for every sense
        // you want your neural network to have. This method should be called if your model is a q-based model
        // currently returns 64 random numbers
    // Gets all vals that MIGHT be used for calculcating util. val, and put into an array.


    public Matrix getSensorValues(final BattleView state, final MoveView action) {

        //Commented out below is a getSensorValules which is much more in depth with many many values, 
        // but this just focuses on move stats, category, and effectiveness rather than trying to do too many things at once
        TeamView myTeam = agent.getMyTeamView(state);
        TeamView opponentTeam = agent.getOpponentTeamView(state);
        List<Double> features = new ArrayList<>();

        PokemonView ours = myTeam.getActivePokemonView();
        PokemonView theirs = opponentTeam.getActivePokemonView();
        features.add((double) ours.getCurrentStat(Stat.ATK) / 200.0); //small vals making it all within a small range
        features.add((double) ours.getCurrentStat(Stat.SPATK) / 200.0);
        features.add((double) ours.getCurrentStat(Stat.SPD) / 200.0);
        features.add((double) theirs.getCurrentStat(Stat.DEF) / 200.0);
        features.add((double) theirs.getCurrentStat(Stat.SPDEF) / 200.0);
        features.add(action.getPower() != null ? action.getPower() / 200.0 : 0.0);
        features.add(action.getCategory() == Move.Category.PHYSICAL ? 1.0 : 0.0); //doing physical special not status for this simple
        features.add(action.getCategory() == Move.Category.SPECIAL  ? 1.0 : 0.0);
        double overallTypeEffectiveness = calculateTypeEffectiveness(action.getType(),theirs.getCurrentType1(), theirs.getCurrentType2());
        features.add(overallTypeEffectiveness);
         // Updates # of features
        setNumFeatures(features.size());
        Matrix rowVector = Matrix.zeros(1, features.size());
        for (int i = 0; i < features.size(); i++) {
            rowVector.set(0, i, features.get(i));
        }
        return rowVector;
    }
    /*
    public Matrix getSensorValues(final BattleView state, final MoveView action)
    {
        // We will collect 4 main types of info:
        // General sensor values (of pokemons, the battlefield, etc.)
        // Individual pokemon stats
        // Battle-level stats
        // Move stats

        List<Double> features = new ArrayList<>();

        addPokemonState(features, state.getTeam1View().getActivePokemonView(), true); // Our pokemon
        addPokemonState(features, state.getTeam2View().getActivePokemonView(), false); // Their pokemon
        
        addMoveFeatures(features, state, action);
        
        addTeamFeatures(features, state.getTeam1View(), true); // Our team
        addTeamFeatures(features, state.getTeam2View(), false); // Their team

        addBattleFeatures(features, state);

        // DEBUG: Print feature count on first call
        if (getNumFeatures() == 0) {
            System.err.println("[CustomSensorArray] Feature count: " + features.size());
            // Optional: print feature breakdown
            System.err.println("[CustomSensorArray] First 10 features: ");
            for (int i = 0; i < Math.min(120, features.size()); i++) {
                System.err.println("  Feature " + i + ": " + features.get(i));
            }
        }

        // Updates # of features
        setNumFeatures(features.size());

        Matrix rowVector = Matrix.zeros(1, features.size());
        for (int i = 0; i < features.size(); i++) {
            rowVector.set(0, i, features.get(i));
        }
        return rowVector;
    }
     */

    private void addPokemonState(List<Double> features, PokemonView pokemon, boolean isOurs) {
        // HP
        double hpRatio = (double) pokemon.getCurrentStat(Stat.HP) / 
                        pokemon.getInitialStat(Stat.HP);
        features.add(hpRatio);
        
        // LV (normalize)
        features.add((double) pokemon.getLevel() / 100.0);
        
        // Stage multipliers (normalized from +/-6)
        features.add((double) pokemon.getStatMultiplier(Stat.ATK) / 6.0);    
        features.add((double) pokemon.getStatMultiplier(Stat.DEF) / 6.0);
        features.add((double) pokemon.getStatMultiplier(Stat.SPD) / 6.0);
        features.add((double) pokemon.getStatMultiplier(Stat.SPATK) / 6.0);
        features.add((double) pokemon.getStatMultiplier(Stat.SPDEF) / 6.0);
        features.add((double) pokemon.getStatMultiplier(Stat.ACC) / 6.0);
        features.add((double) pokemon.getStatMultiplier(Stat.EVASIVE) / 6.0);
        
        // Height status
        Height height = pokemon.getHeight();
        features.add(height == Height.IN_AIR ? 1.0 : 0.0);        // Flying
        features.add(height == Height.UNDERGROUND ? 1.0 : 0.0);   // Underground  
        features.add(height == Height.NONE ? 1.0 : 0.0);          // Normal
        
        // Non-volatile statuses
        NonVolatileStatus status = pokemon.getNonVolatileStatus();
        features.add(status == NonVolatileStatus.SLEEP ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.POISON ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.BURN ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.PARALYSIS ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.FREEZE ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.TOXIC ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.NONE ? 1.0 : 0.0);
        
        // Volatile statuses
        features.add(pokemon.getFlag(Flag.CONFUSED) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.TRAPPED) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.FLINCHED) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.FOCUS_ENERGY) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.SEEDED) ? 1.0 : 0.0);
        
        // Pokemon types, one-hot encoded
        Type type1 = pokemon.getCurrentType1();
        Type type2 = pokemon.getCurrentType2();
        for (Type t : Type.values()) {
            features.add((type1 == t || type2 == t) ? 1.0 : 0.0);
        }
        
        // Active move status
        features.add(pokemon.getActiveMoveView() != null ? 1.0 : 0.0);
        
        // Sub. status
        features.add(pokemon.getSubstitute() != null ? 1.0 : 0.0);
        
        // Unchangeable stats
        features.add(pokemon.getStatsUnchangeable() ? 1.0 : 0.0);
    }

    private void addMoveFeatures(List<Double> features, BattleView state, MoveView action) {
        // move prop.
        features.add(action.getPower() != null ? (double) action.getPower() / 200.0 : 0.0);
        features.add(action.getAccuracy() != null ? (double) action.getAccuracy() / 100.0 : 1.0);
        features.add((double) action.getPP() / 40.0);
        features.add((double) action.getPriority());
        features.add((double) action.getCriticalHitRatio());
        
        // move categories
        features.add(action.getCategory() == Move.Category.PHYSICAL ? 1.0 : 0.0);
        features.add(action.getCategory() == Move.Category.SPECIAL ? 1.0 : 0.0);
        features.add(action.getCategory() == Move.Category.STATUS ? 1.0 : 0.0);
        
        // move types
        Type moveType = action.getType();
        for (Type t : Type.values()) {
            features.add(moveType == t ? 1.0 : 0.0);
        }
        
        // height restrictions
        features.add(action.getCanHitHeights().contains(Height.IN_AIR) ? 1.0 : 0.0);
        features.add(action.getCanHitHeights().contains(Height.UNDERGROUND) ? 1.0 : 0.0);
        features.add(action.getCanHitHeights().contains(Height.NONE) ? 1.0 : 0.0);
        
        // type effectiveness
        PokemonView opponent = state.getTeam2View().getActivePokemonView();
        if (opponent != null && action.getPower() != null && action.getPower() > 0) {
            double effectiveness = calculateTypeEffectiveness(action.getType(), 
                opponent.getCurrentType1(), opponent.getCurrentType2());
            features.add(effectiveness);
        } else {
            features.add(1.0); // Neutral effectiveness
        }
    }
    private double calculateTypeEffectiveness(Type moveType, Type defenderType1, Type defenderType2) {
        double effectiveness = 1.0;
        
        // calc. effectiveness against first type
        effectiveness *= Type.getEffectivenessModifier(moveType, defenderType1);
        
        // calc. effectiveness against second type (if possible)
        if (defenderType2 != null) {
            effectiveness *= Type.getEffectivenessModifier(moveType, defenderType2);
        }
        
        return effectiveness;
    }


    private void addTeamFeatures(List<Double> features, TeamView team, boolean isOurs) {
        // Alive count and team health
        int aliveCount = 0;
        double totalHealth = 0.0;
        double maxHealth = 0.0;
        
        for (int i = 0; i < team.size(); i++) {
            PokemonView p = team.getPokemonView(i);
            if (!p.hasFainted()) {
                aliveCount++;
                totalHealth += p.getCurrentStat(Stat.HP);
                maxHealth += p.getInitialStat(Stat.HP);
            }
        }
        
        features.add((double) aliveCount / 6.0); // alive count (normalized)
        features.add(maxHealth > 0 ? totalHealth / maxHealth : 0.0); // team HP ratio
        
        // Team buffs (Light Screen, Reflect)
        features.add((double) team.getNumLightScreenTurnsRemaining() / 8.0); // Normalized
        features.add((double) team.getNumReflectTurnsRemaining() / 8.0); // Normalized
    }

    private void addBattleFeatures(List<Double> features, BattleView state) {       
        // state of the battle
        features.add(state.isOver() ? 1.0 : 0.0);
    }
}
