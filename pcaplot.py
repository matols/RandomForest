import matplotlib.gridspec as gridspec
import matplotlib.pyplot as plt
import numpy as np
import sys

def main(PCAResults, figureSaveDir, PCsoPlot):
	"""
	"""

	PCAScores = np.genfromtxt(PCAResults, delimiter='\t', skip_header=1, autostrip=True, dtype=None)

	principalComponentsToPlot = int(PCsoPlot)

	# Setup the records of the values for each PC for each class.
	class1PCValues = dict([(i, []) for i in range(principalComponentsToPlot)])
	class1Color = '#FF0000'  # Red
	class1Size = 20

	class2PCValues = dict([(i, []) for i in range(principalComponentsToPlot)])
	class2Color = '#000000'  # Black
	class2Size = 20

	# Record the values of each PC of interest for each class.
	for i in PCAScores:
		if i[-1] == b'Positive':  # Have to use b'Positive' as np.genfromtext reads in characters as byte strings.
			for j in range(principalComponentsToPlot):
				class1PCValues[j].append(i[j])
		else:
			for j in range(principalComponentsToPlot):
				class2PCValues[j].append(i[j])

	# Setup the plot.
	currentFigure = plt.figure()
	gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
	gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
	for i in range(principalComponentsToPlot):
		for j in range(principalComponentsToPlot):
			plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
			#plot.axis('scaled')
			plot.set_xticks([])
			plot.set_yticks([])
			plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=2 if i <= j else 1)
			plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=1 if i <= j else 2)
			if j == 0:
				plot.set_ylabel('PC' + str(i + 1), rotation=0)
			if i == principalComponentsToPlot - 1:
				plot.set_xlabel('PC' + str(j + 1))

	plt.savefig(figureSaveDir + '/Balanced', bbox_inches='tight', transparent=True)

	# Setup the plot.
	currentFigure = plt.figure()
	gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
	gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
	for i in range(principalComponentsToPlot):
		for j in range(principalComponentsToPlot):
			plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
			#plot.axis('scaled')
			plot.set_xticks([])
			plot.set_yticks([])
			plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=2)
			plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=1)
			if j == 0:
				plot.set_ylabel('PC' + str(i + 1), rotation=0)
			if i == principalComponentsToPlot - 1:
				plot.set_xlabel('PC' + str(j + 1))

	plt.savefig(figureSaveDir + '/PositiveOnTop', bbox_inches='tight', transparent=True)

	# Setup the plot.
	currentFigure = plt.figure()
	gs = gridspec.GridSpec(principalComponentsToPlot, principalComponentsToPlot)
	gs.update(left=0, right=1, bottom=0.05, top=1, wspace=0.05, hspace=0.05)
	for i in range(principalComponentsToPlot):
		for j in range(principalComponentsToPlot):
			plot = plt.subplot(gs[i, j])  # Create a 1x1 plot in the ith row and jth column
			#plot.axis('scaled')
			plot.set_xticks([])
			plot.set_yticks([])
			plot.scatter(class1PCValues[i], class1PCValues[j], s=class1Size, c=class1Color, zorder=1)
			plot.scatter(class2PCValues[i], class2PCValues[j], s=class2Size, c=class2Color, zorder=2)
			if j == 0:
				plot.set_ylabel('PC' + str(i + 1), rotation=0)
			if i == principalComponentsToPlot - 1:
				plot.set_xlabel('PC' + str(j + 1))

	plt.savefig(figureSaveDir + '/UnlabelledOnTop', bbox_inches='tight', transparent=True)


if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2], sys.argv[3])