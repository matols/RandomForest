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
positiveRankSum <- c()
superiority <- c()

# Perform the statistical significance tests.
for (i in 1:(numberOfFeatures - 1))
{
	# Determine the name of the feature being analysed.
	feature <- colnames(proteinData)[i]
	
	# Determine the values of the positive and unlabelled observations for the feature.
	positiveData <- proteinData[positiveMask, i]
	numberOfPositive <- length(positiveData)
	unlabelledData <- proteinData[unlabelledMask, i]
	numberOfUnlabelled <- length(unlabelledData)
	
	# Only test the feature if there is more than one value for the positive and unlabelled observations.
	if (length(unique(proteinData[,i])) != 1)
	{
		g <- factor(c(rep("Unlabelled", numberOfUnlabelled), rep("Positive", numberOfPositive)))
		v <- c(unlabelledData, positiveData)

		# Calculate the expected and actual rank sums for the positive class.
		actualRankSum <- sum(rank(v)[g=='Positive'])
		expectedRankSum <- numberOfPositive * (numberOfUnlabelled + numberOfPositive + 1) / 2
	
		# Calculate the exact p value when there aren't enough observations to use an approximation.
		if (min(numberOfUnlabelled, numberOfPositive) > 300)
		{
			wt <- wilcox_test(v ~ g)
		}
		else
		{
			wt <- wilcox_test(v ~ g, distribution="exact")
		}

		# Calculate the p value directly from the z score in order to obtain "reasonable" p values (not just 0) when Z is large and positive.
		zScore <- statistic(wt, "standardized")
		if (zScore >= 0)
		{
			pValue[i] <- pnorm(zScore, lower.tail=FALSE) * 2
		}
		else
		{
			pValue[i] <- pnorm(zScore) * 2
		}

		U <- actualRankSum - (numberOfPositive * (numberOfPositive + 1) / 2)  # The rank sum of the observations minus the min rank sum they could obtain.
		superiority[i] <- U / (numberOfPositive * numberOfUnlabelled)
	}
	else
	{
		pValue[i] <- '-'
		actualRankSum <- 0
		expectedRankSum <- 0
	}
	
	# Calculate descriptive statistics for the feature.
	unlabelledMean[i] <- mean(unlabelledData)
	unlabelledMedian[i] <- median(unlabelledData)
	unlabelledVariance[i] <- var(unlabelledData)
	positiveMean[i] <- mean(positiveData)
	positiveMedian[i] <- median(positiveData)
	positiveVariance[i] <- var(positiveData)
	if (actualRankSum <= expectedRankSum)
	{
		positiveRankSum[i] <- 'Lower'
	}
	else
	{
		positiveRankSum[i] <- 'Higher'
	}
}

results <- data.frame(pValue, superiority, unlabelledMean, unlabelledMedian, unlabelledVariance, positiveMean, positiveMedian, positiveVariance, positiveRankSum, row.names=header[2:numberOfFeatures])

write.csv(results, file=outputFile, quote=FALSE)