import os
import shutil
import subprocess
import sys

def main(args):
	"""
	Creates 4 datasets and a list of protien accessions.
	The 4 datasets are:
		A dataset of the non-redundant known positives (those that are positive from DrugBank, TTD, UniProt) and non-redundant unlabelled proteins that are
			not too similar to a known positive.
		A dataset of proteins removed as redudant when calculating the non-redundant known positives and non-redundat unlabelleds.
		A dataset of the non-redudant positives, where the positives are all known positives and unlabelleds too similar to a known positive, and the
			non-redundat unlabelleds.
		A dataset of all the proteins removed as redundant when calculating the non-redunadnt all positives and non-redunadnt unlabelleds.
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

	# Determine the non-redundant proteins from the set of known poitive proteins
	knownPositiveFastaFile = outputDirectory + '/KnownPositiveFasta.fasta'
	knownPositiveDirectory = outputDirectory + '/KnownPositives'
	nonredundantKnownPositives = nonredundantPositives = generate_nonredundant_proteins(fastaDict, knownPositiveProteinAccessions, positiveFastaFile, positiveDirectory)

	# Determine the non-redundant proteins from the set of all poitive proteins.
	positiveFastaFile = outputDirectory + '/AllPositiveFasta.fasta'
	positiveDirectory = outputDirectory + '/AllPositives'
	nonredundantPositives = generate_nonredundant_proteins(fastaDict, allPositives, positiveFastaFile, positiveDirectory)

	# Determine the non-redundant unlabelled proteins.
	unlabelledFastaFile = outputDirectory + '/UnlabelledFasta.fasta'
	unlabelledDirectory = outputDirectory + '/Unlabelleds'
	nonredundantUnlabelleds = generate_nonredundant_proteins(fastaDict, [i for i in fastaDict if i not in allPositives], unlabelledFastaFile, unlabelledDirectory)

	# Create non-redundant dataset when using only the known positives.
	nonredundantDataset = outputDirectory + '/NonRedundantProteins-KnownPositives.txt'
	generate_dataset(datasetDict, nonredundantKnownPositives, nonredundantUnlabelleds, variableNames, variableTypes, varaiableCats, nonredundantDataset)

	# Create the redundant dataset when using only the known positives.
	redundantPositives = set(knownPositiveProteinAccessions) - set(nonredundantKnownPositives)
	redundantUnlabelleds = fastaDict.keys() - set(knownPositiveProteinAccessions) - set(nonredundantUnlabelleds)
	redundantDataset = outputDirectory + '/RedundantProteins-KnownPositives.txt'
	generate_dataset(datasetDict, redundantPositives, redundantUnlabelleds, variableNames, variableTypes, varaiableCats, redundantDataset)

	# Create non-redundant dataset when using all the positives.
	nonredundantDataset = outputDirectory + '/NonRedundantProteins-AllPositives.txt'
	generate_dataset(datasetDict, nonredundantPositives, nonredundantUnlabelleds, variableNames, variableTypes, varaiableCats, nonredundantDataset)

	# Create the redundant dataset when using all the positives.
	redundantPositives = set(allPositives) - set(nonredundantPositives)
	redundantUnlabelleds = fastaDict.keys() - set(allPositives) - set(nonredundantUnlabelleds)
	redundantDataset = outputDirectory + '/RedundantProteins-AllPositives.txt'
	generate_dataset(datasetDict, redundantPositives, redundantUnlabelleds, variableNames, variableTypes, varaiableCats, redundantDataset)

	# Record the unlabelled proteins switched to positives.
	unlabelledSwitchedToPositive = set(allPositives) - set(knownPositiveProteinAccessions)
	likelyPositives = outputDirectory + '/LikelyTargets.txt'
	writeLikely = open(likelyPositives, 'w')
	for i in unlabelledSwitchedToPositive:
		writeLikely.write(i + '\n')
	writeLikely.close()

	def generate_dataset(datasetDict, positivesToOutput, unlabelledsToOutput, variableNames, variableTypes, varaiableCats, outputLocation):
		writeDataset = open(outputLocation, 'w')
		writeDataset.write(variableNames)
		writeDataset.write(variableTypes)
		writeDataset.write(varaiableCats)
		for i in positivesToOutput:
			dataRow = datasetDict[i]
			dataRow[-1] = 'Positive'
			writeDataset.write('\t'.join(dataRow))
			writeDataset.write('\n')
		for i in unlabelledsToOutput:
			dataRow = datasetDict[i]
			dataRow[-1] = 'Unlabelled'
			writeDataset.write('\t'.join(dataRow))
			writeDataset.write('\n')
		writeDataset.close()

	def generate_nonredundant_proteins(fastaDict, inputProteins, fastaOutputLoc, leafOutputLoc):
		# Create the fasta file of the input proteins.
		writeFasta = open(fastaOutputLoc, 'w')
		for i in fastaDict:
			if i not in inputProteins:
				writeFasta.write(fastaDict[i])
		writeFasta.close()

		# Run Leaf on the input proteins.
		leafArgs = []
		leafArgs.append('python')
		leafArgs.append(leafLocation)
		leafArgs.append(fastaOutputLoc)
		leafArgs.append('-v')
		leafArgs.append('-o')
		leafArgs.append(leafOutputLoc)
		subprocess.call(leafArgs)

		# Calculate the non-redundant proteins.
		keptProteins = leafOutputLoc + '/KeptList.txt'
		nonredundantProteins = []
		readKept = open(keptProteins, 'r')
		readKept.readline()
		for line in readKept:
			nonredundantProteins.append((line.strip()).split('\t')[0])
		readKept.close()

		return nonredundantProteins


if __name__ == '__main__':
	main(sys.argv[1:])