package it.polimi.middleware.akkaProject;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;


import java.util.Optional;


public class MyActor extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(MyActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(myMessage.class, this::handleMessage)
                .matchAny(m -> log.info("I just received {}", m))
                .build();
    }

    private void handleMessage(myMessage message){
        log.error("Error {}", message);
        log.info("info {}", message);
        log.warning("warning {}", message);
        log.debug("debug {}", message);
    }


    @Override
    public void preRestart(Throwable reason, Optional<Object> message) {
        log.error(
                reason,
                "Restarting due to [{}] when processing [{}]",
                reason.getMessage(),
                message.orElse(""));
    }


    static class myMessage{
        private final String message;

        public myMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}