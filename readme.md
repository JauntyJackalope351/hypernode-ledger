# Hypernode distributed ledger system

This program aims to create a distributed ledger with the goal of illustrating the Hypernode validation algorithm.  
It is a Proof of Stake system that supports delegation, can scale well, requires little power,supports multiple signing
algorithms(tested with RSA and ML-DSA-87),can relay messages, has a built-in voting system to update its own ledger parameters
and a built in DNS system.  
It was written from scratch in Java using the Spring framework, as it is a very easy language to read, understand and validate.  
One of the reason for the creation of this program was that the Bitcoin code was not intuitive to read or easy to deploy,
so auditability and ease of startup were important objectives of this project.  
Ease of use and security were considered, as you need both a signature from the user (like chip and pin)
and from your validatorNode (which acts like a sort of bank), so that incidents like MtGox cannot happen anymore.  
You then get the ease of use of having normal people operate thin clients while validatornodes do the heavy lifting,
but at the same time a motivated user could run his own validatorNode.  
The power and computation were also a concern, especially as that GPU power could now be allocated to AI.  
Another reason I was not fully happy with Bitcoin was the lack of an internal formal voting system, and the inability to
issue new currency without recurring to an hard fork.  
This system is based on delegated validatornodes to manage monetary policy, which means that the ledger continuity (the creation of new blocks)
is based off the agreement of 50% +1 of the electronic voting, casted directly or via delegation through electronic signatures.
This is, therefore, a democratic instrument.  
If you so desire you do not even need the Internet to run this kind of ledger, you could run one entirely via avian carriers,
therefore becoming an important asset for communities that can't or won't connect to the mainstream Internet.  
Despite those critiques Bitcoin has undoubtedly paved the road for a lot of these concepts, this project is more of a fine tune of a good idea according to my specific taste, requirements and my personal flavour of autism and not about throwing shade at a successful project.

# IMPORTANT:

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

