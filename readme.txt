

    Hypernode distributed ledger


This program aims to create a distributed ledger with the goal of illustrating the Hypernode validation algorithm.
It is a Proof of Stake system that supports delegation, can scale well, requires little power,supports multiple signing
algorithms(tested with RSA and ML-DSA-87),can relay messages and has a built-in voting system to update its own ledger parameters.
It was written from scratch in Java using the Spring framework, as it is a very easy language to read, understand and validate.
One of the reason for the creation of this program was that the Bitcoin code was not intuitive to read or easy to deploy,
so auditability and ease of startup were important objectives of this project
Ease of use and security were considered, as you need both a signature from the user (like chip and pin)
and from your validatorNode (which acts like a sort of bank), so that incidents like MtGox cannot happen anymore.
You then get the ease of use of having normal people operate thin clients while validatornodes do the heavy lifting,
but at the same time a motivated user can run his own validatorNode.
The power and computation were also a concern, especially as that GPU power could now be allocated to AI.
Another reason I was not fully happy with Bitcoin was the lack of an internal formal voting system, and the inability to
issue new currency without recurring to an hard fork.
Since this system is based on the delegated validatornodes to manage monetary policy this is, in fact, a democratic instrument.
If you so desire you do not even need the Internet to run this kind of ledger, you could run one entirely via avian carriers,
therefore becoming an important asset for communities that can't or won't connect to the mainstream Internet.
Despite those critiques Bitcoin has undoubtedly paved the road for a lot of concepts, this is more like tying up loose ends


IMPORTANT:

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

