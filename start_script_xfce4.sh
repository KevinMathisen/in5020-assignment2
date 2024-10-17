#!/bin/bash

# Set where to start the java files from
WORK_DIR="./"

# Run Rep1
xfce4-terminal --hold --working-directory="$WORK_DIR" --command="java -cp target/assignment2-1.0-SNAPSHOT.jar:lib/spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep1.txt --replicas 2 --id Rep1"

# Run Rep2
xfce4-terminal --hold --working-directory="$WORK_DIR" --command="java -cp target/assignment2-1.0-SNAPSHOT.jar:lib/spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep2.txt --replicas 2 --id Rep2"

sleep 15

# Run Rep3
xfce4-terminal --hold --working-directory="$WORK_DIR" --command="java -cp target/assignment2-1.0-SNAPSHOT.jar:lib/spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep3.txt --replicas 2 --id Rep3"
