import argparse
import numpy
import random
import sys

def main(args):
    """
    """

    parser = argparse.ArgumentParser(description='Process the command line input for the significance testing.')
    parser.add_argument('dataset', help='the location containing the dataset')
    parser.add_argument('output', help='the location to save the test results')
    parser.add_argument('-f', '--features', default='', help='the features to remove (csv)')
    parser.add_argument('-N', '--permutations', default=1000, type=int, help='the number of permutations to perform')
    parser.add_argument('-t', '--statistic', default='mean', choices=['mean', 'median', 'ranksum'], help='the test statistic')
    parser.add_argument('-s', '--alpha', default=[], action='append', help='the significance level(s) to use')
    parser.add_argument('-c', '--correct', default=0.0, type=float, help='the alpha level for the multiple comparison correction')
    args = parser.parse_args()
    
    # Parse the command line arguments.
    dataset = data_processing(args.dataset, args.features.split(','))
    resultsLocation = args.output
    numberOfPermutations = args.permutations
    testStatistic = test_statistic_mean if args.statistic == 'mean' else (test_statistic_median if args.statistic == 'median' else test_statistic_ranksum)
    alphaLevels = [0.05] if not args.alpha else [float(i) for i in args.alpha]
    correctionAlpha = args.correct
    
    # Determine original permutation, number of positive proteins and statistics about the features.
    originalPositiveProteins = [i for i in range(len(dataset)) if dataset['Classification'][i] == b'Positive']
    originalUnlabeledProteins = [i for i in range(len(dataset)) if i not in originalPositiveProteins]
    numberOfPositiveProteins = len(originalPositiveProteins)
    permutationIndices = [i for i in range(len(dataset))]
    originalPositiveMeans, originalUnlabelledMeans = test_statistic_mean(dataset, originalPositiveProteins, originalUnlabeledProteins)
    originalPositiveMedians, originalUnlabelledMedians = test_statistic_median(dataset, originalPositiveProteins, originalUnlabeledProteins)
    originalPositiveRankSums, originalUnlabelledRankSums = test_statistic_ranksum(dataset, originalPositiveProteins, originalUnlabeledProteins)

    # Determine the original test statistic for each feature, along with its sign.
    originalPositiveStat, originalUnlabelledStat = testStatistic(dataset, originalPositiveProteins, originalUnlabeledProteins)
    originalStatistic = dict([(i, originalPositiveStat[i] - originalUnlabelledStat[i]) for i in originalPositiveStat])
    originalStatisticSigns = dict([(i, originalStatistic[i] < 0) for i in originalStatistic])

    largerEffectPermutations = dict([(i, 1) for i in originalStatisticSigns])
    permutationsChecked = set([])
    permutationsChecked.add(','.join([str(i) for i in originalPositiveProteins]))
    for i in range(numberOfPermutations):
        if len(permutationsChecked) % 1000 == 0:
            print(len(permutationsChecked))

        # Determine the next permutation to use.
        permutation = sorted(random.sample(permutationIndices, numberOfPositiveProteins))
        permutationAsStr = ','.join([str(i) for i in permutation])
        while permutationAsStr in permutationsChecked:
            permutation = sorted(random.sample(permutationIndices, numberOfPositiveProteins))
            permutationAsStr = ','.join([str(i) for i in permutation])
        permutationsChecked.add(permutationAsStr)

        # Determine the unlablled protein indices.
        unlabelledProteinIndices = [i for i in range(len(dataset)) if i not in permutation]

        positiveStat, unlabelledStat = testStatistic(dataset, permutation, unlabelledProteinIndices)
        permutationStatistic = dict([(i, positiveStat[i] - unlabelledStat[i]) for i in positiveStat])
        for j in permutationStatistic:
            if originalStatisticSigns[j]:
                largerEffectPermutations[j] += 1 if permutationStatistic[j] <= originalStatistic[j] else 0
            else:
                largerEffectPermutations[j] += 1 if permutationStatistic[j] >= originalStatistic[j] else 0

    pValues = dict([(i, largerEffectPermutations[i] / (numberOfPermutations + 1)) for i in largerEffectPermutations])

    correctedSignificance = {}
    if correctionAlpha:
        # Correct the p values using the Holm-Bonferroni correction.
        sortedPValues = sorted(pValues.items(), key=lambda x : x[1])
        numberOfFeatures = len(pValues)
        cutoffs = [(correctionAlpha / (numberOfFeatures + 1 - (i + 1))) for i in range(numberOfFeatures)]
        isNoneFailed = True
        for i in range(numberOfFeatures):
            feature = sortedPValues[i][0]
            if isNoneFailed and sortedPValues[i][1] <= cutoffs[i]:
                correctedSignificance[feature] = 'True'
            else:
                correctedSignificance[feature] = 'False'
                isNoneFailed = False

    writeResults = open(resultsLocation, 'w')
    writeResults.write('Feature\tPositiveMean\tPositiveMedian\tPositiveRankSum\tUnlabelledMean\tUnlabelledMedian\tUnlabelledRankSum\tPermutations\tOriginalStatistic\tStatsNoLessExtreme\tPValue')
    for i in alphaLevels:
        writeResults.write('\tSignificantAt-' + str(i))
    if correctionAlpha:
        writeResults.write('\tCorrectedSignificantAt-' + str(correctionAlpha))
    writeResults.write('\n')
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        writeResults.write(i + '\t')
        writeResults.write(str(originalPositiveMeans[i]) + '\t' + str(originalPositiveMedians[i]) + '\t' + str(originalPositiveRankSums[i]) + '\t')
        writeResults.write(str(originalUnlabelledMeans[i]) + '\t' + str(originalUnlabelledMedians[i]) + '\t' + str(originalUnlabelledRankSums[i]) + '\t')
        writeResults.write(str(numberOfPermutations + 1) + '\t' + str(originalStatistic[i]) + '\t' + str(largerEffectPermutations[i]) + '\t' + str(pValues[i]))
        for j in alphaLevels:
            writeResults.write('\t' + str(pValues[i] <= j))
        if correctionAlpha:
            writeResults.write('\t' + str(correctedSignificance[i]))
        writeResults.write('\n')
    writeResults.close()

