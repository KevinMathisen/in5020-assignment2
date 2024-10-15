# in5020-assignment2

## Requirements
- Maven (If you want to compile)
- Java
- Docker

## Usage

### Start the spread server
To start the spread server, do the following steps:
1. Modify the [`Spread configuration`](./spread.conf) with the IP adress and port the spread server should run on
2. Run `docker build -t spread-image .` to build a docker image called *spread-image*
3. Run `docker run --rm --name spread -it -p 4803:4803 spread-image` to start a docker container

Now spread should be running and accepting new members. 

### Start the client
To start the clients, do the following steps:
1. Compile the java files using `mvn clean package`
2. Run the following commands for starting the clients:
   - For Windows:
     ```sh
     java -cp "target\assignment2-1.0-SNAPSHOT.jar;lib\spread.jar" com.example.Client --server 127.0.0.1 --account Group10 --file exampleinputfile.txt --replicas 2 --id 1
     ```
   - For Linux:
     ```sh
     java -cp target/assignment2-1.0-SNAPSHOT.jar:lib/spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file exampleinputfile.txt --replicas 2 --id 1
     ```

TODO: describe command line arguments in more detail.

#### Note for joining clients with already running clients: 
When starting a client with other, already running clients, the new client will join and sync with them.
However, if the new client has `num_of_rep` set at the same amount of group members, the new client will think it can start executing and is not a new member.
This will cause the new client to exit as it detects it is not synchronized. 
To prevent this simply set `num_of_client` to be **less or more** than amount of member including the new member. 