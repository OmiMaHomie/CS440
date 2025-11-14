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
    private double smoothingAlpha = 0.1; // smoothing val (laplace)

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

    // Essentially is training the Naive Bayes model on the data.
    public void fit(Matrix X, Matrix y_gt)
    {
        if (X == null || y_gt == null) {
            System.err.println("fit() received null matrices");
            return;
        }

        // Count classes, calc the priors
        int totalExamples = X.getShape().getNumRows(); // # of rows
        Map<Integer, Integer> classCounts = new HashMap<>();
        
        // Count the # of occurrences of each class
        for (int i = 0; i < totalExamples; i++) {
            int classLabel = (int) y_gt.get(i, 0);
            classCounts.put(classLabel, classCounts.getOrDefault(classLabel, 0) + 1);
        }

        // System.out.println("Class distribution: " + classCounts);

        // Calc prob. of each prior
        for (Map.Entry<Integer, Integer> entry : classCounts.entrySet()) {
            classPriors.put(entry.getKey(), (double) entry.getValue() / totalExamples);
        }

        // System.out.println("Class counts: " + classCounts);
        // System.out.println("Class priors: " + classPriors);

        // Process each feature
        int numFeatures = featureHeader.size();
        
        // Log feature info
        // System.out.println("Number of features: " + numFeatures);
        // for (int i = 0; i < numFeatures; i++) {
        //     Pair<FeatureType, Integer> feature = featureHeader.get(i);
        //     System.out.println("Feature " + i + ": " + feature.getFirst() + " with " + feature.getSecond() + " values");
        // }   
    
        // init the data structure w/ correct size
        for (int classLabel : classCounts.keySet()) {
            discreteProbabilities.put(classLabel, new ArrayList<>(numFeatures));
            continuousMeans.put(classLabel, new ArrayList<>(numFeatures));
            continuousStdDevs.put(classLabel, new ArrayList<>(numFeatures));
            
            // init with null/zeros
            for (int i = 0; i < numFeatures; i++) {
                discreteProbabilities.get(classLabel).add(null);
                continuousMeans.get(classLabel).add(0.0);
                continuousStdDevs.get(classLabel).add(0.0);
            }
        }
        
        // Process each feature
        for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
            Pair<FeatureType, Integer> featureInfo = featureHeader.get(featureIdx);
            FeatureType type = featureInfo.getFirst();
            
            if (type == FeatureType.DISCRETE) {
                processDiscreteFeature(X, y_gt, featureIdx, featureInfo.getSecond());
            } else if (type == FeatureType.CONTINUOUS) {
                processContinuousFeature(X, y_gt, featureIdx);
            }
        }
        
        // Check what we stored
        // System.out.println("FINAL MODEL STATE");
        // for (int classLabel : classCounts.keySet()) {
        //     System.out.println("Class " + classLabel + ":");
        //     System.out.println("Discrete features: " + discreteProbabilities.get(classLabel).size());
        //     System.out.println("Continuous means: " + continuousMeans.get(classLabel).size());
        //     System.out.println("Continuous stddevs: " + continuousStdDevs.get(classLabel).size());
        // }
    }

    // Calc a discrete feature. Calc. conditional probabilities for each feature value given for each class (smooting for unseen feature vals).
    private void processDiscreteFeature(Matrix X, Matrix y_gt, int featureIdx, int numValues) {
       int totalExamples = X.getShape().getNumRows();
    
        // System.out.println("Processing discrete feature " + featureIdx + " with " + numValues + " possible values");
        
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
            
            // System.out.println("Class " + classLabel + " has " + classTotal + " examples for feature " + featureIdx);
            // System.out.println("Value counts: " + valueCounts);
            
            // Calc the probabilities
            Map<Integer, Double> probabilities = new HashMap<>();
            for (int value = 0; value < numValues; value++) {
                int count = valueCounts.getOrDefault(value, 0);
                // (count + alpha) / (classTotal + alpha * numValues)
                double probability = (count + smoothingAlpha) / (classTotal + smoothingAlpha * numValues);
                probabilities.put(value, probability);
            }
            
            // System.out.println("Probabilities for feature " + featureIdx + " class " + classLabel + ": " + probabilities);
            discreteProbabilities.get(classLabel).set(featureIdx, probabilities);
        }
    }

    // Processes a continuous feature by calculating mean, std dev, for gaussian dis. for each class.
    private void processContinuousFeature(Matrix X, Matrix y_gt, int featureIdx) {
        int totalExamples = X.getShape().getNumRows();
        
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
            
            continuousMeans.get(classLabel).set(featureIdx, mean);
            continuousStdDevs.get(classLabel).set(featureIdx, stdDev);
        }
    }

    // Helper method to calc. the probability density function for a gaussian dis. Essentially, within the naive bayes model, calc. the P(feature_value | class).
    private double gaussianPDF(double x, double mean, double stdDev) {
        double exponent = Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(stdDev, 2)));
        return (1.0 / (stdDev * Math.sqrt(2 * Math.PI))) * exponent;
    }

    // Predicts the class label for a single feature vector from the data produced from fit()
    public int predict(Matrix x)
    {
        // if (x == null) {
        //     System.err.println("predict() received null matrix");
        //     return 0;
        // }
        // if (classPriors.isEmpty()) {
        //     System.err.println("Model not trained yet, returning default (human)");
        //     return 0;
        // }
        // System.out.println("Predicting for feature vector...");

        // Use log probabilities to avoid underflow
        Map<Integer, Double> logProbabilities = new HashMap<>();
        
        // Init w/ log of prior probabilities
        for (int classLabel : classPriors.keySet()) {
            logProbabilities.put(classLabel, Math.log(classPriors.get(classLabel)));
        }
        
        // Add log probabilities for each feature
        for (int featureIdx = 0; featureIdx < featureHeader.size(); featureIdx++) {
            Pair<FeatureType, Integer> featureInfo = featureHeader.get(featureIdx);
            double featureValue = x.get(0, featureIdx); // x --> row vec
            
            for (int classLabel : classPriors.keySet()) {
                double featureProb;
                
                if (featureInfo.getFirst() == FeatureType.DISCRETE) {
                    int discreteValue = (int) featureValue;
                    
                    if (!discreteProbabilities.containsKey(classLabel) || 
                        discreteProbabilities.get(classLabel).size() <= featureIdx ||
                        discreteProbabilities.get(classLabel).get(featureIdx) == null) {
                        // System.err.println("Missing discrete probabilities for class " + classLabel + " feature " + featureIdx);
                        featureProb = 1e-6; // small default probability
                    } else {
                        Map<Integer, Double> probs = discreteProbabilities.get(classLabel).get(featureIdx);
                        featureProb = probs.getOrDefault(discreteValue, 1e-6);
                    }
                } else { // continuous
                    if (!continuousMeans.containsKey(classLabel) || 
                        continuousMeans.get(classLabel).size() <= featureIdx) {
                        // System.err.println("Missing continuous parameters for class " + classLabel + " feature " + featureIdx);
                        featureProb = 1e-6; // small default probability
                    } else {
                        double mean = continuousMeans.get(classLabel).get(featureIdx);
                        double stdDev = continuousStdDevs.get(classLabel).get(featureIdx);
                        featureProb = gaussianPDF(featureValue, mean, stdDev);
                    }
                }
                
                // Add log probability
                logProbabilities.put(classLabel, logProbabilities.get(classLabel) + Math.log(featureProb));
            }
        }

        // Return class with highest probability, w/ padding to classifiying humans (make it easier to classify humans)
        // get(1) --> HUMAN PROB
        // get(0) --> ZOMBIE PROB
        double humanBias = 0.0;
        return (logProbabilities.get(1) + humanBias) > logProbabilities.get(0) ? 1 : 0;
    }

}