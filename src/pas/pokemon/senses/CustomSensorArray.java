package src.pas.pokemon.senses;

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
import edu.bu.pas.pokemon.core.enums.Type;

public class CustomSensorArray
    extends SensorArray
{
    //
    // FIELDS
    //
    int numFeatures;

    //
    // CONSTRUCTOR(S)
    //
    public CustomSensorArray()
    {
        numFeatures = 29; // # of features
    }

    //
    // GET/SET
    //
    public int getNumFeatures() {
        return numFeatures;
    }

    //
    // METHODS
    //
    
        // TODO: Convert a BattleView and a MoveView into a row-vector containing measurements for every sense
        // you want your neural network to have. This method should be called if your model is a q-based model
        // currently returns 64 random numbers
    // Gets all vals that MIGHT be used for calculcating util. val, and put into an array.
    public Matrix getSensorValues(final BattleView state, final MoveView action)
    {
        // We will collect 3 main types of info:
        // General sensor values (of pokemons, the battlefield, etc.)
        // Individual pokemon stats
        // Move stats

        List<Double> features = new ArrayList<>();

        addPokemonState(features, state.getTeam1View().getActivePokemonView(), true); // Our pokemon
        addPokemonState(features, state.getTeam2View().getActivePokemonView(), false); // Their pokemon
        
        addMoveFeatures(features, action);
        
        addTeamFeatures(features, state.getTeam1View(), true); // Our team
        addTeamFeatures(features, state.getTeam2View(), false); // Their team

        //making the row vector size of feature list
        Matrix rowVector = Matrix.zeros(1, features.size());
        for (int i = 0; i < features.size(); i++) {
            rowVector.set(0, i, features.get(i));
        }
        return rowVector;
    }

    private void addPokemonState(List<Double> features, PokemonView pokemon, boolean isOurs) {
        // HP
        double hpRatio = (double) pokemon.getCurrentStat(Stat.HP) / 
                        pokemon.getInitialStat(Stat.HP);
        features.add(hpRatio);
        
        // status conditions
        NonVolatileStatus status = pokemon.getNonVolatileStatus();
        features.add(status == NonVolatileStatus.SLEEP ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.POISON ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.BURN ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.PARALYSIS ? 1.0 : 0.0);
        features.add(status == NonVolatileStatus.FREEZE ? 1.0 : 0.0);
        
        // other statues (more unpreictable)
        features.add(pokemon.getFlag(Flag.CONFUSED) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.TRAPPED) ? 1.0 : 0.0);
        features.add(pokemon.getFlag(Flag.FLINCHED) ? 1.0 : 0.0);
        
        // lv
        features.add((double) pokemon.getLevel() / 100.0);
    }

    private void addMoveFeatures(List<Double> features, MoveView action) {
        // move prop.
        features.add(action.getPower() != null ? (double) action.getPower() / 200.0 : 0.0);
        features.add(action.getAccuracy() != null ? (double) action.getAccuracy() / 100.0 : 1.0);
        features.add((double) action.getPP() / 40.0); // normalize PP
        features.add((double) action.getPriority());
        
        // move category
        features.add(action.getCategory() == Move.Category.PHYSICAL ? 1.0 : 0.0);
        features.add(action.getCategory() == Move.Category.SPECIAL ? 1.0 : 0.0);
        features.add(action.getCategory() == Move.Category.STATUS ? 1.0 : 0.0);
    }

    private void addTeamFeatures(List<Double> features, TeamView team, boolean isOurs) {
        // # of alive pokemons
        int aliveCount = 0;
        for (int i = 0; i < team.size(); i++) {
            if (!team.getPokemonView(i).hasFainted()) {
                aliveCount++;
            }
        }
        features.add((double) aliveCount / 6.0); // normalized
    }
}
