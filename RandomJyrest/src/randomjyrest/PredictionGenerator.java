package randomjyrest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class PredictionGenerator implements Callable<Map<Integer, Map<String, Double>>>
{
	
	Map<Integer, Map<String, Double>> datasetToPredict = new HashMap<Integer, Map<String, Double>>();
	
	Tree treeToPredictFrom;
	
	public PredictionGenerator(Tree treeToPredictFrom, Map<Integer, Map<String, Double>> datasetToPredict)
	{
		this.treeToPredictFrom = treeToPredictFrom;
		this.datasetToPredict = datasetToPredict;
	}

	public Map<Integer, Map<String, Double>> call()
	{
		return this.treeToPredictFrom.predict(this.datasetToPredict);
	}

}
