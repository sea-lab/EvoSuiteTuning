
import java.io.IOException;
import java.util.ArrayList;

public class Exhaustive {
    public Exhaustive() {
    }

    public static void main(String[] args) {
        String project_name = args[0];
        int start = Integer.parseInt(args[1]);
        int end = Integer.parseInt(args[2]);
        ArrayList<Solution> candidates = new ArrayList();

        int cross;
        for(cross = 0; cross < Solution.cross.length; ++cross) {
            for(int pop = 0; pop < Solution.pop.length; ++pop) {
                for(int elite = 0; elite < Solution.elite.length; ++elite) {
                    for(int selection = 0; selection < Solution.selection_set.length; ++selection) {
                        for(int parent = 0; parent < Solution.parent_check.length; ++parent) {
                            int[] p = new int[]{cross, pop, elite, selection, parent};
                            Solution s = new Solution(p);
                            candidates.add(s);
                        }
                    }
                }
            }
        }

        RunOnEvoSuite.counter = start + 1;

        for(cross = start; cross < end; ++cross) {
            try {
                RunOnEvoSuite r = new RunOnEvoSuite((Solution)candidates.get(cross), project_name);
                r.multiThreadRun();
                ++RunOnEvoSuite.counter;
            } catch (IOException var12) {
                var12.printStackTrace();
            } catch (InterruptedException var13) {
                var13.printStackTrace();
            }
        }

    }
}
