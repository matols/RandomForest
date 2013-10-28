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
    parser.add_argument('-s', '--slope', type=int, default=4, help='the number of significant figures to display for the trend line slope')
    args = parser.parse_args()

    # Parse the command line arguments.
    collatedFeatureImportancesFile = args.featureImportances  # The file containing the collated feature importance data.
    resultsLocation = args.output  # The location of the directory where the graphs will be saved.
    slopeSigFigures = args.slope  # The number of significant figures to display for the trend line slope.
    if not os.path.exists(resultsLocation):
        os.mkdir(resultsLocation)

    collatedFeatureImportances = feature_information_extraction(collatedFeatureImportancesFile)

    # Generate the graph.
    graph_entire_dataset_features(collatedFeatureImportances, slopeSigFigures, resultsLocation)


def graph_entire_dataset_features(collatedFeatureImportances, slopeSigFigures, resultsLocation):
    """Graph the comparison of the feature importance measures.

    :parma collatedFeatureImportances: the record of the feature importances for each feature, and whether the feature is significant
    :type collatedFeatureImportances: dict
    :parma resultsLocation: the directory location where the graphs will be saved
    :type resultsLocation: string

    """

    # Determine the features that will be plotted. Only features that have been tested for significnace are plotted.
    featureData = [[i, float(collatedFeatureImportances[i]['PValue']), collatedFeatureImportances[i]['Significant'],
                    float(collatedFeatureImportances[i]['VarImp']), float(collatedFeatureImportances[i]['GA'])]
                    for i in collatedFeatureImportances if collatedFeatureImportances[i]['PValue'] != '-']
    varImpAgainstPValData = sorted(featureData, key=lambda x : (x[1], x[3]))  # Sort the features by p value (smallest first), and then variable importance (smallest first).
    gaAgainstPValData = sorted(featureData, key=lambda x : (x[1], -x[4]))  # Sort the features by p value (smallest first), and then ga fraction (largest first).
    gaAgainstVarImpData = sorted(featureData, key=lambda x : (x[3], -x[4]))  # Sort the features by variable importance (smallest first), and then ga fraction (largest first).
    numberOfFeatures = len(featureData)  # The number of features in the dataset.

    # Set up the y values for the graph of variable importance against p value. The y value for a feature is the rank of the feature in terms of its
    # variable importance, while the x value will the be the rank of the feature's p value. If the feature with the smallest p value (rank 0) has a
    # variable importance that is the 5th smallest (rank 4), then the point in the scatter plot for this feature will be at (0, 4).
    varImpAgainstPValYValues = np.array([float(i[3]) for i in varImpAgainstPValData])
    varImpAgainstPValDataSorted = sorted(zip(varImpAgainstPValYValues, range(numberOfFeatures)), key=lambda x : x[0])

    # Set up the y values for the graph of ga fraction against p value. The y value for a feature is the rank of the feature in terms of its
    # ga fraction, while the x value will the be the rank of the feature's p value. If the feature with the smallest p value (rank 0) has a
    # ga fraction that is the 5th smallest (rank 4), then the point in the scatter plot for this feature will be at (0, 4).
    gaAgainstPValYValues = np.array([float(i[4]) for i in gaAgainstPValData])
    gaAgainstPValYDataSorted = sorted(zip(gaAgainstPValYValues, range(numberOfFeatures)), key=lambda x : x[0], reverse=True)  # Sorted in reverse order as a higher fraction is better.

    # Set up the y values for the graph of ga fraction against variable importance. The y value for a feature is the rank of the feature in terms of its
    # ga fraction, while the x value will the be the rank of the feature's variable importance. If the feature with the smallest variable importance (rank 0)
    # has a ga fraction that is the 5th smallest (rank 4), then the point in the scatter plot for this feature will be at (0, 4).
    gaAgainstVarImpYValues = np.array([float(i[4]) for i in gaAgainstVarImpData])
    gaAgainstVarImpYDataSorted = sorted(zip(gaAgainstVarImpYValues, range(numberOfFeatures)), key=lambda x : x[0], reverse=True)  # Sorted in reverse order as a higher fraction is better.

    # Setup the ranks for plotting (the true y values).
    for i in range(numberOfFeatures):
        varImpAgainstPValYValues[varImpAgainstPValDataSorted[i][1]] = i
        gaAgainstPValYValues[gaAgainstPValYDataSorted[i][1]] = i
        gaAgainstVarImpYValues[gaAgainstVarImpYDataSorted[i][1]] = i

    plot_figure(yValues=varImpAgainstPValYValues, numberOfFeatures=numberOfFeatures, xLabel='Feature P Value Rank', yLabel='Feature Variable Importance Rank',
        slopeSigFigures=slopeSigFigures, resultsLocation=resultsLocation + '/VariableImportanceAgainstPValue')
    plot_figure(yValues=gaAgainstPValYValues, numberOfFeatures=numberOfFeatures, xLabel='Feature P Value Rank', yLabel='Feature GA Fraction Rank',
        slopeSigFigures=slopeSigFigures, resultsLocation=resultsLocation + '/GAFractionAgainstPValue')
    plot_figure(yValues=gaAgainstVarImpYValues, numberOfFeatures=numberOfFeatures, xLabel='Feature Variable Importance Rank', yLabel='Feature GA Fraction Rank',
        slopeSigFigures=slopeSigFigures, resultsLocation=resultsLocation + '/GAFractionAgainstVariableImportance')


def plot_figure(yValues, numberOfFeatures, xLabel, yLabel, slopeSigFigures, resultsLocation):
    """Create a scatter plot of two feature importance measures against one another.

    :parma yValues: the y values of the points to plot
    :type yValues: numpy array
    :parma numberOfFeatures: the numbre of features in the dataset
    :type numberOfFeatures: int
    :parma xLabel: the label for the x axis
    :type xLabel: string
    :parma yLabel: the label for the y axis
    :type yLabel: string
    :parma slopeSigFigures: the number of significant figures to display for the trend line slope
    :type slopeSigFigures: int
    :parma resultsLocation: the location where the graph will be saved
    :type resultsLocation: string

    """

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=-1, right=numberOfFeatures)
    axes.set_ylim(bottom=-1, top=numberOfFeatures)
    axes.set_xlabel(xLabel, fontsize=15)
    axes.set_ylabel(yLabel, fontsize=15)

    # Plot the points.
    xValues = np.array([i for i in range(numberOfFeatures)])  # X values range from 0 to the number of features minus 1.
    axes.scatter(xValues, yValues, s=20, c='black', marker='o', edgecolor='none', zorder=2)

    # Calculate and plot a trend line.
    trendFit = np.polyfit(xValues, yValues, deg=1)
    trendFunction = np.poly1d(trendFit)
    trendSlope = trendFit[0]
    axes.plot(xValues, trendFunction(xValues), markersize=0, color='black', linestyle='--', linewidth=2, zorder=1,
        label='Slope of {0:.{1}f}'.format(round(trendSlope, slopeSigFigures), slopeSigFigures))

    # Create the legend.
    axes.legend(loc='upper center', bbox_to_anchor=(0.5, 1.1), fancybox=True, shadow=True, ncol=2)

    # Save the figure.
    plt.savefig(resultsLocation, bbox_inches='tight', transparent=True)


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