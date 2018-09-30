## How to dockerize Postchain ##

1. Build `chromaway/postgres` image

    1. Run `postgres` container with postchain's initdb script:  
```docker run --name postgres -e POSTGRES_PASSWORD=postchain -p 5432:5432 -d postgres```

    2. Create a new image from `postgres` container:  
```docker commit -m="Postchain postgres image" postgres chromaway/postgres:2.3.5-beta```

    3. Push `chromaway/postgres` repository to DockerHub registry:  
```docker push chromaway/postgres:2.3.5-beta```


2. Build `chromaway/postchain` image

    1. Build `postchain` image:  
```docker build . -t postchain:2.3.5-beta```

    2. Create a new image `chromaway/postchain` (a copy of `postchain` one):  
```docker commit -m="Postchain image" postchain chromaway/postchain:2.3.5-beta```

    3. Push `chromaway/postchain` repository to DockerHub registry:  
```docker push chromaway/postchain:2.3.5-beta```


3. Orchestrate with `docker-compose`

    1. Builds, (re)creates, starts, and attaches to containers:  
```docker-compose up```

    2. Stop services:  
```Ctrl+C```
```docker-compose stop```

    3. Start services:  
```docker-compose start```

    4. Stop and remove containers, networks, volumes, and images created by 'up':  
```docker-compose down```

    5. Displays log output from services:  
```docker-compose logs -f```