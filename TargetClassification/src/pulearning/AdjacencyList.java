package pulearning;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon
 * 
 * An adjacency list representation of a matrix.
 * 
 * The weights in the matrix are specified first by row and then by column. Therefore, the weight for M[i][j]
 * would be found as this.weights.get(i).get(j).
 */
public class AdjacencyList
{

	private final int numberOfRows;
	private final int numberOfColumns;
	private final Map<Integer, Map<Integer, Double>> weights = new HashMap<Integer, Map<Integer, Double>>();


	public AdjacencyList(AdjacencyList matrix)
	{
		this.numberOfRows = matrix.getNumberRows();
		this.numberOfColumns = matrix.getNumberColumns();
		Map<Integer, Map<Integer, Double>> matrixValues = matrix.getWeights();
		for (Integer i : matrixValues.keySet())
		{
			if (i >= this.numberOfRows)
			{
				System.out.format("ERROR : Index %d encountered as a row index in the weights, but only %d rows specified.", i, this.numberOfRows);
				System.exit(0);
			}
			this.weights.put(i, new HashMap<Integer, Double>());
			for (Integer j : matrixValues.get(i).keySet())
			{
				if (j >= this.numberOfColumns)
				{
					System.out.format("ERROR : Index %d encountered as a column index in the weights, but only %d columns specified.", j, this.numberOfColumns);
					System.exit(0);
				}
				this.weights.get(i).put(j, matrixValues.get(i).get(j));
			}
		}
	}

	public AdjacencyList(Integer rows, Integer columns, Map<Integer, Map<Integer, Double>> matrixValues)
	{
		this.numberOfRows = rows;
		this.numberOfColumns = columns;
		for (Integer i : matrixValues.keySet())
		{
			if (i >= rows)
			{
				System.out.format("ERROR : Index %d encountered as a row index in the weights, but only %d rows specified.", i, rows);
				System.exit(0);
			}
			this.weights.put(i, new HashMap<Integer, Double>());
			for (Integer j : matrixValues.get(i).keySet())
			{
				if (j >= columns)
				{
					System.out.format("ERROR : Index %d encountered as a column index in the weights, but only %d columns specified.", j, columns);
					System.exit(0);
				}
				this.weights.get(i).put(j, matrixValues.get(i).get(j));
			}
		}
	}


	public int getNumberRows()
	{
		return this.numberOfRows;
	}

	public int getNumberColumns()
	{
		return this.numberOfColumns;
	}

	public Map<Integer, Map<Integer, Double>> getWeights()
	{
		return this.weights;
	}


	public AdjacencyList add(AdjacencyList matrix)
	{
		int matrixRows = matrix.getNumberRows();
		int matrixColumns = matrix.getNumberColumns();
		if (this.numberOfColumns != matrixColumns || this.numberOfRows != matrixRows)
		{
			System.out.println("ERROR : The number of rows and colums msut be the same for both matrices.");
			System.exit(0);
		}
		Map<Integer, Map<Integer, Double>> matrixWeights = matrix.getWeights();

		Map<Integer, Map<Integer, Double>> newMatrixWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < this.numberOfRows; i++)
		{
			if (!this.weights.containsKey(i) && !matrixWeights.containsKey(i))
			{
				// Skip this row if neither adjacency list contains any values for it (equivalent to the whole row being 0s).
				continue;
			}
			newMatrixWeights.put(i, new HashMap<Integer, Double>());
			for (int j = 0; j < matrixColumns; j++)
			{
				if (!this.weights.containsKey(j) && !matrixWeights.containsKey(j))
				{
					// Skip this column if neither adjacency list contains any values for it (equivalent to the whole column being 0s).
					continue;
				}
				double thisMatrixEntryIJ = 0.0;
				double otherMatrixEntryIJ = 0.0;
				try
				{
					thisMatrixEntryIJ = this.weights.get(i).get(j);
				}
				catch (NullPointerException e)
				{
					;
				}
				try
				{
					otherMatrixEntryIJ = matrixWeights.get(i).get(j);
				}
				catch (NullPointerException e)
				{
					;
				}
				newMatrixWeights.get(i).put(j, thisMatrixEntryIJ + otherMatrixEntryIJ);
			}
		}

