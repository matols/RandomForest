import os
import math
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import numpy.ma
import shutil
import sys

def main(args):
    """
    """

    nrDatasetFeatureCollation = args[0]  # The file containing the feature importance collation information for the non-redundant dataset.
    entireDatasetFeatureCollation = args[1]  # The file containing the feature importance collation informaation for the entire dataset.
    outputDirectory = args[2]  # The location of the directory where the images should be saved.
    if os.path.exists(outputDirectory):
        shutil.rmtree(outputDirectory)
    os.mkdir(outputDirectory)

    # For each dataset, determine the values of interest about each feature.
    nrDatasetFeatureData = feature_information_extraction(nrDatasetFeatureCollation)
    entireDatasetFeatureData = feature_information_extraction(entireDatasetFeatureCollation)

    # Generate the graphs.
    graph_entire_dataset_features(entireDatasetFeatureData, outputDirectory)
    graph_pvalue_feature_comparison(nrDatasetFeatureData, entireDatasetFeatureData, outputDirectory)
    graph_varimp_feature_comparison(nrDatasetFeatureData, entireDatasetFeatureData, outputDirectory)


def graph_entire_dataset_features(entireDatasetFeatureData, saveLocation):
    """
    """

    # Determine the features that will be plotted.
    featureData = [[i, entireDatasetFeatureData[i]['PValue'], entireDatasetFeatureData[i]['Significant'], entireDatasetFeatureData[i]['VarImp'],
                    entireDatasetFeatureData[i]['GA']] for i in entireDatasetFeatureData if entireDatasetFeatureData[i]['PValue'] != '-']
    featureData = sorted(featureData, key=lambda x : x[1])  # Sort the features by p value (smallest first).
    numberOfFeatures = len(featureData)

    # Determine the x and y values for the scatter plot.
    xValues = np.array([i for i in range(numberOfFeatures)])
    numberOfSignificantFeatures = sum([1 for i in featureData if i[2] == 'True'])
    variableImportanceRanks = np.array([float(i[3]) for i in featureData])
    sortedVarImps = sorted(zip(variableImportanceRanks, range(numberOfFeatures)), key=lambda x : x[0])
    gaRanks = np.array([float(i[4]) for i in featureData])
    sortedGAFractions = sorted(zip(gaRanks, range(numberOfFeatures)), key=lambda x : x[0], reverse=True)  # Sorted in reverse order as a higher fraction is better.
    for i in range(numberOfFeatures):
        variableImportanceRanks[sortedVarImps[i][1]] = i
        gaRanks[sortedGAFractions[i][1]] = i

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=-1, right=numberOfFeatures)
    axes.set_ylim(bottom=-1, top=numberOfFeatures)
    axes.set_xlabel('Feature P Value Rank', fontsize=15)
    axes.set_ylabel('Feature Importance Rank', fontsize=15)

    # Plot the points.
    axes.scatter(xValues, variableImportanceRanks, s=20, c='red', marker='o', edgecolor='none', zorder=2, label='RF Importances')
    axes.scatter(xValues, gaRanks, s=20, c='black', marker='o', edgecolor='none', zorder=2, label='GA Fractions')

    # Calcualte a trend line for the variable importances and the GA fractions.
    varImpFit = np.polyfit(xValues, variableImportanceRanks, deg=1)
    variableImportanceRankTrendFunction = np.poly1d(varImpFit)
    gaFit = np.polyfit(xValues, gaRanks, deg=1)
    gaRanksTrendFunction = np.poly1d(gaFit)

    # Plot the trend lines.
    axes.plot(xValues, variableImportanceRankTrendFunction(xValues), markersize=0, color='red', linestyle='--', linewidth=2, zorder=1)
    axes.plot(xValues, gaRanksTrendFunction(xValues), markersize=0, color='black', linestyle='--', linewidth=2, zorder=1)

    # Create the significant feature dividing line.
    axes.plot([numberOfSignificantFeatures + 0.5, numberOfSignificantFeatures + 0.5], [-1, numberOfFeatures], markersize=0, color='black', linestyle='--', linewidth=1, zorder=0)

    # Create the legend.
    axes.legend(loc='upper center', bbox_to_anchor=(0.5, 1.1), fancybox=True, shadow=True, ncol=2)

    # Save the figure.
    plt.savefig(saveLocation + '/FeatureComparison_EntireDataset', bbox_inches='tight', transparent=True)

    # Record the trend line slopes.
    writeSlopes = open(saveLocation + '/FeatureComparison_EntireDataset.txt', 'w')
    writeSlopes.write('Variable Importance Slope = {0:f}'.format(varImpFit[0]) + '\n')
    writeSlopes.write('GA Fraction Slope = {0:f}'.format(gaFit[0]))
    writeSlopes.close()


