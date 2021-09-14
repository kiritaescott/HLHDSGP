package ec.app.bcdsgp;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class RatioGenerator {

    public static void main (String [] args){

        ArrayList <Double> small = new ArrayList<>();
        ArrayList <Double> medium = new ArrayList<>();
        ArrayList <Double> large = new ArrayList<>();
        ArrayList <Double> xlarge = new ArrayList<>();

        boolean allFull = false;

        for (int i=0; i < 200; i++){
            if (allFull){ break; }

            double num = Math.random();

            if (num > 0 && num <= 0.275){
                if (small.size() < 2){
                    small.add(num);
                }
            }
            else if (num > 0.275 && num <= 0.55) {
                if (medium.size() < 2) {
                    medium.add(num);
                }
            }
            else if (num > 0.55 && num <= 0.825) {
                if (large.size() < 2) {
                    large.add(num);
                }
            }
            else if (num > 0.825) {
                if (xlarge.size() < 2) {
                    xlarge.add(num);
                }
            }

            if (small.size()==2 && medium.size()==2 && large.size()==2 && xlarge.size()==2){
                System.out.println("All sorted");
                allFull = true;
            }
        }

        for (int j=0; j < small.size(); j++){
            System.out.println(j + " : " + small.get(j));
        }

        for (int j=0; j < medium.size(); j++){
            System.out.println(j + " : " + medium.get(j));
        }

        for (int j=0; j < large.size(); j++){
            System.out.println(j + " : " + large.get(j));
        }

        for (int j=0; j < xlarge.size(); j++){
            System.out.println(j + " : " + xlarge.get(j));
        }
    }
}
