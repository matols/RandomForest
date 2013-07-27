package randomjyrest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class PredictionGenerator implements Callable<Map<Integer, Map<String, Double>>>
{
	
	Map<String, double[]> datasetToPredict = new HashMap<String, double[]>();
	
	Set<Integer> obsToPredict;
	
	Tree treeToPredictFrom;
	
	public PredictionGenerator(Tree treeToPredictFrom, Map<String, double[]> datasetToPredict, Set<Integer> obsToPredict)
	{
		this.treeToPredictFrom = treeToPredictFrom;
		this.obsToPredict = obsToPredict;
		this.datasetToPredict = datasetToPredict;
	}

	public Map<Integer, Map<String, Double>> call()
	{
		return this.treeToPredictFrom.predict(this.datasetToPredict, this.obsToPredict);
	}

}