def graph_pvalue_feature_comparison(nrDatasetFeatureData, entireDatasetFeatureData, saveLocation):
    """
    """

    # Extract the p value data, and sort the features by the p values of the features in the entire dataset.
    entireDatasetFeatureData = [[i, entireDatasetFeatureData[i]['PValue'], entireDatasetFeatureData[i]['Significant']]
                                for i in entireDatasetFeatureData if entireDatasetFeatureData[i]['PValue'] != '-' and nrDatasetFeatureData[i]['PValue'] != '-']
    entireDatasetFeatureData = sorted(entireDatasetFeatureData, key=lambda x : x[1])  # Sort the features by p value (smallest first).
    entireDatasetFeatureData = [[entireDatasetFeatureData[i][0], i, entireDatasetFeatureData[i][2]] for i in range(len(entireDatasetFeatureData))]
    nrDatasetFeatureData = [[i[0], nrDatasetFeatureData[i[0]]['PValue'], nrDatasetFeatureData[i[0]]['Significant']] for i in entireDatasetFeatureData]
    numberOfFeatures = len(entireDatasetFeatureData)

    # Determine the x and y values for the scatter plot.
    xValues = np.array([i for i in range(numberOfFeatures)])
    entireDatasetPValueRanks = np.array([float(i[1]) for i in entireDatasetFeatureData])
    entireDatasetNumberOfSignificantFeatures = sum([1 for i in entireDatasetFeatureData if i[2] == 'True'])
    nrDatasetNumberOfSignificantFeatures = sum([1 for i in nrDatasetFeatureData if i[2] == 'True'])

    # Sort the non-redundant p values into ranks.
    nrDatasetPValueRanks = np.array([float(i[1]) for i in nrDatasetFeatureData])
    sortedNRPVals = sorted(zip(nrDatasetPValueRanks, range(numberOfFeatures)), key=lambda x : x[0])
    for i in range(numberOfFeatures):
        nrDatasetPValueRanks[sortedNRPVals[i][1]] = i
    

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=-1, right=numberOfFeatures)
    axes.set_ylim(bottom=-1, top=numberOfFeatures)
    axes.set_xlabel('Entire Dataset P Value Ranks', fontsize=15)
    axes.set_ylabel('Non-redundant Dataset P Value Ranks', fontsize=15)

    # Plot the points.
    axes.scatter(entireDatasetPValueRanks, nrDatasetPValueRanks, s=20, c='black', marker='o', edgecolor='none', zorder=2)

    # Calculate a trend line for the data.
    fit = np.polyfit(entireDatasetPValueRanks, nrDatasetPValueRanks, deg=1)
    trendFunction = np.poly1d(fit)

    # Plot the trend line.
    axes.plot([-1, numberOfFeatures], [trendFunction(-1), trendFunction(numberOfFeatures)], markersize=0, color='black', linestyle='-', linewidth=2, zorder=1)

    # Create the significant feature dividing lines.
    axes.plot([entireDatasetNumberOfSignificantFeatures + 0.5, entireDatasetNumberOfSignificantFeatures + 0.5], [-1, numberOfFeatures], markersize=0, color='black', linestyle='--', linewidth=1, zorder=0)
    axes.plot([-1, numberOfFeatures], [nrDatasetNumberOfSignificantFeatures + 0.5, nrDatasetNumberOfSignificantFeatures + 0.5], markersize=0, color='black', linestyle='--', linewidth=1, zorder=0)

    # Save the figure.
    plt.savefig(saveLocation + '/FeatureComparison_PValues', bbox_inches='tight', transparent=True)

    # Record the trend line slopes.
    writeSlope = open(saveLocation + '/FeatureComparison_PValues.txt', 'w')
    writeSlope.write('Slope of the trend line of the non-redundant dataset against the entire dataset = {0:f}'.format(fit[0]))
    writeSlope.close()


