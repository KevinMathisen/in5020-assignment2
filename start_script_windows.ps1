# Set where to start the java files from
$workDir = ".\"

# Run Rep1
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $workDir; java -cp target/assignment2-1.0-SNAPSHOT.jar;lib\spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep1.txt --replicas 2 --id Rep1"

# Run Rep2
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $workDir; java -cp target/assignment2-1.0-SNAPSHOT.jar;lib\spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep2.txt --replicas 2 --id Rep2"

Start-Sleep -Seconds 15

# Run Rep3
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $workDir; java -cp target/assignment2-1.0-SNAPSHOT.jar;lib\spread.jar com.example.Client --server 127.0.0.1 --account Group10 --file Rep3.txt --replicas 2 --id Rep3"