/**
 * 
 */
package featureselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.Forest;
import tree.ImmutableTwoValues;
import tree.ProcessDataForGrowing;
import tree.TreeGrowthControl;

/**
 * @author Simon Bull
 *
 */
public class TestDriver
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TreeGrowthControl ctrl = new TreeGrowthControl();
		ctrl.isReplacementUsed = true;
		ctrl.numberOfTreesToGrow = 500;
		ctrl.mtry = 10;
		ctrl.isStratifiedBootstrapUsed = true;
		int gaRepetitions = 20;
		boolean isXValUsed = false;
		boolean isCHCUsed = true;
		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 1.0);


//		ctrl.calculateOOB = false;
//		new Controller(args, ctrl, weights, true);
//		System.exit(0);


//		new Controller(args, ctrl, gaRepetitions, isXValUsed, weights, isCHCUsed);
//		System.exit(0);


//		Integer[] obsToUse = {66, 43, 40, 103, 4, 9, 74, 37, 39, 65, 41, 90, 94, 104, 71, 1, 63, 73, 31, 68, 101, 11, 61, 10, 14, 7, 95, 97, 92, 96, 91, 6, 64, 53, 51, 110, 52, 111, 86, 27, 56, 16, 49, 83, 58, 113, 114, 55, 77, 108, 48, 117, 22, 47, 87, 109, 105, 23, 80, 26, 21, 107, 81, 17, 32, 112, 106, 3, 70, 78, 84, 38, 29, 2, 46, 15, 62};
//		ctrl.trainingObservations = new ArrayList<Integer>(Arrays.asList(obsToUse));
		String training = "C:\\Users\\Simonial\\Documents\\PhD\\Datasets\\TestingIonChannelObsSelection\\TrainingDatasetOfDesiredComposition.txt";
		String testing = "C:\\Users\\Simonial\\Documents\\PhD\\Datasets\\TestingIonChannelObsSelection\\TestingDatasetOfDesiredComposition.txt";
//		Forest forest = new Forest(args[0], ctrl, weights);
		Forest forest = new Forest(training, ctrl, weights);
		System.out.println(forest.oobErrorEstimate);
		System.out.println(forest.oobConfusionMatrix);
		String posClass = "Positive";
		String negClass = "Unlabelled";
		Double oobTP = forest.oobConfusionMatrix.get(posClass).get("TruePositive");
		Double oobFP = forest.oobConfusionMatrix.get(posClass).get("FalsePositive");
		Double oobTN = forest.oobConfusionMatrix.get(negClass).get("TruePositive");
		Double oobFN = forest.oobConfusionMatrix.get(negClass).get("FalsePositive");
		Double posError = oobFN / (oobFN + oobTP);
		Double negError = oobFP / (oobFP + oobTN);
		Double MCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
		System.out.println(posError);
		System.out.println(negError);
		System.out.println(MCC);
		Map<String, Map<String, Double>> confMat = forest.predict(new ProcessDataForGrowing(testing, ctrl)).second;
		System.out.println("--------------");
		System.out.println(confMat);
		oobTP = confMat.get(posClass).get("TruePositive");
		oobFP = confMat.get(posClass).get("FalsePositive");
		oobTN = confMat.get(negClass).get("TruePositive");
		oobFN = confMat.get(negClass).get("FalsePositive");
		posError = oobFN / (oobFN + oobTP);
		negError = oobFP / (oobFP + oobTN);
		MCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
		System.out.println(posError);
		System.out.println(negError);
		System.out.println(MCC);
		Date startTime = new Date();
	    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    String strDate = sdfDate.format(startTime);
	    System.out.println(strDate);
	    System.exit(0);


