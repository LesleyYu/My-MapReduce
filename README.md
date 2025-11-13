# My MapReduce Word Count Project

> A Word Count project in Java that runs on Hadoop MapReduce. A personal usage guidance

## Installation

To get a simplist project running, we follow the [official guide](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html). The guide default OS is Linux but I am using a Mac. Below are the commands for Mac installation.

### 1. Apache Hadoop

**MacOS** installation guide:

#### Install homebrew

#### Use homebrew to install hadoop

```bash
brew install hadoop
```

Check where your hadoop is by:

```bash
hadoop version
```

or

```bash
brew info hadoop
```

If you used *homebrew*, it will be in somewhere like `/opt/homebrew/Cellar/hadoop/3.4.2` (3.4.2 is the version)

##### Error Shooting

I came across this installation failure:

> Error: Cannot install hadoop because conflicting formulae are installed.
> yarn: because both install yarn binaries
> 
> Please brew unlink yarn before continuing.

The issue is that I have the yarn formula installed, which conflicts with Hadoop because both provide a yarn binary (Hadoop's YARN resource manager vs. Yarn the JavaScript package manager).

I have several options to get around that. I chose to Unlink yarn (the js manager) temporarily:

```bash
bashbrew unlink yarn
brew install hadoop
brew link yarn  # Re-link yarn after if you still need it
```

After I unlinked yarn, I decided to rename the js manager yarn to **js-yarn**. I don't like yarn anyways I prefer npm manager.

##### Add path to environment

After installing hadoop I used command `[ -d "etc/hadoop" ] && echo "hadoop exist"` to check if my hadoop exists. But it echos nothing. I had to find where it is by `hadoop version` or `brew info hadoop` and decided to add the path to environment by editing `~/.zshrc`.

```
export HADOOP_HOME=/opt/homebrew/Cellar/hadoop/3.4.2/libexec
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
```

### 2. SSH & pdsh

MacOS has its built-in SSH. run `which ssh` and `ssh -V` to check if you have it on your mac.

On macOS, you can install pdsh using Homebrew:

```bash
brew install pdsh
```

> Heads up
> For a single-node Hadoop setup on your Mac, you probably won't actually need pdsh.
> **pdsh (Parallel Distributed Shell)** is used to execute commands on multiple machines simultaneously in a real distributed cluster. Since you're running everything on one machine (localhost), it's not really necessary for your setup.
> But if the guide mentions it and you want to follow along completely, go ahead and install it with the command above. It won't hurt to have it installed even if you don't end up using it.

### 3. Java

#### Java version 11

Hadoop runs best with Java openjdk version 11. To check if you have openjdk@11 installed, run:

```bash
/usr/libexec/java_home -V
```

It displays all java versions you have in your Mac.

#### Add openjdk@11 path to hadoop env

Once you find where you installed openjdk@11, add that path to Hadoop environment vairables. To achieve that, we need to edit file `etc/hadoop/hadoop-env.sh`. 

1. run `hadoop version` to locate your hadoop
2. cd into `hadoop/3.4.2/libexec`. This is the directory where we usually run all the hadoop commands.
3. Use **vim** (or whatever editor) to add java path to `etc/hadoop/hadoop-env.sh`:

    ```
    export JAVA_HOME=$(/usr/libexec/java_home)
    ```

    If you have multiple java installed, specify Java version using:

    ```
    export JAVA_HOME=$(/usr/libexec/java_home -v 11)
    ```

  Alternative:
  Set it to a specific path by adding something like:

  ```
  export JAVA_HOME=/Users/lesley/Library/Java/JavaVirtualMachines/ms-11.0.28/Contents/Home
  ```

## basic Q&A

### Why SSH for Hadoop on local machine?
Even though you're running Hadoop locally on your Mac, Hadoop's architecture was designed for distributed clusters. The Hadoop daemons (like NameNode, DataNode, ResourceManager, etc.) communicate with each other using SSH, even when they're all on the same machine.
So when you run Hadoop in "pseudo-distributed mode" (single node acting like a cluster), the processes still use SSH to talk to localhost. It's a bit odd, but that's how Hadoop works!

## Configuration

> For Pseudo-Distributed Operation

### 1. Use the following:

`etc/hadoop/core-site.xml`:

  ```xml
  <configuration>
      <property>
          <name>fs.defaultFS</name>
          <value>hdfs://localhost:9000</value>
      </property>
  </configuration>
  ```

`etc/hadoop/hdfs-site.xml`:

  ```xml
  <configuration>
      <property>
          <name>dfs.replication</name>
          <value>1</value>
      </property>
  </configuration>
  ```

### 2. Setup passphraseless SSH

run `ssh localhost`. If warning like permission deinied pops up (e.g. Google Cloud operation not permitted), ignore it (The errors are unrelated - they're just permission issues with your .zshrc file trying to load Google Cloud scripts).

However, to avoid having to enter password everytime, we can set up passphraseless SSH (*optional*).

1. Generate an SSH key (if you don't have one):

```bash
bashssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
```

This generates a pair of SSH keys (public and private):
* `ssh-keygen` - Generates SSH keys
* `-t rsa` - Type of encrytion to use
* `-P ''` - Set the passphrase to empty string(no password protection on the key)
* `-f ~/.ssh/id_rsa` - File location to save the key

Result: Creates two files:
~/.ssh/id_rsa - Your private key (keep this secret!)
~/.ssh/id_rsa.pub - Your public key (safe to share)

2. Add your public key to authorized_keys:

```bash
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
```

* `~/.ssh/id_rsa.pub` - Your public key file
* `>>` - Append operator (adds to end of file without overwriting)
* `~/.ssh/authorized_keys` - File that lists which public keys are allowed to log in

Result: Your localhost now trusts your own public key, so when you SSH with your private key, it lets you in without a password


3. Set correct permissions:

```bash
bashchmod 0600 ~/.ssh/authorized_keys
```

* `chmod` - Change file permissions command
* `0600` - Permission code meaning:
    6 (owner): read + write
    0 (group): no permissions
    0 (others): no permissions

### 3. Format hadoop filesystem

cd into `/opt/homebrew/Cellar/hadoop/3.4.2/libexec`, run:

```bash
bin/hdfs namenode -format
```

## Execution (READ THIS)

> For Pseudo-Distributed Operation

The above commands sets me ready for execution. Now if I want to test my program, I need to run the following commands everytime.

> heads up
> We do not need to SSH to localhost everytime. In configuration, we SSHed so  

1. Start NameNode daemon and DataNode daemon:
  cd into `/opt/homebrew/Cellar/hadoop/3.4.2/libexec`, run:

  ```bash
  sbin/start-dfs.sh
  ```

  to check what's running, run `jps`. Following should appear:

  ```
  15348 Jps
  15237 SecondaryNameNode
  15093 DataNode
  14983 NameNode
  ```

2. Browse the web interface for the NameNode; by default it is available at: NameNode - http://localhost:9870/

3. Make the HDFS directories required to execute MapReduce jobs:

  ```bash
  bin/hdfs dfs -mkdir -p <my project>
  ```

  or `hadoop dfs` will also work.

4. Compile your WordCount.java
   
  cd to `/Users/lesley/Documents/USC/CSCI572/HW3/MY-MAPREDUCE/wordcount` where I store my java program

  ```bash
  hadoop com.sun.tools.javac.Main WordCount.java
  jar cf wc.jar WordCount*.class
  ```

5. Upload input data

  ```bash
  hadoop fs -put input /wordcount/input
  ```

6. Run your WordCount job
  ```bash
  hadoop jar wc.jar WordCount /wordcount/input /wordcount/output
  ```

7. View the results
```bash
hadoop fs -cat /user/lesley/wordcount/output/part-r-00000
```

Or copy to local:
```bash
hadoop fs -get /user/lesley/wordcount/output ./output
cat output/part-r-00000
```

### Explanation

1. My `WordCount.jav`a is local, when I do `hadoop com.sun.tools.javac.Main WordCount.java`, I use the javac tool in hadoop fs?
    This uses the Java compiler (javac) that comes with **Hadoop**, but it compiles your local file. It's **NOT** using anything in HDFS. Think of it as:
    * **hadoop** = a wrapper script that sets up the Java classpath
    * **com.sun.tools.javac.Main** = the Java compiler
    * `**WordCount.java**` = your local file
    It's equivalent to running javac WordCount.java but with Hadoop's libraries in the classpath.
2. Why do I get compiled `.class` files local under the same directory as my `WordCount.java` file? Shouldn't it be in hadoop fs?
    Because you compiled a local file, the output goes to the local filesystem. The compiler reads from local, writes to local. HDFS is not involved at all in compilation.
3. The command `jar cf wc.jar WordCount*.class` is completely local? **Yes.**
4. When I run `hadoop jar wc.jar WordCount /wordcount/input /wordcount/output`, I am reading local `wc.jar` file and apply it to the files in hadoop fs directory /wordcount/input?
   Now it gets interesting! This command:

  Reads the local wc.jar file
  Uploads it temporarily to the Hadoop cluster
  Runs the MapReduce job
  Reads input from HDFS: `/wordcount/input`
  Writes output to HDFS: `/wordcount/output`

#### Visual Summary:

LOCAL FILESYSTEM:
/Users/lesley/Documents/USC/CSCI572/HW3/wordcount/
├── WordCount.java          ← Your source code (local)
├── WordCount.class         ← Compiled class (local)
├── wc.jar                  ← JAR file (local)
└── input/                  ← Your original data (local)
    └── file.txt

HDFS (Hadoop Distributed File System):
/user/lesley/wordcount/
├── input/                  ← Data copied to HDFS with `hadoop fs -put`
│   └── file.txt
└── output/                 ← MapReduce output (in HDFS)
    ├── part-r-00000
    └── _SUCCESS