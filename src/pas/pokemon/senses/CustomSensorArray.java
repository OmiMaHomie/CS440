package src.pas.pokemon.senses;


import java.util.ArrayList;
import java.util.List;

// SYSTEM IMPORTS


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

    // TODO: make fields if you want!
    int numFeatures;

    public CustomSensorArray()
    {
        // TODO: intialize those fields if you make any!
        numFeatures = 21; //number of features
    }
    public int getNumFeatures() {
        return numFeatures;
    }
    public Matrix getSensorValues(final BattleView state, final MoveView action)
    {


        // TODO: Convert a BattleView and a MoveView into a row-vector containing measurements for every sense
        // you want your neural network to have. This method should be called if your model is a q-based model
        // currently returns 64 random numbers
        List<Double> battleFeatures = new ArrayList<>();
        //Getting action attributes
        battleFeatures.add((double) action.getAccuracy());
        battleFeatures.add((double) action.getPP());
        battleFeatures.add((double) action.getPower());
        battleFeatures.add((double) action.getCriticalHitRatio());
        battleFeatures.add((double) action.getPriority());
        battleFeatures.add((double) action.getNumDisabledTurnsRemaining());
        if (action.getCategory() == Move.Category.PHYSICAL) {
            battleFeatures.add((double) 1);
        }
        if (action.getCategory() == Move.Category.SPECIAL) {
            battleFeatures.add((double) 2);
        }
        if (action.getCategory() == Move.Category.STATUS) {
            battleFeatures.add((double) 3);
        }
        //Getting player 1 state values
        battleFeatures.add((double) state.getTeam1View().getLastDamageTaken());
        battleFeatures.add((double) state.getTeam1View().getNumLightScreenTurnsRemaining());
        battleFeatures.add((double) state.getTeam1View().getNumReflectTurnsRemaining());
        battleFeatures.add((double) state.getTeam1View().getActivePokemonView().getLastDamageDealt());
        battleFeatures.add((double) state.getTeam1View().getActivePokemonView().getHeight().ordinal());
        //type 1 team 1
        Type type1team1 = state.getTeam1View().getActivePokemonView().getCurrentType1();
        battleFeatures.add((double) type1team1.ordinal());
        //type 2 team 1
        Type type2team1 = state.getTeam1View().getActivePokemonView().getCurrentType2();
        battleFeatures.add((double) type2team1.ordinal());
        

        //Getting player 2 state values
        battleFeatures.add((double) state.getTeam2View().getLastDamageTaken());
        battleFeatures.add((double) state.getTeam2View().getNumLightScreenTurnsRemaining());
        battleFeatures.add((double) state.getTeam2View().getNumReflectTurnsRemaining());
        battleFeatures.add((double) state.getTeam2View().getActivePokemonView().getLastDamageDealt()); 
        battleFeatures.add((double) state.getTeam2View().getActivePokemonView().getHeight().ordinal());
        //type 1 team 2
        Type type1team2 = state.getTeam2View().getActivePokemonView().getCurrentType1();

        battleFeatures.add((double) type1team2.ordinal());
        //type 2 team 2
        Type type2team2 = state.getTeam2View().getActivePokemonView().getCurrentType2();
        battleFeatures.add((double) type2team2.ordinal());
        Matrix rowVector =  Matrix.zeros(1, battleFeatures.size()); //making the row vector size of feature list
        for (int i = 0; i < battleFeatures.size(); i++) {
            rowVector.set(0, i, battleFeatures.get(i));
        }
        return rowVector;
    }


}
