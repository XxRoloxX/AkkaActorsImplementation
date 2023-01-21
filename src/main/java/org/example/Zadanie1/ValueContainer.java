package org.example.Zadanie1;

public class ValueContainer {
    public static int globalValue;

    public ValueContainer(){
        globalValue=0;
    }

    public static synchronized int getGlobalValue() {
        return globalValue;
    }

    public static synchronized void setGlobalValue(int globalValue) {
        ValueContainer.globalValue = globalValue;
    }

    public static synchronized void increment(){
        System.out.println("Entered increment: ");
        globalValue+=1;
        System.out.println("Left increment: ");

    }
    public static synchronized void decrement(){

        System.out.println("Entered decrement: ");
        globalValue-=1;
        System.out.println("Left decrement: ");;
    }


}
