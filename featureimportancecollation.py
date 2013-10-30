import argparse
import sys

def main(args):
    """Collects the results of the three feature importance measures in one file.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the feature collation.')
    parser.add_argument('statTest', help='the location containing the results of the statistical significance testing')
    parser.add_argument('varImp', help='the location containing the results of the variable importance calculations')
    parser.add_argument('output', help='the location to save the results')
    parser.add_argument('-g', '--ga', default=None, help='the location containing the results of the genetic algorithm runs')
    args = parser.parse_args()

    # Parse the command line arguments.
    statisticalTestFile = args.statTest  # The file containing the results of the statistical testing.
    variableImportanceFile = args.varImp  # The file containing the results of the random forest variable importance calculations.
    gaAnalysisFile = args.ga  # The file containing the results of the repeated genetic algorithm runs.
    resultsLocation = args.output  # The location of the file where the collated feature importance measures should be written.

    featureDict = {}  # A dictionary containing the feature importanc measures for each feature.

    # Parse the results of the statistical testing.
    readStats = open(statisticalTestFile, 'r')
    header = readStats.readline()
    for line in readStats:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        featureDict[feature] = feature + '\t' + '{0:.10f}'.format(float(chunks[10])) + '\t' + chunks[11] + '\t' + chunks[12] + '\t' + chunks[13] + '\t'
    readStats.close()

    # Parse the results of the variable importance calculations.
    readImp = open(variableImportanceFile, 'r')
    varImpFeaturesTested = (readImp.readline()).strip().split('\t')
    variableImpLevels = dict([(i, []) for i in varImpFeaturesTested])  # Dictionary to hold the variable immportance levels for each feature.
    for line in readImp:
        chunks = (line.strip()).split('\t')
        for i in range(len(chunks)):
            variableImpLevels[varImpFeaturesTested[i]].append(chunks[i])
    readImp.close()

    # Update featureDict to contain any features that were not tested for statistical significance, but do have a variable importance.
    featureDict = dict([(i, featureDict[i] if i in featureDict else i + '\t-\t-\t-\t-\t') for i in varImpFeaturesTested])

    # Update featureDict with the variable importances.
    for i in variableImpLevels:
        meanLevel, medianLevel, maxLevel, minLevel, stdDevLevel, rangeLevel = calculate_level_stats(variableImpLevels[i])
        featureDict[i] += str(meanLevel) + '\t' + str(medianLevel) + '\t' + str(maxLevel) + '\t' + str(minLevel) + '\t' + str(stdDevLevel) + '\t' + str(rangeLevel) + '\t'

    # Parse the genetic algorithm run results. The genetic algorithm and variable improtance claculations are assumed to have been done with the same
    # set of features.
    if gaAnalysisFile:
        readGA = open(gaAnalysisFile, 'r')
        for line in readGA:
            line = line.strip()
            if not line:
                # If the line is blank, then skip it.
                continue
            chunks = line.split('\t')
            feature = chunks[0]
            if feature in ['Fitness', 'Seed']:
                # If the first entry on the line is Fitness or Seed, then the line does not contain information about a feature.
                continue
            featureDict[feature] += chunks[-1] + '\n'
        readGA.close()
    else:
        for feature in featureDict:
            featureDict[feature] += 'X\n'

    # Write out the collated feature importance.
    writeTo = open(resultsLocation, 'w')
    writeTo.write('Feature\tPValue\tSignificantAt-0.05\tSignificantAt-0.01\tCorrectedSignificantAt-0.05\tMeanImpLevel\tMedianImpLevel\tMaxImpLevel\tMinImpLevel\tStdDevImpLevel\tRangeImpLevel\tFractionOfGARuns\n')
    for i in varImpFeaturesTested:
        writeTo.write(featureDict[i])
    writeTo.close()

def calculate_level_stats(levels):
    """Calculate the statistics of the variable importance levels for a given feature.

    :parma levels: the importance levels of a feature
    :type levels: list

    """

    levels = [int(i) for i in levels if i != '-']
    meanLevel = sum(levels) / len(levels)
    maxLevel = max(levels)
    minLevel = min(levels)
    rangeLevel = maxLevel - minLevel

    levels = sorted(levels)
    numberLevels = len(levels)

    medianLevel = 0
    if numberLevels % 2 == 0:
        midPointOne = numberLevels // 2
        midPointTwo = midPointOne - 1
        medianLevel = (levels[midPointOne] + levels[midPointTwo]) / 2
    else:
        medianLevel = levels[numberLevels // 2]

    stdDevLevel = 0
    for i in levels:
        stdDevLevel += (i - meanLevel) ** 2
    stdDevLevel /= numberLevels
    stdDevLevel = stdDevLevel ** 0.5

    return meanLevel, medianLevel, maxLevel, minLevel, stdDevLevel, rangeLevel

if __name__ == '__main__':
    main(sys.argv)