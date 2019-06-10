# EvoSuiteTuning
Here is the code to run EvoSuite exhaustively as part of our research on effectiveness of tuning parameters.

Initially, we were about to apply hyper-heuristics to tune the EvoSuite tool. Therefore, we encoded the parameters of our interest in the form of chromosomes using the implementation of evosuite itself.
However, later we changed our mind and assesssed our search space exhaustively.
In other words, this can be used much more easier, and there is no need to implement it in this way to search exhaustively.

- Parameters of out interest are encoded in the "Solution.java" file.
- The Exhaustive study is done using the code in the file "Exhaustive.java" file, in which we assessed all possible values for the parameters of our interest in a few for-loops.
- In order to run the EvoSuite with the specified configuration, we use the "RunOnEvoSuite.java" implementation.
- F-Race method is also implemented to be used as a hyper-heuristic to tune the meta-heuristic in use in EvoSuite. However, due to the high cost, we did not use it. Therefore, we are not sure if the implementation is correct hundred percent or not.


The results of this work are summarized in the data.xlsx excel file. It has three sheets and each sheet coresponds to one of the Java projects that we studied, namely, JOpenChart, Geo-Google, and JSecurity--all from SF100 benchmark.
