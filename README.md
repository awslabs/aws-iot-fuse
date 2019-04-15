# AWS IoT FUSE Client
The AWS IoT FUSE client is a Java application that provides easy access to the AWS IoT service through Filesystem in Userspace (FUSE). It supports access to AWS IoT things, certificates,
policies, rules, shadow documents and MQTT pub/sub messages. Unlike the AWS CLI, the FUSE client exposes access through the UNIX filesystem interface, so users don't need to memorize
or type complex commands. For example, to list all of the things under an AWS account, you just type ```ls things```.

* [Download and Install](#download-and-install)
* [Use the Client](#use-the-client)
* [Examples](#examples)
* [FAQ](#faq)
* [License](#license)
* [Support](#support)

The AWS IoT FUSE client is a cross-platform application, built indirectly on [libfuse][libfuse-url] through an
intermediate JNA library, [fuse-jna][fuse-jna-url]. It supports Linux, Macintosh, and, potentially, other UNIX
systems that have FUSE support. 

## Download and Install
### Minimum Requirements
To use the FUSE client, you will need the following:

* A recent version of Linux or Mac OS X
* Java Runtime Environment (JRE) or Java Development Kit (JDK) 1.7 or later
* An AWS account
* libfuse (FUSE can be found in most Linux distributions. For Mac OS X, you can download it [here][mac-fuse-url])

### Download and Install Binary Package
You can either download the pre-built binary package [here][download-url] or check out the code from [GitHub][github-url].

Extract the binary package to a local directory like the following:

```sh
$ tar -zxvf ~/Downloads/aws-iot-fuse-LATEST-binary.tar.gz
```

### Build from Source
To build the FUSE client from the source, use the following commands:

```sh
$ git clone https://github.com/awslabs/aws-iot-fuse.git
$ cd aws-iot-fuse
$ mvn clean package
```

Binary packages will be created under the ```target``` directory after the build.

### Extract binaries
To use the client, first extract the binaries to an installation directory.
The following creates the "aws-iot-fuse-0.9.1" installation directory under the source root
```sh
$ tar -xvzf target/aws-iot-fuse-0.9.1-binary.tar.gz
```
## Use the Client

To mount the FUSE filesystem, you will need an AWS Identity and Access Management (IAM) user, new or existing, who has access to the AWS IoT service. The IAM user must be attached with a policy that enables the user to access the AWS IoT
service. You can attach a policy like the following to grant full access to the IAM user, or you can use a more restrictive
policy. If you use a restrictive policy, the actions supported by the filesystem may be limited.   

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "iot:*"
            ],
            "Resource": "*"
       }
    ]
}
```

After you extract the binary package, you will find the FUSE client, ```mount-iot-fuse```, under the ```scripts``` directory.
You must set up the IAM user and attach the access policy before you execute the client script. 

```
$ scripts/mount-iot-fuse -dest <mount-point> -region <aws-region> [-access-key-id <access-key-id>] [-secret-access-key <secret-access-key>] [-certificate <certificate-file>] [-private-key <private-key-file>] [-topic <topic>]...
```

* ***```-dest <mount-point>```***
The directory where the FUSE filesystem is mounted. You must have write permission to this directory.

* ***```-region <aws-region>```***
The name of the AWS region (for example, us-east-1 or eu-west-1).

* ***```-access-key-id <access-key-id>```*** and ***```-secret-access-key <secret-access-key>```*** (optional)
The IAM credentials used for connecting to the AWS IoT service. If not provided, the default IAM credentials 
(configured through ```aws configure```) will be used. You can download the AWS CLI [here][awscli-url].

* ***```-certificate <certificate-file>```*** and ***```-private-key <private-key-file>```*** (optional)
The client certificate file used for connecting to the AWS IoT service through MQTT. If you do not provide a client certificate file and the filesystem needs
to access through MQTT (for example, when ***```-topic <topic>```*** is specified), a WebSocket-based MQTT connection will be
established using the same IAM credentials provided through ***```-access-key-id```*** and ***```-secret-access-key```*** or ```aws configure```.

* ***```-topic <topic>```***
The topic that the client will subscribe to. You can specify multiple topic arguments if you want to subscribe to
multiple topics. There is a limit of 50 subscriptions per MQTT connection. Because the client uses a few subscriptions to support the 
shadow operations, the maximum number of topic arguments supported
here will be less than 50.

#### Mount the Filesystem
The following is an example of mounting the FUSE filesystem to a local directory called ```iot-fs``` in the ***us-east-1*** region. The example uses the default IAM user credentials configured earlier through ```aws configure```. 

```sh
$ mkdir ~/iot-fs
$ scripts/mount-iot-fuse -dest ~/iot-fs -region us-east-1 -topic my/topic
```

After the client script is running, a directory structure similar to the following will show up under the ```iot-fs``` directory.

```
iot-fs
|---- certificates
|   |---- b42f389d62d9f0a3e20b23bbb6f95959e2257aaf842ff59d60b76af0ee1d810d
|       |---- arn
|       |---- id
|       |---- policies
|       |   |---- kitchen-light-Policy -> ../../../policies/kitchen-light-Policy
|       |---- status
|---- endpoint
|---- policies
|   |---- kitchen-light-Policy
|       |---- arn
|       |---- document
|       |---- versions
|           |---- 1
|           |---- default -> 1
|---- rules
|   |---- test_sqs
|       |---- actions
|       |   |---- sqs
|       |       |---- queue-url
|       |       |---- role-arn
|       |       |---- use-base64
|       |---- arn
|       |---- rule-pattern
|       |---- sql
|       |---- status
|---- things
|   |---- kitchen-light
|   |   |---- principals
|   |   |   ----- b42f389d62d9f0a3e20b23bbb6f95959e2257aaf842ff59d60b76af0ee1d810d -> ../../../certificates/b42f389d62d9f0a3e20b23bbb6f95959e2257aaf842ff59d60b76af0ee1d810d
|   |   |---- state
|   |---- thing2
|       |---- principals
|       |---- state
----- topics
    |---- my_topic
        |---- messages
        |---- publish
```

The filesystem includes things, certificates, policies, and rules you have under your AWS account
in the specified region. Each of these entities is a directory, under which there are subdirectories
or files that contain more information. Under the ***topics*** directory, you will
find a subdirectory called *** messages***, which contains all of the incoming messages to the
corresponding topic. A lot of these directories and files support read, write, and delete; some
even support symbolic links. For some common actions you can perform on the filesystem, see the [Examples section](#examples).

#### Unmount the Filesystem

To umount the filesystem, you can simply terminate the client by pressing ```Ctrl-C``` in the
client terminal. Most of the time, the filesystem will be umounted automatically, but you can run the following command to force an unmount:

```sh
$ cd
(It's important to set your current directory to be outside the FUSE filesystem so you won't get resource busy error when you unmount)
$ sudo umount -f ~/iot-fs
```

## Examples
### Managing Things
You can list, create, and delete things like this:

```sh
$ cd things
$ ls
(list all the things)
$ mkdir my-first-thing
(create a thing named "my-first-thing")
$ rm -rf my-first-thing
(delete the thing named "my-first-thing")
```

### Managing Policies
You can list, create, and delete policies like this:

```sh
$ cd policies
$ ls
(list all the policies)
$ mkdir my-first-policy
(create a policy named "my-first-policy")
$ rm -rf my-first-policy
(delete the policy named "my-first-policy")
```

When a policy is created, it's not attached to any certificates. The default
policy document is an allow-all policy. You can edit the policy document by
editing the policy document file ```document ```. For examples, see [this section](#other-examples).

### Managing Certificates
You can list, create, and delete certificates like this:

```sh
$ cd certificates
$ ls
(list all the certificates)
$ mkdir my-first-certificate
(create a certificate named "my-first-certificate")
$ rm -rf my-first-thing
(delete the certificate named "my-first-certificate")
```

For certificates created using the preceding ```mkdir``` method, you can find the 
X.509 certificate and the private and public keys under the certificate directory. They are persisted
in the client heap only, and will be lost if you unmount the filesystem. Make sure to copy them
to safe and permanent storage before you unmount the filesystem.

Although you use the name to reference the certificate in the FUSE filesystem,
the name is not persisted on the AWS IoT service end. It cannot be used anywhere other
than the mounted filesystem. The name will be lost after you unmount
the filesystem, but the certificate and its linked policies and things won't be
affected.

### Managing Rules
You can list, create, and delete rules like this:

```sh
$ cd rules
$ ls
(list all the rules)
$ mkdir my-first-rule
(create a rule named "my-first-rule")
$ rm -rf my-first-rule
(delete the rule named "my-first-rule")
```

### Managing Relationships
By using symbolic links, you can attach policies to certificates and certificates to things. 

```sh
$ cd iot/certificates/my-first-certificate/policies
$ ln -s ../../../my-first-policy .
(This attaches my-first-policy to my-first-certificate, and now the certificate has the permissions granted by the policy)
$ cd iot/things/my-first-thing/principals 
$ ln -s ../../../my-first-certificate .
(This attaches my-first-certificate to my-first-thing, and now the thing has the permissions granted by my-first-policy)
```

Similarly, you can remove these relationships just by removing the symbolic links:

```sh
$ cd iot/certificates/my-first-certificate/policies
$ rm my-first-policy
(This detaches my-first-policy from my-first-certificate)
$ cd iot/things/my-first-thing/principals 
$ rm my-first-certificate
(This detaches my-first-certificate from my-first-thing)
```

### Managing Topics
The topics you specified on the command line when you mounted the filesystem are under the ```topics``` directory. Forward slashes (/)
in the topics are replaced with underscores (_) so they won't be misinterpreted as directory separators.

Incoming messages for the topic are under the ```messages``` subdirectory. The file creation timestamp is set to the time the message
is received. The messages are stored in the heap of the FUSE application, so when the filesystem is unmounted, those
messages will be lost. 

To publish a message to the topic, you can write to the ```publish``` file under the same directory. All writes to
that file will be buffered in memory. They are published in one message only when ```close(2)``` is called. For that reason, the
maximum file size of ```publish``` is 128,000 bytes, the maximium message size supported. Messages published
through the ```publish``` file will also show up in the ```messages``` subdirectory because the client subscribes to the same topic.

```sh
$ cd iot/topics/my_topic
$ echo "my first message\n\n hello world" > publish
(This publishes a multi-line messages)
$ cat /tmp/my-photo.jpg > publish
(This publishes a binary message. Remember the 128k bytes limit.)
$ ls -ltr messages
(List received messages in reversed time order - last received at the bottom)
$ grep "ERROR" messages
(Grep the mssages to find ones containing text "ERROR")
$ cat  messages/<message-uuid>
(Print message content)
$ rm messages/*
(Remove all the messages to reduce the application's memory usage). 
```

### Managing Shadow Documents
For each thing under the ```things``` directory, there's a file named ```state```. You can use this file
to create or update the shadow document. Update document request is sent only when ```close(2)``` is called on
the file descriptor. Keep in mind that the total size limit of the shadow document is 4,000 bytes.

The shadow document returned by reading the ```state``` file contains data and metadata of the document in JSON format.
When updating the shadow document (the ```state``` file), make sure the entire file is properly formatted
in JSON. Otherwise, you may receive an I/O error when you try to update the file. 

```sh
$ cd iot/things/my_first_things
$ cat state
(This prints the thing's shadow document)
$ echo '{"state":{"desired":{"switch_state":"on"},"reported":{"switch_state":"on"}}}' > state
(This updates the thing's shadow document)
```

### Other Examples
Here are some other ways you can use the FUSE filesystem. 

* Print the endpoint
```sh
$ cd ~/iot-fs
$ cat endpoint
```

* Update the policy document
```sh
$ cd ~/iot-fs/policies/my-first-policy
$ echo "{"Version":"2012-10-17","Statement":[{"Action":["iot:*"],"Resource":["*"],"Effect":"Deny"}]}" > document
(a new policy document verion will be created under the directory versions)
$ cd versions
$ ln -sf 2 default
(Make version 2 of the document as the default version)
```

* Update the rule SQL
```sh
$ cd ~/iot-fs/rules/my-first-rule
$ echo "SELECT * FROM 'sdkTest/sub'" > sql
```

## FAQ
* If I create or delete things, rules, or certificates and so on, will they show up in the FUSE filesystem
after it has been mounted?

Yes, they will show up after a preconfigured delay, generally one or two minutes. The FUSE filesystem will periodically refresh its local copies so it won't become too out-of-sync with the entites in
the AWS IoT service.

* Can other users on the same machine see my data under the FUSE filesystem?

No, access to the mounted FUSE filesystem is restricted to the user who mounted it. No one, including
```root```, will be able to access the directories and files under the FUSE filesystem.

* How do I increase the maximum heap size of the application?

You can specify extra JVM arguments through environment variable ```JVM_ARGS``` like so,

```sh
$ export JVM_ARGS='-Xms=128M -Xmx=128M'
$ scripts/mount-iot-fuse ...
```

## License
This SDK is distributed under the [Apache License, Version 2.0][apache-license-2]. For more information, see
LICENSE.txt and NOTICE.txt.

## Support
If you have technical questions about the AWS IoT Device SDK, use the [AWS IoT Forum][aws-iot-forum].
For any other questions about AWS IoT, contact [AWS Support][aws-support].

[libfuse-url]: https://github.com/libfuse/libfuse
[fuse-jna-url]: https://github.com/EtiennePerot/fuse-jna
[github-url]: http://github.com/awslabs/aws-iot-fuse
[mac-fuse-url]: https://osxfuse.github.io/
[download-url]: https://s3.amazonaws.com/aws-iot-fuse/aws-iot-fuse-LATEST-binary.tar.gz
[awscli-url]: https://aws.amazon.com/cli/
[aws-iot-forum]: https://forums.aws.amazon.com/forum.jspa?forumID=210
[aws-support]: https://aws.amazon.com/contact-us
[apache-license-2]: http://www.apache.org/licenses/LICENSE-2.0
