import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;
import org.evosuite.shaded.org.apache.commons.lang3.ArrayUtils;

public class FRace {
    int max_instances = 2000;
    ArrayList<Solution> candidates;
    int experiments_soFar = 0;
    int instances_soFar = 0;
    ArrayList<String> classes;

    public FRace() {
    }

    public Solution generic_race(int M) {
        this.fill_classes();
        this.fill_candidates();
        HashMap<Solution, ArrayList<Double>> cost = new HashMap();
        Iterator var3 = this.candidates.iterator();

        while(var3.hasNext()) {
            Solution candidate = (Solution)var3.next();
            cost.put(candidate, new ArrayList());
        }

        ArrayList<Solution> S = (ArrayList)this.candidates.clone();
        ArrayList names = new ArrayList();

        while(this.experiments_soFar + S.size() < M && this.instances_soFar + 1 <= this.max_instances && S.size() != 1) {
            String name = this.sample_class();
            ++this.instances_soFar;
            names.add(name);
            ExecutorService executor = Executors.newFixedThreadPool(8);
            Iterator var7 = S.iterator();

            while(var7.hasNext()) {
                Solution candidate = (Solution)var7.next();
                executor.execute(new 1(this, candidate, name, cost));
            }

            executor.shutdown();

            try {
                executor.awaitTermination((long)S.size(), TimeUnit.MINUTES);
            } catch (InterruptedException var9) {
                var9.printStackTrace();
            }

            if (this.instances_soFar != 1) {
                S = this.drop_candidates(S, cost);
            }
        }

        Solution best = this.select_best_survivor(S, cost);
        System.out.println(best + " solution is this one!");
        return best;
    }

