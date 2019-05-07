import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import com.opencsv.CSVReader;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.localsearch.LocalSearchObjective;


public class Solution extends Chromosome{
    private double crossover_rate;
    private int population;
    private int elitism;
    private String selection;
    private String parent_replacement;
    private int[] index;
    static int count=0;
    static String[] selection_set= {"ROULETTEWHEEL","TOURNAMENT","RANK", "BINARY_TOURNAMENT"};
    static String [] replace = {"FITNESSREPLACEMENT","DEFAULT"};
    static double [] cross = {0,0.2,0.5,0.8,1};
    static int[] pop = {10,20,50,100,200};
    static int[] elite= {0,1,10,50};
    static int size[] = {cross.length,pop.length,elite.length,selection_set.length,replace.length};






    public Solution(int[] param){
        index=param;
        crossover_rate = cross[index[0]];
        population=pop[index[1]];
        if (index[2]==2){
            elitism = (int)0.1*population;
        }
        else if (index[2]==3) {
            elitism =(int)0.5*population;
        }
        else {
            elitism=elite[index[2]];
        }
        selection=selection_set[index[3]];
        parent_replacement=replace[index[4]];
    }
    public void runEvosuite() throws IOException, InterruptedException { //fitness
        count++;
        Runtime rt = Runtime.getRuntime();
        String cmd = String.format("java -jar /home/ubuntu/evosuite-1.0.6.jar -target /home/ubuntu/SF100/1_tullibee/tullibee.jar" +
                        " -Dcrossover_rate=%f" +
                        " -Dpopulation=%d -Dselection_function=%s -Dshow_progress=False" +
                        " -criterion branch -Dsearch_budget=120 -Delite=%d -Dreplacement_function=%s" +
                        " -Doutput_variables=TARGET_CLASS,BranchCoverage,MutationScore" +
                        " -Dreport_dir=/home/ubuntu/results/metaga/%d",
                crossover_rate, population, selection, elitism,parent_replacement, count);
        Process pr = rt.exec(cmd);
        pr.waitFor();
    }

    public double getBranchCoverage(){
        try {
            String strFile = String.format("/home/ubuntu/results/metaga/%d/statistics.csv",count);
            CSVReader reader = new CSVReader(new FileReader(strFile));
            String[] nextLine;
            int lineNumber = 0;
            double b_coverage=0;
            while ((nextLine = reader.readNext()) != null) {
                lineNumber++;
                if (lineNumber!=1){
                    b_coverage = Double.parseDouble(nextLine[1]);
                }
                // nextLine[] is an array of values from the line
            }
            return b_coverage/lineNumber;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }

    @Override
    public Chromosome clone() {
            return new Solution(this.index);
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public <T extends Chromosome> int compareSecondaryObjective(T t) {
        return 0;
    }

    @Override
    public void mutate() {
        int random = new Random().nextInt(index.length); //which parameter to change
        int random2 = new Random().nextInt(size[random]); //which is going to be assigned
        index[random]=random2;
    }

    @Override
    public void crossOver(Chromosome chromosome, int i, int i1) throws ConstructionFailedException {
        Solution s =(Solution)chromosome;
        if (i1<i){
            int x = i1;
            i1= i;
            i=i1;
        }
        if (i==i1){
            i1=this.index.length;
        }

        for (int j =i;j<i1;j++) {
            int temp = this.index[j];
            this.index[j] = s.index[j];
            s.index[j]=temp;
        }
    }

    @Override
    public boolean localSearch(LocalSearchObjective<? extends Chromosome> localSearchObjective) {
        return false;
    }

    @Override
    public int size() {
        return 5;
    }
}
