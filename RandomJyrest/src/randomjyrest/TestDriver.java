package randomjyrest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class TestDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		int numberOfTrees = 1000;
		int mtry = 10;
		int numberOfProcesses = 1;
		boolean isCalculateOOB = true;
		Forest forest = new Forest();
		
		DateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date startTime = new Date();
	    String strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
		forest.main(args[0], numberOfTrees, mtry, Arrays.asList(new String[]{"UPAccession"}), new double[]{}, numberOfProcesses, isCalculateOOB);
		sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    startTime = new Date();
	    strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
	    
	    numberOfProcesses = 2;
	    
	    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    startTime = new Date();
	    strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
		forest.main(args[0], numberOfTrees, mtry, Arrays.asList(new String[]{"UPAccession"}), new double[]{}, numberOfProcesses, isCalculateOOB);
		sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    startTime = new Date();
	    strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
	    
	    numberOfProcesses = 4;
	    
	    sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    startTime = new Date();
	    strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
		forest.main(args[0], numberOfTrees, mtry, Arrays.asList(new String[]{"UPAccession"}), new double[]{}, numberOfProcesses, isCalculateOOB);
		sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    startTime = new Date();
	    strDate = sdfDate.format(startTime);
	    System.out.format("at %s.\n", strDate);
	}

}
