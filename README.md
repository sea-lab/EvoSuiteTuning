# EvoSuiteTuning
Here is the code to run EvoSuite exhaustively as part of our research on effectiveness of tuning parameters.

Initially, we were about to apply hyper-heuristics to tune the EvoSuite tool. Therefore, we encoded the parameters of our interest in the form of chromosomes using the implementation of evosuite itself.
However, later we changed our mind and assesssed our search space exhaustively.
In other words, this can be used much more easier, and there is no need to implement it in this way to search exhaustively.

The results of this work are summarized in the data.xlsx excel file. It has three sheets and each sheet coresponds to one of the Java projects that we studied, namely, JOpenChart, Geo-Google, and JSecurity--all from SF100 benchmark.
