import sys

def main(args):
	resultsPCA = args[0]  # The scores from the RCA.
	datasetOriginal = args[1]  # The file used to generate the scores.
	datasetPCA = args[2]  # The location where the new dataset will be written out.

	classification = {}
	readOrig = open(datasetOriginal, 'r')
	readOrig.readline()
	readOrig.readline()
	readOrig.readline()
	observationIndex = 0
	for line in readOrig:
		line = line.strip()
		chunks = line.split('\t')
		classification[observationIndex] = chunks[-1]
		observationIndex += 1
	readOrig.close()

	readPCA = open(resultsPCA, 'r')
	writePCA = open(datasetPCA, 'w')
	header = (readPCA.readline()).strip()
	headerChunks = header.split(',')[1:]
	writePCA.write('\t'.join(headerChunks) + '\tClassification\n')
	writePCA.write('\t'.join(['n'] * len(headerChunks)) + '\tr\n')
	writePCA.write('\t'.join([''] * len(headerChunks)) + '\t\n')
	numberPCAs = len(header)
	for line in readPCA:
		line = line.strip()
		chunks = line.split(',')
		obsIndex = int(chunks[0]) - 1
		newLine = chunks[1:] + [classification[obsIndex]]
		writePCA.write(('\t'.join(newLine) + '\n'))
	readPCA.close()
	writePCA.close()

if __name__ == '__main__':
	main(sys.argv[1:])