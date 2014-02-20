import argparse
import sys

def main(args):
    """Collects the results of the two feature importance measures in one file.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the feature collation.')
    parser.add_argument('statTest', help='the location containing the results of the statistical significance testing')
    parser.add_argument('output', help='the location to save the results')
    parser.add_argument('-v', '--varImp', default=None, help='the location containing the results of the variable importance calculations')
    parser.add_argument('-p', '--pValCol', type=int, default=1, help='the column index of the record of the p value')
    parser.add_argument('-u', '--supCol', type=int, default=2, help='the column index of the record of the probability of superiority')
    parser.add_argument('-s', '--sigLevel', type=float, default=0.05, help='the significance level to use')
    parser.add_argument('-r', '--relationCol', type=int, default=-1, help='the column index of the record of the positive rank sum compared to expected')
    args = parser.parse_args()

    # Parse the command line arguments.
    statisticalTestFile = args.statTest  # The file containing the results of the statistical testing.
    variableImportanceFile = args.varImp  # The file containing the results of the random forest variable importance calculations.
    resultsLocation = args.output  # The location of the file where the collated feature importance measures should be written.
    pValueColumn = args.pValCol  # The index of the column in the statistical results file recording a feature's p value.
    relationColumn = args.relationCol  # The index of the column in the statistical results file recording the positive measure relative to the unlabelled (i.e. rank sums).
    superiorityColumn = args.supCol  # The index of the column in the statistical results file recording the probability of superiority.
    significanceLevel = args.sigLevel  # The level of significance to use.

    featureDict = {}  # A dictionary containing the feature importance measures for each feature.

    # Parse the results of the statistical testing.
    readStats = open(statisticalTestFile, 'r')
    header = readStats.readline()
    for line in readStats:
        chunks = (line.strip()).split(',')
        feature = chunks[0]
        if chunks[pValueColumn] != '-' and not '_' in feature and not feature in ['InstabilityIndex', 'HalfLife']:
            # Only add the feature if its p value could be calculated, it is not an expression level, it is not the instability index and it is not
            # the half life.
            featureDict[feature] = {}
            featureDict[feature]['PValue'] = float(chunks[pValueColumn])
            featureDict[feature]['Superiority'] = float(chunks[superiorityColumn])
            featureDict[feature]['Relative'] = chunks[relationColumn]
            featureDict[feature]['Importance'] = '-'
    readStats.close()

    # Determine the number of features tested for significance.
    numberOfFeaturesTested = len(featureDict)

    for i in featureDict:
        if featureDict[i]['PValue'] <= (significanceLevel / numberOfFeaturesTested):
            featureDict[i]['Significance'] = 'TRUE'
        else:
            featureDict[i]['Significance'] = 'FALSE'

    # Parse the results of the variable importance calculations.
    if variableImportanceFile:
        readImp = open(variableImportanceFile, 'r')
        varImpFeaturesTested = (readImp.readline()).strip().split('\t')
        variableImportances = dict([(i, []) for i in varImpFeaturesTested])  # Dictionary to hold the variable importances for each feature.
        for line in readImp:
            chunks = (line.strip()).split('\t')
            for i in range(len(chunks)):
                variableImportances[varImpFeaturesTested[i]].append(chunks[i])
        readImp.close()

        # Update featureDict to contain any features that were not tested for statistical significance, but do have a variable importance.
        for i in varImpFeaturesTested:
            if not i in featureDict:
                featureDict[i] = {}
                featureDict[i]['PValue'] = 1.0
                featureDict[i]['Relative'] = '-'
                featureDict[i]['Significance'] = '-'
                featureDict[i]['Superiority'] = '-'

        # Update featureDict with the variable importances.
        for i in variableImportances:
            meanImportance, maxImportance, minImportance, stdDevImportance, rangeImportance = calculate_level_stats(variableImportances[i])
            featureDict[i]['Importance'] = meanImportance

    # Write out the collated feature importance.
    writeTo = open(resultsLocation, 'w')
    writeTo.write('Feature\tPValue\tSignificant\tPositiveRankSumComparedToExpected\tMeanImportance\tSuperiority\n')
    for i in featureDict:
        writeTo.write(i + '\t' + str(featureDict[i]['PValue']) + '\t' + featureDict[i]['Significance'] + '\t' +
            featureDict[i]['Relative'] + '\t' + str(featureDict[i]['Importance']) + '\t' + str(featureDict[i]['Superiority']) + '\n')
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