		return new AdjacencyList(this.numberOfRows, this.numberOfColumns, newMatrixWeights);
	}


	public AdjacencyList multiply(AdjacencyList matrix)
	{
		int matrixRows = matrix.getNumberRows();
		int matrixColumns = matrix.getNumberColumns();
		if (this.numberOfColumns != matrixRows)
		{
			System.out.println("ERROR : Cannot multiply by a matrix that does not have the same number of rows as I have columns.");
			System.exit(0);
		}
		Map<Integer, Map<Integer, Double>> matrixWeights = matrix.getWeights();

		Map<Integer, Map<Integer, Double>> newMatrixWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < this.numberOfRows; i++)
		{
			if (!this.weights.containsKey(i))
			{
				// Skip this row if it is not in the adjacency list (equivalent to the whole row being 0s).
				continue;
			}
			newMatrixWeights.put(i, new HashMap<Integer, Double>());
			for (int j = 0; j < matrixColumns; j++)
			{
				if (!matrixWeights.containsKey(j))
				{
					// Skip this column if it is not in the adjacency list (equivalent to the whole column being 0s).
					continue;
				}
				double entryIJ = 0.0;
				for (int k = 0; k < matrixRows; k++)
				{
					try
					{
						entryIJ += this.weights.get(i).get(k) * matrixWeights.get(k).get(j);
					}
					catch (NullPointerException e)
					{
						;
					}
				}
				newMatrixWeights.get(i).put(j, entryIJ);
			}
		}

		return new AdjacencyList(this.numberOfRows, matrixColumns, newMatrixWeights);
	}


	public AdjacencyList scale(double constant)
	{
		Map<Integer, Map<Integer, Double>> newMatrixWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (Integer i : this.weights.keySet())
		{
			if (!this.weights.containsKey(i))
			{
				// Skip this row if it is not in the adjacency list (equivalent to the whole row being 0s).
				continue;
			}
			newMatrixWeights.put(i, new HashMap<Integer, Double>());
			for (Integer j : this.weights.get(i).keySet())
			{
				newMatrixWeights.get(i).put(j, this.weights.get(i).get(j) * constant);
			}
		}

		return new AdjacencyList(this.numberOfRows, this.numberOfColumns, newMatrixWeights);
	}


	public AdjacencyList subtract(AdjacencyList matrix)
	{
		int matrixRows = matrix.getNumberRows();
		int matrixColumns = matrix.getNumberColumns();
		if (this.numberOfColumns != matrixColumns || this.numberOfRows != matrixRows)
		{
			System.out.println("ERROR : The number of rows and colums msut be the same for both matrices.");
			System.exit(0);
		}
		Map<Integer, Map<Integer, Double>> matrixWeights = matrix.getWeights();

		Map<Integer, Map<Integer, Double>> newMatrixWeights = new HashMap<Integer, Map<Integer, Double>>();
		for (int i = 0; i < this.numberOfRows; i++)
		{
			if (!this.weights.containsKey(i) && !matrixWeights.containsKey(i))
			{
				// Skip this row if neither adjacency list contains any values for it (equivalent to the whole row being 0s).
				continue;
			}
			newMatrixWeights.put(i, new HashMap<Integer, Double>());
			for (int j = 0; j < matrixColumns; j++)
			{
				if (!this.weights.containsKey(j) && !matrixWeights.containsKey(j))
				{
					// Skip this column if neither adjacency list contains any values for it (equivalent to the whole column being 0s).
					continue;
				}
				double thisMatrixEntryIJ = 0.0;
				double otherMatrixEntryIJ = 0.0;
				try
				{
					thisMatrixEntryIJ = this.weights.get(i).get(j);
				}
				catch (NullPointerException e)
				{
					;
				}
				try
				{
					otherMatrixEntryIJ = matrixWeights.get(i).get(j);
				}
				catch (NullPointerException e)
				{
					;
				}
				newMatrixWeights.get(i).put(j, thisMatrixEntryIJ - otherMatrixEntryIJ);
			}
		}

		return new AdjacencyList(this.numberOfRows, this.numberOfColumns, newMatrixWeights);
	}

}
