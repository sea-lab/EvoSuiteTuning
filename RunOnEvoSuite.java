
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;

public class RunOnEvoSuite {
    Solution solution;
    static String projectName;
    int projectId;
    int numberOfClasses;
    ArrayList<String> classList;
    static int counter = 1;
    static boolean full_set = true;

    public RunOnEvoSuite(Solution bestIndividual, String pname) throws IOException {
        projectName = this.getProjectName(pname);
        this.projectId = this.getProjectId(pname);
        this.solution = bestIndividual;
        this.classList = this.getClassList();
    }

    public String getProjectName(String name) {
        String[] splited = name.split("_");
        projectName = splited[1];
        return projectName;
    }

    public int getProjectId(String name) {
        String[] splited = name.split("_");
        this.projectId = Integer.parseInt(splited[0]);
        return this.projectId;
    }

    public String getDependencies() {
        ArrayList<String> results = new ArrayList();
        File[] files = (new File(MetaGA.address + "/SF100/" + this.projectId + "_" + projectName + "/lib/")).listFiles();
        File[] var3 = files;
        int var4 = files.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];
            if (file.isFile() && file.getName().endsWith(".jar")) {
                String address = String.format("%s/SF100/" + this.projectId + "_" + projectName + "/lib/" + file.getName(), MetaGA.address);
                results.add(address);
            }
        }

        String listString = String.join(":", results);
        return listString;
    }

    public ArrayList<String> getClassList() throws IOException {
        System.out.println(full_set);
        System.out.println(projectName);
        if (full_set) {
            Properties.CP = MetaGA.address;
            this.classList = new ArrayList();
            Set<String> classes = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getAllClasses("SF100/" + this.projectId + "_" + projectName + "/" + projectName + ".jar", false);
            Iterator var2 = classes.iterator();

            while(var2.hasNext()) {
                String sut = (String)var2.next();
                if (!ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).isClassAnInterface(sut) && ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).hasClass(sut)) {
                    try {
                        Class.forName(sut, false, TestGenerationContext.getInstance().getClassLoaderForSUT());
                        this.classList.add(sut);
                    } catch (ClassNotFoundException var5) {
                        ;
                    }
                }
            }
        } else {
            this.classList = (ArrayList)((ArrayList)MetaGA.topclasses.get(projectName)).clone();
        }

        this.numberOfClasses = this.classList.size();
        return this.classList;
    }

    public void multiThreadRun() throws InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        int effective_cores = (int)((double)cores * 0.6D);
        ExecutorService executor = Executors.newFixedThreadPool(effective_cores);
        System.out.println("number of cores: " + cores);
        System.out.println("#" + counter + ": Solution under study: " + this.solution.toString());
        long start = System.nanoTime();

        for(int i = 0; i < this.numberOfClasses; ++i) {
            String cmd = String.format("java -jar %s/evosuite-1.0.6.jar -target %s/SF100/%d_%s/%s.jar -class %s -Dcrossover_rate=%f -Dpopulation=%d -Dselection_function=%s -Dshow_progress=False -Delite=%d -Dsearch_budget=120 -Dminimize=false -criterion branch -Dwrite_individuals=true -Dparent_check=%s -Doutput_variables=TARGET_CLASS,BranchCoverage,MutationScore,Total_Goals,Covered_Goals,Mutants,Total_Branches,Covered_Branches,Total_Time -Dreport_dir=%s/results/limited/%s/solution%d/class%d", MetaGA.address, MetaGA.address, this.projectId, projectName, projectName, this.classList.get(i), this.solution.getCrossover_rate(), this.solution.getPopulation(), this.solution.getSelection(), this.solution.getElitism(), this.solution.isParent_replacement(), MetaGA.address, projectName, counter, i + 1);
            if (this.solution.index[3] == 1) {
                cmd = cmd + " -Dtournament_size=2";
            }

            if (this.solution.index[3] == 2) {
                cmd = cmd + " -Dtournament_size=7";
            }

            if (this.solution.index[3] == 3) {
                cmd = cmd + " -Drank_bias=1.2";
            }

            cmd = cmd + " -projectCP " + this.getDependencies();
            if (full_set) {
                cmd = cmd + String.format(" -Dreport_dir=%s/results/limited/%s/final_solution%d/class%d", MetaGA.address, projectName, counter, i + 1);
            } else {
                cmd = cmd + String.format(" -Dreport_dir=%s/results/limited/%s/solution%d/class%d", MetaGA.address, projectName, counter, i + 1);
            }

            System.out.println("Time to evaluate class#" + (i + 1) + ": " + (String)this.classList.get(i));
            System.out.println(cmd);
            Runnable worker = new Worker(cmd, i + 1);
            executor.execute(worker);
        }

        executor.shutdown();

        try {
            executor.awaitTermination((long)(this.numberOfClasses * 3), TimeUnit.MINUTES);
        } catch (InterruptedException var12) {
            var12.printStackTrace();
        }

        long end = System.nanoTime();
        long duration = (end - start) / 1000000000L;
        long duration_in_min = duration / 60L;
        Thread.sleep(60000L);
        System.out.println("This solution is finished after " + duration_in_min + " minutes");
    }

    public double[] getResult() throws IOException, InterruptedException {
        this.multiThreadRun();
        double[] result = this.readFromFile();
        ++counter;
        return result;
    }

    public void limitSearch() {
        ArrayList<Double> std = this.getAbsoluteDeviations();
        ArrayList<Double> std_sorted = (ArrayList)std.clone();
        HashSet<String> classes = new HashSet();
        Collections.sort(std_sorted);
        Collections.reverse(std_sorted);

        for(int i = 0; i < Math.round((float)(this.classList.size() / 10)); ++i) {
            for(int j = 0; j < std.size(); ++j) {
                if (((Double)std.get(j)).equals(std_sorted.get(i))) {
                    classes.add(this.classList.get(j));
                }
            }
        }

        MetaGA.classList.clear();
        Iterator var6 = classes.iterator();

        String c;
        while(var6.hasNext()) {
            c = (String)var6.next();
            MetaGA.classList.add(c);
        }

        System.out.println("New classlist set: ");
        var6 = this.classList.iterator();

        while(var6.hasNext()) {
            c = (String)var6.next();
            System.out.println(c);
        }

    }

    public ArrayList<Double> getAbsoluteDeviations() {
        ArrayList<Double> abd = new ArrayList();

        for(int j = 0; j < this.classList.size(); ++j) {
            ArrayList<Integer> covered_goals = new ArrayList();
            double sum = 0.0D;
            String s = String.format("let's see what is the variation of class number %d which is %s", j + 1, this.classList.get(j));
            System.out.println(s);

            int max;
            for(max = 0; max < 50; ++max) {
                String strFile = String.format("%s/results/limited/%s/repetition%d/solution%d/class%d/statistics.csv", MetaGA.address, projectName, 1, max + 1, j + 1);
                CSVReader reader = null;

                try {
                    reader = new CSVReader(new FileReader(strFile));
                    int lineNumber = 0;

                    String[] nextLine;
                    while((nextLine = reader.readNext()) != null) {
                        ++lineNumber;
                        if (lineNumber != 1) {
                            covered_goals.add(Integer.parseInt(nextLine[4]));
                        }
                    }

                    reader.close();
                } catch (Exception var12) {
                    System.out.println("this file is not found for std evaluation at " + strFile);
                }
            }

            if (covered_goals.size() == 0) {
                abd.add(-1.0D);
            } else {
                max = 0;
                Iterator var13 = covered_goals.iterator();

                int num;
                while(var13.hasNext()) {
                    num = (Integer)var13.next();
                    if (num > max) {
                        max = num;
                    }
                }

                for(var13 = covered_goals.iterator(); var13.hasNext(); sum += (double)(max - num)) {
                    num = (Integer)var13.next();
                }

                double avg = sum / (double)covered_goals.size();
                abd.add(avg);
                System.out.println("it's standard deviation of covered goals is " + avg);
            }
        }

        return abd;
    }

    public double[] readFromFile() throws IOException {
        int total_goals = 0;
        int covered_goals = 0;
        int killed_mutants = 0;
        int total_mutants = 0;

        for(int i = 0; i < this.numberOfClasses; ++i) {
            String strFile = "";
            if (full_set) {
                strFile = String.format("%s/results/limited/%s/final_solution%d/class%d/statistics.csv", MetaGA.address, projectName, counter, i + 1);
            } else {
                strFile = String.format("%s/results/limited/%s/solution%d/class%d/statistics.csv", MetaGA.address, projectName, counter, i + 1);
            }

            CSVReader reader = null;

            try {
                reader = new CSVReader(new FileReader(strFile));
                int lineNumber = 0;

                String[] nextLine;
                while((nextLine = reader.readNext()) != null) {
                    ++lineNumber;
                    if (lineNumber != 1) {
                        total_goals += Integer.parseInt(nextLine[3]);
                        covered_goals += Integer.parseInt(nextLine[4]);
                        total_mutants += Integer.parseInt(nextLine[5]);
                        killed_mutants = (int)((long)killed_mutants + Math.round(Double.parseDouble(nextLine[2]) * (double)Integer.parseInt(nextLine[5])));
                    }
                }

                reader.close();
            } catch (FileNotFoundException var10) {
                System.out.println("this file is not found for coverage calculation at " + strFile);
            }
        }

        double[] result = new double[]{(double)covered_goals / (double)total_goals, (double)killed_mutants / (double)total_mutants};
        return result;
    }
}
