# Include standard modules
import sys
import csv
import matplotlib.pyplot as plt
import collections

if (len(sys.argv) < 4):
    print("[src file] [title name] [y-axis label] [x-axis label]")
    exit(0)


DATA_SRC = sys.argv[1]
histDict = {}

def rowGenerator(fname):
    with open(fname) as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            yield row



for row in rowGenerator(DATA_SRC):
    titleLength = int(row[2])
    
    if titleLength in histDict:
        histDict[titleLength] = histDict[titleLength]+1
    else:
        histDict[titleLength] = 1

outDict = collections.OrderedDict(sorted(histDict.items()))


plt.title(sys.argv[2])
plt.ylabel(sys.argv[3])
plt.xlabel(sys.argv[4])

plt.bar(outDict.keys(), outDict.values())
plt.show()