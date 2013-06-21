import os
import shutil
import subprocess
import sys

def main(args):
	"""
	"""

	entireDataset = args[0]  # The dataset containing all redundant and non-redundant proteins
	fastaFile = args[1]  # The fasta format file of the proteins in the entire dataset.
	blastSimilarities = args[2]  # A file containing the similarities between the proteins in the entire dataset.
	leafLocation = args[3]  # The location of the script to run the Leaf algorithm.
	outputDirectory = args[4]  # The location of the directory where the results should be written.
	if os.path.exists(outputDirectory):
		shutil.rmtree(outputDirectory)
	os.mkdir(outputDirectory)

	# Parse the fasta file, and record the ID line and sequence for each protein accession.
	fastaDict = {}  # Indexed by UniProt accession. Records the string that should be put into a fasta file for each protein.
	readFasta = open(fastaFile, 'r')
	isFirstLine = True
	currentAccession = ''
	currentRecord = ''
	for line in readFasta:
		if line[0] == '>':
			if isFirstLine:
				isFirstLine = False
			else:
				fastaDict[currentAccession] = currentRecord
			currentAccession = (line.strip()).replace('>', '')
			currentRecord = line
		else:
			currentRecord += line
	fastaDict[currentAccession] = currentRecord  # Record the last record.
	readFasta.close()

	# Parse the entireDataset.
	datasetDict = {}  # Indexed by UniProt accession. The value recorded with the key is a list of the data in the row for the accession.
	knownPositiveProteinAccessions = []  # A record of the accessions of the proteins that are known drug targets.
	readDataset = open(entireDataset, 'r')
	variableNames = readDataset.readline()
	classColumnIndex = ((variableNames.strip()).split('\t')).index('Classification')
	accessionColumnIndex = ((variableNames.strip()).split('\t')).index('UPAccession')
	variableTypes = readDataset.readline()
	varaiableCats = readDataset.readline()
	for line in readDataset:
		chunks = (line.strip()).split('\t')
		datasetDict[chunks[accessionColumnIndex]] = chunks
		if chunks[classColumnIndex] == 'Positive':
			knownPositiveProteinAccessions.append(chunks[accessionColumnIndex])
	readDataset.close()

	# Parse the blast similarities and record all the proteins that are too similar to the known positives.
	allPositives = set(knownPositiveProteinAccessions)
	readBlast = open(blastSimilarities, 'r')
	for line in readBlast:
		chunks = (line.replace('\n', '')).split('\t')
		accession = chunks[0]
		similarProteins = chunks[1].split(',')
		if accession in knownPositiveProteinAccessions:
			allPositives |= set(similarProteins)
	readBlast.close()
	allPositives -= set([''])

	# Create the fasta file of positive proteins.
	positiveFastaFile = outputDirectory + '/PositiveFasta.fasta'
	writeFasta = open(positiveFastaFile, 'w')
	for i in fastaDict:
		if i in allPositives:
			writeFasta.write(fastaDict[i])
	writeFasta.close()

	# Run Leaf on the positive proteins.
	print('\nRunning Leaf on the positive proteins.')
	positiveDirectory = outputDirectory + '/Positives'
	leafArgs = []
	leafArgs.append('python')
	leafArgs.append(leafLocation)
	leafArgs.append(positiveFastaFile)
	leafArgs.append('-v')
	leafArgs.append('-o')
	leafArgs.append(positiveDirectory)
	subprocess.call(leafArgs)

	# Calculate the non-redundant positives.
	keptPositives = positiveDirectory + '/KeptList.txt'
	nonredundantPositives = []
	readKept = open(keptPositives, 'r')
	readKept.readline()
	for line in readKept:
		nonredundantPositives.append((line.strip()).split('\t')[0])
	readKept.close()

	# Create the fasta file of unlabelled proteins.
	unlabelledFastaFile = outputDirectory + '/UnlabelledFasta.fasta'
	writeFasta = open(unlabelledFastaFile, 'w')
	for i in fastaDict:
		if i not in allPositives:
			writeFasta.write(fastaDict[i])
	writeFasta.close()

	# Run Leaf on the unlabelled proteins.
	print('\nRunning Leaf on the unlabelled proteins.')
	unlabelledDirectory = outputDirectory + '/Unlabelleds'
	leafArgs = []
	leafArgs.append('python')
	leafArgs.append(leafLocation)
	leafArgs.append(unlabelledFastaFile)
	leafArgs.append('-v')
	leafArgs.append('-o')
	leafArgs.append(unlabelledDirectory)
	subprocess.call(leafArgs)

	# Calculate the non-redundant unlabelleds.
	keptUnlabelleds = unlabelledDirectory + '/KeptList.txt'
	nonredundantUnlabelleds = []
	readKept = open(keptUnlabelleds, 'r')
	readKept.readline()
	for line in readKept:
		nonredundantUnlabelleds.append((line.strip()).split('\t')[0])
	readKept.close()

	# Create non-redundant dataset.
	nonredundantDataset = outputDirectory + '/NonRedundantProteins.txt'
	writeDataset = open(nonredundantDataset, 'w')
	writeDataset.write(variableNames)
	writeDataset.write(variableTypes)
	writeDataset.write(varaiableCats)
	for i in nonredundantPositives:
		dataRow = datasetDict[i]
		dataRow[-1] = 'Positive'
		writeDataset.write('\t'.join(dataRow))
		writeDataset.write('\n')
	for i in nonredundantUnlabelleds:
		dataRow = datasetDict[i]
		dataRow[-1] = 'Unlabelled'
		writeDataset.write('\t'.join(dataRow))
		writeDataset.write('\n')
	writeDataset.close()

	# Create the redundant dataset.
	redundantPositives = set(allPositives) - set(nonredundantPositives)
	redundantUnlabelleds = fastaDict.keys() - set(allPositives) - set(nonredundantUnlabelleds)
	redundantDataset = outputDirectory + '/RedundantProteins.txt'
	writeDataset = open(redundantDataset, 'w')
	writeDataset.write(variableNames)
	writeDataset.write(variableTypes)
	writeDataset.write(varaiableCats)
	for i in redundantPositives:
		dataRow = datasetDict[i]
		dataRow[-1] = 'Positive'
		writeDataset.write('\t'.join(dataRow))
		writeDataset.write('\n')
	for i in redundantUnlabelleds:
		dataRow = datasetDict[i]
		dataRow[-1] = 'Unlabelled'
		writeDataset.write('\t'.join(dataRow))
		writeDataset.write('\n')
	writeDataset.close()

	# Record the unlabelled proteins switched to positives.
	unlabelledSwitchedToPositive = set(allPositives) - set(knownPositiveProteinAccessions)
	likelyPositives = outputDirectory + '/LikelyTargets.txt'
	writeLikely = open(likelyPositives, 'w')
	for i in unlabelledSwitchedToPositive:
		writeLikely.write(i + '\n')
	writeLikely.close()


if __name__ == '__main__':
	main(sys.argv[1:])