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

import tree.CARTTree;
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
		ctrl.minNodeSize = 1;
		Map<String, Double> weights = new HashMap<String, Double>();
		weights.put("Unlabelled", 1.0);
		weights.put("Positive", 3.0);



//		ctrl.isCalculateOOB = false;
//		new Controller(args, ctrl, weights);
//		System.exit(0);


//		new Controller(args, ctrl, gaRepetitions, weights);
//		System.exit(0);


//		Integer[] obsToUse = {126, 4990, 5463, 2982, 3024, 1008, 5258, 5124, 563, 381, 4404, 1711, 2881, 4926, 4264, 1878, 522, 3268, 1487, 3695, 5242, 4100, 4415, 3585, 1516, 5014, 1396, 476, 4642, 288, 1790, 930, 1545, 5601, 4947, 2617, 4349, 4408, 4697, 395, 3504, 952, 1074, 2559, 510, 4141, 1349, 4514, 1461, 3196, 1542, 2420, 243, 5496, 4418, 4283, 5602, 1571, 971, 5526, 1915, 4301, 1842, 3070, 2985, 2645, 1814, 488, 5490, 4242, 3744, 3288, 3005, 1591, 4180, 1952, 4793, 2511, 2749, 4564, 3055, 841, 323, 2911, 3710, 702, 4745, 1871, 223, 1570, 1231, 3389, 898, 4717, 4904, 4127, 3828, 4071, 5321, 4012, 3009, 449, 4107, 206, 3230, 4259, 4339, 3216, 1172, 4288, 4315, 2886, 5360, 1925, 264, 1706, 5462, 1150, 3446, 4222, 3314, 2441, 463, 4760, 2637, 5706, 228, 1229, 4560, 1547, 1189, 5192, 5660, 1685, 5063, 4938, 938, 5256, 4521, 4960, 3333, 2235, 1596, 3242, 4976, 533, 4285, 2897, 3709, 2826, 4778, 2730, 5337, 3802, 4138, 761, 4649, 10, 1209, 4815, 2613, 3238, 900, 2814, 3307, 5387, 2545, 4997, 852, 374, 1574, 4686, 3534, 4170, 3868, 4523, 4914, 151, 1486, 3665, 5430, 1187, 144, 5066, 5529, 5118, 3019, 5505, 660, 3972, 1253, 3158, 4609, 5185, 1398, 3940, 4610, 2026, 200, 5672, 571, 1658, 1016, 2306, 3591, 3193, 5724, 771, 3563, 2495, 2494, 3642, 1513, 2870, 294, 1171, 3360, 5180, 1353, 849, 4043, 4568, 100, 4669, 3131, 3769, 4196, 195, 5352, 4823, 538, 708, 4647, 4476, 2205, 5479, 4590, 2646, 4752, 3885, 1743, 1400, 2894, 3980, 3215, 1327, 3231, 4816, 4907, 5020, 5637, 989, 4375, 2682, 5079, 25, 800, 1035, 738, 2638, 984, 5403, 2691, 2066, 4119, 520, 5193, 5368, 2723, 3041, 4908, 1602, 4643, 4620, 4548, 5426, 1738, 3856, 3662, 4086, 3043, 5148, 1356, 3601, 1725, 1779, 1694, 2038, 2006, 5152, 3506, 2213, 1179, 3641, 795, 4757, 755, 1588, 3918, 4441, 1130, 5483, 217, 2893, 56, 4428, 991, 339, 2562, 2675, 3229, 2995, 553, 2758, 3756, 4486, 2986, 4819, 1386, 5582, 1484, 613, 4771, 5405, 3626, 5386, 833, 2137, 1141, 3339, 4983, 4385, 2276, 2699, 5136, 3787, 5701, 5427, 5718, 709, 1000, 1698, 4308, 5451, 1359, 4052, 4128, 4891, 4855, 1071, 1515, 3819, 786, 3521, 5106, 564, 4744, 2468, 2128, 2085, 4399, 1234, 3319, 468, 4337, 2565, 4728, 2952, 2185, 2805, 2916, 4440, 4004, 3571, 1124, 2697, 946, 1449, 1716, 3846, 310, 1242, 924, 1137, 4942, 949, 3851, 2647, 3379, 5147, 4537, 4457, 2266, 4305, 4994, 1881, 1093, 2770, 2949, 880, 4984, 799, 2930, 114, 5184, 5440, 1345, 2365, 2504, 1589, 5085, 5329, 4731, 2378, 3125, 3559, 3917, 452, 368, 4913, 421, 1511, 2396, 29, 5399, 5346, 54, 2373, 4153, 5350, 2837, 5088, 5445, 3098, 4549, 1184, 1920, 5104, 5303, 117, 3516, 950, 2744, 3066, 3110, 1512, 1903, 1673, 3541, 1876, 487, 1529, 548, 4220, 1792, 2592, 1856, 1622, 2555, 1504, 3818, 5622, 454, 607, 5396, 5092, 34, 5130, 5666, 3627, 2094, 462, 4169, 4871, 1684, 1998, 133, 3185, 4954, 102, 3343, 3711, 1734, 4668, 5209, 2788, 5632, 1060, 993, 4830, 4214, 1686, 1001, 3808, 4655, 4633, 3285, 4089, 3637, 3006, 1959, 3401, 2243, 3699, 4872, 2303, 698, 4132, 365, 1931, 2021, 4145, 2098, 975, 4536, 2182, 3327, 2368, 3378, 5041, 816, 619, 3761, 4147, 3475, 254, 4838, 2908, 1894, 5370, 3667, 3722, 3947, 647, 5722, 3498, 1305, 818, 2690, 2702, 2654, 4468, 3746, 3854, 5728, 2290, 3804, 2452, 168, 1544, 27, 3348, 4352, 4563, 3927, 104, 4110, 494, 5531, 2540, 2530, 3737, 333, 2197, 1384, 2348, 450, 2793, 4378, 2097, 3657, 4161, 1643, 1872, 5044, 2024, 4896, 524, 780, 2404, 5040, 5361, 5046, 1230, 5056, 5491, 5188, 1552, 3840, 4079, 5547, 308, 4615, 1705, 2067, 3853, 4426, 3264, 1567, 3226, 5412, 1346, 4726, 2537, 2171, 1443, 3130, 5659, 5344, 2449, 1278, 1583, 139, 1783, 3152, 4611, 4978, 3038, 737, 3677, 1733, 2819, 1693, 4289, 3417, 1993, 4445, 3151, 4377, 2934, 2231, 3458, 4135, 5158, 3247, 1107, 1813, 2228, 3050, 181, 960, 2347, 1953, 3058, 2591, 5265, 1258, 437, 806, 2890, 3878, 2161, 5083, 1269, 3042, 1997, 73, 2250, 2953, 2081, 1803, 2651, 2767, 1630, 2064, 1341, 469, 2810, 4944, 3297, 5627, 42, 1660, 4202, 2443, 5336, 828, 107, 3971, 3895, 4174, 5536, 3651, 1420, 499, 413, 5515, 2338, 96, 1212, 1059, 2278, 1869, 4025, 1073, 3355, 1247, 5028, 5155, 1563, 2083, 2207, 1160, 466, 3663, 307, 3141, 4955, 554, 4850, 2688, 2750, 2684, 1995, 1688, 1153, 4313, 5096, 4865, 2725, 2141, 1781, 2429, 1013, 1560, 2140, 5480, 5278, 3860, 4624, 4630, 646, 3994, 1789, 3489, 5143, 569, 5186, 5237, 1670, 1004, 2653, 5168, 1340, 242, 4861, 55, 3134, 4619, 4519, 5689, 3764, 5163, 5220, 3342, 4483, 3951, 3353, 5550, 4184, 5304, 1770, 1901, 5312, 5341, 5664, 1561, 1700, 794, 45, 3990, 5268, 1593, 344, 5011, 3848, 5001, 3897, 4885, 2453, 1760, 2639, 4290, 4594, 4109, 4706, 5492, 2153, 1355, 2191, 2398, 1972, 2586, 3597, 1888, 3296, 5382, 43, 2135, 2241, 1280};
//		ctrl.trainingObservations = new ArrayList<Integer>(Arrays.asList(obsToUse));
//		String testing;
//		double averagePredMCC = 0.0;
//		int reps = 10;
//		for (int i = 0; i < reps; i++)
//		{
//			Forest forest = new Forest(args[0], ctrl, (long) i);
//			forest.setWeightsByClass(weights);
//			forest.growForest();
//			Double oobTP = forest.oobConfusionMatrix.get("Positive").get("TruePositive");
//			Double oobFP = forest.oobConfusionMatrix.get("Positive").get("FalsePositive");
//			Double oobTN = forest.oobConfusionMatrix.get("Unlabelled").get("TruePositive");
//			Double oobFN = forest.oobConfusionMatrix.get("Unlabelled").get("FalsePositive");
//			Double MCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
//			System.out.println(forest.oobConfusionMatrix);
//			System.out.println(forest.oobErrorEstimate);
//			System.out.println(MCC);
//			ImmutableTwoValues<Double, Map<String, Map<String, Double>>> predRes = forest.predict(new ProcessDataForGrowing(testing, new TreeGrowthControl()));
//			Double predError = predRes.first;
//			Map<String, Map<String, Double>> confMat = predRes.second;
//			oobTP = confMat.get("Positive").get("TruePositive");
//			oobFP = confMat.get("Positive").get("FalsePositive");
//			oobTN = confMat.get("Unlabelled").get("TruePositive");
//			oobFN = confMat.get("Unlabelled").get("FalsePositive");
//			MCC = (((oobTP * oobTN)  - (oobFP * oobFN)) / Math.sqrt((oobTP + oobFP) * (oobTP + oobFN) * (oobTN + oobFP) * (oobTN + oobFN)));
//			System.out.println(confMat);
//			System.out.println(predError);
//			System.out.println(MCC);
//			averagePredMCC += MCC;
//			System.out.println("++++++++++++++++++++++++++++");
//		}
//		System.out.println(averagePredMCC / reps);
//	    System.exit(0);


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


//		Forest forest = new Forest(args[0], ctrl);
//		forest.setWeightsByClass(weights);
//		forest.growForest();
//		forest.save("C:\\Users\\Simonial\\Documents\\PhD\\FeatureSelection\\TreeSave");
//		Forest loadForest = new Forest("C:\\Users\\Simonial\\Documents\\PhD\\FeatureSelection\\TreeSave", true);
//		boolean isSeedEqual = forest.seed == loadForest.seed;
//		boolean isOobEstEqual = forest.oobErrorEstimate == loadForest.oobErrorEstimate;
//		boolean isDataFileEqual = forest.dataFileGrownFrom.equals(loadForest.dataFileGrownFrom);
//		boolean isWeightsEqual = forest.weights.equals(loadForest.weights);
//		boolean isOobObsEqual = forest.oobObservations.equals(loadForest.oobObservations);
//		boolean isTreeOobEqual = true;
//		for (Integer i : forest.oobOnTree.keySet())
//		{
//			if (!forest.oobOnTree.get(i).equals(loadForest.oobOnTree.get(i)))
//			{
//				isTreeOobEqual = false;
//			}
//		}
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
//		System.out.println(isTreeOobEqual);
//		System.out.println(isForestEqual);
//		System.exit(0);
	}

}