def graph_varimp_feature_comparison(nrDatasetFeatureData, entireDatasetFeatureData, saveLocation):
    """
    """

    # Extract the p value data, and sort the features by the p values of the features in the entire dataset.
    entireDatasetFeatureData = [[i, entireDatasetFeatureData[i]['VarImp'], entireDatasetFeatureData[i]['Significant']] for i in entireDatasetFeatureData]
    entireDatasetFeatureData = sorted(entireDatasetFeatureData, key=lambda x : x[1])  # Sort the features by p value (smallest first).
    entireDatasetFeatureData = [[entireDatasetFeatureData[i][0], i, entireDatasetFeatureData[i][2]] for i in range(len(entireDatasetFeatureData))]
    nrDatasetFeatureData = [[i[0], nrDatasetFeatureData[i[0]]['VarImp'], nrDatasetFeatureData[i[0]]['Significant']] for i in entireDatasetFeatureData]
    numberOfFeatures = len(entireDatasetFeatureData)

    # Determine the x and y values for the scatter plot.
    xValues = np.array([i for i in range(numberOfFeatures)])
    entireDatasetPValueRanks = np.array([float(i[1]) for i in entireDatasetFeatureData])
    entireDatasetNumberOfSignificantFeatures = sum([1 for i in entireDatasetFeatureData if i[2] == 'True'])
    nrDatasetNumberOfSignificantFeatures = sum([1 for i in nrDatasetFeatureData if i[2] == 'True'])

    # Sort the non-redundant p values into ranks.
    nrDatasetPValueRanks = np.array([float(i[1]) for i in nrDatasetFeatureData])
    sortedNRPVals = sorted(zip(nrDatasetPValueRanks, range(numberOfFeatures)), key=lambda x : x[0])
    for i in range(numberOfFeatures):
        nrDatasetPValueRanks[sortedNRPVals[i][1]] = i

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=-1, right=numberOfFeatures)
    axes.set_ylim(bottom=-1, top=numberOfFeatures)
    axes.set_xlabel('Entire Dataset RF Importance Ranks', fontsize=15)
    axes.set_ylabel('Non-redundant Dataset RF Importance Ranks', fontsize=15)

    # Plot the points.
    axes.scatter(entireDatasetPValueRanks, nrDatasetPValueRanks, s=20, c='black', marker='o', edgecolor='none', zorder=2)

    # Calculate a trend line for the data.
    fit = np.polyfit(entireDatasetPValueRanks, nrDatasetPValueRanks, deg=1)
    trendFunction = np.poly1d(fit)

    # Plot the trend line.
    axes.plot([-1, numberOfFeatures], [trendFunction(-1), trendFunction(numberOfFeatures)], markersize=0, color='black', linestyle='-', linewidth=2, zorder=1)

    # Create the significant feature dividing lines.
    axes.plot([entireDatasetNumberOfSignificantFeatures + 0.5, entireDatasetNumberOfSignificantFeatures + 0.5], [-1, numberOfFeatures], markersize=0, color='black', linestyle='--', linewidth=1, zorder=0)
    axes.plot([-1, numberOfFeatures], [nrDatasetNumberOfSignificantFeatures + 0.5, nrDatasetNumberOfSignificantFeatures + 0.5], markersize=0, color='black', linestyle='--', linewidth=1, zorder=0)

    # Save the figure.
    plt.savefig(saveLocation + '/FeatureComparison_VariableImportance', bbox_inches='tight', transparent=True)

    # Record the trend line slopes.
    writeSlope = open(saveLocation + '/FeatureComparison_VariableImportance.txt', 'w')
    writeSlope.write('Slope of the trend line of the non-redundant dataset against the entire dataset = {0:f}'.format(fit[0]))
    writeSlope.close()


def feature_information_extraction(inputFile):
    """
    """

    featureDict = {}

    # Define the indices of the columns in the input files that are of interest.
    pValueColumn = 1
    correctedSignificanceColumn = 4
    meanVariableImportanceColumn = 5
    fractionOfGARunsColumn = 11

    readIn = open(inputFile, 'r')
    header = readIn.readline()
    for line in readIn:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        featureDict[feature] = {}
        featureDict[feature]['PValue'] = chunks[pValueColumn]
        featureDict[feature]['Significant'] = chunks[correctedSignificanceColumn]
        featureDict[feature]['VarImp'] = chunks[meanVariableImportanceColumn]
        featureDict[feature]['GA'] = chunks[fractionOfGARunsColumn]
    readIn.close()

    return featureDict


if __name__ == '__main__':
    main(sys.argv[1:])