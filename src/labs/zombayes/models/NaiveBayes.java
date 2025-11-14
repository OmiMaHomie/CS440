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

        // Process each feature
        int numFeatures = featureHeader.size();
        
        for (int classLabel : classCounts.keySet()) {
            discreteProbabilities.put(classLabel, new ArrayList<>());
            continuousMeans.put(classLabel, new ArrayList<>());
            continuousStdDevs.put(classLabel, new ArrayList<>());
        }
        
        for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
            Pair<FeatureType, Integer> featureInfo = featureHeader.get(featureIdx);
            FeatureType type = featureInfo.getFirst();
            
            if (type == FeatureType.DISCRETE) {
                processDiscreteFeature(X, y_gt, featureIdx, featureInfo.getSecond());
            } else if (type == FeatureType.CONTINUOUS) {
                processContinuousFeature(X, y_gt, featureIdx);
            }
        }
    }

    // For discrete features, we need to calculate its own properties/probablities
    private void processDiscreteFeature(Matrix X, Matrix y_gt, int featureIdx, int numValues) {
        int totalExamples = X.getShape().getFirst();
        
        for (int classLabel : classPriors.keySet()) {
            // Count the # of time we see each feature value for this class
            Map<Integer, Integer> valueCounts = new HashMap<>();
            int classTotal = 0;
            
            for (int i = 0; i < totalExamples; i++) {
                if ((int) y_gt.get(i, 0) == classLabel) {
                    int featureValue = (int) X.get(i, featureIdx);
                    valueCounts.put(featureValue, valueCounts.getOrDefault(featureValue, 0) + 1);
                    classTotal++;
                }
            }
            
            // Calc the probabilities
            Map<Integer, Double> probabilities = new HashMap<>();
            for (int value = 0; value < numValues; value++) {
                int count = valueCounts.getOrDefault(value, 0);
                // (count + alpha) / (classTotal + alpha * numValues)
                double probability = (count + smoothingAlpha) / (classTotal + smoothingAlpha * numValues);
                probabilities.put(value, probability);
            }
            
            discreteProbabilities.get(classLabel).add(probabilities);
        }
    }

    // For continuous features, we can calculate its properties/probabilties with gaussian distribution
    private void processContinuousFeature(Matrix X, Matrix y_gt, int featureIdx) {
        int totalExamples = X.getShape().getFirst();
        
        for (int classLabel : classPriors.keySet()) {
            // Get all feature vals for this class
            List<Double> values = new ArrayList<>();
            
            for (int i = 0; i < totalExamples; i++) {
                if ((int) y_gt.get(i, 0) == classLabel) {
                    values.add(X.get(i, featureIdx));
                }
            }
            
            // mean
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            // standard deviation
            double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
            double stdDev = Math.sqrt(variance);
            
            if (stdDev == 0) stdDev = 1e-6; // Make sure not to divide by 0
            
            continuousMeans.get(classLabel).add(mean);
            continuousStdDevs.get(classLabel).add(stdDev);
        }
    }

    // Helper method to allow for continuous feature probability calc.
    private double gaussianPDF(double x, double mean, double stdDev) {
        double exponent = Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(stdDev, 2)));
        return (1.0 / (stdDev * Math.sqrt(2 * Math.PI))) * exponent;
    }

    // TODO: complete me!
    public int predict(Matrix x)
    {
        return -1;
    }

}