This program is a proof of concept. It was not possible to test it at scale before its release to the public.  
Feel free to modify it to suit your needs, but remember to share the code using AGPL
(see https://www.gnu.org/licenses/agpl-3.0.html) and to credit this project by linking back here.  
If you want to turn this program into an official production system please hire someone competent to tailor the system to your needs
and validate this software by paying for a third party audit.  
In the meantime feel free to start with the default code and settings, you will be able to export the ledger
and restart it onto a compatible chain at a later date if you want to.  

## the Hypernode transmission algorithm

### How does it work?

<details><summary>Collapse / Expand</summary> The easiest real life example on how to visualize the Hypernode is by looking at a Rubick's Cube.<br>
The cube is composed by 27 individual smaller cubes (one central element and those who expose the colored faces).  
Place the cube on the table:  
if you were to slice it like an hamburger you would see that those 27 individual cubes components
can be represented this way:  

Bottom (1)

| | 1 | 2 | 3 |
|-|-|-|-|
|1 |1.1.1| 1.1.2| 1.1.3 |
|2 |1.2.1| 1.2.2| 1.2.3 |
|3 |1.3.1| 1.3.2| 1.3.3 |

Middle (2)

| | 1 | 2 | 3 |
|-|-|-|-|
|1 | 2.1.1| 2.1.2 |2.1.3 |
|2 | 2.2.1| 2.2.2 |2.2.3 |
|3 | 2.3.1| 2.3.2 |2.3.3 |

Top(3)

| | 1 | 2 | 3 |
|-|-|-|-|
|1 | 3.1.1| 3.1.2 |3.1.3|
|2 | 3.2.1| 3.2.2 |3.2.3|
|3 | 3.3.1| 3.3.2 |3.3.3|

We can now identify each element univocally by the value of its height, row or column.  
Let's take 1.1.1 as a node that wants to propagate a message, to demonstrate the logic behind the Hypernode.  
First of all let's define as peers the nodes that only differ by 1 element of the address  
(in this case 1.1.2, 1.1.3, 1.2.1,1.3.1, 2.1.1, 3.1.1).  
As you can see those are the elements on the same row, column or vertical as our chosen element.  
All of these elements will read the data of the element, and on the next turn they can forward it too
on their own peers.  
This will mean that the data of 1.1.1 is now be available to all who differ by 1 element of the address,
and at the next stage it will be available for everyone who differ with one of those peers by 1 element in the address.  
This means that everyone whi differs by 2 elements now should have access to that data.  
If we continue further we reach 3 iterations, and since we have 3 address values we have propagated the original message
to all of the elements of the network.  

#### Progression of the message emanating from 1.1.1

Bottom

| | 1 | 2 | 3 |
|-|-|-|-|
|1|0|1|1|
|2|1|2|2|
|3|1|2|2|

Middle

| | 1 | 2 | 3 |
|-|-|-|-|
|1|1|2|2|
|2|2|3|3|
|3|2|3|3|

Top

| | 1 | 2 | 3 |
|-|-|-|-|
|1|1|2|2|
|2|2|3|3|
|3|2|3|3|

Now the element 1.1.1 needs to receive the signature from the elements reached in our last iteration,
so by repeating the procedure 3 further times the original sender has propagated to everyone his message and
received the receipt signature from everyone else in the network.  
At this point it is possible to calculate the hash of the resulting transaction block, and doing another pass
we can diffuse the hash value and make sure that there is a majority of the voting that has the same hash for this transaction block,
therefore validating it and using that hash as the seed for the next encryption block.  

</details>

### Performance

<details><summary>Collapse / Expand</summary>
In the previous example to have a complete validated frame every node you had to contact
6 (peers) * 3 (iterations per frame) * 3 (frames per valid block), or 54 connections.  
The general formula to calculate the necessary number of connections to complete a transmission event for a node is  
roundup(dimensions * (servers ^ (1/dimensions) -1))  
and the number of connection required to validate a block is  
nodes * dimensions * 3  
As you will see in the examples below a higher dimension number will reduce the required number of peers (up to a point)
but will also increase the number of iterations, making a block validation more time consuming,
and after reaching an optimum the total number of connection will rise too.  
The practical way is to determine the maximum number of peers you would like to have and set the staking cost accordingly,
so you can decide how much decentralization vs performance you want to have

Top row :Exponent  
first column: nodes  
value: number of peers  

|        | 1     | 2   | 3   | 4  | 5  | 6  | 7  | 8  | 9  | 10 |
|--------|-------|-----|-----|----|----|----|----|----|----|----|      
| 10     | 9     | 5   | 4   | 4  | 3  | 3  | 3  | 3  | 3  | 3  |
| 100    | 99    | 18  | 11  | 9  | 8  | 7  | 7  | 7  | 7  | 6  |
| 1000   | 997   | 62  | 27  | 19 | 15 | 13 | 12 | 11 | 11 | 10 |
| 10000  | 9999  | 198 | 62  | 36 | 27 | 22 | 20 | 18 | 17 | 16 |
| 100000 | 99999 | 630 | 137 | 68 | 45 | 35 | 30 | 26 | 24 | 22 |

Top row :Exponent  
first column: nodes  
value: total connections per block  

|        | 1      | 2    | 3    | 4   | 5   | 6   | 7   | 8   | 9   | 10  |
|--------|--------|------|------|-----|-----|-----|-----|-----|-----|-----| 
| 10     | 27     | 30   | 36   | 48  | 45  | 54  | 63  | 72  | 81  | 90  |
| 100    | 297    | 108  | 99   | 108 | 120 | 126 | 147 | 168 | 189 | 180 |
| 1000   | 2997   | 372  | 243  | 228 | 225 | 234 | 252 | 264 | 297 | 300 |
| 10000  | 29997  | 1188 | 558  | 432 | 405 | 396 | 420 | 432 | 459 | 480 |
| 100000 | 299997 | 3786 | 1233 | 816 | 675 | 630 | 630 | 624 | 648 | 660 |

Top row :Exponent  
first column: nodes  
value: total connections per complete block when adding 1 redundancy transmission frame

|        | 1      | 2    | 3    | 4    | 5   | 6   | 7   | 8   | 9   | 10  |
|--------|--------|------|------|------|-----|-----|-----|-----|-----|-----|
| 10     | 54     | 45   | 48   | 60   | 54  | 63  | 72  | 81  | 90  | 99  |
| 100    | 594    | 162  | 132  | 135  | 144 | 147 | 168 | 189 | 210 | 198 |
| 1000   | 5994   | 558  | 324  | 285  | 270 | 273 | 288 | 297 | 330 | 330 |
| 10000  | 59994  | 1782 | 744  | 540  | 486 | 462 | 480 | 486 | 510 | 528 |
| 100000 | 599994 | 5679 | 1644 | 1020 | 810 | 735 | 720 | 702 | 720 | 726 |

Top row :Exponent  
first column: peers  
value: max number of nodes of the system  

|     | 1   | 2    | 3     | 4      | 5       | 6        | 7         | 8         | 9          | 10          |
|-----|-----|------|-------|--------|---------|----------|-----------|-----------|------------|-------------|
| 10  | 11  | 36   | 64    | 81     | 243     | 64       | 128       | 256       | 512        | 1024        |
| 20  | 21  | 121  | 343   | 1296   | 3125    | 4096     | 2187      | 6561      | 19683      | 59049       |
| 50  | 51  | 676  | 4913  | 28561  | 161051  | 531441   | 2097152   | 5764801   | 10077696   | 60466176    |
| 100 | 101 | 2601 | 39304 | 456976 | 4084101 | 24137569 | 170859375 | 815730721 | 5159780352 | 25937424601 |

</details>

## Project contents

### Project structure

<details><summary>Collapse / Expand</summary> 

This project contains all the logic required to keep the distributed ledger running.  
- DistributedLedgerApplication, starting point of the Spring system (mostly empty)
- ErrorHandling, used to have a hook point to log the exceptions while keeping the system running

#### controller

- WebServiceEndpoints, used to expose the Endpoints, called by WebServiceCaller and processed in WebServiceEngine
- ClientEndpoints, used to expose the endpoints used by the ClientEngine
- EncryptionEndpointController, used to mock up the endpoints of an external encryption entity server

#### webService

- WebServiceEngine, contains the core logic of the system
- WebServiceInitializer, used only at startup to configure the WebServiceEngine
- WebServiceCaller, helper class of static methods used to call a WebService endpoint

#### EncryptionInterface

- Encryption, helper class of static methods used to perform encryption operations
- EncryptionEntity_BaseInterface, the interface to implement if you want to create a new encryption device
  (like a smart card reader or to connect to a signature server)
- EncryptionEntity_Integrated, a base implementation of a local system that can sign a message.
  Would recommend to use a different system if your network starts to have significant value,
  as it is not best practice to store your private key in the JVM memory.
- EncryptionEntity_AuthenticationServer, a bridge class that allows you to store your keys in a different server,
  significantly improving security.

#### Client

- ClientEngine, used to showcase how a client is supposed to connect and interact with the network
- ExternalPayment, used to allow the client to schedule a payment and spend his money

#### Contracts

- AccountAttributesUpdate
- BlockRevisionResult
- DistributedLedgerAccount
- LedgerHostory
- LedgerParameters
- Payment
- Signature
- SignedvalidatorMessage
- StatusDataContract
- TransportMessageDataContract
- ValidatorMessageDataContract
- ValidatorNode

</details>

### Program lifecycle:

<details><summary>Collapse / Expand</summary> 

#### Pre-lauch setup

You will need the Java 24 virtual machine on your system.  
To launch the problem you can run this command on the command line  
`java -jar /path/to/file/ledger.jar`  
or if you want to specify a port that is not 8080 you can use  
`java -jar /path/to/file/ledger.jar --server.port=8081`  
or if you want to use quantum safe encryption you can run it with  
`java -jar /path/to/file/ledger.jar --server.port=8081 --encryption.keyAlgo=ML-DSA-87 --encryption.signAlgo=ML-DSA-87`  

It is important to use nginx to only forward specific endpoints and keep the management of the app in local.  
The endpoints to forward are contained in the /hdls/ subfolder (Hypernode Distributed Ledger System)
/hdls/requestAuthenticationStringToSign  
/hdls/authenticateServer  
/hdls/initialize  
/hdls/AccountTotals  
/hdls/getAmount  
/hdls/getStatus  
/hdls/spend  
/hdls/updateAccountAttributes  
/hdls/getBlockId  
/hdls/getLedgerHistory  
/hdls/getCurrentlyTransmittedTransportMessage
/hdls/getAccountInfo
/hdls/getIPAddress
/hdls/receivePreviousMessageSignature

#### Initializing the server

You can use this program to connect to an existing distributed ledger or to create your own new ledger.  
First of all assign an EncryptionEntity  
(endpoint WebServiceEndpoints.setEncryptionEntityIntegrated(), webpage /hdls-server-admin/setEncryptionEntityIntegrated)
you can either generate a key pair from Terminal with ssh-keygen or call /hdls-client/newKeyPair.  

##### Connect to an existing ledger:

Call the endpoint InitAndConnectToExistingLedger  
(endpoint WebServiceEndpoints.initAndConnectToExistingLedger(), webpage /hdls-server-admin/createNewLedger)  
to connect to the specified target node.  
A template is included in the webpage, you will need to specify your address and your target server's address.
Please use the IP address instead of domain names (i.e. http://1.2.3.4/ instead of http://mysite.net)

##### Create a new ledger:  

Call the endpoint InitCreateNewLedger  
(endpoint WebServiceEndpoints.initCreateNewLedger(), webpage /hdls-server-admin/joinExistingLedger),  
specifying the parameters in the initial StatusDataContract.  
You will then be able to connect to this server from another instance of this program as described in "Connect to an existing ledger".  
A template is included in the default webpage, you will need to replace the address placeholders with the actual addresses.  
Alternatively you could import a previous StatusDataContract and create a fork of someone else's ledger  

#### Client Operation

Once the network is initialized it is possible to register transactions

##### Managing private keys

Every account holder can access the system through a client connected to one of the nodes of the ledger.  
This client needs to utilize an EncryptionInterface, like a chip and pin, a dedicated signable server
or the integrated app option.  
This EncryptionInterface needs to sign messages and show its public key,
to verify that it is really the user doing the authorization.  
You can generate a private and public key by calling the endpoint /hdls-client/newKeyPair, and the public key is effectively
the account name and the privateKey is the equivalent of the password.  
If your currency starts to gain value I'd recommend to switch to chip&pin systems to avoid keeping the key
in the JVM memory, but this is not really a concern when starting out a low value ledger.  
You can find a code sample to instantiate an EncryptionInterface in the Test area of the project

##### Submit a payment

The object ExternalPayment represents a payment as it needs to be compiled by the client.  
It does specify the sender and the recipient key (in base64) or his friendly name, the comment, which block is going to be processed in,
the amount to be paid and the signature of the message itself.  
The resulting payments can be grouped into a list and communicated to the validatornode by calling the endpoint /hdls/spend.  

##### Update Account Attributes

If your current validatorNode is not representing you correctly you can choose to change it.  
Contact any other validatorNode at the /hdls/votedelegation endpoint, and sign your desire to change your delegation.  
You could even point the vote to yourself, and if you have enough votes log in as a validatornode as specified on point 1a.  
The same process can be used to change your public friendly name, if the name you have chosen is not already taken.  

##### Retrieve account information

The endpoint /hdls/getAccountInfo will provide you with all the informations associated with a specific name or public key,
if you only want to find out the IP address for a specific public key (or for a specific name) you can call /hdls/getIPAddress

#### Server operation

##### WebService State

The WebService state is determined by:  
StatusDataContract, which contains the results after the last agreed upon block  
TransportMessageDataContract, which is the block currently being processed  
PendingNextMessage, which stores the transactions that will be discussed in the next block  
List of ValidatorNodes, which contains the references to the other peers in the network  

##### Processing loop

- Setting up the data
  Once one block is verified, the quest to establish the next block begins.  
  First of all the peers take the transactions stored in their PendingNextMessage,
  they move it into the TransportMessageDataContract, which will then be copied into
  currentlyTransmittedTransportMessage and exposed for every peer to see.
- Exposing the data
  currentlyTransmittedTransportMessage is then made available by calling a GET endpoint /hdls/getCurrentlyTransmittedTransportMessage.  
  This has the advantage that it is way more resilient to DDOS as the HTTP request can be distributed
  by a CDN, using a load balancer to forward the original HTTP message to a pool of servers.  
  It can also be used to detach the machines sharing the data from the machine that's actually doing the processing.  
  Once enough time (as specified in the LedgerParameters) has passed every node will show both his own messages
  and the messages they gathered by the peers, behaving like a p2p network and signing other's messages.  
  This is important because it is by others relaying the message of other peers that we are able to validate
  that one specific node has been acknowledged by the whole network.  
- Propagating messages and read receipts
  At regular intervals (specified in the LedgerParameter) the message that is exposed in the /hdls/getCurrentlyTransmittedTransportMessage
  is updated,and a new read is performed on this node's peers, as illustrated in the Explaining the Hypernode section.  
  We do add one extra turn of message exchange, so that if one of your contacts is offline you would still receive
  the messages he's supposed to carry.  
  Once we have accumulated every message and every read receipt we can start the validation process.
- Validation and block processing
  Every validator node is weighted by the amount delegated toward them.  
  At the end of the message exchange it is time to sum the votes (accounting for delegation).  
  To do so you verify which elements you possess with their message, and which receipt signatures they signed.  
  You then establish which elements will need to be banned (due to invalid or doubled messages) and once removed
  you can compute the hash of the map<publickey,messages>. This is done in BlockRevisionResult.  
  If least 50% + 1 agree on the same signature then it becomes the new founding block, the hash is written
  and is used to validate the new frame, linking it back to it to make it more difficoult to pre compute hashes.  
  That hash will also be used by the validatornode when paying, so that every payment works like a cheque
  and the validator will act like the bank teller, signing it on behalf of the bank. This makes it possible to
  make payments in advance, and maintaining it securely because only the delegated validatornode can authorize the spend.  
  If your node disagrees it will stay in that frame, allowing you to decide of you want to reattach to the main network
  or if you want to create a fork with the other dissidents.  
- Voted parameter changes
  It is possible for the validator nodes to change the network LedgerParameters values as well as to reassign the amount in an account,
  if more than 50% of the voting delegation agrees to it.  
  This is how you can "print money" and finance your community endeavors.  
  Call the endpoint /hdls/changeVotedParameters to pass the desired parameters, and keep an eye on the transaction history
  (as you need to coordinate, because only when you get over 50% of the votes the system changes setup)
- Getting unstuck
  Since only the people currently in the validatornodes are communicating it is possible that due to disconnections
  there might not be enough voting weights in the current pool of connected validatornodes to approve a new block.
  In that case if you have voting weight but are not connected to the network you can call the endpoint InitAndConnectToExistingLedger  
  (endpoint WebServiceEndpoints.generatePreviousMessageSignature(), webpage /hdls-server-admin/receivePreviousMessageSignature)  
  to connect to the specified target node and cosign the valid elements of that node.
  Your signature will be computed with the other ones for that specific frame, adding the required missing weights
  and allowing the system to get out of that deadlock scenario

#### Tips for a better experience

This system uses the delegated amounts to express votes. This means that the richer you are the more influence you will have
in the voting parts of the program. This is balanced by the fact that it is not the only "voting" system, as this system
is very easy to fork. If the community believes you are not operating in the interest of the whole community
they can create a fork and, for all intents and purposes, operating on the forked system is a vote to kick you out of the community.  
To enhance the robustness of the network do not assign more than 3% of the total votes to the same validator node.  
Use a CDN/ load balancer to expose the /hdls/getCurrentlyTransmittedTransportMessage, so the computer doing the calculations is not directly exposed to the web.  
Use some sort of VPN or similar to limit the amount of people who could DDoS your server, maybe even write an interface
so that those gathering data can be blocked by a login page that is disconnected from the core system.  
Ideally the private key and public key should be implemented in a chip and pin card or equivalent, making key extraction
something not really possible. If you cannot do that it is wise to use multiple private keys, so that if you are unlucky
enough to get your key guessed or your computer hacked you will still have the rest of your money.  
If you want to run multiple instances (maybe to help different currencies) you can use nginx to route different subdomains
(i.e. currency1.wallethost.net, currency2.wallethost.net, currency3.wallethost.net) to different instances of the java applet
</details>

## Managing a community

### Basic concepts of central banking

<details> <summary>Collapse / Expand</summary> 

This program aims to provide small communities with the ability to manage their own currency.  
If you have read this far in the file you have the knowledge of the technical instrument,
but you also need to understand the basics of Central Banks and monetary policy
in order to give value to your community's currency

#### What is a currency?

Currencies are a tool created to facilitate the exchange of goods and services.  
One of the first instances of currency were clay tablets near Babylon, which were claims on next yearâ€™s grain harvest
Historically precious metals have been used, but the discovery of gold and silver mines could crash your economy at any time
and you are wasting manpower to extract something which is only used symbolically.  
Recently cryptocurrencies have offered an alternative to those systems, by maintaining a decentralized ledger of performed payments.  
Ultimately the field of economics has observed that being able to emit money in a way that maintains a small price growth (between 2% and 4% /year)
improves economic activity, minimises unemployment and creates confidence in the economy and its managerial class

#### What gives value to a currency?

There are various interpretations of this question.  
Some believe that the currency has value because it buys things, which is true but partial.  
The reality is that a currency is a way for a community to maintain a ledger
and it is the access to the participation of that community that makes the currency valuable.  
Try bringing Canadian Dollars into Europe: there won't be much interest in them because
the Canadian community is not easily accessible, while it is considerably easier to
pay someone in Canada with USD, as the Canadian community is closer and has regular ties with the US

#### How can you create a currency out of thin air?

First of all you need a community of some sort.  
You can then mint â€œmoneyâ€ to reward members of the community who perform services for the community itself.  
For example lets say that your community has 10.000 people and needs to pay one doctor, one judge and the central banker / mayor  
Those three people are effectively community employees, so your community can decide to â€œprintâ€ their salary, which they will accept
as long as they know that they will be able to trade their salary with services and necessities within the economy of that community.  
That money will be more or less valuable depending on what the free citizens in the private sector can provide in exchange.  
The act of balancing the amount of public expenses with how much money to print and how much tax revenue to demand
is the skill required to be the economics minister, as there is a trilemma to be managed:  
- too much taxation means reducing productivity and it does incentivise the black market, and will encourage the most honest productive people to leave
- too much money printing creates inflation, which cannot always be mitigated via taxation (going that route creates imbalance between public and private sector)
- too few public expenses create discontent in the population, which might decide to leave your community and join a different community
In conclusion, currency is created when the private sector decides to live in a community where the correct amount of public services are provided
balancing low taxes, high services and a stable currency

#### How do you start the circulation of your currency?

If you want to make a big purchase, or get store credit to make a series of small purchases, you will need to transfer money to a recipient.  
It is the prerogative of every community to decide how to initiate the process, but here are some possible suggestion:  
1) pay the people doing community work with this currency with the promise of accepting it back with your business
2) those who have money in this system can extend some credit to potential new members, by granting a personal loan or monetary gift
3) create or buy something for the community using your own money or effort, and make it available for those who use that currency
4) establish a small community of 5/10 people who use this currency to settle your exchanges, and allow people you like to join your community
5) as a group of people build something together, and then reward each other with a new print of currency that keeps track of the contributions of each
</details>

