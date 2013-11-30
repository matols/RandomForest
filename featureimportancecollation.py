import argparse
import sys

def main(args):
    """Collects the results of the two feature importance measures in one file.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the feature collation.')
    parser.add_argument('statTest', help='the location containing the results of the statistical significance testing')
    parser.add_argument('varImp', help='the location containing the results of the variable importance calculations')
    parser.add_argument('output', help='the location to save the results')
    parser.add_argument('-p', '--pValCol', type=int, default=1, help='the column index of the record of the p value')
    parser.add_argument('-s', '--sigCol', type=int, default=-1, help='the column index of the record of significance')
    args = parser.parse_args()

    # Parse the command line arguments.
    statisticalTestFile = args.statTest  # The file containing the results of the statistical testing.
    variableImportanceFile = args.varImp  # The file containing the results of the random forest variable importance calculations.
    resultsLocation = args.output  # The location of the file where the collated feature importance measures should be written.
    pValueColumn = args.pValCol  # The index of the column in the statistical results file recording a feature's p value.
    significanceColumn = args.sigCol  # The index of the column in the statistical results file recording whether a feature is significant.

    featureDict = {}  # A dictionary containing the feature importance measures for each feature.

    # Parse the results of the statistical testing.
    readStats = open(statisticalTestFile, 'r')
    header = readStats.readline()
    for line in readStats:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        featureDict[feature] = feature + '\t' + '{0:.10f}'.format(float(chunks[pValueColumn])) + '\t' + chunks[significanceColumn] + '\t'
    readStats.close()

    # Parse the results of the variable importance calculations.
    readImp = open(variableImportanceFile, 'r')
    varImpFeaturesTested = (readImp.readline()).strip().split('\t')
    variableImportances = dict([(i, []) for i in varImpFeaturesTested])  # Dictionary to hold the variable importances for each feature.
    for line in readImp:
        chunks = (line.strip()).split('\t')
        for i in range(len(chunks)):
            variableImportances[varImpFeaturesTested[i]].append(chunks[i])
    readImp.close()

    # Update featureDict to contain any features that were not tested for statistical significance, but do have a variable importance.
    featureDict = dict([(i, featureDict[i] if i in featureDict else i + '\t-\t-\t') for i in varImpFeaturesTested])

    # Update featureDict with the variable importances.
    for i in variableImportances:
        meanImportance, maxImportance, minImportance, stdDevImportance, rangeImportance = calculate_level_stats(variableImportances[i])
        featureDict[i] += str(meanImportance) + '\t' + str(maxImportance) + '\t' + str(minImportance) + '\t' + str(stdDevImportance) + '\t' + str(rangeImportance) + '\t'

    # Write out the collated feature importance.
    writeTo = open(resultsLocation, 'w')
    writeTo.write('Feature\tPValue\tSignificant\tMeanImportance\tMaxImportance\tMinImportance\tStdDevImportance\tRangeImportance\n')
    for i in varImpFeaturesTested:
        writeTo.write(featureDict[i] + '\n')
    writeTo.close()

def calculate_level_stats(importances):
    """Calculate the statistics of the variable importances for a given feature.

    :param importances: the importances of a feature
    :type importances: list

    """

    importances = sorted([float(i) for i in importances])
    importanceRepetitions = len(importances)
    meanImportance = sum(importances) / importanceRepetitions
    maxImportance = importances[-1]
    minImportance = importances[0]
    rangeImportance = maxImportance - minImportance

    stdDevImportance = 0
    for i in importances:
        stdDevImportance += (i - meanImportance) ** 2
    stdDevImportance /= importanceRepetitions
    stdDevImportance = stdDevImportance ** 0.5

    return meanImportance, maxImportance, minImportance, stdDevImportance, rangeImportance

if __name__ == '__main__':
    main(sys.argv)