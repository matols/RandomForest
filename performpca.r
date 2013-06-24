args <- commandArgs(trailingOnly=TRUE)
inputFile <- args[1]
outputFolder <- args[2]
header <- colnames(read.table(inputFile, sep='\t', header=TRUE))
proteinData <- read.table(inputFile, sep='\t', skip=3, col.names=header)[c(-1, -106, -107, -108)]
numberOfFeatures = length(proteinData)
columnsToDrop <- c()
for (i in 1:numberOfFeatures)
{
	if (length(unique(proteinData[, i])) == 1)
	{
		columnsToDrop <- c(colnames(proteinData)[i], columnsToDrop)
	}
}
proteinData <- proteinData[,!(colnames(proteinData) %in% columnsToDrop)]
pcaResults <- prcomp(proteinData, scale=TRUE)
capture.output(columnsToDrop, file=paste(outputFolder, 'DroppedColumns.txt', sep='/'))
capture.output(summary(pcaResults), file=paste(outputFolder, 'Summary.txt', sep='/'))
write.csv(pcaResults$sdev, paste(outputFolder, 'SqrtEignenvalues.csv', sep='/'), quote=FALSE)
write.csv(pcaResults$rotation, paste(outputFolder, 'Loadings.csv', sep='/'), quote=FALSE)
write.csv(pcaResults$x, paste(outputFolder, 'Scores.csv', sep='/'), quote=FALSE)