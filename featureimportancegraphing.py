import argparse
import math
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import numpy.ma
import os
import shutil
import sys

def main(args):
    """Graphs the collated feature data.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the feature collation graphing.')
    parser.add_argument('featureImportances', help='the location containing the collated featre importance data')
    parser.add_argument('output', help='the location to save the graph')
    args = parser.parse_args()

    # Parse the command line arguments.
    collatedFeatureImportancesFile = args.featureImportances  # The file containing the collated feature importance data.
    resultsLocation = args.output  # The location of the directory where the image and its information should be saved.
    if not os.path.exists(resultsLocation):
        os.mkdir(resultsLocation)

    collatedFeatureImportances = feature_information_extraction(collatedFeatureImportancesFile)

    # Generate the graph.
    graph_entire_dataset_features(collatedFeatureImportances, resultsLocation)


def graph_entire_dataset_features(collatedFeatureImportances, resultsLocation):
    """Graph the comparison of the feature importance measures.

    :parma collatedFeatureImportances: the record of the feature importances for each feature, and whether the feature is significant
    :type collatedFeatureImportances: dict
    :parma resultsLocation: the location where the graph and information about it will be saved
    :type resultsLocation: string

    """

    # Determine the features that will be plotted. Only features that have been tested for significnace are plotted.
    featureData = [[i, float(collatedFeatureImportances[i]['PValue']), collatedFeatureImportances[i]['Significant'],
                    float(collatedFeatureImportances[i]['VarImp']), float(collatedFeatureImportances[i]['GA'])]
                    for i in collatedFeatureImportances if collatedFeatureImportances[i]['PValue'] != '-']
    varImpFeatureData = sorted(featureData, key=lambda x : (x[1], x[3]))  # Sort the features by p value (smallest first), and then variable importance (smallest first).
    gaFeatureData = sorted(featureData, key=lambda x : (x[1], -x[4]))  # Sort the features by p value (smallest first), and then ga fraction (largest first).
    numberOfFeatures = len(featureData)

    # Determine the x and y values for the scatter plot. Each feature is given an integer x value according to its p value. A lower p value corresponds
	# to a lower rank. Each feature is gven two y values, one for the variable importance and one for the ga fraction. Using the variable importance as
	# an example, the y value (variable importance rank) is determined by giving the feature a rank (where 0 is the smallest rank and most importance
	# feature according to the variable importance) based on its variable importance. If the feature with the smallest p value (rank 0) has a variable
	# importance that is the 5th smallest (rank 4), then the point in the scatter plot for this feature for the variable importance will be at (0, 4).
    xValues = np.array([i for i in range(numberOfFeatures)])
    numberOfSignificantFeatures = sum([1 for i in featureData if i[2] == 'True'])
    variableImportanceRanks = np.array([float(i[3]) for i in varImpFeatureData])
    sortedVarImps = sorted(zip(variableImportanceRanks, range(numberOfFeatures)), key=lambda x : x[0])
    gaRanks = np.array([float(i[4]) for i in gaFeatureData])
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
    plt.savefig(resultsLocation + '/FeatureComparison', bbox_inches='tight', transparent=True)

    # Record the trend line slopes.
    writeSlopes = open(resultsLocation + '/FeatureComparison_Slopes.txt', 'w')
    writeSlopes.write('Variable Importance Slope = {0:f}'.format(varImpFit[0]) + '\n')
    writeSlopes.write('GA Fraction Slope = {0:f}'.format(gaFit[0]))
    writeSlopes.close()


def feature_information_extraction(inputFile):
    """Extracts the feature importance measures for each feature.

    :parma inputFile: the location of the file containing hte collated feature inportance data
    :type inputFile: string

    """

    # Initialise the dictionary that will hold the feature importance data.
    featureDict = {}

    # Define the indices of the columns in the input files that are of interest.
    pValueColumn = 1
    correctedSignificanceColumn = 4
    meanVariableImportanceColumn = 5
    fractionOfGARunsColumn = 11

    # Extract the feature importance data.
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
    main(sys.argv)