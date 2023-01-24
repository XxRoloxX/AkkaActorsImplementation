package org.example.Zadanie3;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class Filtration extends AbstractBehavior<Production.Commands> {
    private static final double REQUIRED_UNFILTERED_WINE=25;

    private static final double FILTERED_WINE_OUTPUT=24;

    private static final double FAILURE_PROPABILITY = 0.05;

    private static final double TIME_TO_PRODUCE = 12*14;

    private double amountOfUnfilteredWine;

    private double amountOfFilteredWine;

    private ArrayList<ActorRef<Production.Commands>>fermentationStations;
    private ArrayList<ActorRef<Production.Commands>>bottlingStations;

    private boolean occupied;
    private double timeModifier;

    private HashMap<ActorRef<Production.Commands>,FilteredWineTransferResponse> reservedResources;
    private Filtration(ActorContext<Production.Commands> context, double timeModifier) {
        super(context);
        amountOfFilteredWine=0;
        amountOfUnfilteredWine=0;
        occupied=false;
        this.timeModifier=timeModifier;
        reservedResources = new HashMap<>();

    }


    public boolean isOccupied() {
        return occupied;
    }

    //public interface FermentationCommands {};




    public static Behavior<Production.Commands> create(double timeModifierArg){
        return Behaviors.setup(context-> new Filtration(context,timeModifierArg));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Production.ReportState.INSTANCE,this::onReportState)
                .onMessageEquals(Production.triggerProduction.INSTANCE,this::onTriggerProduction)
                .onMessage(FilteredWineTransferAcknowledgement.class,this::onFilteredWineTransferAcknowledgement)
                .onMessage(FilteredWineTransferRequest.class, this::onFilteredWineTransferRequest)
                .onMessage(Fermentation.UnfilteredWineTransferResponse.class,this::onUnfilteredWineTransferResponse)
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .build();
    }

    private Behavior<Production.Commands> onTriggerProduction(){


        for(ActorRef<Production.Commands> fermentation: fermentationStations){
            fermentation.tell(Production.triggerProduction.INSTANCE);
        }


        for(ActorRef<Production.Commands> bottling: bottlingStations){
            bottling.tell(Production.triggerProduction.INSTANCE);
        }
       // onReportState();

        if(!produce()){

            for(ActorRef<Production.Commands> fermentation: fermentationStations){
                fermentation.tell(new Fermentation.UnfilteredWineTransferRequest(getContext().getSelf(), Math.max(REQUIRED_UNFILTERED_WINE-amountOfUnfilteredWine,0)));
            }


        }

        return this;

    }
    private FilteredWineTransferResponse getAllReservedResources(){
        //ResourcesTransferRequest result = new ResourcesTransferRequest(getContext().getSelf(),0,0,0,0);
        FilteredWineTransferResponse element;
        double filteredWine =0;

        Set<ActorRef<Production.Commands>> keys= reservedResources.keySet();
        for(ActorRef<Production.Commands> key: keys){
            element = reservedResources.get(key);
            filteredWine += element.filteredWine;
        }

        return new FilteredWineTransferResponse(getContext().getSelf(),filteredWine);
    }

    private Behavior<Production.Commands> onUnfilteredWineTransferResponse(Fermentation.UnfilteredWineTransferResponse commands){
        amountOfUnfilteredWine+=commands.unfilteredWine;
        commands.from.tell(new Fermentation.UnfilteredWineTransferAcknowledgement(getContext().getSelf(), commands.unfilteredWine));
        getContext().getLog().info("Received Unfiltered Wine Transfer Response: {}", commands);
        return this;
    }
    private Behavior<Production.Commands> onFilteredWineTransferRequest(FilteredWineTransferRequest commands){
        /*
        reservedResources.put(commands.from, new FilteredWineTransferResponse(commands.from,0));

        FilteredWineTransferResponse loanedResources = new FilteredWineTransferResponse(
                getContext().getSelf(), Math.min(Math.max(amountOfFilteredWine- getAllReservedResources().filteredWine,0), commands.filteredWine));


        commands.from.tell(loanedResources);
        reservedResources.put(commands.from,loanedResources);
        getContext().getLog().info("Received Filtered Wine Transfer Request: {}", commands);

         */

        FilteredWineTransferResponse loanedResources = new FilteredWineTransferResponse(
                getContext().getSelf(), Math.min(amountOfFilteredWine, commands.filteredWine));

        if(loanedResources.filteredWine>0){
            amountOfFilteredWine-= loanedResources.filteredWine;
            commands.from.tell(loanedResources);
        }

        getContext().getLog().info("Received Filtered Wine Transfer Request: {}", commands);
        return this;

    }
    private Behavior<Production.Commands>onFilteredWineTransferAcknowledgement(FilteredWineTransferAcknowledgement commands){

        //amountOfFilteredWine -= reservedResources.get(commands.from).filteredWine;
        //reservedResources.put(commands.from, new FilteredWineTransferResponse(commands.from,0));
        getContext().getLog().info("Received Filtered Wine Transfer Acknowledgement: {}", commands);
        return this;
    }
    public boolean produce(){
        Random rand = new Random();
        if(amountOfUnfilteredWine>=REQUIRED_UNFILTERED_WINE && !occupied){
            occupied=true;

            Utils.waitFor((int)(TIME_TO_PRODUCE*1000*timeModifier));


            amountOfUnfilteredWine-=REQUIRED_UNFILTERED_WINE;

            if(rand.nextDouble()>FAILURE_PROPABILITY){
                amountOfFilteredWine+=FILTERED_WINE_OUTPUT;
            }
            //onReportState();
            occupied=false;

            return true;

        }else{
            return false;
        }
    }

    public static class FilteredWineTransferRequest implements Production.Commands {
        public final ActorRef<Production.Commands>from;
        public final double filteredWine;

        public FilteredWineTransferRequest(ActorRef<Production.Commands> from, double filteredWine){
            this.from =from;
            this.filteredWine=filteredWine;
        }

        @Override
        public String toString() {
            return "FilteredWineTransferRequest{" +
                    "from=" + from +
                    ", filteredWine=" + filteredWine +
                    '}';
        }
    }
    public static class FilteredWineTransferResponse extends FilteredWineTransferRequest{
        public FilteredWineTransferResponse(ActorRef<Production.Commands> from, double filteredWine){
            super(from,filteredWine);
        }
    }

    public static class FilteredWineTransferAcknowledgement extends FilteredWineTransferRequest{
        public FilteredWineTransferAcknowledgement(ActorRef<Production.Commands> from, double filteredWine){
            super(from,filteredWine);
        }
    }

    private Behavior<Production.Commands>onInitializeProduction(Production.InitializeProduction commands){
        fermentationStations= commands.fermentationStations;
        bottlingStations = commands.bottlingStations;
        return this;
    }

    private Behavior<Production.Commands>onReportState(){
        System.out.println("Filtration Station Resources: ");
        System.out.println("Filtered Wine: "+amountOfFilteredWine);
        System.out.println("Unfiltered Wine: "+amountOfUnfilteredWine);

        return this;

    }







}
