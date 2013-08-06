import os
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
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

    ###################################################################
    ## Parameters
    numberOfBins = 30
    ###################################################################

    # Parse the protein predictions.
    proteinData = parse_predictions(predictionsFile)

    # Determine the non-redundat and redundant proteins accessions.
    nonredundantAccessions = determine_proteins_in_dataset(nonredundantProteinDataset)
    redundantAccessions = determine_proteins_in_dataset(redundantProteinDataset)

    # Generate the predictions and graph for the non-redudnant proteins.
    nonRedundantDirectory = outputDirectory + '/Non-Redundant'
    os.mkdir(nonRedundantDirectory)
    parse_input(proteinData, nonredundantAccessions, nonRedundantDirectory, numberOfBins)

    # Generate the predictions and graph for the redudnant proteins.
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

    # Write ou statistics about the confusion matrix.
    writeConfusion = open(outputDirectory + '/ConfusionMatrix.txt', 'w')
    writeConfusion.write('PAsP\tUAsP\tUAsU\tPAsU\n')
    writeConfusion.write(str(len(positivePosWeightFraction) - len(positivePredictedUnlabelled)))  # The number of positive proteins predicted as positive (TPs).
    writeConfusion.write('\t')
    writeConfusion.write(str(len(unlabelledPredictedPositive)))  # The number of unlabelled proteins predicted as positive (FPs).
    writeConfusion.write('\t')
    writeConfusion.write(str(len(unlabelledPosWeightFraction) - len(unlabelledPredictedPositive)))  # The number of unlabelled proteins predicted as unlabelled (TNs).
    writeConfusion.write('\t')
    writeConfusion.write(str(len(positivePredictedUnlabelled)))  # The number of positive proteins predicted as unlabelled (FNs).
    writeConfusion.write('\n')
    writeConfusion.close()

    # Create the histogram for the predictions of all the proteins.
    generate_graph(unlabelledPosWeightFraction, positivePosWeightFraction, outputDirectory + '/ProteinPredictions.png', numberOfBins)

def generate_graph(unlabelledPosWeightFraction, positivePosWeightFraction, saveLocation, numberOfBins=10):
    """
    TODO
    Control legend placement
    set colors of the bars          facecolor=['#FF0000', '#000000'] (red for U and black for P)
    set edge colors of the bars     edgecolor=['#FF0000', '#000000'] (red for U and black for P)
    maybe put the number of occurences on top of each bar (so like blue bar from 0 to 10 has a 10 on top of it and then the green stack from 10 to 12 has a 2 on top of it)
    make only one 0 where the axes meet
    """

    # Create the figure
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    histPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()

    # Plot the protein predictions.
    bins = [i / float(numberOfBins) for i in range(numberOfBins + 1)]
    axes.hist([unlabelledPosWeightFraction, positivePosWeightFraction], bins, label=['Non-Target', 'Target'], align='mid', histtype='barstacked', rwidth=1.0)
    axes.legend()

    plt.savefig(saveLocation, bbox_inches='tight', transparent=True)
    plt.show()

if __name__ == '__main__':
    main(sys.argv[1:])