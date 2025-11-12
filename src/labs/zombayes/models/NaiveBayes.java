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
    //
    // FIELDS
    //

    private final List<Pair<FeatureType, Integer> >    featureHeader;    // array of (FEATURE_TYPE, NUM_FEATURE_VALUES)

    // parameters of the model
    // TODO: decide what fields you need and what datatypes they should be and add them here!

    private Map<Integer, Double> classPriors; // P(class)
    private Map<Integer, List<Map<Integer, Double>>> discreteProbabilities; // P(feature_value | class) for the discrete features
    private Map<Integer, List<Double>> continuousMeans; // the mean for continuous features per class
    private Map<Integer, List<Double>> continuousStdDevs; // std dev for continuous features per class
    private double smoothingAlpha = 1.0; // smooting val (laplace)

    //
    // CONSTRUCTOR(S)
    //

    public NaiveBayes(List<Pair<FeatureType, Integer> > featureHeader)
    {
        this.featureHeader = featureHeader;

        // TODO: if you add fields you will need to initialize them here!

        this.classPriors = new HashMap<>();
        this.discreteProbabilities = new HashMap<>();
        this.continuousMeans = new HashMap<>();
        this.continuousStdDevs = new HashMap<>();
    }

    //
    // GET/SET
    //

    public List<Pair<FeatureType, Integer> > getFeatureHeader() { return this.featureHeader; }
    // TODO: if you add fields they probably should get getters and setters!

    //
    // METHODS
    //

    // TODO: complete me!
    public void fit(Matrix X, Matrix y_gt)
    {
        // Count classes, calc the priors
        int totalExamples = X.getShape().getFirst(); // # of rows
        Map<Integer, Integer> classCounts = new HashMap<>();
        
        // Count the # of occurances of each class
        for (int i = 0; i < totalExamples; i++) {
            int classLabel = (int) y_gt.get(i, 0);
            classCounts.put(classLabel, classCounts.getOrDefault(classLabel, 0) + 1);
        }
        
        // Calc prob. of each prior
        for (Map.Entry<Integer, Integer> entry : classCounts.entrySet()) {
            classPriors.put(entry.getKey(), (double) entry.getValue() / totalExamples);
        }
        
        // System.out.println("Class counts: " + classCounts);
        // System.out.println("Class priors: " + classPriors);
    }

    // TODO: complete me!
    public int predict(Matrix x)
    {
        return -1;
    }

}

