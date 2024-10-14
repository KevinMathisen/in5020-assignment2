# in5020-assignment2

Note: 
When starting a client with other, already running clients, the new client will join and sync with them.
However, if the new client has num_of_rep set at the same amount of group members, the new client will think it can start executing and is not a new member.
This will cause the new client to exit as it detects it is not synchronized. 
To prevent this simply set num_of_client to be less or more than amount of member including the new member. 