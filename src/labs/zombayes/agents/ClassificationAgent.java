package src.labs.zombayes.agents;


// SYSTEM IMPORTS
import edu.bu.labs.zombayes.agents.SurvivalAgent;
import edu.bu.labs.zombayes.features.Features.FeatureType;
import edu.bu.labs.zombayes.linalg.Matrix;
import edu.bu.labs.zombayes.utils.Pair;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import src.labs.zombayes.models.NaiveBayes;



public class ClassificationAgent
    extends SurvivalAgent
{

    private NaiveBayes model;

    public ClassificationAgent(int playerNum, String[] args)
    {
        super(playerNum, args);

        // System.out.println("ClassificationAgent constructor called with playerNum: " + playerNum);

        List<Pair<FeatureType, Integer> > featureHeader = new ArrayList<>(4);
        featureHeader.add(new Pair<>(FeatureType.CONTINUOUS, -1));
        featureHeader.add(new Pair<>(FeatureType.CONTINUOUS, -1));
        featureHeader.add(new Pair<>(FeatureType.DISCRETE, 3));
        featureHeader.add(new Pair<>(FeatureType.DISCRETE, 4));

        this.model = new NaiveBayes(featureHeader);

        // System.out.println("ClassificationAgent initialized successfully");


    }

    public NaiveBayes getModel() { return this.model; }

    @Override
    public void train(Matrix X, Matrix y_gt)
    {
        // System.out.println("TRAIN called - X shape: " + X.getShape() + " y_gt shape: " + y_gt.getShape());
        this.getModel().fit(X, y_gt);
        // System.out.println("Training completed");
    }

    @Override
    public int predict(Matrix featureRowVector)
    {
        //System.out.println("PREDICT called");
        int result = this.getModel().predict(featureRowVector);
        //.out.println("Prediction result: " + result);
        return result;
    }

}
