package org.workflowsim.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Scanner;

public class AnalyseOutput {

    public static void main(String [] args){
        File[] statFiles = getStatFileList();

        for (File stf : statFiles){
            Scanner sc = null;
            double countTS, countVS, countET, countWT, countRFT, countECT=0;

            try {
                sc = new Scanner(stf);
                while (sc.hasNextLine()){


                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }



    /** Return the list of files in either the training or testing set */
    public static File[] getStatFileList() {
        String path = "/Users/koe/Documents/Gitlab-Thesis/thesis/Conferences/IEEECLOUD2020/wfs-dsgp-results/results";

        File fd = new File(path);
        return fd.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".stat");
            }
        });
    }
}