//		Forest forest;
//		String inputCrossValLocation = args[2];
//		File inputCrossValDirectory = new File(inputCrossValLocation);
//		String subDirs[] = inputCrossValDirectory.list();
//		String crossValDirLoc = inputCrossValDirectory.getAbsolutePath();
//		List<List<Object>> crossValFiles = new ArrayList<List<Object>>();
//		for (String s : subDirs)
//		{
//			List<Object> trainTestLocs = new ArrayList<Object>();
//			trainTestLocs.add(crossValDirLoc + "/" + s + "/Train.txt");
//			trainTestLocs.add(new ProcessDataForGrowing(crossValDirLoc + "/" + s + "/Test.txt", ctrl));
//			crossValFiles.add(trainTestLocs);
//		}
//		Double averageError = 0.0;
//		Map<String, Map<String, Double>> averageConf = new HashMap<String, Map<String, Double>>();
//		Map<String, Double> emptyConf = new HashMap<String, Double>();
//		emptyConf.put("TruePositive", 0.0);
//		emptyConf.put("FalsePositive", 0.0);
//		averageConf.put("Unlabelled", new HashMap<String, Double>(emptyConf));
//		averageConf.put("Positive", new HashMap<String, Double>(emptyConf));
//		for (List<Object> l : crossValFiles)
//		{
//			forest = new Forest((String) l.get(0), ctrl, weights);
//			System.out.println(forest.oobErrorEstimate);
//			ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predRes = forest.predict((ProcessDataForGrowing) l.get(1));
//			averageError += predRes.first;
//			System.out.println(predRes.first);
//			for (String s : predRes.second.keySet())
//			{
//				for (String p : predRes.second.get(s).keySet())
//				{
//					averageConf.get(s).put(p, averageConf.get(s).get(p) + predRes.second.get(s).get(p));
//				}
//			}
//			System.out.println();
//		}
//		System.out.println(averageError / 5);
//		System.out.println(averageConf.entrySet());
//		String posClass = "Positive";
//		String negClass = "Unlabelled";
//		Double oobTP = averageConf.get(posClass).get("TruePositive");
//		Double oobFP = averageConf.get(posClass).get("FalsePositive");
//		Double oobTN = averageConf.get(negClass).get("TruePositive");
//		Double oobFN = averageConf.get(negClass).get("FalsePositive");
//		Double posError = oobFN / (oobFN + oobTP);
//		Double negError = oobFP / (oobFP + oobTN);
//		Double MCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
//		System.out.println(posError);
//		System.out.println(negError);
//		System.out.println(MCC);


//		ctrl.numberOfTreesToGrow = 500;
//		weights.put("Positive", 20.0);
//		Map<String, Integer> sampSize = new HashMap<String, Integer>();
//		sampSize.put("Unlabelled", 3000);
//		sampSize.put("Positive", 1000);
//		ctrl.sampSize = sampSize;
//		ctrl.isStratifiedBootstrapUsed = false;
//		ctrl.minNodeSize = 1;
//		Forest forest = new Forest(args[0], ctrl, weights);
//		Date startTime = new Date();
//	    DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	    String strDate = sdfDate.format(startTime);
//		System.out.println("\tDone - " + strDate);
//		System.out.println(weights.get("Positive"));
//		System.out.println(forest.oobErrorEstimate);
//		System.out.println(forest.oobConfusionMatrix);


//		Forest forest = new Forest(args[0], ctrl, weights);
//		forest.save("C:\\Users\\Simon\\Documents\\PhD\\FeatureSelection\\TreeSave");
//		Forest loadForest = new Forest("C:\\Users\\Simon\\Documents\\PhD\\FeatureSelection\\TreeSave", true);
//		boolean isSeedEqual = forest.seed == loadForest.seed;
//		boolean isOobEstEqual = forest.oobErrorEstimate == loadForest.oobErrorEstimate;
//		boolean isDataFileEqual = forest.dataFileGrownFrom.equals(loadForest.dataFileGrownFrom);
//		boolean isWeightsEqual = forest.weights.equals(loadForest.weights);
//		boolean isOobObsEqual = forest.oobObservations.equals(loadForest.oobObservations);
//		boolean isForestEqual = true;
//		for (int i = 0; i < forest.forest.size(); i++)
//		{
//			String forestDisplay = forest.forest.get(i).display();
//			String loadForestDisplay = loadForest.forest.get(i).display();
//			boolean isDisplayEqual = forestDisplay.equals(loadForestDisplay);
//			if (!isDisplayEqual)
//			{
//				isForestEqual = false;
//			}
//		}
//		System.out.println(isSeedEqual);
//		System.out.println(isOobEstEqual);
//		System.out.println(isDataFileEqual);
//		System.out.println(isWeightsEqual);
//		System.out.println(isOobObsEqual);
//		System.out.println(isForestEqual);
//		System.exit(0);
	}

}
