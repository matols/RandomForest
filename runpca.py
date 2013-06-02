import os
import subprocess
import sys

def main(args):
	"""
	"""

	inputFile = args[0]
	outputDirR = args[1]
	datasetName = args[2]
	pcsToPlot = args[3]

	if not os.path.isdir(outputDirR):
		os.mkdir(outputDirR)

	pcaScores = outputDirR + '/Scores.csv'
	pcaDataset = outputDirR + '/PCA_' + datasetName + '.txt.'
	plotDir = outputDirR + '/Plots'

	if not os.path.isdir(plotDir):
		os.mkdir(plotDir)

	subprocess.call('Rscript.exe performpca.r ' + inputFile + ' ' + outputDirR)
	subprocess.call('python pcapostprocess.py ' + pcaScores + ' ' + inputFile + ' ' + pcaDataset)
	subprocess.call('python pcaplot.py ' + pcaDataset + ' ' + plotDir + ' ' + pcsToPlot)


if __name__ == '__main__':
	main(sys.argv[1:])