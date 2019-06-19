public class GlostTaskCreator{
public GlostTaskCreator(){
}
public static void main(String[] args) throws IOException, InterruptedException {
    File tasks = new File("./tasks.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(tasks);

            // create CSVWriter object filewriter object as parameter
            for (int i=0;i<1200;i++) {
                for (int p = 0; p < 10; p++) {
                    for (int c = 0; c < 5; c++) {
                        for (int rep = 1; rep < 11; rep++) {
                            String t = String.format("java -jar glost.jar %d %d %d %d", p, c, i, rep);
                            t+="\n";
                            System.out.println(t);
                            outputfile.write(t);
                        }
                    }
                }
            }
            outputfile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
 }
 }
