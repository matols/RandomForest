import os
import shutil
import subprocess
import sys

def main(args):
    """
    Creates 3 datasets and a list of protien accessions for each positive fraction used.
    The 3 datasets for each positive fraction are:
        1) A non-redundant dataset of known target proteins and unlabelled proteins that were not misclassified at the given positive fraction.
        2) A dataset of proteins deemed to be redundant when generating dataset 1 (with the unlabelled proteins that were misclassifed being treated as redundant).
        3) A non-redundant dataset of known target proteins, unlabelled proteins that were not misclassified at the given positive fraction and possible target proteins that are composed
           of the unlabelled proteins that were misclassified at the given positive fraction.
    """

    entireDataset = args[0]  # The dataset containing all redundant and non-redundant proteins.
    nonRedundantDataset = args[1]  # The dataset containing only the non-redundant proteins.
    fastaFile = args[2]  # The fasta format file of the proteins in the entire dataset.
    indicesToRemoveFromUnlabelled = args[3]  # The file containing the lists of indices that should be removed from the non-redunadnt unlabelled proteins.
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

    # Parse the entire dataset.
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

    # Parse the non-redundant dataset.
    indexToAcc = {}
    knownNonRedundantPositiveProteinAccessions = []  # A record of the accessions of the proteins that are non-redundant known drug targets.
    nonredundantUnlabelleds = []
    readDataset = open(nonRedundantDataset, 'r')
    variableNames = readDataset.readline()
    classColumnIndex = ((variableNames.strip()).split('\t')).index('Classification')
    accessionColumnIndex = ((variableNames.strip()).split('\t')).index('UPAccession')
    variableTypes = readDataset.readline()
    varaiableCats = readDataset.readline()
    lineIndex = 0
    for line in readDataset:
        chunks = (line.strip()).split('\t')
        accession = chunks[accessionColumnIndex]
        if chunks[classColumnIndex] == 'Positive':
            knownNonRedundantPositiveProteinAccessions.append(accession)
        else:
            nonredundantUnlabelleds.append(accession)
        indexToAcc[lineIndex] = accession
        lineIndex += 1
    readDataset.close()

    # Parse the file of proteins that are to be removed from the set of unlabelled proteins.
    poteinsToRemoveFromU = {}
    readRemoval = open(indicesToRemoveFromUnlabelled, 'r')
    header = readRemoval.readline()
    for line in readRemoval:
        chunks = (line.strip()).split('\t')
        posFracUsed = chunks[0]
        indicesToRemove = [] if len(chunks) == 1 else [int(i) for i in (chunks[1].strip()).split(',')]
        poteinsToRemoveFromU[posFracUsed] = indicesToRemove
    readRemoval.close()

    for i in poteinsToRemoveFromU:
        posFracOutputDir = outputDirectory + '/' + i
        if os.path.exists(posFracOutputDir):
            shutil.rmtree(posFracOutputDir)
        os.mkdir(posFracOutputDir)
        outputPUNonRedundant = posFracOutputDir + '/PU-NonRedundant.txt'
        outputPUPPNonRedundant = posFracOutputDir + '/PUPP-NonRedundant.txt'
        outputPURedundant = posFracOutputDir + '/Redundant.txt'

        # Determine the accessions of the proteins that are being removed from the set of unlabelled proteins.
        unlabelledSwitchedToPositive = []
        for j in poteinsToRemoveFromU[i]:
            unlabelledSwitchedToPositive.append(indexToAcc[j])

        possiblePositives = set(unlabelledSwitchedToPositive)
        definiteNonRedundantUnlabelled = set(nonredundantUnlabelleds) - possiblePositives
        redundantPositives = set(knownPositiveProteinAccessions) - set(knownNonRedundantPositiveProteinAccessions)
        redundantUnlabelleds = set(fastaDict.keys()) - set(knownPositiveProteinAccessions) - set(definiteNonRedundantUnlabelled)

        # Create non-redundant dataset when using only the known positives.
        generate_dataset(datasetDict, knownNonRedundantPositiveProteinAccessions, definiteNonRedundantUnlabelled, variableNames, variableTypes, varaiableCats, outputPUNonRedundant)

        # Create non-redundant dataset when using the possible positives.
        generate_dataset(datasetDict, knownNonRedundantPositiveProteinAccessions, definiteNonRedundantUnlabelled, variableNames, variableTypes, varaiableCats, outputPUPPNonRedundant, possiblePositives)

        # Create the redundant dataset.
        generate_dataset(datasetDict, redundantPositives, redundantUnlabelleds, variableNames, variableTypes, varaiableCats, outputPURedundant)

        # Record the unlabelled proteins switched to positives.
        outputLikelyPositives = posFracOutputDir + '/DatasetOfProteinsRemovedFromU.txt'
        generate_dataset(datasetDict, [], unlabelledSwitchedToPositive, variableNames, variableTypes, varaiableCats, outputLikelyPositives)
        likelyPositives = posFracOutputDir + '/UnlabelledProteinsTooSimilarToPositives.txt'
        writeLikely = open(likelyPositives, 'w')
        for i in unlabelledSwitchedToPositive:
            writeLikely.write(i + '\n')
        writeLikely.close()

def generate_dataset(datasetDict, positivesToOutput, unlabelledsToOutput, variableNames, variableTypes, varaiableCats, outputLocation, possiblePositiveToOutput=[]):
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
    for i in possiblePositiveToOutput:
        dataRow = datasetDict[i]
        dataRow[-1] = 'PossiblePositive'
        writeDataset.write('\t'.join(dataRow))
        writeDataset.write('\n')
    writeDataset.close()

def generate_nonredundant_proteins(fastaDict, inputProteins, fastaOutputLoc, leafLocation, leafOutputLoc):
    # Create the fasta file of the input proteins.
    writeFasta = open(fastaOutputLoc, 'w')
    for i in fastaDict:
        if i in inputProteins:
            writeFasta.write(fastaDict[i])
    writeFasta.close()

    # Run Leaf on the input proteins.
    leafArgs = []
    leafArgs.append('python')
    leafArgs.append(leafLocation)
    leafArgs.append(fastaOutputLoc)
#    leafArgs.append('-v')
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