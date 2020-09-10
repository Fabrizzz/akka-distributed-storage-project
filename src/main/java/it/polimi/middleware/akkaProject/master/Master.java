package it.polimi.middleware.akkaProject.master;


import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Collections;

public class Master
{
    public static void main( String[] args ) throws UnknownHostException {
        final Config config = ConfigFactory //
                .parseFile(new File("conf/cluster.conf")) //
                .withValue("akka.remote.classic.netty.tcp.port", ConfigValueFactory.fromAnyRef(1234)) //
                .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Collections.singletonList("master")));
                //.withValue("akka.remote.classic.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(Inet4Address.getLocalHost().getHostAddress()));
                //todo da cambiare con Ip pubblico del Master (o local in caso di Lan)

        System.out.println("This is the Ip of the master: " + Inet4Address.getLocalHost().getHostAddress());


        int numberOfNodes = config.getInt("akka.cluster.role.server.min-nr-of-members");
        int numberOfPartitions = config.getInt("numberOfPartitions");
        int numberOfReplicas = config.getInt("numberOfReplicas");
        if (numberOfNodes < numberOfReplicas){
            System.out.println("Not enough initial nodes");
            System.exit(1);
        }


        final ActorSystem sys = ActorSystem.create("ClusterSystem", config);
        ActorRef master = sys.actorOf(MasterActor.props(numberOfPartitions, numberOfReplicas), "master");
        sys.actorOf(ClusterListener.props(master), "clusterListener");

    }
}
