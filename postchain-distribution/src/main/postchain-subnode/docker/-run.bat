docker build . -t chromaway/postchain-subnode:3.2.1

REM docker run --name postchain-subnode -p 7741:7741 -it chromaway/postchain-subnode:3.2.0

docker run ^
    --name postchain-subnode ^
    -p 7742:7741 ^
    -v d:\Home\Dev\ChromaWay\postchain2\postchain-subnode\src\main\docker\rte:/opt/chromaway/postchain-subnode/rte ^
    -it chromaway/postchain-subnode:3.2.1
