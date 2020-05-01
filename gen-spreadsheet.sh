#!/bin/bash

java -jar ./out/artifacts/ejudge_tools_jar/ejudge_tools.jar -i /Users/mihailperveev/Desktop/Gramotey_Registratsia.xlsx -o gramotey2020.xlsx -e ejudge_res_gramotey.csv -l gramotey-2020-finals-%04d -p markov -pl 9 -s text.txt -n "%s %s %s %s %s" "1 2 3 5 4" -c -sn "gramotey-2020registr (1)"