### Some considerations about government

<details><summary>Collapse / Expand</summary>

To implement your own currency means implementing a system for your community,
which inevitably will evolve into a government as you will need to resolve disputes to maintain unity within that community.  
Failure to do so will disintegrate the community, and the currency that represented it will soon follow the same fate.  
Those below are all suggestions, you do not have to follow them to use this program,
but they could be a good starting point and a way to maintain good relations with your neighbours.  

####  Internal policy

A tax revenue of 26% of GDP has historically been the most effective in developed economies (and could be a sensible starting point)
because it encourages and rewards private enterprise while there are still enough money to build critical infrastructure.  
This means that people are more willing to hire a professional than doing something by themselves in an inefficient way.  
(i.e. If the restaurant owner can make cheaper prices with a 26% taxation and I can eat out 5 times a week
then the tax revenue is higher compared to a situation where the tax rate is at 50% and I go there once a week.  
This also saves time, which I can use to do something at which I'm more effective, increasing the GDP and therefore tax revenue.  
This principle combines the concept of "velocity of money" and "specialization", which sparked the industrial revolution).  
This level of taxation also has the benefit of limiting the size of government,
because after a certain size every system loses efficiency (see Group intercommunication formula: n(n - 1)/2).  
If you set up a fair market system those who are closer to their own problems and have more interest to fix it will often do it by themselves.  
VAT is the best way to tax, as it can be automated and can be applied to everything, is only paid once you receive money,
does not artificially stop people from working (unlike income tax thresholds)
allows old people to keep living in their own homes until they die (unlike property taxes)
and while it can be postponed it cannot be avoided.  
Every law has a cost, so try to write them only when they are absolutely necessary. Try to prune your laws every 10/20 years.  
Balancing taxation, money printing, laws and ethics is not easy, but if your community is united
it is going to be easier to find a common agreement on what to do.  
The best moral guide to unite a community is to try to create a good place for your children to grow
so that they can start their own families, become good men and women and be ready to defend their own children.  
This forces your community to think for the future and not waste your energy on temporary good times
and it makes painfully obvious who's going to pay the price for dumb choices.  

