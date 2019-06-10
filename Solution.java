import java.util.Arrays;
import java.util.Random;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.localsearch.LocalSearchObjective;

public class Solution extends Chromosome {
    private double crossover_rate;
    private int population;
    private int elitism;
    private String selection;
    private boolean parent_replacement;
    public int[] index;
    static String[] selection_set = new String[]{"ROULETTEWHEEL", "TOURNAMENT", "TOURNAMENT", "RANK", "RANK"};
    static boolean[] parent_check = new boolean[]{true, false};
    static double[] cross = new double[]{0.0D, 0.2D, 0.5D, 0.75D, 0.8D, 1.0D};
    static int[] pop = new int[]{4, 10, 50, 100, 200};
    static int[] elite = new int[]{0, 1, 10, 50};
    static int[] size;

    public Solution(int[] param) {
        this.index = param;
    }

    public Chromosome clone() {
        int[] copiedArray = Arrays.copyOf(this.index, this.index.length);
        Solution s = new Solution(copiedArray);
        return s;
    }

    public boolean equals(Object o) {
        Solution oprime = (Solution)o;
        return Arrays.equals(this.index, oprime.index);
    }

    public int hashCode() {
        return 0;
    }

    public <T extends Chromosome> int compareSecondaryObjective(T t) {
        return 0;
    }

    public void mutate() {
        int random = (new Random()).nextInt(this.index.length);
        int random2 = (new Random()).nextInt(size[random]);
        this.index[random] = random2;
        this.setChanged(true);
    }

    public void crossOver(Chromosome chromosome, int i, int i1) {
        Solution s = (Solution)chromosome;
        int j;
        if (i1 < i) {
            j = i1;
            i1 = i;
            i = j;
        }

        if (i == i1) {
            i1 = this.index.length;
        }

        for(j = i; j < i1; ++j) {
            int temp = this.index[j];
            this.index[j] = s.index[j];
            s.index[j] = temp;
        }

        s.setChanged(true);
        this.setChanged(true);
    }

    public boolean localSearch(LocalSearchObjective<? extends Chromosome> localSearchObjective) {
        return false;
    }

    public int size() {
        return 5;
    }

    public boolean isParent_replacement() {
        this.parent_replacement = parent_check[this.index[4]];
        return this.parent_replacement;
    }

    public double getCrossover_rate() {
        this.crossover_rate = cross[this.index[0]];
        return this.crossover_rate;
    }

    public int getPopulation() {
        this.population = pop[this.index[1]];
        return this.population;
    }

    public String getSelection() {
        this.selection = selection_set[this.index[3]];
        return this.selection;
    }

    public int getElitism() {
        if (this.index[2] == 2) {
            this.elitism = this.getPopulation() / 10;
            if (this.getPopulation() == 4) {
                this.elitism = 1;
            }
        } else if (this.index[2] == 3) {
            this.elitism = this.getPopulation() / 2;
        } else {
            this.elitism = elite[this.index[2]];
        }

        return this.elitism;
    }

    public String toString() {
        String s = "population: " + this.getPopulation() + " crossover rate: " + this.getCrossover_rate() + " elitism: " + this.getElitism() + " selection: " + this.getSelection() + " parent: " + this.isParent_replacement() + " fitness: " + this.getFitness();
        if (this.index[3] == 1) {
            s = s + " tournament size: 2";
        }

        if (this.index[3] == 2) {
            s = s + " tournament size: 7";
        }

        if (this.index[3] == 3) {
            s = s + " rank bias: 1.2";
        }

        if (this.index[3] == 4) {
            s = s + " rank bias: 1.7";
        }

        return s;
    }

    static {
        size = new int[]{cross.length, pop.length, elite.length, selection_set.length, parent_check.length};
    }
}