This program is a proof of concept. It was not possible to test it properly before its release to the public.
Feel free to modify it to suit your needs, but remember to share the code using AGPL
(see https://www.gnu.org/licenses/agpl-3.0.html).
If you want to turn this program into an official system please hire someone competent to tailor the system to your needs
and validate this software by paying for a third party audit.
In the meantime feel free to start with the default code and settings, you will be able to export the ledger
and restart it onto a compatible chain at a later date if you want to.


    Explaining the Hypernode


The easiest real life example on how to visualize the Hypernode is by looking at a Rubick's Cube.
The cube is composed by 27 individual smaller cubes (one central element and those who expose the colored faces).
Place the cube on the table:
if you were to slice it like an hamburger you would see that those 27 individual cubes components
can be represented this way:

     Bottom (1)            Middle (2)              Top (3)
    1     2     3         1     2     3         1     2     3
1 1.1.1 1.1.2 1.1.3   1 2.1.1 2.1.2 2.1.3   1 3.1.1 3.1.2 3.1.3
2 1.2.1 1.2.2 1.2.3   2 2.2.1 2.2.2 2.2.3   2 3.2.1 3.2.2 3.2.3
3 1.3.1 1.3.2 1.3.3   3 2.3.1 2.3.2 2.3.3   3 3.3.1 3.3.2 3.3.3

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

      Progression of the message emanating from 1.1.1
      Bottom               Middle                Top
  0     1     1        1     2     2        1     2     2
  1     2     2        2     3     3        2     3     3
  1     2     2        2     3     3        2     3     3

Now the element 1.1.1 needs to receive the signature from the elements reached in our last iteration,
so by repeating the procedure 3 further times the original sender has propagated to everyone his message and
received the receipt signature from everyone else in the network.


    Project contents


This project contains all the logic required to keep the distributed ledger running.
- DistributedLedgerApplication, starting point of the Spring system (mostly empty)
- ErrorHandling, used to have a hook point to log the exceptions while keeping the system running
controller
- WebServiceEndpoints, used to expose the Endpoints, called by WebServiceCaller and processed in WebServiceEngine
- ClientEndpoints, used to expose the endpoints used by the ClientEngine
- EncryptionEndpointController, used to mock up the endpoints of an external encryption entity server
webService
- WebServiceEngine, contains the core of the system
- WebServiceInitializer, used only at startup to configure the WebServiceEngine
- WebServiceCaller, helper class of static methods used to call a WebService endpoint
EncryptionInterface
- Encryption, helper class of static methods used to perform encryption operations
- EncryptionEntity_BaseInterface, the interface to implement if you want to create a new encryption device
    (like a smart card reader or to connect to a signature server)
- EncryptionEntity_Integrated, a base implementation of a local system that can sign a message.
    Would recommend to use a different system if your network starts to have significant value,
    as it is not best practice to store your private key in the JVM memory.
- EncryptionEntity_AuthenticationServer, a bridge class that allows you to store your keys in a different server,
    significantly improving security.
Client
- ClientEngine, used to showcase how a client is supposed to connect and interact with the network
- ExternalPayment, used to allow the client to schedule a payment and spend his money
Contracts
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


    Program lifecycle:

0 - Pre-lauch setup

You will need the Java 24 virtual machine on your system.

To launch the problem you can run this command on the command line
java -jar /path/to/file/ledger.jar
or if you want to specify a port that is not 8080 you can use
java -jar /path/to/file/ledger.jar --server.port=8081
or if you want to use quantum safe encryption you can run it with
java -jar /path/to/file/ledger.jar --server.port=8081 --encryption.keyAlgo=ML-DSA-87 encryption.signAlgo=ML-DSA-87

It is important to use nginx to only forward specific endpoints and keep the management of the app in local.
The endpoints to forward are
/server
/newKeyPair
/requestAuthenticationStringToSign
/authenticateServer
/initialize
/AccountTotals
/getAmount
/getStatus
/spend
/updateAccountAttributes
/getBlockId
/getLedgerHistory
/setNewLedgerHistoryOrigin

1 - Initializing the server
You can use this program to connect to an existing distributed ledger or to create your own new ledger.
First of all assign an EncryptionEntity
(endpoint WebServiceEndpoints.setEncryptionEntityIntegrated(), webpage server/setEncryptionEntityIntegrated)
you can either generate a key pair from Terminal with ssh-keygen or call /newKeyPair.
    1.a - Connect to an existing ledger:
Call the endpoint InitAndConnectToExistingLedger
(endpoint WebServiceEndpoints.initAndConnectToExistingLedger(), webpage /server/createNewLedger)
to connect to the specified target node.
A template is included in the webpage, you will need to specify your http address and your target server's http address.
    1.b - Create a new ledger:
Call the endpoint InitCreateNewLedger (endpoint WebServiceEndpoints.initCreateNewLedger(), webpage /server/joinExistingLedger),
specifying the parameters in the initial StatusDataContract.
You will then be able to connect to this server from another instance of this program as described in 1.a
A template is included in the webpage, you will need to replace the address placeholders with the actual addresses.
Alternatively you could import a previous StatusDataContract and create a fork of someone else's ledger

2 - Client Operation
Once the network is initialized it is possible to register transactions
    2a - Managing private keys
Every account holder can access the system through a client connected to one of the nodes of the ledger
This client needs to utilize an EncryptionInterface, like a chip and pin, a dedicated signable server
or the integrated app option.
This EncryptionInterface needs to sign messages and show its public key,
to verify that it is really the user doing the authorization.
You can generate a private and public key by calling the endpoint /newKeyPair, and the public key is effectively
the account name and the privateKey is the equivalent of the password.
If your currency starts to gain value I'd recommend to switch to chip&pin systems to avoid keeping the key
in the JVM memory, but this is not really a concern when starting out a low value ledger.
You can find a code sample to instantiate an EncryptionInterface in the Test area of the project
    2b - Submit a payment
The object ExternalPayment represents a payment as it needs to be compiled by the client.
It does specify the sender and the recipient key (in base64) or his friendly name, the comment, which block is going to be processed in,
the amount to be paid and the signature of the message itself.
The resulting payments can be grouped into a list and communicated to the validatornode by calling the endpoint /spend.
    2c - Update Account Attributes
If your current validatorNode is not representing you correctly you can choose to change it.
Contact any other validatorNode at the /votedelegation endpoint, and sign your desire to change your delegation.
You could even point the vote to yourself, and if you have enough votes log in as a validatornode as specified on point 1a.
The same process can be used to change your public friendly name, if the name you have chosen is not already taken.

3 - Server operation
    3a - WebService State
The WebService state is determined by:
StatusDataContract, which contains the results after the last agreed upon block
TransportMessageDataContract, which is the block currently being processed
PendingNextMessage, which stores the transactions that will be discussed in the next block
List of ValidatorNodes, which contains the references to the other peers in the network
    3b - Processing loop
- Setting up the data
Once one block is verified, the quest to establish the next block begins
First of all the peers take the transactions stored in their PendingNextMessage,
they move it into the TransportMessageDataContract, which will then be copied into
currentlyTransmittedTransportMessage and exposed for every peer to see.
- Exposing the data
currentlyTransmittedTransportMessage is then made available by calling a GET endpoint /getCurrentMessage.
This has the advantage that it is way more resilient to DDOS as the HTTP request can be distributed
by a CDN, using a load balancer to forward the original HTTP message to a pool of servers.
It can also be used to detach the machines sharing the data from the machine that's actually doing the processing.
Once enough time (as specified in the LedgerParameters) has passed every node will show both his own messages
and the messages they gathered by the peers, behaving like a p2p network and signing other's messages.
This is important because it is by others relaying the message of other peers that we are able to validate
that one specific node has been acknowledged by the whole network.
- Propagating messages and read receipts
At regular intervals (specified in the LedgerParameter) the message that is exposed in the /getCurrentMessage
is updated,and a new read is performed on this node's peers, as illustrated in the Explaining the Hypernode section.
We do add one extra turn of message exchange, so that if one of your contacts is offline you would still receive
the messages he's supposed to carry.
Once we have accumulated every message and every read receipt we can start the validation process
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
It is possible for the validator nodes to change the network LedgerParameters and to reassign the amount in an account,
if more than 50% of the voting delegation agrees to it. This is how you can "print money" and finance your community endeavors.
Call the endpoint /changeVotedParameters to pass the desired parameters, and keep an eye on the transaction history
(as you need to coordinate, because only when you get over 50% of the votes the system changes setup)

4 - Tips for a better experience
This system uses the delegated amounts to express votes. This means that the richer you are the more influence you will have
in the voting parts of the program. This is balanced by the fact that it is not the only "voting" system, as this system
is very easy to fork. If the community believes you are not operating in the interest of the whole community
they can create a fork and, for all intents and purposes, operating on the forked system is a vote to kick you out of the community.
To enhance the robustness of the network do not assign more than 3% of the total votes to the same validator node.
Use a CDN/ load balancer to expose the /getCurrentMessage, so the computer doing the calculations is not directly exposed to the web.
Use some sort of VPN or similar to limit the amount of people who could DDoS your server, maybe even write an interface
so that those gathering data can be blocked by a login page that is disconnected from the core system.
Ideally the private key and public key should be implemented in a chip and pin card or equivalent, making key extraction
something not really possible. If you cannot do that it is wise to use multiple private keys, so that if you are unlucky
enough to get your key guessed or your computer hacked you will still have the rest of your money.
If you want to run multiple instances (maybe to help different currencies) you can use nginx to route different subdomains
(i.e. currency1.wallethost.net, currency2.wallethost.net, currency3.wallethost.net) to different instances of the java applet


    Basic concepts of central banking


This program aims to provide small communities with the ability to manage their own currency.
If you have read this far in the file you have the knowledge of the technical instrument,
but you also need to understand the basics of Central Banks and monetary policy
in order to give value to your community's currency
   What is a currency?
Currencies are a tool created to facilitate the exchange of goods and services
One of the first instances of currency were clay tablets near Babylon, which were claims on next year’s grain harvest
Historically precious metals have been used, but the discovery of gold and silver mines could crash your economy at any time
and you are wasting manpower to extract something which is only used symbolically
Recently cryptocurrencies have offered an alternative to those systems, by maintaining a decentralized ledger of performed payments
Ultimately the field of economics has observed that being able to emit money in a way that maintains a small price growth (between 2% and 4% /year)
improves economic activity, minimises unemployment and creates confidence in the economy and its managerial class
   What gives value to a currency?
There are various interpretations of this question.
Some believe that the currency has value because it buys things, which is true but partial
The reality is that a currency is a way for a community to maintain a ledger
and it is the access to the participation of that community that makes the currency valuable
Try bringing Canadian Dollars into Europe: there won't be much interest in them because
the Canadian community is not easily accessible, while it is considerably easier to
pay someone in Canada with USD, as the Canadian community is closer and has regular ties with the US
   How can you create a currency out of thin air?
First of all you need a community of some sort
You can then mint “money” to reward members of the community who perform services for the community itself
For example lets say that your community has 10.000 people and needs to pay one doctor, one judge and the central banker / mayor
Those three people are effectively community employees, so your community can decide to “print” their salary, which they will accept
as long as they know that they will be able to trade their salary with services and necessities within the economy of that community.
That money will be more or less valuable depending on what the free citizens in the private sector can provide in exchange.
The act of balancing the amount of public expenses with how much money to print and how much tax revenue to demand
is the skill required to be the economics minister, as there is a trilemma to be managed:
too much taxation means reducing productivity and it does incentivise the black market, and will encourage the most honest productive people to leave
too much money printing creates inflation, which cannot always be mitigated via taxation (going that route creates imbalance between public and private sector)
and too few public expenses create discontent in the population, which might decide to leave your community and join a different community
In conclusion, currency is created when the private sector decides to live in a community where the correct amount of public services are provided
balancing low taxes, high services and a stable currency
    How do you start the circulation of your currency?
If you want to make a big purchase, or get store credit to make a series of small purchases, you will need to transfer money to a recipient.
It is the prerogative of every community to decide how to initiate the process, but here are some possible suggestion:
1) pay the people doing community work with this currency with the promise of accepting it back with your business
2) those who have money in this system can extend some credit to potential new members, by granting a personal loan or monetary gift
3) create or buy something for the community using your own money or effort, and make it available for those who use that currency
4) establish a small community of 5/10 people who use this currency to settle your exchanges, and allow people you like to join your community
5) as a group of people build something together, and then reward each other with a new print of currency that keeps track of the contributions of each


    Some considerations about government


   To implement your own currency means implementing a system for your community,
