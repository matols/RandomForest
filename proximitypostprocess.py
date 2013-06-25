import sys

def main(args):
	""" Procces the file containing the proximity data from the varable importance feature selection.
	
	The processing consists of making the leading diagonal all 1.0s instead of 0.0s, filling in the half of the matrix
	that is kept as 0.0s for space reasons while calculating the proximities and reordering the observations so that
	all the positive observations are together at the lowest indices.
	
	@type args[0] - str
	@use  args[0] - The location of the file containing the proximity information.
	@type args[1] - str
	@use  args[1] - The location of the file containing the dataset that the proximity information was calculated from.
	@type args[2] - str
	@use  args[2] - The location where the processed proximity information should be saved.
	"""

	# Parse the proximity information. Cut off the first element of each line as this contains an index not a proximity value.
	readIn = open(args[0], 'r')
	matrix = [((line.strip()).split('\t'))[1:] for line in readIn]
	readIn.close()

	# Parse the entire dataset information.
	readIn = open(args[1], 'r')
	dataFile = [(line.strip()).split('\t') for line in readIn]
	readIn.close()

	# Determine the indices of the positive and unlabelled observations.
	positives = []
	unlabelleds = []
	for i in range(len(dataFile[3:])):  # Ignore the first three lines as these are header lines.
		if dataFile[i+3][-1] == 'Positive':
			positives.append(i)
		else:
			unlabelleds.append(i)

	# Determine the mapping from old observations indices to new indices.
	newOrdering = positives + unlabelleds
	indexMapping = dict([(i[0], i[1]) for i in list(zip(range(len(matrix)), newOrdering))])

	# Determine the new similarity values, and write out the new proximity matrix.
	writeTo = open(args[2], 'w')
	for i in range(len(matrix)):
		# Loop through the rows of what will be the new proximity matrix.
		rowOutput = []
		oldRowIndex = indexMapping[i]  # The index of this row in the old proximity matrix.
		for j in range(len(matrix)):
			# Loop through the columns of the row.
			oldColumnIndex = indexMapping[j]  # The index of this column in the old proximity matrix.
			if i == j:
				# If the index of the row ad column are the same, then set the proximity to 1.0.
				rowOutput.append('1.0')
			elif oldRowIndex < oldColumnIndex:
				# If the index of the row in the old matrix was less than the index of the column (e.g. looking at entry (3,5)), then
				# we are loking at a cell in the old matrix that was above the leading diagonal. This means that the cell has a proximity
				# recorded for it, so you use that proximity.
				rowOutput.append(matrix[oldRowIndex][oldColumnIndex])
			else:
				# If the index of the row in the old matrix was less than the index of the column (e.g. looking at entry (5,3)), then
				# we are loking at a cell in the old matrix that was below the leading diagonal. This means that the cell did not have a proximity
				# recorded for it, so you use the symetric proximity from the cell above the leading diagonal.
				rowOutput.append(matrix[oldColumnIndex][oldRowIndex])
		writeTo.write('\t'.join(rowOutput) + '\n')
	writeTo.close()

if __name__ == '__main__':
	main(sys.argv[1:])