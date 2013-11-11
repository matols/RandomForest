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
    featureImportances = {}
    readIn = open(featureImportancesFile, 'r')
    header = readIn.readline()
    for line in readIn:
        chunks = (line.strip()).split('\t')
        feature = chunks[0]
        significance = chunks[4] == 'True'
        importance = float(chunks[5])
        featureImportances[feature] = [significance, importance]
    readIn.close()

    # Define the different feature classes.
    aminoAcidComps = ['A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'P', 'N', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y', 'NegativelyCharged',
                      'PositivelyCharged', 'Charged', 'Polar', 'NonPolar', 'Aromatic', 'Aliphatic', 'Small', 'Tiny']
    postTransModification = ['OGlycosylation', 'NGlycosylation', 'Phosphoserine', 'Phosphothreonine', 'Phosphotyrosine']
    secondaryStructure = ['TransmembraneHelices', 'AlphaHelices', 'BetaStrands', 'Turns']
    variants = ['3Untranslated', '5Untranslated', 'NonSynonymousCoding', 'SynonymousCoding']
    expression = ['DS_Embryoid_Body', 'DS_Blastocyst', 'DS_Fetus', 'DS_Neonate', 'DS_Infant', 'DS_Juvenile', 'DS_Adult', 'BS_Adipose_Tissue',
                  'BS_Adrenal_Gland', 'BS_Ascites', 'BS_Bladder', 'BS_Blood', 'BS_Bone', 'BS_Bone_Marrow', 'BS_Brain', 'BS_Cervix',
                  'BS_Connective_Tissue', 'BS_Ear', 'BS_Embryonic_Tissue', 'BS_Esophagus', 'BS_Eye', 'BS_Heart', 'BS_Intestine', 'BS_Kidney',
                  'BS_Larynx', 'BS_Liver', 'BS_Lung', 'BS_Lymph', 'BS_Lymph_Node', 'BS_Mammary_Gland', 'BS_Mouth', 'BS_Muscle', 'BS_Nerve',
                  'BS_Ovary', 'BS_Pancreas', 'BS_Parathyroid', 'BS_Pharynx', 'BS_Pituitary_Gland', 'BS_Placenta', 'BS_Prostate', 'BS_Salivary_Gland',
                  'BS_Skin', 'BS_Spleen', 'BS_Stomach', 'BS_Testis', 'BS_Thymus', 'BS_Thyroid', 'BS_Tonsil', 'BS_Trachea', 'BS_Umbilical_Cord',
                  'BS_Uterus', 'BS_Vascular']
    miscMeasured = ['Sequence', 'PESTMotif', 'SignalPeptide', 'Paralogs', 'BinaryPPI', 'AlternativeTranscripts']
    miscCalculated = ['LowComplexity', 'Hydrophobicity', 'Isoelectric', 'HalfLife', 'InstabilityIndex']

    # Generate the coordinates for the points.
    xValuesNotSig = []
    yValuesNotSig = []
    xValuesSig = []
    yValuesSig = []
    dividingLines = []
    currentXValue = 1
    for i in aminoAcidComps:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in postTransModification:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in secondaryStructure:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in variants:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in expression:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in miscMeasured:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1
    currentXValue += 1  # Pad the division after the current section.
    dividingLines.append(currentXValue)
    currentXValue += 2  # Pad the division before the next section.
    for i in miscCalculated:
        significant = featureImportances[i][0]
        importance = featureImportances[i][1]
        if significant:
            xValuesSig.append(currentXValue)
            yValuesSig.append(importance)
        else:
            xValuesNotSig.append(currentXValue)
            yValuesNotSig.append(importance)
        currentXValue += 1

    # Determine the boundaries for the y axis.
    minImportance = min(min(yValuesNotSig), min(yValuesSig))
    minBoundary = minImportance - 0.0025
    maxImportance = max(max(yValuesNotSig), max(yValuesSig))
    maxBoundary = maxImportance + 0.0025

    # Determine the partition coordinates.
    partitionLabels = ['(a)', '(b)', '(c)', '(d)', '(e)', '(f)', '(g)']
    partitionLineXValues = [np.array([i, i]) for i in dividingLines]
    partitionLineYValues = [np.array([minBoundary, maxBoundary]) for i in dividingLines]
    dividingLines = [0] + dividingLines
    dividingLines.append(currentXValue)
    partitionLabelXValues = [sum(dividingLines[i:i+2]) / 2 for i in range(0, len(dividingLines), 1)[:-1]]
    partitionLabelYValues = [maxBoundary - 0.002 for i in partitionLabels]

    # Create the figure.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(10, 10)
    gs.update(left=0, right=1, bottom=0, top=1, wspace=0.05, hspace=0.05)
    scatterPlot = plt.subplot(gs[1:-1, 1:-1])
    axes = currentFigure.gca()
    axes.set_xlim(left=0, right=dividingLines[-1])
    axes.set_ylim(bottom=minBoundary, top=maxBoundary)
    axes.set_ylabel("Variable Importance", fontsize=15)

    # Plot the points.
    axes.scatter(xValuesNotSig, yValuesNotSig, s=15, c='white', marker='o', edgecolor='black', zorder=2)
    axes.scatter(xValuesSig, yValuesSig, s=15, c='black', marker='o', edgecolor='none', zorder=2)

    # Plot the partitions.
    for i in range(len(partitionLabels) - 1):
        axes.plot(partitionLineXValues[i], partitionLineYValues[i], markersize=0, color='black', linestyle='--', linewidth=1, zorder=1)
    for i in range(len(partitionLabels)):
        axes.text(partitionLabelXValues[i], partitionLabelYValues[i], partitionLabels[i], size=10, color='black', horizontalalignment='center',
            verticalalignment='center', zorder=1)

    # Add the horizontal line at the origin.
    axes.plot([0, dividingLines[-1]], [0, 0], markersize=0, color='black', linestyle='-', linewidth=1, zorder=0)

    # Hide the x ticks, and make the y axis ticks only on the left.
    axes.set_xticks([])
    axes.yaxis.tick_left()

    # Save the figure.
    plt.savefig(resultsLocation, bbox_inches='tight', transparent=True)


if __name__ == '__main__':
    main(sys.argv)