####  Foreign policy

It is easy to be tolerant with the things you agree, less easy to tolerate things that disgust you.  
Some of those disagreement can be solved if you admit that you have differences and you can agree to leave each other alone.  
Being honest with this and adopting a federal (or special district) system can save a lot of police costs,
but you have to keep the federal entities in check and give them only the minimum level of authority.  
It should always be possible to leave the federation if things are going badly.  
The federal system can also turn a potential enemy into a potential ally, as long as both respect treaties, show integrity and
establish proper channels to maintain good relationships and maintain the option to stop contact if things start to go badly.  
When inquiring if something is immoral and requires intervention ask yourself if all the people involved
are consenting to what is happening, and if they really have the ability to say stop.  
The real end of slavery happens when people can stand on their own legs. It is the only way that you can express true consent.  
Unless people make a conscious effort they will perpetrate what they perceive is being done to them,
and a lot of our human history has been about subjugation.  
To help others gain autonomy is therefore a gift that will keep on giving.  
Do not trust those who look like they are helping if they are creating a situation where you become dependent on them.  
If you want to help other communities to stand on their own legs you can
contribute to Free Software, organize courses in your spare time, read books to be more competent,
be an active part of your community, be available if someone is trying to grow
and, most importantly, make a point to refuse to engage with those who do not respect consent.  
Let's help each other become free people.  
</details>

