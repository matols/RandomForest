package randomjyrest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class TreeGrower implements Callable<Tree>
{
	
	Map<String, Map<String, double[]>> dataset = new HashMap<String, Map<String, double[]>>();
	
	Random treeRNG;

	
	public TreeGrower(Map<String, Map<String, double[]>> entireDataset, List<Integer> inBagObservations, long seed)
	{
		//TODO generate the subset to be used to train the tree.
		treeRNG = new Random(seed);
	}

	public Tree call()
	{
		//TODO grow the tree.
		return new Tree();
	}
	
}
