package randomjyrest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class PredictionGenerator implements Callable<Map<Integer, Map<String, Double>>>
{
	
	Map<String, Map<String, double[]>> datasetToPredict = new HashMap<String, Map<String, double[]>>();
	
	Tree treeToPredictFrom;
	
	public PredictionGenerator(Map<String, Map<String, double[]>> datasetToPredict, Tree treeToPredictFrom)
	{
		this.datasetToPredict = datasetToPredict;
		this.treeToPredictFrom = treeToPredictFrom;
	}

	public Map<Integer, Map<String, Double>> call()
	{
		//TODO make the predictions.
		return this.treeToPredictFrom.predict(this.datasetToPredict);
	}

}