# Credits

<details> <summary>Collapse / Expand</summary> 

The dev team, for the reasons illustrated in this readme, has decided that the best way to release this program
is to license it under a Free Software license, and to remain anonymous.  
This dev team does not seek profit nor fame, but in case further communications were needed
consider adding those public keys into your ledger, so that by virtue of spending a payment the dev team
will have the option to communicate with your community using the payment comments

Shinji Ikari
"MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAunH4nCceRoBkLW1Ec0j4plIxBnUVH6LcPRk2s5wGOrW8SP7gdMHH2wywh7fVTH8GWo0VLr/9Wz1kyS8GWm3XOTmlX0YEyz1ogHBSy4Sp2cDls5XMX9ct4SssMoz6EU+tkHcNYHyM0KUd8hvynrT7YTbCxSWJKmz+Wez1K93wb+jV6pi4tH9x0+4fyxq38Z3z/MK7U/D00neOCMQGB7KH0qUrTG6ZEB57X4h1frOmmwvBqnQiGDDdsVf+yHJEvbPJjoCshg8l3O3TxqZutzEup7D2tPDKkm8xmRGl1FqoWaHoa86urW1+JOrIpaUhujLZWgGUuKv6yyF47MdKV4PbQMDmbHAXhLM81wbnnv7nYpNIOwTA93OXjHPfKH2EkCP2a7U/qz9H6Oe7lKyd9oxkbcdXxU2+bbtO94mgBPsIewhspBuUWyqgXeJo8ByI73j6ynow0vIc7v8EHRuLPgAZl2/zmXuwIChYf46w8auNeMV68aNll+Au1ILE5SGy3kHRAgMBAAE="

