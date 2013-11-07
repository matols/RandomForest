import argparse
from matplotlib import cm
from matplotlib import colors
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import sys

def main(args):
    """Make a heatmap of the dataset.

    """

    parser = argparse.ArgumentParser(description='Process the command line input for the heatmap generation.')
    parser.add_argument('dataset', help='the location of the file containing the dataset')
    parser.add_argument('output', help='the location to save the heatmap')
    parser.add_argument('-c', '--colorMap', default='binary', help='the color map to use')
    parser.add_argument('-f', '--features', default='UPAccession', help='the features to remove (csv)')
    args = parser.parse_args()

    # Parse the command line arguments.
    datasetFile = args.dataset  # The file containing the dataset.
    resultsLocation = args.output  # The location where the heatmap will be saved.
    colorMap = args.colorMap  # The colormap to use.
    featuresToRemove = args.features.split(',')  # The features to remove from the dataset.

    dataset = np.genfromtxt(datasetFile, dtype=None, delimiter='\t', names=True, case_sensitive=True)  # Parse the file containing the dataset.
    dataset = dataset[[i for i in dataset.dtype.names if i not in featuresToRemove]]

    # Determine the Positiveness of each value for each feature (Positiveness being the fraction of observations with the given value for the feaature that
    # are classed as Positive).
    positiveMapWeightings = np.empty((len(dataset.dtype.names) - 1, dataset.shape[0]))
    currentFeatureIndex = 0
    for i in [j for j in dataset.dtype.names if j != 'Classification']:
        # Go through each feature.
        featureData = list(dataset[i])
        sortedFeatureData = sorted(zip(featureData, [1 if j == b'Positive' else 0 for j in dataset['Classification']]))  # A list of tuples of (value, classValue) pairs sorted by values.
        featureValuePositiveClassValues = dict([(j, 0) for j in set(featureData)])  # The mapping from feature values to the weight of the positive class for that value.
        # Count the number of observations with the value for feature i == j[0] that are classed as Positive.
        for j in sortedFeatureData:
            featureValuePositiveClassValues[j[0]] += j[1]
        # Get the average Positiveness of value j for feature i.
        for j in featureValuePositiveClassValues:
            featureValuePositiveClassValues[j] = featureValuePositiveClassValues[j] / featureData.count(j)
        # Generate the list of sorted feature values and their Positiveness for this feature.
        positiveness = [featureValuePositiveClassValues[j[0]] for j in sortedFeatureData]
        positiveMapWeightings[currentFeatureIndex,:] = positiveness
        currentFeatureIndex += 1

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=0, right=positiveMapWeightings.shape[1])
    axes.set_ylim(bottom=0, top=positiveMapWeightings.shape[0])

    norm = colors.Normalize(vmin=0, vmax=1)  # Normalise the values for the heatmap to be between 0 and 1.
    heatmap = axes.pcolormesh(positiveMapWeightings, norm=norm, cmap=colorMap)  # Generate the heatmap.
    currentFigure.colorbar(heatmap)  # Add the colorbar.

    # Hide the x ticks.
    axes.set_xticks([])

    # Save the figure.
    plt.savefig(resultsLocation, bbox_inches='tight', transparent=True)

if __name__ == '__main__':
    main(sys.argv)