import os
import re
import numpy
import matplotlib.pyplot as pyplot

generations = []
average_results = []
max_results = []
min_results = []

print("Which experiment do you want me to plot? (1/2/3 etc)")
experiment_number = input()

working_directory = os.getcwd() + "\\experiment" + experiment_number;
route = os.listdir(working_directory);
route.sort()
for directory in route:
  if not os.path.isdir(working_directory + "\\" + directory):
    continue
  generation_number = re.findall("(\\d+)", directory)
  generations.append(int(generation_number[0]))
  file = open(working_directory + "/" + directory + "/summary.txt")
  lines = str.join("", file.readlines())
  compiled = re.compile("Average: (-?\d*\.\d*)")
  average_results.append(float(compiled.findall(lines)[0]))
  compiled = re.compile("Max: (-?\d+) - player: \d+")
  max_results.append(float(compiled.findall(lines)[0]))
  compiled = re.compile("Min: (-?\d+) - player: \d+")
  min_results.append(float(compiled.findall(lines)[0]))

generations, average_results, max_results, min_results = zip(*sorted(zip(generations, average_results, max_results, min_results)))

pyplot.subplot(211)
average_plot, = pyplot.plot(generations, average_results, label="Average")
min_plot, = pyplot.plot(generations, min_results, label="Min")
pyplot.xlabel("generation")
pyplot.ylabel("fitness")

pyplot.legend(handles=[average_plot, min_plot])

pyplot.subplot(212)
pyplot.xlabel("generation")
pyplot.ylabel("fitness")
max_plot, = pyplot.plot(generations, max_results, label="Max")
pyplot.legend(handles=[max_plot])
pyplot.show()