library(coin)

# Read in the command line arguments.
args <- commandArgs(trailingOnly=TRUE)
inputFile <- args[1]
outputFile <- args[2]

# Select the first line of the dataset file (which contains the feature names).
header <- colnames(read.table(inputFile, sep='\t', header=TRUE))

# Read in the dataset (excluding the header line), and drop the columns that are not to be used (in this case the UniProt accession column).
proteinData <- read.table(inputFile, sep='\t', skip=1, col.names=header)[c(-1)]

# The number of features is the number of columns in proteinData.
numberOfFeatures <- length(proteinData)

# Generate the class masks for the positive and unlabelled observations.
positiveMask <- proteinData[[numberOfFeatures]] == 'Positive'
unlabelledMask <- proteinData[[numberOfFeatures]] == 'Unlabelled'

# Set up the vectors used for recording the results of the statistical tests.
pValue <- c()
unlabelledMean <- c()
unlabelledMedian <- c()
unlabelledVariance <- c()
positiveMean <- c()
positiveMedian <- c()
positiveVariance <- c()

# Perform the statistical significance tests.
for (i in 1:(numberOfFeatures - 1))
{
	# Determine the name of the feature being analysed.
	feature <- colnames(proteinData)[i]
	
	# Determine the values of the positive and unlabelled observations for the feature.
	positiveData <- proteinData[positiveMask, i]
	unlabelledData <- proteinData[unlabelledMask, i]
	
	# Only test the feature if there is more than one value for the positive and unlabelled observations.
	if (length(unique(positiveData)) != 1 || length(unique(unlabelledData)) != 1)
	{
		g <- factor(c(rep("Unlabelled", length(unlabelledData)), rep("Positive", length(positiveData))))
		v <- c(unlabelledData, positiveData)
		
		# Calculate the exact p value when there aren't enough observations to use an approximation.
		if (min(length(unlabelledData), length(positiveData)) > 300)
		{
			wt <- wilcox_test(v ~ g)
		}
		else
		{
			wt <- wilcox_test(v ~ g, distribution="exact")
		}
		pValue[i] <- pvalue(wt)
	}
	else
	{
		pValue[i] <- '-'
	}
	
	# Calculate descriptive statistics for the feature.
	unlabelledMean[i] <- mean(unlabelledData)
	unlabelledMedian[i] <- median(unlabelledData)
	unlabelledVariance[i] <- var(unlabelledData)
	positiveMean[i] <- mean(positiveData)
	positiveMedian[i] <- median(positiveData)
	positiveVariance[i] <- var(positiveData)
}

results <- data.frame(pValue, unlabelledMean, unlabelledMedian, unlabelledVariance, positiveMean, positiveMedian, positiveVariance, row.names=header[2:numberOfFeatures])

write.csv(results, file=outputFile, quote=FALSE)