which inevitably will evolve into a government as you will need to resolve disputes to maintain unity within that community.
Failure to do so will disintegrate the community, and the currency that represented it will soon follow the same fate.
Those below are all suggestions, you do not have to follow them to use this program,
but they could be a good starting point and a way to maintain good relations with your neighbours.
   A tax revenue of 26% of GDP has historically been the most effective because it encourages and rewards private enterprise
while there are still enough money to build critical infrastructure.
This means that people are more willing to hire a professional than doing something by themselves in an inefficient way.
(i.e. If the restaurant owner can make cheaper prices with a 26% taxation and I can eat out 5 times a week
then the tax revenue is higher compared to a situation where the tax rate is at 50% and I go there once a week.
This also saves time, which I can use to do something at which I'm more effective, increasing the GDP and therefore tax revenue.
This principle combines the concept of "velocity of money" and "specialization", which sparked the industrial revolution).
This level of taxation also has the benefit of limiting the size of government,
because after a certain size every system loses efficiency (see Group intercommunication formula: n(n − 1)/2).
If you set up a fair market system those who are closer to their own problems and have more interest to fix it will often do it by themselves.
VAT is the best way to tax, as it can be automated and can be applied to everything, is only paid once you receive money,
does not artificially stop people from working (unlike income tax thresholds)
allows old people to keep living in their own homes until they die (unlike property taxes)
and while it can be postponed it cannot be avoided.
Every law has a cost, so try to write them only when they are absolutely necessary. Try to prune your laws every 10 years.
   Balancing taxation, money printing, laws and ethics is not easy, but if your community is united
