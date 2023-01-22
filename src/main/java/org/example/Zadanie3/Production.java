package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Production extends AbstractBehavior<Production.Commands> {

    private ActorRef<Warehouse.WarehouseCommands>warehouse;
    private ActorRef<WinePress.WinePressCommands>winePress;
    private ActorRef<Fermentation.FermentationCommands>fermentation;

    public interface Commands {};

    private Production(ActorContext<Commands> context) {
        super(context);
    }

    public static Behavior<Commands> create(){
        return Behaviors.setup(Production::new);
    }


    public static class InitializeProduction implements Commands, WinePress.WinePressCommands, Warehouse.WarehouseCommands, Fermentation.FermentationCommands {

        public final ActorRef<Warehouse.WarehouseCommands>warehouse;
        public final ActorRef<WinePress.WinePressCommands>winePress;
        public final ActorRef<Fermentation.FermentationCommands>fermentation;

        public InitializeProduction(ActorRef<Warehouse.WarehouseCommands>warehouse
        ,ActorRef<WinePress.WinePressCommands>winePress, ActorRef<Fermentation.FermentationCommands>fermentation){

            this.warehouse= warehouse;
            this.winePress=winePress;
            this.fermentation=fermentation;
        }
    }


    public static class StartProduction implements Commands{

        public final double grapes;
        public final double water;
        public final double sugar;
        public final int bottles;
        public final double timeModifier;


        public StartProduction(double grapes, double water, double sugar, int bottles, double timeModifier){
            this.grapes=grapes;
            this.water =water;
            this.sugar=sugar;
            this.bottles=bottles;
            this.timeModifier=timeModifier;

        }
    }
    private Behavior<Commands> onStartProduction(StartProduction productionParameters){

        warehouse = getContext().spawn(Warehouse.create(productionParameters.grapes,
                productionParameters.water,productionParameters.sugar,productionParameters.bottles),"Warehouse");

        winePress = getContext().spawn(WinePress.create(productionParameters.timeModifier), "Wine Press");

        fermentation = getContext().spawn(Fermentation.create(productionParameters.timeModifier), "Fermentation");


        return this;
    }


    @Override
    public Receive<Commands> createReceive() {
        return newReceiveBuilder().onMessage(StartProduction.class,this::onStartProduction).build();
    }



}
