machine=$1
port=$2
id=$3

ssh tarequl@$machine.cs.colostate.edu -t "cd /s/chopin/b/grad/tarequl/Desktop/CS555/HW2; echo $machine:; java Peer $port $id; bash --login"
