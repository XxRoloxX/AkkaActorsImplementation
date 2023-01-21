package org.example.Zadanie1;

import java.util.Date;
import java.util.Random;

public class Main {

    public static void modifyGlobalValue(int n, int time){

        Thread[] threads = new Thread[n];

        for(int i=0;i<n;i++){
            threads[i] = new Thread(new ValueModifier(time));
        }

        for(Thread t: threads){
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
        }catch (InterruptedException e){
            System.out.println(e);
        }


        System.out.println("Done with incrementing/decrementing");
        System.out.println("Final value: "+ValueContainer.getGlobalValue());

    }

    public static void main(String[] arg){


        modifyGlobalValue(10,3);
        //System.out.println("Done with incrementing/decrementing");
        //System.out.println("Final value: "+ValueContainer.getGlobalValue());

    }

}
