# As a developer you need PostgreSQL

To develop Postchain you need a database, and this guide shows how to setup a default user with a default password. 
In the code below we assume you are running Linux, Ubuntu. We begin with installing the Postgres Database (if you don't have it already):

```bash
  1 sudo apt update
  2 sudo apt install postgresql
  3
```

Once the Postgres software has been installed we must create a "postchain" user and db. Before we have it we must use the "postgres" user to login, like this:

```bash
  4 sudo -u postgres psql  
  5
```

When we are inside we create the standard developer's default setup. "<YOUR_NAME>" below is the name of your Linux user.

```sql
  6 postgres=# create database postchain;
  7  CREATE DATABASE
  8 postgres=# create user postchain with encrypted password 'postchain';
  9  CREATE ROLE
 10 postgres=# grant all privileges on database postchain to postchain;
 11  GRANT
 12 postgres=# create user <YOUR_NAME> with encrypted password '<YOUR_PW>';
 13  CREATE ROLE
 14 postgres=# grant postchain to <YOUR_NAME>;
 15  GRANT ROLE
 16 postgres-# \q
 17
```

Now we should be able to use operative system's user <YOUR_NAME>, but we still have to specify the DB name using the "-d" flag. No easy way to make that automatic.

```bash 
 18 psql -d postchain    
 19
 20 postgres=#  #--- IT WORKED!
 21
```

Try build Postchain with:

```bash 
 22 mvn clean install
```


## Copyright & License information

Copyright (c) 2017-2021 ChromaWay AB. All rights reserved.

This software can used either under terms of commercial license
obtained from ChromaWay AB, or, alternatively, under the terms
of the GNU General Public License with additional linking exceptions.
See file LICENSE for details.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

