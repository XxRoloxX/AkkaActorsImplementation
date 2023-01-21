package org.example.Zadanie1;

import java.util.Date;
import java.util.Random;

public class ValueModifier implements Runnable{

    private final int lifeTime;

    public ValueModifier(int lifeTime){
        this.lifeTime=lifeTime;
    }

    @Override
    public void run() {

        Random rand = new Random();
        Date date = new Date();
        long startTime = date.getTime();

        while(date.getTime()<startTime+lifeTime){
            date = new Date();

            if(rand.nextInt(2)==1){
                ValueContainer.increment();
            }else{
                ValueContainer.decrement();
            }
        }
    }
}
