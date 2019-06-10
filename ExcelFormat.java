
import com.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;

public class ExcelFormat {
    double[][] covered_branches;
    ArrayList<String> classes;
    public double maximumc;
    public double minimumc;
    public double averagec;
    public double defaultc;
    public double medianc;
    public double stdc;

    public ExcelFormat() {
    }

    public ArrayList<String> getClassList(String project) throws IOException {
        Properties.CP = MetaGA.address;
        ArrayList<String> classList = new ArrayList();
        String[] splited = project.split("_");
        Set<String> classes = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getAllClasses("SF100/" + splited[0] + "_" + splited[1] + "/" + splited[1] + ".jar", false);
        Iterator var5 = classes.iterator();

        while(var5.hasNext()) {
            String sut = (String)var5.next();
            if (!ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).isClassAnInterface(sut) && ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).hasClass(sut)) {
                try {
                    Class.forName(sut, false, TestGenerationContext.getInstance().getClassLoaderForSUT());
                    classList.add(sut);
                } catch (ClassNotFoundException var8) {
                    ;
                }
            }
        }

        return classList;
    }

    public void fillArray(String project, int repetition) throws IOException {
        this.classes = this.getClassList(project);
        this.covered_branches = new double[this.classes.size()][1200];
        Iterator var3 = this.classes.iterator();

        while(var3.hasNext()) {
            String s = (String)var3.next();

            for(int j = 1; j <= 1200; ++j) {
                int covered_goals = 0;
                int killed_mutants = 0;
                int counter = 0;

                for(int i = 1; i <= repetition; ++i) {
                    String strFile = String.format("/Users/shayan/Desktop/Project/Exhaustive/%s/%d/merged/merged%d.csv", project, i, j);
                    CSVReader reader = null;

                    try {
                        reader = new CSVReader(new FileReader(strFile));

                        String[] nextLine;
                        while((nextLine = reader.readNext()) != null) {
                            if (s.equals(nextLine[0])) {
                                covered_goals += Integer.parseInt(nextLine[4]);
                                killed_mutants = (int)((long)killed_mutants + Math.round(Double.parseDouble(nextLine[2]) * (double)Integer.parseInt(nextLine[5])));
                                ++counter;
                                break;
                            }
                        }

                        reader.close();
                    } catch (FileNotFoundException var13) {
                        System.out.println("this file is not found for coverage calculation at " + strFile);
                    }
                }

                if (counter != repetition) {
                    System.out.println("for class " + s + ", some of the classes are missing.");
                }

                if (counter == 0) {
                    this.covered_branches[this.classes.indexOf(s)][j - 1] = 0.0D;
                } else {
                    this.covered_branches[this.classes.indexOf(s)][j - 1] = (double)(covered_goals / counter);
                }
            }
        }

    }

    public void processDataSolutionWise() {
        double[] overall_coverage = new double[1200];
        this.maximumc = 0.0D;
        this.minimumc = 0.0D;
        double sumsum = 0.0D;

        int i;
        for(i = 0; i < 1200; ++i) {
            double sum = 0.0D;

            for(int j = 0; j < this.classes.size(); ++j) {
                sum += this.covered_branches[j][i];
            }

            if (sum > this.maximumc) {
                this.maximumc = sum;
            }

            if (i == 0) {
                this.minimumc = sum;
            }

            if (sum < this.minimumc) {
                this.minimumc = sum;
            }

            sumsum += sum;
            overall_coverage[i] = sum;
        }

        this.averagec = sumsum / 1200.0D;
        this.stdc = 0.0D;
        double[] var9 = overall_coverage;
        int var10 = overall_coverage.length;

        for(int var6 = 0; var6 < var10; ++var6) {
            double num = var9[var6];
            this.stdc += Math.pow(num - this.averagec, 2.0D);
        }

        this.stdc = Math.sqrt(this.stdc / 1200.0D);
        this.defaultc = overall_coverage[696];
        Arrays.sort(overall_coverage);
        this.medianc = (overall_coverage[600] + overall_coverage[601]) / 2.0D;
        i = Arrays.asList(overall_coverage).indexOf(this.defaultc);
    }

    static void sum_up_recursive(ArrayList<Integer> numbers, int target, ArrayList<Integer> partial) {
        int s = 0;

        int x;
        for(Iterator var4 = partial.iterator(); var4.hasNext(); s += x) {
            x = (Integer)var4.next();
        }

        if (s == target && partial.size() == 10) {
            System.out.println("sum(" + Arrays.toString(partial.toArray()) + ")=" + target);
        }

        if (s < target && partial.size() <= 10) {
            for(int i = 0; i < numbers.size(); ++i) {
                ArrayList<Integer> remaining = new ArrayList();
                int n = (Integer)numbers.get(i);

                for(int j = i + 1; j < numbers.size(); ++j) {
                    remaining.add(numbers.get(j));
                }

                ArrayList<Integer> partial_rec = new ArrayList(partial);
                partial_rec.add(n);
                sum_up_recursive(remaining, target, partial_rec);
            }

        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int[] default_values = new int[]{3, 2, 1, 3, 0};
        Solution def = new Solution(default_values);
        RunOnEvoSuite r = new RunOnEvoSuite(def, "okio");
        double fitness = r.getResult()[0];
        System.out.println(fitness);
        ExcelFormat x = new ExcelFormat();
        x.fillArray("webmagic", 4);
        x.processDataSolutionWise();
        System.out.println("Maximum:" + x.maximumc);
        System.out.println("Minimum:" + x.minimumc);
        System.out.println("Average:" + x.averagec);
        System.out.println("Median:" + x.medianc);
        System.out.println("Default:" + x.defaultc);
        System.out.println("STD:" + x.stdc);

        for(int i = 0; i < x.classes.size(); ++i) {
            int counter = 0;

            for(int j = 0; j < 1200; ++j) {
                if (x.covered_branches[i][j] == 0.0D) {
                    ++counter;
                }
            }

            if (counter == 1200) {
                System.out.println("class " + (String)x.classes.get(i) + " has problem");
            }
        }

    }
}