it is going to be easier to find a common agreement on what to do.
The best moral guide to unite a community is to try to create a good place for your children to grow
so that they can start their own families, become good men and women and be ready to defend their own children.
This forces your community to think for the future and not waste your energy on temporary good times
and it makes painfully obvious who's going to pay the price for dumb choices.
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

The dev team, for the aforementioned motives, has decided that the best way to release this program
is to license it under a Free Software license, and to remain anonymous.
This dev team does not seek profit nor fame, but in case further communications were needed
consider adding those public keys into your ledger, so that by virtue of spending a payment the dev team
will have the option to communicate with your community using the payment comments

Shinji Ikari
"MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAunH4nCceRoBkLW1Ec0j4plIxBnUVH6LcPRk2s5wGOrW8SP7gdMHH2wywh7fVTH8GWo0VLr/9Wz1kyS8GWm3XOTmlX0YEyz1ogHBSy4Sp2cDls5XMX9ct4SssMoz6EU+tkHcNYHyM0KUd8hvynrT7YTbCxSWJKmz+Wez1K93wb+jV6pi4tH9x0+4fyxq38Z3z/MK7U/D00neOCMQGB7KH0qUrTG6ZEB57X4h1frOmmwvBqnQiGDDdsVf+yHJEvbPJjoCshg8l3O3TxqZutzEup7D2tPDKkm8xmRGl1FqoWaHoa86urW1+JOrIpaUhujLZWgGUuKv6yyF47MdKV4PbQMDmbHAXhLM81wbnnv7nYpNIOwTA93OXjHPfKH2EkCP2a7U/qz9H6Oe7lKyd9oxkbcdXxU2+bbtO94mgBPsIewhspBuUWyqgXeJo8ByI73j6ynow0vIc7v8EHRuLPgAZl2/zmXuwIChYf46w8auNeMV68aNll+Au1ILE5SGy3kHRAgMBAAE="
Auron FX
"MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAk5GJ451dsBR9rPqxDmWEOab/FVIYItP2O8JHjCe6Pfvf+73vJuXIRLs+yZxohAp++zqmguo1gXJHZsVn4CVvEWlmitOCm4U7P3HugOXtiQJjBpK0U9gz+4s+qMSBEE+zSTZq+1QCUXESzoQ/+bGfnYTsH7xYk2BqexCwi4TAxSLclqJrDRGkRX8AynR4A8KjnJZjlFmtYaHNalpVuLMIorRd5IEYhIlf/mgmFyJwSpcJq+PozR7yDZFrMg2388e1Z4N8DjcxlaDpEguzdRCpd6pRW9Akb1jvfOlZJ4g394+cvz3ybrRM4jv4BfCyWicrG6THPz/ACz8YOIqGqcM3M/JdtKUe7aoxYwY+CzHA+1lyaJxigjU16uesAmv4pqxtqlTspeIiYzj5WoW3QpCWD1sC+amlDo2bpz8l3N5gFLqOaX2LAl8NYAOOPOHcHkp7Lj9/BUHV9uCpbLYxO8NFD8ubTpFooaKR6g9MCdZ4zTF6SxkRuk0l5wfANSUIYoxvAgMBAAE="
