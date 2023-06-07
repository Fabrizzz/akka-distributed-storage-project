package it.polimi.middleware.akkaProject.server;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Collections;

public class Server {
    public static void main(String[] args) throws UnknownHostException {
        String masterIp = "127.0.0.1"; //cambiare con ip del Master
        final Config config = ConfigFactory //
                .parseFile(new File("conf/cluster.conf")) //
                .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Collections.singletonList("server")));
        //.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(Collections.singleton("akka.tcp://ClusterSystem@"+masterIp+":1234")));//
        //.withValue("akka.remote.classic.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(Inet4Address.getLocalHost().getHostAddress()));
        //da cambiare con ip che altri utilizzano per contattare questo

        int numberOfPartitions = config.getInt("numberOfPartitions");
        int numberOfRouters = config.getInt("numberOfRouters");
        final ActorSystem sys = ActorSystem.create("ClusterSystem", config);
        sys.actorOf(SupervisorActor.props(numberOfPartitions), "supervisor");
        sys.actorOf(RouterManagerActor.props(numberOfRouters), "routerManager");
    }
}
