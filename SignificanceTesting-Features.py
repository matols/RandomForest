import sys

#from scipy.stats import mannwhitneyu
from scipy.stats import ttest_ind
import numpy

def main(args):
    """Uses a two sided unpaired T test with unequal variances to determine significance of features.
    """

    inputFile = args[0]  # The location of the dataset that the significances should be calcualted for.
    outputFile = args[1]  # The location of the file where the significances should be saved.

    numberOfFeatures = 0
    positiveData = {}
    numberOfPositiveObs = 0
    unlabelledData = {}
    numberOfUnlabelledObs = 0

    readIn = open(inputFile, 'r')
    featureNames = readIn.readline().strip()
    featureNames = featureNames.split('\t')
    for name in featureNames[:-1]:
        positiveData[name] = []
        unlabelledData[name] = []
    featureTypes = readIn.readline().strip()
    featureTypes = featureTypes.split('\t')
    featuresToIgnore = [featureNames[i] for i in range(len(featureNames)) if featureTypes[i] in ['x', 'r']]
    numberOfFeatures = len(featureNames[:-1]) - len(featuresToIgnore)
    categories = readIn.readline()
    for line in readIn:
        line = (line.strip()).split('\t')
        if line[-1] == 'Positive':
            numberOfPositiveObs += 1
            for i in range(len(featureNames)):
                if not featureNames[i] in featuresToIgnore:
                    positiveData[featureNames[i]].append(float(line[i]))
        else:
            numberOfUnlabelledObs += 1
            for i in range(len(featureNames)):
                if not featureNames[i] in featuresToIgnore:
                    unlabelledData[featureNames[i]].append(float(line[i]))
    readIn.close()

    writeTo = open(outputFile, 'w')
    writeTo.write('Feature\tPValue\tSigAt0.05\tSigAt0.01\tSigAtBonferroni0.05\tClassWithGreaterMean\tPositiveMean\tPositiveMedian\tPositiveVariance\tUnlabelledMean\tUnlabelledMedian\tUnlabelledVariance\n')
    for name in [i for i in featureNames if not i in featuresToIgnore]:
        writeTo.write(name + '\t')
        twoSidedPValue = ''
        significantAtFivePercent = False
        significantAtOnePercent = False
        significantAtBonferroniFivePercent = False
        positiveMean = numpy.mean(positiveData[name], dtype=numpy.float64)
        positiveMedian = numpy.median(positiveData[name])
        positiveVariance = numpy.var(positiveData[name], dtype=numpy.float64)
        unlabelledMean = numpy.mean(unlabelledData[name], dtype=numpy.float64)
        unlabelledMedian = numpy.median(unlabelledData[name])
        unlabelledVariance = numpy.var(unlabelledData[name], dtype=numpy.float64)
        greaterMean = 'Positive' if positiveMean > unlabelledMean else ('Equal' if positiveMean == unlabelledMean else 'Unlabelled')
        try:
            t, twoSidedPValue = ttest_ind(positiveData[name], unlabelledData[name], equal_var=False)
            significantAtFivePercent = True if twoSidedPValue <= 0.05 else False
            significantAtOnePercent = True if twoSidedPValue <= 0.01 else False
            significantAtBonferroniFivePercent = True if twoSidedPValue * numberOfFeatures <= 0.05 else False
        except:
            # The T test can not be performed (most likely due to every value for the feature being identical).
            pass
        writeTo.write(str(twoSidedPValue) + '\t' + str(significantAtFivePercent) + '\t' + str(significantAtOnePercent) + '\t' + str(significantAtBonferroniFivePercent) + '\t' + greaterMean + '\t' + str(positiveMean) + '\t' + str(positiveMedian) + '\t' + str(positiveVariance) + '\t' + str(unlabelledMean) + '\t' + str(unlabelledMedian) + '\t' + str(unlabelledVariance) + '\n')
    writeTo.close()


if __name__ == '__main__':
    main(sys.argv[1:])