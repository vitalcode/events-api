Expose events data as GraphQL web service


#### Vars
```
PROJECT_NAME=events-api
PROJECT_VERSION=
HOST_SERVER=
PSQL_URL=
PSQL_USER=
PSQL_PASSWORD=
ES_URL=
ES_INDEX=
ES_TYPE=
ES_CLUSTER=
```

#### Build Image
```
docker rmi -f '$PROJECT_NAME:$PROJECT_VERSION'; sbt docker:publishLocal
```

#### Deploy Image
```
sudo ssh $HOST_SERVER "docker rm -f $PROJECT_NAME; docker rmi -f '$PROJECT_NAME:$PROJECT_VERSION'";
docker save $PROJECT_NAME | bzip2 | sudo ssh $HOST_SERVER "bunzip2 | docker load"
```

#### Run Container
```
sudo ssh $HOST_SERVER "docker rm -f $PROJECT_NAME; true"; sudo ssh $HOST_SERVER "docker run --name $PROJECT_NAME -e PSQL_USER=$PSQL_USER -e PSQL_PASSWORD=$PSQL_PASSWORD -e ES_URL=$ES_URL -e ES_INDEX=$ES_INDEX -e ES_TYPE=$ES_TYPE -e ES_CLUSTER=$ES_CLUSTER -e PSQL_URL=$PSQL_URL -p 8083:8083 -d '$PROJECT_NAME:$PROJECT_VERSION'"
```