Auron FX
"MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAk5GJ451dsBR9rPqxDmWEOab/FVIYItP2O8JHjCe6Pfvf+73vJuXIRLs+yZxohAp++zqmguo1gXJHZsVn4CVvEWlmitOCm4U7P3HugOXtiQJjBpK0U9gz+4s+qMSBEE+zSTZq+1QCUXESzoQ/+bGfnYTsH7xYk2BqexCwi4TAxSLclqJrDRGkRX8AynR4A8KjnJZjlFmtYaHNalpVuLMIorRd5IEYhIlf/mgmFyJwSpcJq+PozR7yDZFrMg2388e1Z4N8DjcxlaDpEguzdRCpd6pRW9Akb1jvfOlZJ4g394+cvz3ybrRM4jv4BfCyWicrG6THPz/ACz8YOIqGqcM3M/JdtKUe7aoxYwY+CzHA+1lyaJxigjU16uesAmv4pqxtqlTspeIiYzj5WoW3QpCWD1sC+amlDo2bpz8l3N5gFLqOaX2LAl8NYAOOPOHcHkp7Lj9/BUHV9uCpbLYxO8NFD8ubTpFooaKR6g9MCdZ4zTF6SxkRuk0l5wfANSUIYoxvAgMBAAE="

</details>

