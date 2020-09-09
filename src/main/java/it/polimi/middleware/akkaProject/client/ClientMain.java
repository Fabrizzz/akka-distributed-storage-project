package it.polimi.middleware.akkaProject.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import it.polimi.middleware.akkaProject.dataStructures.DataWithTimestamp;
import it.polimi.middleware.akkaProject.messages.ForwardGetData;
import it.polimi.middleware.akkaProject.messages.PutNewData;
import it.polimi.middleware.akkaProject.messages.UpdateMemberListRequest;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws UnknownHostException {
        String masterIp = "127.0.0.1"; //cambiare con ip del Master
        final Config config = ConfigFactory //
                .parseFile(new File("conf/client.conf")) //
        .withValue("akka.remote.classic.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(Inet4Address.getLocalHost().getHostAddress()));
        // da cambiare con ip che altri utilizzano per contattare questo

        final ActorSystem sys = ActorSystem.create("Client", config);
        ActorRef client = sys.actorOf(ClientActor.props(masterIp));
        Scanner scanner = new Scanner(System.in);

        String instruction = "";

        while (!instruction.equals("exit")) {
            System.out.println("put or get or updateRouters or exit?");
            instruction = scanner.nextLine();
            switch (instruction) {
                case "updateRouters":
                    client.tell(new UpdateMemberListRequest(), ActorRef.noSender());
                    break;
                case "put":
                    System.out.println("Insert key:");
                    String key = scanner.nextLine();
                    System.out.println("Insert data: ");
                    String data = scanner.nextLine();
                    client.tell(new PutNewData(key, new DataWithTimestamp(data, ZonedDateTime.now())), ActorRef.noSender());
                    break;
                case "get":
                    System.out.println("Insert key:");
                    key = scanner.nextLine();
                    client.tell(new ForwardGetData(key), ActorRef.noSender());
            }
        }
    }
}