    void fill_classes() {
        new File(MetaGA.address + "/SF100");
        this.classes = new ArrayList();
        String[] directories = new String[]{"1_tullibee"};
        String[] var3 = directories;
        int var4 = directories.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String classPath = var3[var5];
            String projectName = classPath.split("_")[1];
            Properties.CP = MetaGA.address;
            Set<String> project = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getAllClasses("SF100/" + classPath + "/" + projectName + ".jar", false);
            Iterator var9 = project.iterator();

            while(var9.hasNext()) {
                String sut = (String)var9.next();

                try {
                    if (!ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).isClassAnInterface(sut) && ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).hasClass(sut)) {
                        try {
                            Class.forName(sut, false, TestGenerationContext.getInstance().getClassLoaderForSUT());
                            this.classes.add(classPath + "," + sut);
                        } catch (ClassNotFoundException var12) {
                            ;
                        }
                    }
                } catch (IOException var13) {
                    var13.printStackTrace();
                }
            }

            System.out.println("Project " + classPath + " is imported and it has " + project.size() + " classes in it.");
        }

    }

    void fill_candidates() {
        this.candidates = new ArrayList();

        for(int cross = 0; cross < Solution.cross.length; cross += 4) {
            for(int pop = 0; pop < Solution.pop.length; pop += 4) {
                for(int elite = 0; elite < Solution.elite.length; elite += 4) {
                    for(int selection = 0; selection < Solution.selection_set.length; selection += 3) {
                        for(int parent = 0; parent < Solution.parent_check.length; ++parent) {
                            int[] p = new int[]{cross, pop, elite, selection, parent};
                            Solution s = new Solution(p);
                            this.candidates.add(s);
                        }
                    }
                }
            }
        }

        System.out.println("Candidates are imported and initially we have " + this.candidates.size() + " possible solutions for our meta heuristic.");
    }

    String sample_class() {
        Random r = new Random();
        int randomInt = r.nextInt(this.classes.size());
        String name = (String)this.classes.get(randomInt);
        System.out.println("In this step " + name + " is selected.");
        return name;
    }

    double run_experiment(Solution candidate, String class_id) {
        String project = class_id.split(",")[0];
        String projectName = class_id.split(",")[0].split("_")[1];
        String class_name = class_id.split(",")[1];
        String cmd = String.format("java -jar %s/evosuite-1.0.6.jar -target %s/SF100/%s/%s.jar -class %s -Dcrossover_rate=%f  -Dpopulation=%d -Dselection_function=%s -Dshow_progress=False -Delite=%d -Dstopping_condition=maxtime -Dsearch_budget=120 -criterion branch -Dwrite_individuals=true -Dparent_check=%s  -Doutput_variables=TARGET_CLASS,BranchCoverage,MutationScore,Total_Goals,Covered_Goals,Mutants -Dreport_dir=%s/frace/%s", MetaGA.address, MetaGA.address, project, projectName, class_name, candidate.getCrossover_rate(), candidate.getPopulation(), candidate.getSelection(), candidate.getElitism(), candidate.isParent_replacement(), MetaGA.address, this.instances_soFar + "_" + this.candidates.indexOf(candidate));
        if (candidate.index[3] == 1) {
            cmd = cmd + " -Dtournament_size=2";
        }

        if (candidate.index[3] == 2) {
            cmd = cmd + " -Dtournament_size=7";
        }

        if (candidate.index[3] == 3) {
            cmd = cmd + " -Drank_bias=1.2";
        }

        Process reader;
        try {
            Runtime rt = Runtime.getRuntime();
            reader = rt.exec(cmd);
            reader.waitFor();
            reader.destroy();
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        String strFile = String.format("%s/frace/%s/statistics.csv", MetaGA.address, this.instances_soFar + "_" + this.candidates.indexOf(candidate));
        reader = null;
        double coverage = 0.0D;

        try {
            CSVReader reader = new CSVReader(new FileReader(strFile));
            int lineNumber = 0;

            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                ++lineNumber;
                if (lineNumber != 1) {
                    coverage += Double.parseDouble(nextLine[1]);
                }
            }

            reader.close();
        } catch (Exception var14) {
            System.out.println("this file is not found for coverage calculation at " + strFile);
        }

        System.out.println("Candidate number " + this.candidates.indexOf(candidate) + " for instance number " + this.instances_soFar + " has coverage of " + coverage);
        return coverage;
    }

    ArrayList<Solution> drop_candidates(ArrayList<Solution> S, HashMap<Solution, ArrayList<Double>> cost) {
        System.out.println(cost);
        HashMap<Solution, ArrayList<Double>> rank = this.getRankingMap(cost);
        System.out.println(rank);
        if (S.size() == 2) {
            double[] first = new double[this.instances_soFar];
            double[] second = new double[this.instances_soFar];

            for(int j = 0; j < this.instances_soFar; ++j) {
                first[j] = (Double)((ArrayList)cost.get(S.get(0))).get(j);
                second[j] = (Double)((ArrayList)cost.get(S.get(1))).get(j);
            }

            WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();
            double test_statistics = (double)(this.instances_soFar * (this.instances_soFar + 1)) / 2.0D - wilcoxon.wilcoxonSignedRank(first, second);
            double critical_value = (test_statistics - (double)(this.instances_soFar * (this.instances_soFar + 1) / 4)) / Math.sqrt((double)(this.instances_soFar * (this.instances_soFar + 1) * (2 * this.instances_soFar + 1) / 24));
            if (test_statistics > critical_value) {
                S.remove(1);
            } else {
                S.remove(0);
            }
        } else if (this.friedman_test(rank, 0.95D, S)) {
            Solution best = this.select_best_survivor(S, cost);
            System.out.println("Null hypothesis is rejected and After stage number " + this.instances_soFar + " best solution is " + best);
            System.out.println("Let's compare other solutions with the best one and cancel them out for next stage");
            Iterator iterator = S.iterator();

            while(iterator.hasNext()) {
                Solution s = (Solution)iterator.next();
                if (this.is_significantly_worse(best, s, rank, S)) {
                    iterator.remove();
                    System.out.println(s + " is removed!");
                }
            }
        } else {
            System.out.println("Stage " + this.instances_soFar + ": solutions are not different that much and so the friedman null hypothesis is not rejected.");
        }

        return S;
    }

    Solution select_best_survivor(ArrayList<Solution> S, HashMap<Solution, ArrayList<Double>> cost) {
        HashMap<Solution, ArrayList<Double>> rank = this.getRankingMap(cost);
        double min = 0.0D;
        Solution best = null;

        for(int i = 0; i < S.size(); ++i) {
            double sum = 0.0D;

            for(int j = 0; j < this.instances_soFar; ++j) {
                sum += (Double)((ArrayList)rank.get(S.get(i))).get(j);
            }

            if (i == 0) {
                min = sum;
            }

            if (sum < min) {
                min = sum;
                best = (Solution)((Solution)S.get(i)).clone();
            }
        }

        return best;
    }

    boolean is_significantly_worse(Solution best, Solution solution, HashMap<Solution, ArrayList<Double>> rank, ArrayList<Solution> S) {
        int degree_of_freedom = this.instances_soFar - 1;
        double sum1 = 0.0D;
        double sum2 = 0.0D;
        int best_rank_sum = 0;
        int solution_rank_sum = 0;

        for(int i = 0; i < S.size(); ++i) {
            int temp = 0;

            for(int j = 0; j < this.instances_soFar; ++j) {
                sum1 += Math.pow((Double)((ArrayList)rank.get(S.get(i))).get(j), 2.0D);
                temp = (int)((double)temp + (Double)((ArrayList)rank.get(S.get(i))).get(j));
            }

            if (S.get(i) == best) {
                best_rank_sum = temp;
            }

            if (S.get(i) == solution) {
                solution_rank_sum = temp;
            }

            sum2 += Math.pow((double)(temp - this.instances_soFar * (S.size() + 1) / 2), 2.0D);
        }

        sum1 -= (double)(this.instances_soFar * S.size()) * Math.pow((double)(S.size() + 1), 2.0D) / 4.0D;
        double quantile = (new TDistribution((double)degree_of_freedom)).inverseCumulativeProbability(0.975D);
        double value = quantile * Math.sqrt(2.0D * ((double)this.instances_soFar * sum1 - sum2) / (double)degree_of_freedom);
        if ((double)Math.abs(solution_rank_sum - best_rank_sum) > value) {
            return true;
        } else {
            return false;
        }
    }

    HashMap<Solution, ArrayList<Double>> getRankingMap(HashMap<Solution, ArrayList<Double>> data) {
        HashMap<Solution, ArrayList<Double>> ranked = new HashMap();
        Iterator var3 = data.keySet().iterator();

        while(var3.hasNext()) {
            Solution candid = (Solution)var3.next();
            ranked.put(candid, new ArrayList());
        }

        NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.AVERAGE);

        for(int i = 0; i < this.instances_soFar; ++i) {
            ArrayList<Double> block = new ArrayList();
            ArrayList<Solution> block_sol = new ArrayList();
            Iterator var7 = data.keySet().iterator();

            while(var7.hasNext()) {
                Solution candidate = (Solution)var7.next();
                block.add(((ArrayList)data.get(candidate)).get(i));
                block_sol.add(candidate);
            }

            double[] target = new double[block.size()];

            for(int j = 0; j < target.length; ++j) {
                target[j] = (Double)block.get(j);
            }

            double[] ranked_target = ranking.rank(target);
            Double[] castType = ArrayUtils.toObject(ranked_target);
            Iterator var10 = data.keySet().iterator();

            while(var10.hasNext()) {
                Solution candidate = (Solution)var10.next();
                ArrayList<Double> my_ranks = (ArrayList)ranked.get(candidate);
                my_ranks.add(castType[block_sol.indexOf(candidate)]);
            }
        }

        System.out.println("Costs are ranked!");
        return ranked;
    }

    boolean friedman_test(HashMap<Solution, ArrayList<Double>> ranked, double certainty, ArrayList<Solution> S) {
        double sum = 0.0D;

        double sumsum;
        for(Iterator var7 = S.iterator(); var7.hasNext(); sum += Math.pow(sumsum, 2.0D)) {
            Solution s = (Solution)var7.next();
            sumsum = 0.0D;
            ArrayList<Double> arr = (ArrayList)ranked.get(s);

            Double d;
            for(Iterator var12 = arr.iterator(); var12.hasNext(); sumsum += d) {
                d = (Double)var12.next();
            }
        }

        double test_statistics = 12.0D * sum / (double)S.size() * (double)this.instances_soFar * (double)(this.instances_soFar + 1) - (double)(3 * S.size() * (this.instances_soFar + 1));
        sumsum = (new ChiSquaredDistribution((double)(this.instances_soFar - 1))).inverseCumulativeProbability(1.0D - certainty);
        return test_statistics > sumsum;
    }

    public static void main(String[] args) {
        new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.AVERAGE);
        FRace f = new FRace();
        f.generic_race(10000000);
    }
}
