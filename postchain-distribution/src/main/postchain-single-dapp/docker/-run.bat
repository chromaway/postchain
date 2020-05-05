docker build . -t chromaway/postchain-single-dapp:3.2.0
docker run --name single-dapp -p 7741:7741 -it chromaway/postchain-single-dapp:3.2.0