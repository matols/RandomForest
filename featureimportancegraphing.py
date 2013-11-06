import argparse
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import sys

def main(args):
    """Graphs the collated feature data.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the feature collation graphing.')
    parser.add_argument('featureImportances', help='the location containing the collated featre importance data')
    parser.add_argument('output', help='the location to save the graph')
    args = parser.parse_args()

    # Parse the command line arguments.
    featureImportancesFile = args.featureImportances  # The file containing the collated feature importance data.
    resultsLocation = args.output  # The location where the graph will be saved.

    # Extract the variable importance and significance for each feature in the dataset.
    featureImportances = []
    readIn = open(featureImportancesFile, 'r')
    header = readIn.readline()
    for line in readIn:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        significance = chunks[4] == 'True'
        importance = float(chunks[5])
        featureImportances.append([feature, significance, importance])
    readIn.close()

    # Generate the coordinates for the points.
    xValuesNotSig = np.array([i + 1 for i in range(len(featureImportances)) if not featureImportances[i][1]])
    yValuesNotSig = np.array([i[2] for i in featureImportances if not i[1]])
    xValuesSig = np.array([i + 1 for i in range(len(featureImportances)) if featureImportances[i][1]])
    yValuesSig = np.array([i[2] for i in featureImportances if i[1]])

    # Determine the boundaries for the y axis.
    minImportance = min(min(yValuesNotSig), min(yValuesSig))
    minBoundary = 0
    if minImportance < 0:
        roundedMinImportance = round(minImportance, 2)
        minBoundary = roundedMinImportance if roundedMinImportance < minImportance else roundedMinImportance - 0.01
    maxImportance = max(max(yValuesNotSig), max(yValuesSig))
    roundedMaxImportance = round(maxImportance, 2)
    maxBoundary = roundedMaxImportance if roundedMaxImportance > maxImportance else roundedMaxImportance + 0.01

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=0, right=len(featureImportances) + 1)
    axes.set_ylim(bottom=minBoundary, top=maxBoundary)
    axes.set_ylabel("Variable Importance", fontsize=15)

    # Plot the points.
    axes.scatter(xValuesNotSig, yValuesNotSig, s=10, c='black', marker='o', edgecolor='none', zorder=2)
    axes.scatter(xValuesSig, yValuesSig, s=30, c='red', marker='*', edgecolor='none', zorder=2)

    # Save the figure.
    plt.savefig(resultsLocation, bbox_inches='tight', transparent=True)


if __name__ == '__main__':
    main(sys.argv)