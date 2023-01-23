package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Production extends AbstractBehavior<Production.Commands> {

    private ActorRef<Production.Commands>warehouse;
    private ActorRef<Production.Commands>winePress;
    private ActorRef<Production.Commands>fermentation;

    public interface Commands {};

    private Production(ActorContext<Commands> context) {
        super(context);
    }

    public static Behavior<Commands> create(){
        return Behaviors.setup(Production::new);
    }

    public enum triggerProduction implements Commands{
        INSTANCE
    } ;


    public static class InitializeProduction implements Commands {

        public final ActorRef<Production.Commands>warehouse;
        public final ActorRef<Production.Commands>winePress;
        public final ActorRef<Production.Commands>fermentation;

        public InitializeProduction(ActorRef<Production.Commands>warehouse
        ,ActorRef<Production.Commands>winePress, ActorRef<Production.Commands>fermentation){

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

        winePress = getContext().spawn(WinePress.create(productionParameters.timeModifier), "WinePress");

        fermentation = getContext().spawn(Fermentation.create(productionParameters.timeModifier), "Fermentation");

        warehouse.tell(new InitializeProduction(warehouse,winePress,fermentation));
        winePress.tell(new InitializeProduction(warehouse,winePress,fermentation));
        fermentation.tell(new InitializeProduction(warehouse,winePress,fermentation));


        warehouse.tell(triggerProduction.INSTANCE);
        winePress.tell(triggerProduction.INSTANCE);
        fermentation.tell(triggerProduction.INSTANCE);


        return this;
    }


    @Override
    public Receive<Commands> createReceive() {
        return newReceiveBuilder().onMessage(StartProduction.class,this::onStartProduction).build();
    }



}
