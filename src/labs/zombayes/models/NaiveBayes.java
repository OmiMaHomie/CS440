package src.labs.zombayes.models;


// SYSTEM IMPORTS
import edu.bu.labs.zombayes.agents.SurvivalAgent;
import edu.bu.labs.zombayes.linalg.Matrix;
import edu.bu.labs.zombayes.linalg.Functions;
import edu.bu.labs.zombayes.utils.Pair;
import edu.bu.labs.zombayes.features.Features.FeatureType;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


// JAVA PROJECT IMPORTS


public class NaiveBayes
    extends Object
{

    private final List<Pair<FeatureType, Integer> >    featureHeader;    // array of (FEATURE_TYPE, NUM_FEATURE_VALUES)

    // parameters of the model
    // TODO: decide what fields you need and what datatypes they should be and add them here!

    private Map<Integer, Double> classPriors; // P(class)
    private Map<Integer, List<Map<Integer, Double>>> discreteProbabilities; // P(feature_value | class) for the discrete features
    private Map<Integer, List<Double>> continuousMeans; // the mean for continuous features per class
    private Map<Integer, List<Double>> continuousStdDevs; // std dev for continuous features per class
    private double smoothingAlpha = 1.0; // smooting val (laplace)

    public NaiveBayes(List<Pair<FeatureType, Integer> > featureHeader)
    {
        this.featureHeader = featureHeader;

        // TODO: if you add fields you will need to initialize them here!

        this.classPriors = new HashMap<>();
        this.discreteProbabilities = new HashMap<>();
        this.continuousMeans = new HashMap<>();
        this.continuousStdDevs = new HashMap<>();
    }

    public List<Pair<FeatureType, Integer> > getFeatureHeader() { return this.featureHeader; }
    // TODO: if you add fields they probably should get getters and setters!

    // TODO: complete me!
    public void fit(Matrix X, Matrix y_gt)
    {
        ;
    }

    // TODO: complete me!
    public int predict(Matrix x)
    {
        return -1;
    }

}

