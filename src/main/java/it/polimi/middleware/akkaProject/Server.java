package it.polimi.middleware.akkaProject;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.util.Collections;

public class Server {
    public static void main( String[] args )
    {
        final Config config = ConfigFactory //
                .parseFile(new File("conf/cluster.conf")) //
                .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Collections.singletonList("server")));
        final ActorSystem sys = ActorSystem.create("ClusterSystem", config);
        System.out.println(config.getInt("akka.cluster.role.server.min-nr-of-members"));


    }
}
