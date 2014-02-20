# Read in the command line arguments.
args <- commandArgs(trailingOnly=TRUE)
inputFile <- args[1]
outputFolder <- args[2]

# Select the first line of the dataset file (which contains the feature names).
header <- colnames(read.table(inputFile, sep='\t', header=TRUE))

# Read in the dataset (excluding the header line), and drop the columns that are not to be used (in this case the UniProt accession, half life,
# instability index and classification columns).
proteinData <- read.table(inputFile, sep='\t', skip=1, col.names=header)[c(-1, -105, -106, -107)]

# The number of features is the number of columns in proteinData.
numberOfFeatures <- length(proteinData)

# Determine the features that should be dropped (in addition to the features that were not read in when the dataset was). Features should be dropped
# if there is no variance in the value of the feature (i.e. a feature is dropped if all observations have the same value for it).
columnsToDrop <- c()
for (i in 1:numberOfFeatures)
{
    if (length(unique(proteinData[, i])) == 1)
    {
        # If there is only one value across all observations, then drop the feature.
        columnsToDrop <- c(colnames(proteinData)[i], columnsToDrop)
    }
}
columnsToDrop <- c('UPAccession', 'HalfLife', 'InstabilityIndex', 'Classification', columnsToDrop)
proteinData <- proteinData[,!(colnames(proteinData) %in% columnsToDrop)]  # Remove the features with no variance from the dataset.
capture.output(columnsToDrop, file=paste(outputFolder, 'DroppedColumns.txt', sep='/'))  # Write the names of the features with no variance to a file.

# Perform PCA, and write out the results.
pcaResults <- prcomp(proteinData, scale=TRUE)
capture.output(summary(pcaResults), file=paste(outputFolder, 'Summary.txt', sep='/'))  # Write out the summary data (standard deviation and proportion of variance for each PC).
write.csv(pcaResults$sdev, paste(outputFolder, 'SqrtEignenvalues.csv', sep='/'), quote=FALSE)
write.csv(pcaResults$rotation, paste(outputFolder, 'Loadings.csv', sep='/'), quote=FALSE)
write.csv(pcaResults$x, paste(outputFolder, 'Scores.csv', sep='/'), quote=FALSE)