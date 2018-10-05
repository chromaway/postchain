<!-- Copyright (c) 2017 ChromaWay Inc. See README for license information. --> 

## How to dockerize Postchain ##

1. Build `chromaway/postgres` image (see `./postchain-docker/src/main/postgres`)

    1. Build a new image based on `postgres` one with postchain's initdb script:  
```docker build . -t chromaway/postgres:2.4.0-beta```

    1. Push `chromaway/postgres` image to DockerHub registry:  
```docker push chromaway/postgres:2.4.0-beta```


1. Build `chromaway/postchain` image (see `./postchain-docker/src/main/postchain`)

    1. Build a new `postchain` image:  
```docker build . -t chromaway/postchain:2.4.0-beta```

    1. Push `chromaway/postchain` image to DockerHub registry:  
```docker push chromaway/postchain:2.4.0-beta```


1. Orchestrate with `docker-compose` (see `./postchain-docker/src/main/docker-compose`)

    1. See env variables in `docker-compose.yml` and configs at `./postchain-docker/src/main/docker-compose/postchain_config`

    1. Builds, (re)creates, starts, and attaches to containers:  
```docker-compose up```

    1. Stop services:  
```Ctrl+C``` or ```docker-compose stop```

    1. Start services:  
```docker-compose start```

    1. Stop and remove containers, networks, volumes, and images created by 'up':  
```docker-compose down```

    1. Displays log output from services:  
```docker-compose logs -f```

