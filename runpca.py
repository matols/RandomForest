import os
import subprocess
import sys

def main(args):
    """Runs PCA on a dataset, and then processes and plots the results.

    The dataset used is assumed to be a tab delimited file with a single line at the top of the file containing the names of the features.
    The last column in the file is assumed to be the class.

    :parma args: the command line arguments excluding the script name
    :type args: string

    """

    inputFile = args[0]  # The original (untransformed) dataset.
    outputDirR = args[1]  # The directory where the results will be written out
    datasetName = args[2]  # The name of the dataset (some arbitrary identifier).
    pcsToPlot = args[3]  # The number of principal components to plot against one another.

    # Create the directory that will hold the PCA results and plots.
    if not os.path.isdir(outputDirR):
        os.mkdir(outputDirR)

    # Setup the locations where the results will be recorded.
    pcaScores = outputDirR + '/Scores.csv'
    pcaDataset = outputDirR + '/PCA_' + datasetName + '.txt.'
    plotDir = outputDirR + '/Plots'

    # Create the directory that will hold the plots.
    if not os.path.isdir(plotDir):
        os.mkdir(plotDir)

    # Run the PCA, followed by the processing and the plotting.
    subprocess.call('Rscript.exe performpca.r ' + inputFile + ' ' + outputDirR)
    subprocess.call('python pcapostprocess.py ' + pcaScores + ' ' + inputFile + ' ' + pcaDataset)
    subprocess.call('python pcaplot.py ' + pcaDataset + ' ' + plotDir + ' ' + int(pcsToPlot))


if __name__ == '__main__':
    main(sys.argv[1:])