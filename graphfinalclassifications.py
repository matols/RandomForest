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

    predictionsFile = args[0]  # The file containing the predictions of all proteins.
    nonredundantProteinDataset = args[1]  # The file containing the dataset of non-redundant proteins.
    redundantProteinDataset = args[2]  # The file containing the dataset of redundant proteins.
    outputDirectory = args[3]  # The location of the directory where the results should be written.
    if os.path.exists(outputDirectory):
        shutil.rmtree(outputDirectory)
    os.mkdir(outputDirectory)
    numberOfBins = 10 if len(args) < 5 else int(args[4])  # The number of bins to use in the histograms.

    # Parse the protein predictions.
    proteinData = parse_predictions(predictionsFile)

    # Determine the non-redundant and redundant proteins accessions.
    nonredundantAccessions = determine_proteins_in_dataset(nonredundantProteinDataset)
    redundantAccessions = determine_proteins_in_dataset(redundantProteinDataset)

    # Generate the predictions and graph for the non-redundant proteins.
    nonRedundantDirectory = outputDirectory + '/Non-Redundant'
    os.mkdir(nonRedundantDirectory)
    parse_input(proteinData, nonredundantAccessions, nonRedundantDirectory, numberOfBins)

    # Generate the predictions and graph for the redundant proteins.
    redundantDirectory = outputDirectory + '/Redundant'
    os.mkdir(redundantDirectory)
    parse_input(proteinData, redundantAccessions, redundantDirectory, numberOfBins)

    # Generate the predictions and graph for all proteins.
    allDirectory = outputDirectory + '/All'
    os.mkdir(allDirectory)
    parse_input(proteinData, proteinData.keys(), allDirectory, numberOfBins)

def determine_proteins_in_dataset(dataset):
    proteinAccessions = []
    readDataset = open(dataset, 'r')
    header = readDataset.readline().strip().split('\t')
    accessionIndex = header.index('UPAccession')
    for line in readDataset:
        acc = line.strip().split('\t')[accessionIndex]
        proteinAccessions.append(acc)
    readDataset.close()
    return proteinAccessions

def parse_predictions(predictionsFile):
    """
    """

    proteinData = {}  # A record of the protein prediction data indexed by the protein accession.
    readPredictions = open(predictionsFile, 'r')
    readPredictions.readline()  # Strip off the header.
    for line in readPredictions:
        chunks = (line.strip()).split('\t')
        acc = chunks[0]  # The protein's UniProt accession.
        posWeight = float(chunks[1])  # The weight of the positive prediction for the protein.
        negWeight = float(chunks[2])  # The weight of the negative prediction for the protein.
        originalClass = chunks[3]  # The class of the protein.
        proteinData[acc] = {'PosWeight' : posWeight, 'NegWeight' : negWeight, 'Class' : originalClass}
    readPredictions.close()

    return proteinData

def parse_input(proteinData, proteinAccessions, outputDirectory, numberOfBins=30):
    """
    """

    # Determine the unlabelled proteins that are predicted to be positive, along with the fraction of the prediction weight that is positive for each protein.
    unlabelledPredictedPositive = []  # The unlabelled proteins that have greater than 50% of their predicted weight as positive.
    unlabelledPosWeightFraction = []  # The fraction of the prediction weight that is positive for each unlabelled protein.
    positivePredictedUnlabelled = []  # The positive proteins that have greater than 50% of their predicted weight as unlabelled.
    positivePosWeightFraction = []  # The fraction of the prediction weight that is positive for each positive protein.
    for i in proteinAccessions:
        posWeightFraction = proteinData[i]['PosWeight'] / (proteinData[i]['PosWeight'] + proteinData[i]['NegWeight'])  # The fraction of a protein's predictive weight that is positive.
        if proteinData[i]['Class'] != 'Positive':
            # The protein is not positive.
            unlabelledPosWeightFraction.append(posWeightFraction)
            if posWeightFraction > 0.5:
                # The protein appears to be positive.
                unlabelledPredictedPositive.append([i, posWeightFraction])
        else:
            # The protein is positive.
            positivePosWeightFraction.append(posWeightFraction)
            if posWeightFraction <= 0.5:
                # The protein appears to be unlabelled.
                positivePredictedUnlabelled.append([i, posWeightFraction])

    # Write out the accession for all unlabelled proteins that appear to be positive.
    writePositives = open(outputDirectory + '/ApparentPositives.txt', 'w')
    writePositives.write('Accession\tPosFractionOfPredictiveWeight\n')
    for i in unlabelledPredictedPositive:
        writePositives.write(i[0] + '\t' + str(i[1]) + '\n')
    writePositives.close()

    # Write out the accession for all positive proteins that appear to be unlabelled.
    writeUnlabelleds = open(outputDirectory + '/ApparentUnlabelleds.txt', 'w')
    writeUnlabelleds.write('Accession\tPosFractionOfPredictiveWeight\n')
    for i in positivePredictedUnlabelled:
        writeUnlabelleds.write(i[0] + '\t' + str(i[1]) + '\n')
    writeUnlabelleds.close()

    # Create the confusion matrix.
    generate_confusion_matrix(len(positivePosWeightFraction) - len(positivePredictedUnlabelled), len(positivePredictedUnlabelled),
	    len(unlabelledPredictedPositive), len(unlabelledPosWeightFraction) - len(unlabelledPredictedPositive), outputDirectory + '/ConfusionMatrix')

    # Create the histogram for the predictions of all the proteins.
    generate_histogram(unlabelledPosWeightFraction, positivePosWeightFraction, outputDirectory + '/ProteinPredictions.png', numberOfBins)

