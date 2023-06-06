# Akka Distributed Storage Project

This project implements a distributed storage system using Akka Cluster.

The system consists of:

- Master: The master node manages the cluster. It keeps track of all the nodes in the cluster and partitions data between nodes.

- Server: The server nodes store partitions of the data and handle requests for those partitions.

- Client: The client allows interacting with the storage system to add, retrieve and update data.

To start the Master node:
- Open a terminal in the project folder
- Run: mvn exec:java -Dexec.mainClass="it.polimi.middleware.akkaProject.master.Master"
- The master will start on port 1234

To start a Server node:
- Open a terminal in the project folder
- Run: mvn exec:java -Dexec.mainClass="it.polimi.middleware.akkaProject.server.Server"

To start the Client:
- Open a terminal in the project folder
- Run: mvn exec:java -Dexec.mainClass="it.polimi.middleware.akkaProject.client.ClientMain"
- The client will prompt for input to put, get or update router info

The config files in the conf directory configure the port numbers, seed nodes, and other settings for the Master, Server and Client.
