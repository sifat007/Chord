while read line ; do
	A="$(cut -d' ' -f1 <<<"$line")"
	B="$(cut -d' ' -f2 <<<"$line")"
	C="$(cut -d' ' -f3 <<<"$line")"
	echo $A
	gnome-terminal -e "bash -c './ssh.sh $A $B $C';bash"
	sleep 2
done < config.txt

