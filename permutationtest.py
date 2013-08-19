import argparse
import numpy
import os
import random
import sys

def main(args):
    """
    """

    parser = argparse.ArgumentParser(description='Process the command line input for the significance testing.')
    parser.add_argument('dataset', help='the location containing the dataset')
    parser.add_argument('output', help='the location to save the results')
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
    
    # Setup the results directory and record the parameters.
    statsFile = resultsLocation + '/StatisticalTesting.txt'
    parameterFile = resultsLocation + '/ParametersUsed.txt'
    if os.path.exists(resultsLocation):
        print('The results directory already exists.')
        sys.exit()
    os.mkdir(resultsLocation)
    writeParams = open(parameterFile, 'w')
    writeParams.write('Dataset Used - ' + args.dataset + '\n')
    writeParams.write('Features Removed - ' + args.features + '\n')
    writeParams.write('Permutations Used - ' + str(numberOfPermutations) + '\n')
    writeParams.write('Test Statistic - ' + args.statistic + '\n')
    writeParams.write('Alpha Levels - ' + ('0.05' if not args.alpha else ','.join(args.alpha)) + '\n')
    writeParams.write('Multiple Comparison Correction - ' + ('Performed With Alpha = ' + str(correctionAlpha)) if correctionAlpha else 'Not Performed' + '\n')
    writeParams.close()
    
    # Determine the original permutation and number of positive proteins.
    originalPositiveProteins = [i for i in range(len(dataset)) if dataset['Classification'][i] == b'Positive']
    originalUnlabeledProteins = [i for i in range(len(dataset)) if i not in originalPositiveProteins]
    numberOfPositiveProteins = len(originalPositiveProteins)
    
    # Determine the ranks for the observations in the dataset.
    indicesToRanks = {}
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        featureData = list(dataset[i])
        sortedFeatureData = sorted(zip(featureData, range(len(dataset))))
        ranks = {}
        featureValuesToIndices = {}
        for j in zip(sortedFeatureData, [j + 1 for j in range(len(sortedFeatureData))]):
            featureValue = j[0][0]
            featureIndex = j[0][1]
            if featureValue in ranks:
                ranks[featureValue] += j[1]
                featureValuesToIndices[featureValue].append(featureIndex)
            else:
                ranks[featureValue] = j[1]
                featureValuesToIndices[featureValue] = [featureIndex]

        indicesToRanks[i] = {}
        for key in ranks:
            averageRank = ranks[key] / featureData.count(key)
            for k in featureValuesToIndices[key]:
                indicesToRanks[i][k] = averageRank

    # Calculate statistics about the dataset.
    originalPositiveMeans, originalUnlabelledMeans = test_statistic_mean(dataset, originalPositiveProteins, originalUnlabeledProteins)
    originalPositiveMedians, originalUnlabelledMedians = test_statistic_median(dataset, originalPositiveProteins, originalUnlabeledProteins)
    originalPositiveRankSums, originalUnlabelledRankSums = test_statistic_ranksum(dataset, originalPositiveProteins, originalUnlabeledProteins, indicesToRanks)

    # Determine the original test statistic for each feature, along with its sign.
    originalPositiveStat, originalUnlabelledStat = testStatistic(dataset, originalPositiveProteins, originalUnlabeledProteins, indicesToRanks)
    originalStatistic = dict([(i, originalPositiveStat[i] - originalUnlabelledStat[i]) for i in originalPositiveStat])
    originalStatisticSigns = dict([(i, originalStatistic[i] < 0) for i in originalStatistic])

    # Determine the permutations to used.
    permutationIndices = [i for i in range(len(dataset))]
    permutationsToCheck = set([])
    print(str(len(permutationsToCheck)) + ' permutations created')
    permutationsToCheck.add(','.join([str(i) for i in originalPositiveProteins]))
    while len(permutationsToCheck) < (numberOfPermutations + 1):
        if len(permutationsToCheck)  % 10000 == 0:
            print(str(len(permutationsToCheck)) + ' permutations created')
        permutation = sorted(random.sample(permutationIndices, numberOfPositiveProteins))
        permutationAsStr = ','.join([str(i) for i in permutation])      
        permutationsToCheck.add(permutationAsStr)

    # Calculate the test statistic for each permutation.
    largerEffectPermutations = dict([(i, 0) for i in originalStatisticSigns])
    numberOfPermsChecked = 0
    for i in permutationsToCheck:
        if numberOfPermsChecked % 10000 == 0:
            print(str(numberOfPermsChecked) + ' permutations checked')
        numberOfPermsChecked += 1

        # Determine the protein indices.
        positiveProteinIndices = [int(j) for j in i.split(',')]
        unlabelledProteinIndices = [j for j in range(len(dataset)) if j not in positiveProteinIndices]

        positiveStat, unlabelledStat = testStatistic(dataset, positiveProteinIndices, unlabelledProteinIndices, indicesToRanks)
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

    writeResults = open(statsFile, 'w')
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

def test_statistic_mean(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass, dummyInput=None):
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

def test_statistic_median(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass, dummyInput=None):
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

def test_statistic_ranksum(dataset, indicesOfPositiveClass, indicesOfUnlabelledClass, indicesToRanks):
    """Returns the rank sum of each feature for the Positive and Unlabelled classes.
    """

    resultsPositive = {}
    resultsUnlabelled = {}
    for i in indicesToRanks:
        ranks = indicesToRanks[i]
        sumOfPositiveRanks = sum([ranks[j] for j in indicesOfPositiveClass])
        sumOfUnlabelledRanks = sum([ranks[j] for j in indicesOfUnlabelledClass])
        resultsPositive[i] = sumOfPositiveRanks
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