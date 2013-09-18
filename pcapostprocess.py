import sys

def main(args):
    """Create a new dataset (in the same format) from the transformed features (the principal components).

    The dataset used is assumed to be a tab delimited file with a single line at the top of the file containing the names of the features.
    The last column in the file is assumed to be the class.

    :parma args: the command line arguments excluding the script name
    :type args: string

    """

    resultsPCA = args[0]  # The scores from the PCA.
    datasetOriginal = args[1]  # The file used to generate the scores.
    datasetPCA = args[2]  # The location where the new dataset will be written out.

    # Determine the class of each observation.
    classification = {}
    readOrig = open(datasetOriginal, 'r')
    readOrig.readline()  # Strip the header from the dataset.
    observationIndex = 1  # The index of the observaion in the dataset (with 1 being the first observation).
    for line in readOrig:
        chunks = (line.strip()).split('\t')
        classification[observationIndex] = chunks[-1]  # The last column in the dataset is assumed to contain the class of the observation.
        observationIndex += 1
    readOrig.close()

    # Write out the values of the transformed features (the principal components) for each observation, along with the class of the observation.
    readPCA = open(resultsPCA, 'r')
    writePCA = open(datasetPCA, 'w')
    header = (readPCA.readline()).strip()
    headerChunks = header.split(',')[1:]
    writePCA.write('\t'.join(headerChunks) + '\tClassification\n')
    numberPCAs = len(header)
    for line in readPCA:
        chunks = (line.strip()).split(',')  # Split on ',' as the PCA results file is a csv file NOT tab delimited.
        obsIndex = int(chunks[0])  # The index of the observation is the first column in the PCA scores file.
        newLine = chunks[1:] + [classification[obsIndex]]  # The new values for an observation are the transformed feature values with the classification appended.
        writePCA.write(('\t'.join(newLine) + '\n'))
    readPCA.close()
    writePCA.close()

if __name__ == '__main__':
    main(sys.argv[1:])