def test_statistic_mean(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass):
    """Returns the inter-class difference in the mean of each feature.
    """

    resultsPositive = {}
    resultsUnlabelled = {}
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        positiveProteinValues = dataset[i][indicesOfPositiveClass]
        unlabelledProteinValues = dataset[i][indicesOfUnlabelledClass]
        resultsPositive[i] = numpy.mean(positiveProteinValues)
        resultsUnlabelled[i] = numpy.mean(unlabelledProteinValues)
    
    return resultsPositive, resultsUnlabelled

def test_statistic_median(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass):
    """Returns the inter-class difference in the median of each feature.
    """

    resultsPositive = {}
    resultsUnlabelled = {}
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        positiveProteinValues = dataset[i][indicesOfPositiveClass]
        unlabelledProteinValues = dataset[i][indicesOfUnlabelledClass]
        resultsPositive[i] = numpy.median(positiveProteinValues)
        resultsUnlabelled[i] = numpy.median(unlabelledProteinValues)
    
    return resultsPositive, resultsUnlabelled

def test_statistic_ranksum(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass):
    """Returns the rank sum of each feature for the Positive and Unlabelled classes.
    """

    resultsPositive = {}
    resultsUnlabelled = {}
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        allFeatureData = sorted(dataset[i])
        ranks = {}
        for j in zip(allFeatureData, [i + 1 for i in range(len(allFeatureData))]):
            if j[0] in ranks:
                ranks[j[0]] += j[1]
            else:
                ranks[j[0]] = j[1]
        for key in ranks:
            ranks[key] /= allFeatureData.count(key)
        positiveProteinValues = dataset[i][indicesOfPositiveClass]
        positiveRanks = [ranks[i] for i in positiveProteinValues]
        sumOfPositiveRanks = sum(positiveRanks)
        resultsPositive[i] = sumOfPositiveRanks
        unlabelledProteinValues = dataset[i][indicesOfUnlabelledClass]
        unlabelledRanks = [ranks[i] for i in unlabelledProteinValues]
        sumOfUnlabelledRanks = sum(unlabelledRanks)
        resultsUnlabelled[i] = sumOfUnlabelledRanks
    
    return resultsPositive, resultsUnlabelled

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

    data = numpy.genfromtxt(dataFileLocation, dtype=None, delimiter='\t', names=True, case_sensitive=True)  # Parse the file containing the dataset.

    # Select which features (if any) should be removed.
    if featuresToRemove:
        data = data[[i for i in data.dtype.names if i not in featuresToRemove]]

    return data

if __name__ == '__main__':
    main(sys.argv)