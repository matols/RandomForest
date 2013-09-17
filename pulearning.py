import argparse
import numpy
from numpy import linalg
import sys

def main(args):
    """Perform PU learning.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the PU learning.', epilog='Setting -b, --outfrac to a value x' +
        'will cause the top x fraction of the unlabelled proteins to be output as positive, and the others to be output as unlabelled')
    parser.add_argument('dataset', help='the location of the file containing the dataset')
    parser.add_argument('output', help='the output directory where the results should be saved')
    parser.add_argument('-f', '--features', default='', help='the features to remove (csv)')
    parser.add_argument('-a', '--possim', default=0.5, type=float, help='the fraction of positive similarities to remove (smaller similarities removed)')
    parser.add_argument('-b', '--outnum', default=0, type=int, help='the number of ulnabelled proteins to output as positive')
    parser.add_argument('-c', '--unlabsim', default=0.5, type=float, help='the fraction of unlabelled similarities to remove (smaller similarities removed)')
    args = parser.parse_args()

    # Parse the command line arguments.
    dataFileLocation = args.dataset
    resultsLocation = args.output
    featuresToRemove = args.features.split(',')
    fractionPositiveSimilarityToRemove = args.possim
    numberUnlabelledToConvert = args.outnum
    fractionUnlabelledSimilarityToRemove = args.unlabsim

    # Parse the file containing the dataset.
    proteinAccsAndClasses = numpy.genfromtxt(dataFileLocation, dtype=None, delimiter='\t', names=True, case_sensitive=True, usecols=[0,-1])
    unlabelledData = data_processing(dataFileLocation, featuresToRemove)
    unlabelledData = unlabelledData.view((numpy.float64, len(unlabelledData.dtype.names)))  # Convert the structured array created by genfromtxt to a regular array of floats.
    numberOfFeatures = unlabelledData.shape[1]

    # Standardise the data.
    means = numpy.mean(unlabelledData, 0)
    stdDevs = numpy.std(unlabelledData, 0)
    unlabelledData = (unlabelledData - means) / stdDevs

    # Determine the positive and unlabelled observations.
    positiveData = unlabelledData[proteinAccsAndClasses['Classification'] == b'Positive']
    unlabelledData = unlabelledData[proteinAccsAndClasses['Classification'] == b'Unlabelled']

    # Determine the pairwise distances (Euclidean) between all positive observations.
    positiveDistances = numpy.zeros((positiveData.shape[0], positiveData.shape[0]))
    sortedPositiveDistances = set([])
    for i in range(0, positiveDistances.shape[0]):
        for j in range(i + 1, positiveDistances.shape[1]):
            norm = numpy.linalg.norm(positiveData[i] - positiveData[j])
            sortedPositiveDistances.add((norm, i, j))
            positiveDistances[i, j] = norm
            positiveDistances[j, i] = norm
    sortedPositiveDistances = sorted(sortedPositiveDistances, key=lambda x : x[0])
    maxPositiveDistance = sortedPositiveDistances[-1][0]
    minPositiveDistance = sortedPositiveDistances[0][0]

    # Truncate the fractionPositiveSimilarityToRemove largest distances (this will cause them to be set to 0 after the scaling).
    if fractionPositiveSimilarityToRemove > 0:
        numberOfDistancesToRemove = int(len(sortedPositiveDistances) * fractionPositiveSimilarityToRemove)
        maxPositiveDistance = sortedPositiveDistances[-numberOfDistancesToRemove][0]
        for i in sortedPositiveDistances[-numberOfDistancesToRemove:]:
            indexI = i[1]
            indexJ = i[2]
            positiveDistances[indexI, indexJ] = maxPositiveDistance
            positiveDistances[indexJ, indexI] = maxPositiveDistance
    positiveDistances += (numpy.eye(positiveData.shape[0]) * maxPositiveDistance)  # Set the leading diagonal to have the maximum distance.

    # Inversely scale the positive distances so that larger distances get smaller similarities.
    positiveDistances = (maxPositiveDistance - positiveDistances) / (maxPositiveDistance - minPositiveDistance)

    # Determine the distance between each pair of observations (u, p) where u is an unlabelled protein and p a positive one.
    # The distance in cell unlabelledDistances[i, j] is the distance between positive observation i and unlabelled observation j.
    # Columns therefore represent unlabelled observations, and rows the positive ones.
    unlabelledDistances = numpy.zeros((positiveData.shape[0], unlabelledData.shape[0]))
    sortedUnlabelledDistances = set([])
    for i in range(unlabelledDistances.shape[0]):
        for j in range(unlabelledDistances.shape[1]):
            norm = numpy.linalg.norm(positiveData[i] - unlabelledData[j])
            sortedUnlabelledDistances.add((norm, i, j))
            unlabelledDistances[i, j] = norm
    sortedUnlabelledDistances = sorted(sortedUnlabelledDistances, key=lambda x : x[0])
    maxUnlabelledDistance = sortedUnlabelledDistances[-1][0]
    minUnlabelledDistance = sortedUnlabelledDistances[0][0]

    # Truncate the fractionUnlabelledSimilarityToRemove largest distances (this will cause them to be set to 0 after the scaling).
    if fractionUnlabelledSimilarityToRemove > 0:
        numberOfDistancesToRemove = int(len(sortedUnlabelledDistances) * fractionUnlabelledSimilarityToRemove)
        maxUnlabelledDistance = sortedUnlabelledDistances[-numberOfDistancesToRemove][0]
        for i in sortedUnlabelledDistances[-numberOfDistancesToRemove:]:
            indexI = i[1]
            indexJ = i[2]
            unlabelledDistances[indexI, indexJ] = maxUnlabelledDistance

    # Inversely scale the unlabelled distances so that larger distances get smaller similarities.
    unlabelledDistances = (maxUnlabelledDistance - unlabelledDistances) / (maxUnlabelledDistance - minUnlabelledDistance)

    # Determine the positive likeness each of the unlabelled observations.
    positiveWeightings = numpy.dot(positiveDistances, unlabelledDistances)
    positiveLikeness = numpy.sum(positiveWeightings, 0)

    # Scale the positive likenesses to be between 0 (least like a positive) to 1 (most like  positive).
    minLikeness = numpy.min(positiveLikeness)
    maxLikeness = numpy.max(positiveLikeness)
    positiveLikeness = (positiveLikeness - minLikeness) / (maxLikeness - minLikeness)

    # Output the parameters.
    fileParameters = resultsLocation + '/ParametersUsed.txt'
    writeParams = open(fileParameters, 'w')
    writeParams.write('Features Removed : ' + args.features + '\n')
    writeParams.write('Fraction of lagest positive dstances truncated = {0:0.2f}\n'.format(args.possim))
    writeParams.write('Fraction of lagest unlabelled dstances truncated = {0:0.2f}\n'.format(args.unlabsim))
    writeParams.close()

    # Output the unlabelled UniProt accession along with their scaled likeness in descnding order of likeness.
    fileUPAccessionLikeness = resultsLocation + '/UPAccessionLikeness.txt'
    writeLikeness = open(fileUPAccessionLikeness, 'w')
    orderedLikenesses = sorted(zip(positiveLikeness, proteinAccsAndClasses[proteinAccsAndClasses['Classification'] == b'Unlabelled']['UPAccession']), key=lambda x : x[0], reverse=True)
    for i,j in orderedLikenesses:
        # The UniProt accessions are recorded as binary strings, so need to convert each one to an ascii string.
        writeLikeness.write('{0}\t{1:0.2f}\n'.format(''.join([chr(k) for k in j]), i))
    writeLikeness.close()

    # Output the weight discount for all positive and unlabelled proteins.
    fileAllProteinWeightings = resultsLocation + '/ProteinWeightings.txt'
    writeWeightings = open(fileAllProteinWeightings, 'w')
    currentUnlabelledIndex = 0
    for i in proteinAccsAndClasses:
        proteinClass = i[1]
        if proteinClass == b'Positive':
            writeWeightings.write('1.00\n')
        else:
            writeWeightings.write('{0:0.2f}\n'.format(1 - positiveLikeness[currentUnlabelledIndex]))  # The discount is 1 - the likeness as you want a discount of 0 for the unlabelled that is most like a positive.
            currentUnlabelledIndex += 1
    writeWeightings.close()

    # Output the new dataset.
    proteinsToConvert = [''.join([chr(j) for j in i[1]]) for i in orderedLikenesses[:numberUnlabelledToConvert]]  # Get the accessions of the numberUnlabelledToConvert most positive-like unlabelled proteins.
    fileNewDataset = resultsLocation + '/Dataset_Converted_{0:d}.txt'.format(numberUnlabelledToConvert)
    writeDataset = open(fileNewDataset, 'w')
    readDataset = open(dataFileLocation, 'r')
    writeDataset.write(readDataset.readline())  # Record the header.
    for line in readDataset:
        chunks = (line.strip()).split('\t')
        if chunks[-1] == 'Positive':
            # If the protein is positive, then just output it.
            writeDataset.write(line)
        else:
            if chunks[0] in proteinsToConvert:
                # If the protein is unlabelled, and should be converted, then change its clsas to positive.
                chunks[-1] = 'Positive'
                writeDataset.write('\t'.join(chunks) + '\n')
            else:
                # If the protein is unlabelled, and not to be converted, then just output it.
                writeDataset.write(line)
    readDataset.close()
    writeDataset.close()

def data_processing(dataFileLocation, featuresToRemove=[]):
    """Process a data file.

    Processes the dataset file while ensuring that only the desired features in the dataset are in the processed dataset.

    The data file is expected to be tab separated with the first line containing the names of the features/columns.
    The restrictions on the naming of the features/columns are:
        1) The column containing the class should be headed with Classification.

    :param dataFileLocation: The location on disk of the dataset.
    :type dataFileLocation: string
    :param featuresToRemove: The features in the dataset that should be removed after the processing.
    :type featuresToRemove: list of strings
    :returns: The processed dataset with the desired feature set.
    :rtype: numpy structured array

    """

    data = numpy.genfromtxt(dataFileLocation, dtype='float64', delimiter='\t', names=True, case_sensitive=True)  # Parse the file containing the dataset.

    # Select which features (if any) should be removed.
    if featuresToRemove:
        data = data[[i for i in data.dtype.names if i not in featuresToRemove]]

    return data

if __name__ == '__main__':
    main(sys.argv)