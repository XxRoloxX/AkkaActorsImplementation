package org.example;

import java.util.Date;

public class Utils {

    static double boundaryConstrains(double mainValue, double subtractor){
        if(mainValue>subtractor){
            return subtractor;
        }else{
            return mainValue;
        }
    }

    public static void waitFor(int miliseconds){
        Date date = new Date();
        long startTime = date.getTime();
        while(date.getTime()<startTime+miliseconds){
            date = new Date();
        }

    }

}
