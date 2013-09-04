import sys

def main(args):
    """
    """

    statsFile = args[0]
    variableImportanceFile = args[1]
    gaAnalysisFile = None
    resultsLocation = None
    if len(args) == 3:
        resultsLocation = args[2]
    elif len(args) == 4:
        gaAnalysisFile = args[2]
        resultsLocation = args[3]
    else:
        print('Incorrect number of arguments')
        sys.exit(0)

    featureDict = {}

    readStats = open(statsFile, 'r')
    header = readStats.readline()
    for line in readStats:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        featureDict[feature] = feature + '\t' + '{0:f}'.format(float(chunks[10])) + '\t' + chunks[11] + '\t' + chunks[12] + '\t' + chunks[13] + '\t'
    readStats.close()
    
    variableImpRanks = dict([(i, []) for i in featureDict])
    readImp = open(variableImportanceFile, 'r')
    features = (readImp.readline()).strip().split('\t')
    for line in readImp:
        chunks = (line.strip()).split('\t')
        for i in range(len(chunks)):
            try:
                variableImpRanks[features[i]].append(chunks[i])
            except KeyError:
                # Some features are not tested for sgnificance, but are in the variable importance.
                featureDict[features[i]] = features[i] + '\t-\t-\t-\t-\t'
                variableImpRanks[features[i]] = [chunks[i]]
    readImp.close()
    for i in variableImpRanks:
        meanRank, medianRank, maxRank, minRank, stdDevRank, rangeRank = calculate_rank_stats(variableImpRanks[i])
        featureDict[i] += str(meanRank) + '\t' + str(medianRank) + '\t' + str(maxRank) + '\t' + str(minRank) + '\t' + str(stdDevRank) + '\t' + str(rangeRank) + '\t'

    if gaAnalysisFile:
        readGA = open(gaAnalysisFile, 'r')
        for line in readGA:
            chunks = (line.strip()).split('\t')
            feature = chunks[0]
            try:
                featureDict[feature] += chunks[-1] + '\n'
            except KeyError:
                # Some features are not tested for sgnificance, but are in the genetic algorithm analysis.
                featureDict[feature] = chunks[-1] + '\n'
        readGA.close()
    else:
        for feature in featureDict:
            featureDict[feature] += 'X\n'

    writeTo = open(resultsLocation, 'w')
    writeTo.write('Feature\tPValue\tSignificantAt-0.05\tSignificantAt-0.01\tCorrectedSignificantAt-0.05\tMeanImpRank\tMedianImpRank\tMaxImpRank\tMinImpRank\tStdDevImpRank\tRangeImpRank\tFractionOfGARuns\n')
    for i in features:
        writeTo.write(featureDict[i])
    writeTo.close()

def calculate_rank_stats(ranks):
    """
    """

    ranks = [int(i) for i in ranks]
    meanRank = sum(ranks) / len(ranks)
    maxRank = max(ranks)
    minRank = min(ranks)
    rangeRank = maxRank - minRank
    
    ranks = sorted(ranks)
    numberRanks = len(ranks)
    
    medianRank = 0
    if numberRanks % 2 == 0:
        midPointOne = numberRanks // 2
        midPointTwo = midPointOne - 1
        medianRank = (ranks[midPointOne] + ranks[midPointTwo]) / 2
    else:
        medianRank = ranks[numberRanks // 2]

    stdDevRank = 0
    for i in ranks:
        stdDevRank += (i - meanRank) ** 2
    stdDevRank /= numberRanks
    stdDevRank = stdDevRank ** 0.5
    
    return meanRank, medianRank, maxRank, minRank, stdDevRank, rangeRank

if __name__ == '__main__':
    main(sys.argv[1:])