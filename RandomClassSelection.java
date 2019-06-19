File file = new File("./SF110/");
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        ArrayList<String> dirs = new ArrayList<>(Arrays.asList(directories));
        ArrayList<String> selected_projects = new ArrayList<>();

        int[] default_values = { 3, 2, 1, 3, 0 };
        Solution ss = new Solution(default_values);

        int num_of_project =0;

        File csv = new File("./classlist.csv");
        FileWriter outputfile = new FileWriter(csv);




        while (num_of_project<10){
            //select a random project
            Random random = new Random();
            int project_id = random.nextInt(110);
            String project_name = dirs.get(project_id);

            //get project size
            RunOnEvoSuite run1 = new RunOnEvoSuite(ss, project_name);
            int proj_size = run1.classList.size();

            ArrayList<String> selected_classes = new ArrayList<>();
            ArrayList<String> tried_classes = new ArrayList<>();


            //while 5 classes selected
            int num_of_classes = 0;
            while (num_of_classes<5){

                // select a random class
                int class_id = random.nextInt(proj_size);
                String name_of_class = run1.classList.get(class_id);

                // check if it is already checked
                if (!tried_classes.contains(name_of_class)){
                    // check if it is a good one: not an exception
                    if (name_of_class.contains("Exception")){
                        //go to next class
                    }
                    else {
                        // run evosuite on it and check if it is not trivial (coverage 100%)
                        double coverage = 0;
                        String cmd = String.format("java -jar %s/evosuite-1.0.6.jar -target %s/SF110/%s/%s.jar -class %s" +
                                " -Dshow_progress=False  -Dsearch_budget=120 -Dminimize=false -criterion branch" +
                                " -Doutput_variables=TARGET_CLASS,BranchCoverage,MutationScore,Total_Goals,Covered_Goals,Mutants,Total_Branches,Covered_Branches,Total_Time" +
                                " -Dreport_dir=%s/results/%s",RunOnEvoSuite.address,RunOnEvoSuite.address,project_name,
                                project_name,name_of_class,RunOnEvoSuite.address,project_name);
                        cmd = cmd + " -projectCP " + run1.getDependencies();

                        Runtime rt = Runtime.getRuntime();
                        Process pr = rt.exec(cmd);
                        pr.waitFor();
                        Thread.sleep(15000);

                        String read =String.format("%s/results/%s/statistics.csv", RunOnEvoSuite.address, project_name);
                        CSVReader reader = new CSVReader(new FileReader(read));

                        String[] nextLine;
                        while((nextLine = reader.readNext()) != null) {
                                if (nextLine[0].equals(name_of_class)) {
                                    coverage = Double.parseDouble(nextLine[1]);
                                    break;
                                }
                        }
                        reader.close();



                        if (coverage<1){
                            selected_classes.add(name_of_class);
                            num_of_classes++;
                        }
                        else {
                            tried_classes.add(name_of_class);
                        }


                    }

                }
                else if (tried_classes.size() == proj_size){
                    // check if there is no more classes compared to size of the project
                    //go next project
                    break;

                }
            }
            if (num_of_classes==5){
                //write the project and classes in the csv file
                String  strList = String.join(",",  selected_classes);
                strList+="\n";
                strList=project_name+","+strList;
                System.out.println(strList);
                outputfile.write(strList);
                outputfile.flush();
                selected_projects.add(project_name);
                num_of_project++;
            }
            if (num_of_project==10){
                outputfile.close();
            }
        }
