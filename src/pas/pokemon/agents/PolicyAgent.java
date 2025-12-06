package src.pas.pokemon.agents;


// SYSTEM IMPORTS
import net.sourceforge.argparse4j.inf.Namespace;

import edu.bu.pas.pokemon.agents.NeuralQAgent;
import edu.bu.pas.pokemon.agents.senses.SensorArray;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.linalg.Matrix;
import edu.bu.pas.pokemon.nn.Model;
import edu.bu.pas.pokemon.nn.models.Sequential;
import edu.bu.pas.pokemon.nn.layers.Dense; // fully connected layer
import edu.bu.pas.pokemon.nn.layers.ReLU;  // some activations (below too)
import edu.bu.pas.pokemon.nn.layers.Tanh;
import edu.bu.pas.pokemon.nn.layers.Sigmoid;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;

import edu.bu.pas.pokemon.core.enums.Stat;
import edu.bu.pas.pokemon.core.enums.NonVolatileStatus;
import edu.bu.pas.pokemon.core.enums.Type;


// JAVA PROJECT IMPORTS
import src.pas.pokemon.senses.CustomSensorArray;
import java.util.Random;
import java.util.List;
import java.io.BufferedReader;  
import java.io.InputStreamReader;


public class PolicyAgent
    extends NeuralQAgent
{
    private boolean trainingMode = false;
    private double explorationRate = 0.3; // Start high, decay over time
    private int gamesPlayed = 0;

    public PolicyAgent()
    {
        super();
    }

    public void initializeSenses(Namespace args)
    {
        SensorArray modelSenses = new CustomSensorArray();

        this.setSensorArray(modelSenses);
    }

    @Override
    public void initialize(Namespace args)
    {
        // make sure you call this, this will call your initModel() and set a field
        // AND if the command line argument "inFile" is present will attempt to set
        // your model with the contents of that file.
        super.initialize(args);

        // what senses will your neural network have?
        this.initializeSenses(args);

        // do what you want just don't expect custom command line options to be available
        // when I'm testing your code

        System.err.println("[PolicyAgent] Initializing...");
    }

    @Override
    public Model initModel()
    {
        // Getting input dimensions
        CustomSensorArray sensorArray = (CustomSensorArray) this.getSensorArray();
        
        // Neural network
        // TODO: WE GOTTA CHANGE THE ARCH. LATER (prolly)
        Sequential qFunction = new Sequential();
        
        // Input --> sensorArray.getNumFeatures() should be set after 1st call
        // TODO: FIGURE OUT A WAY TO MAKE THE INPUT DYNAMIC (just guessing the size rn)
        int inputSize = 120;
        
        qFunction.add(new Dense(inputSize, 256)); // 1st layer
        qFunction.add(new ReLU());
        qFunction.add(new Dense(256, 128));      // 2nd layer
        qFunction.add(new Tanh());
        qFunction.add(new Dense(128, 1));        // output layer, which is the Q-val
        
        return qFunction;
        }

    @Override
    public Integer chooseNextPokemon(BattleView view)
    {
        TeamView myTeam = this.getMyTeamView(view);
        TeamView oppTeam = this.getOpponentTeamView(view);
        PokemonView oppActive = oppTeam.getActivePokemonView();
        
        Integer bestIdx = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (int idx = 0; idx < myTeam.size(); ++idx) {
            PokemonView pokemon = myTeam.getPokemonView(idx);
            
            if (!pokemon.hasFainted()) {
                double score = evaluatePokemonForSwitch(pokemon, oppActive);
                
                if (bestIdx == null || score > bestScore) {
                    bestIdx = idx;
                    bestScore = score;
                }
            }
        }
        
        return bestIdx;
    }

    private double evaluatePokemonForSwitch(PokemonView myPokemon, PokemonView oppPokemon) {
        double score = 0.0;
        
        // Type ADV.
        score += calculateTypeMatchupScore(myPokemon, oppPokemon);
        
        // HP %
        double healthRatio = (double) myPokemon.getCurrentStat(Stat.HP) / 
                            myPokemon.getInitialStat(Stat.HP);
        score += healthRatio * 20.0;
        
        // Avoid injured or status-affected pokemons
        if (myPokemon.getNonVolatileStatus() != NonVolatileStatus.NONE) {
            score -= 15.0;
        }
        
        // Get pokemons w/ goot effects
        for (Stat stat : new Stat[]{Stat.ATK, Stat.DEF, Stat.SPD, Stat.SPATK, Stat.SPDEF}) {
            score += myPokemon.getCurrentStat(stat) / 100.0;
        }
        
        return score;
    }

    private double calculateTypeMatchupScore(PokemonView myPokemon, PokemonView oppPokemon) {
        if (oppPokemon == null) return 0.0;
        
        double score = 0.0;
        
        // Check how our types fare against opponent's types
        Type myType1 = myPokemon.getCurrentType1();
        Type myType2 = myPokemon.getCurrentType2();
        Type oppType1 = oppPokemon.getCurrentType1();
        Type oppType2 = oppPokemon.getCurrentType2();
        
        // Calculate DEF ADV. (how well we resist opponent's attacks)
        if (oppType1 != null) {
            if (myType1 != null) {
                double effectiveness = Type.getEffectivenessModifier(oppType1, myType1);
                if (effectiveness >= 2.0) score -= 10.0;  // We're weak
                else if (effectiveness <= 0.5 && effectiveness > 0.0) score += 5.0; // We resist
                else if (effectiveness == 0.0) score += 8.0; // We're immune
            }
            if (myType2 != null) {
                double effectiveness = Type.getEffectivenessModifier(oppType1, myType2);
                if (effectiveness >= 2.0) score -= 10.0;
                else if (effectiveness <= 0.5 && effectiveness > 0.0) score += 5.0;
                else if (effectiveness == 0.0) score += 8.0;
            }
        }
        
        if (oppType2 != null) {
            if (myType1 != null) {
                double effectiveness = Type.getEffectivenessModifier(oppType2, myType1);
                if (effectiveness >= 2.0) score -= 10.0;
                else if (effectiveness <= 0.5 && effectiveness > 0.0) score += 5.0;
                else if (effectiveness == 0.0) score += 8.0;
            }
            if (myType2 != null) {
                double effectiveness = Type.getEffectivenessModifier(oppType2, myType2);
                if (effectiveness >= 2.0) score -= 10.0;
                else if (effectiveness <= 0.5 && effectiveness > 0.0) score += 5.0;
                else if (effectiveness == 0.0) score += 8.0;
            }
        }
        
        return score;
    }
    
    @Override
    public void train() {
        this.trainingMode = true;
        // Decay exploration rate as we play more games
        explorationRate = Math.max(0.05, 0.3 * Math.exp(-gamesPlayed / 10000.0));
    }

    @Override
    public void eval() {
        this.trainingMode = false;
    }

    @Override
    public MoveView getMove(BattleView view)
    {
        // TODO: change this to include random exploration during training and maybe use the transition model to make
        // good predictions?
        // if you choose to use the transition model you might want to also override the makeGroundTruth(...) method
        // to not use temporal difference learning

        // currently always tries to argmax the learned model
        // this is not a good idea to always do when training. When playing evaluation games you *do* want to always
        // argmax your model, but when training our model may not know anything yet! So, its a good idea to sometime
        // during training choose *not* to argmax the model and instead choose something new at random.

        // HOW that randomness works and how often you do it are up to you, but it *will* affect the quality of your
        // learned model whether you do it or not!

        // This code will do 2 things:
        // Training --> explore (sometimes)
        // Eval --> ALWAYS use learned policy
        if (trainingMode && Math.random() < explorationRate) {
                // Random exploration
                List<MoveView> moves = this.getPotentialMoves(view);
                if (!moves.isEmpty()) {
                    return moves.get(new Random().nextInt(moves.size()));
                }
            }
            
        return this.argmax(view);
    }

    @Override
    public MoveView argmax(BattleView state) {
        List<MoveView> moves = getPotentialMoves(state);
        MoveView bestMove = null;
        double bestQValue = Double.NEGATIVE_INFINITY;
        
        for (MoveView move : moves) {
            // Use transition model if available
            double qValue = evaluateMoveWithTransition(state, move);
            
            if (qValue > bestQValue) {
                bestQValue = qValue;
                bestMove = move;
            }
        }
        
        return bestMove;
    }

    private double evaluateMoveWithTransition(BattleView state, MoveView move) {
        // Not properly implemented yet.
        return this.eval(state, move);
    }

    @Override
    public void afterGameEnds(BattleView view)
    {

    }

}