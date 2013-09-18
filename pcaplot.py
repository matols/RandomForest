import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import sys

def main(PCAResults, figureSaveDir, principalComponentsToPlot):
    """Plots a subset of the transformed features (principal components) against one another.

    The dataset used is assumed to be a tab delimited file with a single line at the top of the file containing the names of the features.
    The last column in the file is assumed to be the class.

    There will be principalComponentsToPlot * principalComponentsToPlot subplots in the main plot. Each PC to be plotted will be plotted against each
    other one. There will therefore be a row for each PC being plotted (where it appears on the y axis), and a column for each PC being plotted (where it
    appears on the x axis).

    :parma PCAResults:
    :type PCAResults: string
    :parma figureSaveDir:
    :type figureSaveDir: string
    :parma principalComponentsToPlot: the number of components to plot (the first principalComponentsToPlot components are chosen)
    :type principalComponentsToPlot: int

    """

    # Read in the transformed dataset.
    PCAScores = np.genfromtxt(PCAResults, delimiter='\t', skip_header=1, autostrip=True, dtype=None)

    # Record the values of each PC of interest for each class.
    class1PCValues = dict([(i, []) for i in range(principalComponentsToPlot)])
    class2PCValues = dict([(i, []) for i in range(principalComponentsToPlot)])
    for i in PCAScores:
        if i[-1] == b'Positive':  # Have to use b'Positive' as np.genfromtext reads in characters as byte strings.
            for j in range(principalComponentsToPlot):
                class1PCValues[j].append(i[j])
        else:
            for j in range(principalComponentsToPlot):
                class2PCValues[j].append(i[j])

    # Setup the size and color for the points on the scatter plot.
    class1Color = '#FF0000'  # Red
    class1Size = 20
    class2Color = '#000000'  # Black
    class2Size = 20

    # Setup the plot where the class 1 points are on top (have a higher zorder) on and below the leading diagonal, and the class 2 points on top above the
    # leading diagonal.
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
    gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
    for i in range(principalComponentsToPlot):
        # Need a row in the plot for each PC to be on the x axis.
        for j in range(principalComponentsToPlot):
            # Need a column in the plot for each PC to be on the y axis
            plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
            plot.set_xticks([])  # Remove the x tick marks.
            plot.set_yticks([])  # Remove the y tick marks.
            plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=2 if i <= j else 1)  # Plot the class 1 values.
            plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=1 if i <= j else 2)  # Plot the class 2 values.
            if j == 0:
                # Only put a label on the y axis if this is the leftmost column.
                plot.set_ylabel('PC' + str(i + 1), rotation=0)
            if i == principalComponentsToPlot - 1:
                # Only put a label on th x axis if this is the bottom row.
                plot.set_xlabel('PC' + str(j + 1))
    plt.savefig(figureSaveDir + '/Balanced', bbox_inches='tight', transparent=True)

    # Setup the plot where the class 1 points are on top (have a higher zorder).
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
    gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
    for i in range(principalComponentsToPlot):
        # Need a row in the plot for each PC to be on the x axis.
        for j in range(principalComponentsToPlot):
            # Need a column in the plot for each PC to be on the y axis
            plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
            plot.set_xticks([])  # Remove the x tick marks.
            plot.set_yticks([])  # Remove the y tick marks.
            plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=2)
            plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=1)
            if j == 0:
                # Only put a label on the y axis if this is the leftmost column.
                plot.set_ylabel('PC' + str(i + 1), rotation=0)
            if i == principalComponentsToPlot - 1:
                # Only put a label on th x axis if this is the bottom row.
                plot.set_xlabel('PC' + str(j + 1))
    plt.savefig(figureSaveDir + '/PositiveOnTop', bbox_inches='tight', transparent=True)

    # Setup the plot where the class 2 points are on top (have a higher zorder).
    currentFigure = plt.figure()
    gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
    gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
    for i in range(principalComponentsToPlot):
        # Need a row in the plot for each PC to be on the x axis.
        for j in range(principalComponentsToPlot):
            # Need a column in the plot for each PC to be on the y axis
            plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
            plot.set_xticks([])  # Remove the x tick marks.
            plot.set_yticks([])  # Remove the y tick marks.
            plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=1)
            plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=2)
            if j == 0:
                # Only put a label on the y axis if this is the leftmost column.
                plot.set_ylabel('PC' + str(i + 1), rotation=0)
            if i == principalComponentsToPlot - 1:
                # Only put a label on th x axis if this is the bottom row.
                plot.set_xlabel('PC' + str(j + 1))
    plt.savefig(figureSaveDir + '/UnlabelledOnTop', bbox_inches='tight', transparent=True)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])