def generate_confusion_matrix(truePositives, falseNegatives, falsePositives, trueNegatives, figureSaveLocation):
    """Create a confusion matrix image.

    @type figureSaveLocation - str
    @use  figureSaveLocation - The location where the figure will be saved.
    """
    
    # Define the axes sizes.
    axisMinValue = 0
    axisMaxValue = 8
    
    # Define the lines used for the confusion matrix.
    lineXCoords = [[axisMinValue, axisMaxValue], [axisMinValue, axisMaxValue],  # Lines at the top and bottom of the matrix.
                   [2, 2], [6, 6],                                              # Lines at the left and right of the matrix.
                   [axisMinValue + 1, axisMaxValue],                            # Line separating true classes.
                   [4, 4],                                                      # Line separating predicted classes.
                   [axisMinValue + 1, axisMinValue + 1],                        # Line under true class heading.
                   [2, 6]]                                                      # Line under predicted class heading.
    lineYCoords = [[6, 6], [2, 2],                                              # Lines at the left and right of the matrix.
                   [axisMinValue, axisMaxValue], [axisMinValue, axisMaxValue],  # Lines at the top and bottom of the matrix.
                   [4, 4],                                                      # Line separating true classes.
                   [axisMinValue, axisMaxValue - 1],                            # Line separating predicted classes.
                   [2, 6],                                                      # Line under true class heading.
                   [axisMaxValue - 1, axisMaxValue - 1]]                        # Line under predicted class heading.
    
    # Initialise the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05)
    axes = plt.subplot(gs[1:-1, 1:-1])
    axes.axis('equal')
    axes.axis('off')
    axes.set_xlim(left=axisMinValue, right=axisMaxValue)
    axes.set_ylim(bottom=axisMinValue, top=axisMaxValue)
    
    # Draw the lines for the confusion matrix.
    for x, y in zip(lineXCoords, lineYCoords):
        axes.plot(x, y, markersize=0, color='black', linestyle='-', linewidth=2)

    # Add the text.
    axes.text(axisMinValue + 0.5, 4, 'True Class', size=20, color='black', horizontalalignment='center', verticalalignment='center', rotation=90)
    axes.text(axisMinValue + 1.5, 3, 'Unlabelled', size=15, color='black', horizontalalignment='center', verticalalignment='center', rotation=90)
    axes.text(axisMinValue + 1.5, 5, 'Positive', size=15, color='black', horizontalalignment='center', verticalalignment='center', rotation=90)

    axes.text(4, axisMaxValue - 0.5, 'Predicted Class', size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(5, axisMaxValue - 1.5, 'Unlabelled', size=15, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(3, axisMaxValue - 1.5, 'Positive', size=15, color='black', horizontalalignment='center', verticalalignment='center')

    axes.text(3, 5, str(truePositives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(5, 5, str(falseNegatives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(3, 3, str(falsePositives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(5, 3, str(trueNegatives), size=20, color='black', horizontalalignment='center', verticalalignment='center')

    axes.text(3, axisMinValue + 1, str(truePositives + falsePositives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(5, axisMinValue + 1, str(trueNegatives + falseNegatives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(axisMaxValue - 1, 5, str(truePositives + falseNegatives), size=20, color='black', horizontalalignment='center', verticalalignment='center')
    axes.text(axisMaxValue - 1, 3, str(trueNegatives + falsePositives), size=20, color='black', horizontalalignment='center', verticalalignment='center')

    axes.text(axisMaxValue - 1, axisMinValue + 1, str(truePositives + falseNegatives + falsePositives + trueNegatives), size=20, color='black', horizontalalignment='center', verticalalignment='center')

    # Finalise the figure.
    axes.xaxis.set_visible(False)
    axes.yaxis.set_visible(False)

    # Save the figure.
    plt.savefig(figureSaveLocation, bbox_inches='tight', transparent=True)

def generate_histogram(unlabelledPosWeightFraction, positivePosWeightFraction, saveLocation, numberOfBins=10):
    """
    """

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    histPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlabel('Positive Similarity', fontsize=15)
    axes.set_ylabel('Frequency', fontsize=15)
    axes.set_xlim(left=0.0, right=1.0)

    # Plot the protein predictions.
    bins = [i / float(numberOfBins) for i in range(numberOfBins + 1)]
    axes.set_xticks(bins[1:])
    axes.set_yticks([0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0])
    binRelativeWidth = 0.9
    labelOffsetFromBinCentre = (0.5 * binRelativeWidth) * 0.5

    numberOfPositiveObservations = len(positivePosWeightFraction)
    positiveWeights = np.ones_like(positivePosWeightFraction) / numberOfPositiveObservations
    numberOfUnlabelledObservations = len(unlabelledPosWeightFraction)
    unlabelledWeights = np.ones_like(unlabelledPosWeightFraction) / numberOfUnlabelledObservations
    counts, bins, patches = axes.hist([unlabelledPosWeightFraction, positivePosWeightFraction], bins, color=['#808080', '#000000'], label=['Unlabelled', 'Positive'],
        align='mid', rwidth=binRelativeWidth, linewidth=0.0, weights=[unlabelledWeights, positiveWeights])

    maxUnlabelledCount = 0.0
    unlabelledCounts = counts[0]
    unlabelledBinCentres = (0.5 - labelOffsetFromBinCentre) * np.diff(bins) + bins[:-1]
    for count, x in zip(unlabelledCounts, unlabelledBinCentres):
        if count > maxUnlabelledCount:
            maxUnlabelledCount = count
        # Label the bar with the number of observations in the bin.
        numberOfObservations = int(round(count * numberOfUnlabelledObservations))
        if numberOfObservations:
            axes.annotate(str(numberOfObservations), xy=(x, count), xycoords=('data', 'data'), xytext=(0, 10), textcoords='offset points',
                verticalalignment='top', horizontalalignment='center', size=10, weight='bold')

    maxPositiveCount = 0.0
    positiveCounts = counts[1]
    positiveBinCentres = (0.5 + labelOffsetFromBinCentre) * np.diff(bins) + bins[:-1]
    for count, x in zip(positiveCounts, positiveBinCentres):
        if count > maxPositiveCount:
            maxPositiveCount = count
        # Label the bar with the number of observations in the bin.
        numberOfObservations = int(round(count * numberOfPositiveObservations))
        if numberOfObservations:
            axes.annotate(str(numberOfObservations), xy=(x, count), xycoords=('data', 'data'), xytext=(0, 10), textcoords='offset points',
                verticalalignment='top', horizontalalignment='center', size=10, weight='bold')

    # Create the legend.
    axes.legend(loc='upper center', bbox_to_anchor=(0.5, 1.1), fancybox=True, shadow=True, ncol=2)

    # Set the maximum y axis value by:
    #    Finding the height of the tallest bar, x
    #    Finding the smallest multiple of 0.05 greater than x.
    #    Adding 0.05 to the multiple factor.
    # For example, this will give 0.7 when x = 0.64 and 0.3 when x = 0.21.
    axes.set_ylim(bottom=0.0, top=(math.ceil(max(maxUnlabelledCount, maxPositiveCount) / 0.05) * 0.05) + 0.05)

    # Add the 0 at the intersection of the x and y axes.
    axes.annotate('0', xy=(0, 0), xycoords='axes fraction', xytext=(-7, -4), textcoords='offset points',
        verticalalignment='top', horizontalalignment='center', size=13)

    # Save the figure.
    plt.savefig(saveLocation, bbox_inches='tight', transparent=True)

if __name__ == '__main__':
    main(sys.argv[1:])