# FAQs

<details> <summary>Collapse / Expand</summary> 

#### How does this system handle slashing conditions?

The penalty for double signing is to burn the whole amount associated with that key.  
This is because in order to sign a valid message the key is likely compromised.  
Downtime or censorship are punished makes the node disconnect from the network,
and given the network joining mechanism it means that they will be offline for at least two turns.

#### Why did you use Java 24 and not Java 21?

Java 24 offers support for quantum resistant cryptography.  
If you are ok with RSA you can rebuild the project in Java 21 by commenting out the references to ML-DSA-87

#### How are conflicts resolved if 50% +1 agreement isn't reached?

If a consensus cannot be established right now the system is designed to continue propagating
messages until the consensus is reached.  
This allows humans to step in, evaluate why there is a disagreement and allow the humans to take the latest
valid status, start a new network without the malicious players and effectively secede from the network.  
Of course this issue is more "political" than technical (why is your program running maliciously)
therefore the solution must be political

#### Does this system handle Sybil attacks?

Validator onboarding stake is defined in LedgerParameters (community-adjustable).  
Trade-off: High stake = more security but less decentralization.  
(e.g., minStake = totalSupply / 1000 for 0.1% stake).
Communities can vote to change this parameter at any time to tailor the system to their needs.

#### Is the Hypernode efficiency really at 3k^2 * n^(1/k)?

