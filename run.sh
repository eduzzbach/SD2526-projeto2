
docker rm -f users0 users1 messages0 messages1

docker run --rm -d --network sdnet --name users0 -p 13456:3456 smduarte/sd2526-tp1-smd java -cp sd2526.jar sd2526.trab.impl.rest.servers.RestUsersServer -domain wikipedia.com
docker run --rm -d --network sdnet --name users1 -p 23456:3456 smduarte/sd2526-tp1-smd java -cp sd2526.jar sd2526.trab.impl.rest.servers.RestUsersServer -domain mit.edu 

docker run --rm -d --network sdnet --name messages0 -p 14567:4567 smduarte/sd2526-tp1-smd java -cp sd2526.jar sd2526.trab.impl.rest.servers.RestMessagesServer -domain wikipedia.com
docker run --rm -d --network sdnet --name messages1 -p 24567:4567 smduarte/sd2526-tp1-smd java -cp sd2526.jar sd2526.trab.impl.rest.servers.RestMessagesServer -domain mit.edu 
