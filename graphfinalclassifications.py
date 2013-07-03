import os
import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import shutil
import sys

def main(args):
	"""
	"""

	nrProteinPredictions = args[0]  # The file containing the protein predictions for only the non-redundant proteins.
	rProteinPredictions = args[0]  # The file containing the protein predictions for only the redundant proteins.
	allProteinPredictions = args[0]  # The file containing the protein predictions for all proteins.
	outputDirectory = args[3]  # The location of the directory where the results should be written.
	if os.path.exists(outputDirectory):
		shutil.rmtree(outputDirectory)
	os.mkdir(outputDirectory)

	# Generate the predictions and graph for the non-redudnant proteins.
	os.mkdir(outputDirectory + '/Non-Redundant')
	parse_input(nrProteinPredictions, outputDirectory)

	# Generate the predictions and graph for the redudnant proteins.
	os.mkdir(outputDirectory + '/Redundant')
	parse_input(rProteinPredictions, outputDirectory)

	# Generate the predictions and graph for all proteins.
	os.mkdir(outputDirectory + '/All')
	parse_input(allProteinPredictions, outputDirectory)

def parse_input(proteinPredictions, outputDirectory, numberOfBins=30):
	"""
	"""

	# Parse the predicted classification information.
	proteinData = {}  # A record of the protein prediction data indexed by the protein accession.
	readPredictions = open(proteinPredictions, 'r')
	readPredictions.readline()  # Strip off the header.
	for line in readPredictions:
		chunks = (line.strip()).split('\t')
		acc = chunks[0]  # The protein's UniProt accession.
		posWeight = float(chunks[1])  # The weight of the positive prediction for the protein.
		negWeight = float(chunks[2])  # The weight of the negative prediction for the protein.
		originalClass = chunks[3]  # The class of the protein.
		proteinData[acc] = {'PosWeight' : posWeight, 'NegWeight' : negWeight, 'Class' : originalClass}
	readPredictions.close()

	# Determine the unlabelled proteins that are predicted to be positive, along with the fraction of the prediction weight that is positive for each protein.
	unlabelledPredictedPositive = []  # The unlabelled proteins that have greater the 50% of their predicted weight as positive.
	unlabelledPosWeightFraction = []  # The fraction of the prediction weight that is positive for each unlabelled protein.
	positivePosWeightFraction = []  # The fraction of the prediction weight that is positive for each positive protein.
	for i in proteinData:
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

	# Write out the accession for all unlabelled proteins that appear to be positive.
	writePositives = open(outputDirectory + '/ApparentPositives.txt', 'w')
	writePositives.write('Accession\tPosFractionOfPredictiveWeight\n')
	for i in unlabelledPredictedPositive:
		writePositives.write(i[0] + '\t' + str(i[1]) + '\n')
	writePositives.close()

	# Create the histogram for the predictions of all the proteins.
	generate_graph(unlabelledPosWeightFraction, positivePosWeightFraction, outputDirectory + '/ProteinPredictions.png', numberOfBins)

def generate_graph(unlabelledPosWeightFraction, positivePosWeightFraction, saveLocation, numberOfBins=10):
	"""
	TODO
	Control legend placement
	set colors of the bars 			facecolor=['#FF0000', '#000000'] (red for U and black for P)
	set edge colors of the bars		edgecolor=['#FF0000', '#000000'] (red for U and black for P)
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