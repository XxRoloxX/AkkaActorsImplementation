package org.example;

public class Utils {

    static double boundaryConstrains(double mainValue, double subtractor){
        if(mainValue>subtractor){
            return subtractor;
        }else{
            return mainValue;
        }
    }

}
