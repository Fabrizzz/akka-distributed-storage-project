package it.polimi.middleware.akkaProject;


import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.util.Collections;

public class Master
{
    public static void main( String[] args )
    {
        final Config config = ConfigFactory //
                .parseFile(new File("conf/cluster.conf")) //
                .withValue("akka.remote.classic.netty.tcp.port", ConfigValueFactory.fromAnyRef(1234)) //
                .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Collections.singletonList("master")));
        final ActorSystem sys = ActorSystem.create("ClusterSystem", config);
        sys.actorOf(MasterActor.props(), "master");
        //System.out.println(config.getInt("akka.cluster.role.server.min-nr-of-members"));


    }
}
