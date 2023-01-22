package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Date;
import java.util.Random;

public class WinePress extends AbstractBehavior<WinePress.WinePressCommands> {

    private static final double REQUIRED_GRAPES=15;
    private static final double GRAPE_JUICE_OUTPUT=10;

    private static final double FAILURE_PROPABILITY = 0;

    private static final double TIME_TO_PRODUCE = 12;
    private WinePress(ActorContext<WinePress.WinePressCommands> context, double timeModifier) {
        super(context);
        amountOfGrapeJuice=0;
        amountOfGrapes=0;
        occupied=false;
        this.timeModifier=timeModifier;
    }

    private double amountOfGrapes;

    private double amountOfGrapeJuice;

    private boolean occupied;

    private double timeModifier;

    private ActorRef<Warehouse.WarehouseCommands>warehouse;
    private ActorRef<Fermentation.FermentationCommands>fermentation;
    public boolean isOccupied() {
        return occupied;
    }

    public interface WinePressCommands extends Production.Commands {};

    public enum ReportState implements WinePressCommands {
        INSTANCE;
    }


    public static Behavior<WinePress.WinePressCommands> create(double timeModifierArg){
        return Behaviors.setup(context-> new WinePress(context,timeModifierArg));
    }

    @Override
    public Receive<WinePress.WinePressCommands> createReceive() {
        return newReceiveBuilder()
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .onMessageEquals(ReportState.INSTANCE,this::onReportState)
                .onMessage(GrapeJuiceTransferRequest.class,this::onGrapeJuiceTransportRequest)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onGrapeTransportResponse)
                .build();
    }
    public boolean produce(){
        Random rand = new Random();
        if(amountOfGrapes>=REQUIRED_GRAPES && !occupied){
            occupied=true;

            try{
                Thread.sleep((long) (TIME_TO_PRODUCE*3600*timeModifier));
            }catch(InterruptedException e){
                System.out.println(e);
            }
            occupied=false;
            amountOfGrapes-=REQUIRED_GRAPES;

            if(rand.nextDouble()>FAILURE_PROPABILITY){
                amountOfGrapeJuice+=GRAPE_JUICE_OUTPUT;
            }

            return true;

        }else{
            return false;
        }
    }

    private Behavior<WinePress.WinePressCommands>onReportState(){
        System.out.println("Resources: ");
        System.out.println("Grapes: "+amountOfGrapes);
        System.out.println("Grape Juice: "+amountOfGrapeJuice);
        return this;

    }

    public void startProduction(){
        while(true){
            if(!produce()){
                warehouse.tell(new Warehouse.ResourcesTransferRequest(getContext().getSelf(), REQUIRED_GRAPES-amountOfGrapes,0,0,0))
            }
        }
    }



    private Behavior<WinePress.WinePressCommands> onGrapeJuiceTransportRequest(GrapeJuiceTransferRequest commands){

        commands.from.tell(new GrapeJuiceTransferResponse(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice)));
        amountOfGrapes -= Math.min(commands.grapeJuice, amountOfGrapeJuice);

        return this;

    }
    private Behavior<WinePress.WinePressCommands> onGrapeTransportResponse(Warehouse.ResourcesTransferResponse commands){

        amountOfGrapes += Math.min(commands.grapes, amountOfGrapes);

        return this;

    }

    private Behavior<WinePress.WinePressCommands> onInitializeProduction(Production.InitializeProduction commands){
        warehouse= commands.warehouse;
        fermentation= commands.fermentation;
        return this;
    }

    public static class GrapeJuiceTransferResponse implements WinePressCommands , Fermentation.FermentationCommands {
        public final double grapeJuice;
        public final ActorRef<WinePress.WinePressCommands> from;


        public GrapeJuiceTransferResponse(ActorRef<WinePress.WinePressCommands> from, double grapeJuice){
            this.grapeJuice=grapeJuice;
            this.from = from;

        }


    }

    public static class GrapeJuiceTransferRequest implements WinePressCommands , Fermentation.FermentationCommands {
        public final double grapeJuice;
        public final ActorRef<Fermentation.FermentationCommands> from;


        public GrapeJuiceTransferRequest(ActorRef<Fermentation.FermentationCommands> from, double grapeJuice){
            this.grapeJuice=grapeJuice;
            this.from = from;

        }


    }



}