Yes, but with some caveats.  
At realistic scale (10.000 validator nodes) it will use an exponent of 3 or 4.
Using higher dimensions is beneficial for the number of connections,
but it will demand one extra messaging frame per dimension.  
This is why the system asks you in the LedgerParameters how many connections can you handle
in a single messaging window, and he will calculate the correct exponent for that network load

#### Can you do anonymous transactions?

While some cryptocurrencies use Zero Knowledge Proofs to maintain anonymity, this project does not.  
It is easier to just buy an acccount with some money in it, and then use that account to pay 
a different vendor to buy a different clean account, so no single entity has the full transaction chain.  
This is a low-tech solution, but it helps to keep the codebase small and auditable.

#### Can I use EncryptionEntity_Integrated in production?

You can do what you want, I am not your dad.  
However EncryptionEntity_Integrated is considered not production-safe because the private key
is stored in the same memory as the rest of the program.  
In practical terms you can use it to familiarize yourself with the program,
however if your community has more than 20.000 $ under management it might start to be risky.  
To recap:  
EncryptionEntity_Integrated (in-memory keys) is indeed risky for large networks.  
EncryptionEntity_BaseInterface is secure if implemented correctly (e.g., wrapping an HSM/chip-and-PIN).  

Here is a table comparing the options:

| Method                                | Use Case     | Security        | Example              |
|---------------------------------------|--------------|-----------------|----------------------|
| EncryptionEntity_Integrated           | Low value    | (JVM memory)    | Demo wallets         |
| EncryptionEntity_AuthenticationServer | Medium value | (Remote server) | Corporate validators |
| EncryptionEntity_BaseInterface + HSM	 | High value   | (Hardware)      | Chip-and-PIN, HSM    |

</details>

# Further links

<details> <summary>Collapse / Expand</summary> 

[Latest version of this project](https://github.com/JauntyJackalope351/hypernode-ledger) - The main public repo  
[Latest version of this readme](https://github.com/JauntyJackalope351/hypernode-ledger/blob/main/readme.md) - The last version of the project documentation.  
[Quick start guide](https://github.com/JauntyJackalope351/hypernode-ledger/blob/main/quickstart.pdf)  - Illustrated documentation on how to operate the program  
[Community bootstrap guide (AI generated)](https://github.com/JauntyJackalope351/hypernode-ledger/blob/main/community_bootstrap.md)  - Some ideas on how to start your own community  
[Java applet](https://github.com/JauntyJackalope351/hypernode-ledger/blob/main/ledger.jar) - The actual Java application - 


</details> 