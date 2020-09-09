package it.polimi.middleware.akkaProject.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import it.polimi.middleware.akkaProject.dataStructures.SavedData;
import it.polimi.middleware.akkaProject.messages.ForwardGetData;
import it.polimi.middleware.akkaProject.messages.GetData;
import it.polimi.middleware.akkaProject.messages.PutNewData;
import it.polimi.middleware.akkaProject.messages.UpdateRoutersList;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        String masterIp = "127.0.0.1"; //cambiare con ip del Master
        final Config config = ConfigFactory //
                .parseFile(new File("conf/client.conf")); //
        //.withValue("akka.remote.classic.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(Inet4Address.getLocalHost().getHostAddress())); //da cambiare con ip che altri utilizzano per contattare questo

        final ActorSystem sys = ActorSystem.create("Client", config);
        ActorRef client = sys.actorOf(ClientActor.props(masterIp));
        Scanner scanner = new Scanner(System.in);

        String key = "";

        while (!key.equals("exit")) {
            key = scanner.nextLine();
            switch (key) {
                case "updateRouters":
                    client.tell(new UpdateRoutersList(), ActorRef.noSender());
                    break;
                case "put":
                    String data = scanner.nextLine();
                    client.tell(new PutNewData(key, new SavedData(data, ZonedDateTime.now())), ActorRef.noSender());
                    break;
                case "get":
                    client.tell(new ForwardGetData(key), ActorRef.noSender());
            }
        }
    }
}
