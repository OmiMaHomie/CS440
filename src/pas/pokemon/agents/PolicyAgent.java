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


// JAVA PROJECT IMPORTS
import src.pas.pokemon.senses.CustomSensorArray;
import java.util.Random;
import java.util.List;


public class PolicyAgent
    extends NeuralQAgent
{
    private final double EPSILON = 0.1; // we can fool around with this to find the optimal #

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
        int inputSize = 128;
        
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
        // TODO: change this to something more intelligent!

        // find a pokemon that is alive
        for(int idx = 0; idx < this.getMyTeamView(view).size(); ++idx)
        {
            if(!this.getMyTeamView(view).getPokemonView(idx).hasFainted())
            {
                return idx;
            }
        }
        return null;
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
        boolean isTraining = true; // to ensure we're tracking the 2 different states
        
        if (isTraining) {
            // Epsilon-greedy exploration
            double epsilon = EPSILON;
            Random random = new Random();
            
            if (random.nextDouble() < epsilon) {
                // choosees a rnd move
                List<MoveView> moves = this.getPotentialMoves(view);
                if (!moves.isEmpty()) {
                    return moves.get(random.nextInt(moves.size()));
                }
            }
        }
        
        // Uses the learned policy
        return this.argmax(view);
    }

    @Override
    public void afterGameEnds(BattleView view)
    {

    }

}